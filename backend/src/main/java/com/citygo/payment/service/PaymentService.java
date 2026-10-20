package com.citygo.payment.service;
import com.citygo.payment.entity.*;
import com.citygo.payment.gateway.PaymentGateway;
import com.citygo.payment.gateway.PaymentGateway.*;
import com.citygo.payment.mapper.LifecycleMapper;
import com.citygo.booking.entity.*;
import com.citygo.booking.mapper.BookingMapper;
import com.citygo.booking.service.BookingService;
import com.citygo.common.api.*;
import com.citygo.common.exception.BizException;
import com.fasterxml.jackson.databind.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;

@Service @RequiredArgsConstructor
public class PaymentService {
    private final com.citygo.fulfillment.mapper.FulfillmentMapper fulfillment;
    public LocalDateTime completionBoundary(Long id){return boundary(id,true);}
    private final com.citygo.common.redis.PublicCache publicCache;
    private final LifecycleMapper m;private final BookingMapper bookings;private final BookingService booking;
    private final PaymentGateway gateway;private final PlatformTransactionManager transactions;private final ObjectMapper json;private final Clock clock;
    public record PaymentView(List<PaymentAttempt> payments,List<RefundRecord> refunds){}
    private LocalDateTime now(){return LocalDateTime.now(clock);}
    private <T>T tx(Supplier<T> body){
        TransactionTemplate t=new TransactionTemplate(transactions);t.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        for(int attempt=0;;attempt++){try{return t.execute(s->body.get());}catch(PessimisticLockingFailureException e){if(attempt==2)throw new BizException(ErrorCode.SERVICE_UNAVAILABLE);try{Thread.sleep(20L<<attempt);}catch(InterruptedException x){Thread.currentThread().interrupt();throw new BizException(ErrorCode.SERVICE_UNAVAILABLE);}}}
    }
    private BookingOrder locked(Long id,Long uid){BookingOrder o=m.lockOrder(id);if(o==null || (uid!=null&&!uid.equals(o.getUserId())))throw new BizException(ErrorCode.NOT_FOUND);return o;}
    private void own(Long uid,Long id){BookingOrder o=m.order(id);if(o==null||!uid.equals(o.getUserId()))throw new BizException(ErrorCode.NOT_FOUND);}
    public PaymentView records(Long uid,Long id){own(uid,id);return new PaymentView(m.payments(id),m.refunds(id));}
    public PaymentView adminRecords(Long id){if(m.order(id)==null)throw new BizException(ErrorCode.NOT_FOUND);return new PaymentView(m.payments(id),m.refunds(id));}
    public BookingService.Detail adminDetail(Long id){BookingOrder o=m.order(id);if(o==null)throw new BizException(ErrorCode.NOT_FOUND);return booking.detail(o.getUserId(),id);}
    public PageResult<BookingService.Summary> adminPage(String status,String keyword,int page,int size){
        return PageResult.of(m.adminPage(status,keyword,(page-1)*size,size).stream().map(o->booking.detail(o.getUserId(),o.getId()).order()).toList(),m.adminCount(status,keyword),page,size);
    }
    public PaymentView pay(Long uid,Long id,String scenario){
        PaymentAttempt p=tx(()->{
            BookingOrder o=locked(id,uid);
            if(Set.of("PENDING_CONFIRMATION","CONFIRMED","COMPLETED").contains(o.getStatus()))return m.payments(id).stream().filter(candidate->"SUCCESS".equals(candidate.getStatus())).findFirst().orElseThrow(()->new BizException(ErrorCode.ORDER_NOT_PAYABLE));
            if(!"PENDING_PAYMENT".equals(o.getStatus()) || !now().isBefore(o.getExpireAt()) || m.closeIntent(id)!=null)throw new BizException(ErrorCode.ORDER_NOT_PAYABLE);
            for(PaymentAttempt existing:m.payments(id))if(Set.of("PENDING","UNKNOWN").contains(existing.getStatus()))return existing;
            PaymentAttempt created=new PaymentAttempt();created.setRequestNo("PAY"+UUID.randomUUID().toString().replace("-",""));created.setOrderId(id);created.setChannel("MOCK");created.setAmountCents(o.getTotalCents());created.setStatus("PENDING");created.setCreatedAt(now());created.setUpdatedAt(now());m.insertPayment(created);return created;
        });
        if("SUCCESS".equals(p.getStatus()))return records(uid,id);
        Result result;
        try{result=gateway.pay(request(p),scenario);}catch(BizException e){throw e;}catch(RuntimeException e){result=unknown(request(p));}
        Long refund=settlePayment(result);if(refund!=null)driveRefund(refund);
        return records(uid,id);
    }
    public void validatePayment(Result result){PaymentAttempt p=m.payment(result.requestNo());if(p==null)throw new BizException(ErrorCode.NOT_FOUND);validate(result,request(p));}
    public Long settlePayment(Result result){
        PaymentAttempt original=m.payment(result.requestNo());if(original==null)throw new BizException(ErrorCode.NOT_FOUND);
        return tx(()->{
            BookingOrder o=locked(original.getOrderId(),null);PaymentAttempt p=m.lockPayment(original.getId());validate(result,request(p));
            if("SUCCESS".equals(p.getStatus())){
                if("SUCCESS".equals(result.status())&&!Objects.equals(p.getExternalTradeNo(),result.externalNo()))throw new BizException(ErrorCode.CONFLICT,"支付流水不一致");
                RefundRecord existing=m.paymentRefund(p.getId());return existing!=null&&!"SUCCESS".equals(existing.getStatus())?existing.getId():null;
            }
            if(Set.of("CLOSED","FAILED").contains(p.getStatus())&&Set.of("PENDING","UNKNOWN").contains(result.status())){
                m.paymentResult(p.getId(),p.getStatus(),p.getExternalTradeNo(),p.getPaidAt(),now(),now().plusSeconds(60));return null;
            }
            m.paymentResult(p.getId(),result.status(),result.externalNo(),result.occurredAt(),now(),now().plusSeconds(60));
            if(!"SUCCESS".equals(result.status()))return null;
            boolean alreadyPaid=m.payments(o.getId()).stream().anyMatch(other->!other.getId().equals(p.getId())&&"SUCCESS".equals(other.getStatus()));
            if("PENDING_PAYMENT".equals(o.getStatus())&&!result.occurredAt().isAfter(o.getExpireAt())&&!alreadyPaid){
                transition(o,"PENDING_CONFIRMATION","SYSTEM",null,"支付成功",result.occurredAt());m.setCloseIntent(o.getId(),null,null);return null;
            }
            // Late/excess payment is a money fact, never a reason to resurrect or reallocate a booking.
            if("PENDING_PAYMENT".equals(o.getStatus())){transition(o,"CLOSED","SYSTEM",null,"过期支付补偿",null);release(o.getId());m.setCloseIntent(o.getId(),null,null);}
            m.anomaly(p.getId(),o.getId(),alreadyPaid?"重复付款补偿":"终态或过期付款补偿",now());
            return ensureRefund(o,p,true,null,"异常付款全额补偿").getId();
        });
    }
    private Request request(PaymentAttempt p){BookingOrder o=m.order(p.getOrderId());return new Request(p.getRequestNo(),o.getOrderNo(),p.getAmountCents());}
    private Request request(RefundRecord r){return new Request(r.getRefundNo(),m.order(r.getOrderId()).getOrderNo(),r.getAmountCents());}
    private Result unknown(Request r){return new Result(r.requestNo(),r.orderNo(),r.amountCents(),"UNKNOWN",null,null);}
    private void validate(Result r,Request expected){
        if(r==null||r.requestNo()==null||r.orderNo()==null||r.status()==null||r.amountCents()<0)throw new BizException(ErrorCode.BAD_REQUEST);
        if(!expected.requestNo().equals(r.requestNo())||!expected.orderNo().equals(r.orderNo())||expected.amountCents()!=r.amountCents())throw new BizException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        if(!Set.of("SUCCESS","FAILED","CLOSED","PENDING","UNKNOWN").contains(r.status()))throw new BizException(ErrorCode.BAD_REQUEST);
        if("SUCCESS".equals(r.status())&&(r.externalNo()==null||r.externalNo().isBlank()||r.occurredAt()==null||r.occurredAt().isAfter(now().plusMinutes(5))))throw new BizException(ErrorCode.BAD_REQUEST,"成功支付缺少有效流水或时间");
    }
    public BookingService.Detail cancel(Long uid,Long id){close(uid,id,"CANCELLED");return booking.detail(uid,id);}
    public void expire(Long id){close(null,id,"CLOSED");}
    private void close(Long uid,Long id,String requested){
        boolean proceed=tx(()->{
            BookingOrder o=locked(id,uid);String intent=m.closeIntent(id);
            if(!"PENDING_PAYMENT".equals(o.getStatus())){if(uid!=null&&!Set.of("CANCELLED","CLOSED").contains(o.getStatus()))throw new BizException(ErrorCode.ORDER_NOT_CANCELLABLE);return false;}
            if("CLOSED".equals(requested)&&intent==null&&now().isBefore(o.getExpireAt()))return false;
            m.setCloseIntent(id,intent==null?requested:intent,now().plusSeconds(30));return true;
        });
        if(!proceed)return;
        boolean uncertain=false;
        for(PaymentAttempt p:m.payments(id)){
            Result r;try{r=gateway.close(request(p));}catch(RuntimeException e){uncertain=true;continue;}
            Long refund=settlePayment(r);if(refund!=null)driveRefund(refund);
            if(Set.of("UNKNOWN","PENDING").contains(r.status()))uncertain=true;
        }
        if(uncertain)throw new BizException(ErrorCode.SERVICE_UNAVAILABLE,"交易关闭结果未知，请稍后查询订单");
        tx(()->{
            BookingOrder o=locked(id,uid);
            if(!"PENDING_PAYMENT".equals(o.getStatus())){if(uid!=null&&!Set.of("CANCELLED","CLOSED").contains(o.getStatus()))throw new BizException(ErrorCode.ORDER_NOT_CANCELLABLE);return null;}
            String target=m.closeIntent(id);if(target==null)return null;
            transition(o,target,uid==null?"SYSTEM":"USER",uid,"CLOSED".equals(target)?"未支付到期关闭":"本人取消待支付订单",null);
            release(id);m.setCloseIntent(id,null,null);return null;
        });
    }
    public RefundRecord refund(Long uid,Long id,String reason){Long rid=prepareRefund(uid,id,null,reason,false);driveRefund(rid);return m.refund(rid);}
    public RefundRecord reject(Long actor,Long id,String reason){Long rid=prepareRefund(null,id,actor,reason,true);driveRefund(rid);return m.refund(rid);}
    private Long prepareRefund(Long uid,Long id,Long actor,String reason,boolean reject){return tx(()->{
        BookingOrder o=locked(id,uid);
        if(Set.of("REFUNDING","REFUNDED").contains(o.getStatus()))return m.refunds(id).stream().filter(r->!Boolean.TRUE.equals(r.getCompensation())).findFirst().orElseThrow(()->new BizException(ErrorCode.CONFLICT)).getId();
        if(reject?!"PENDING_CONFIRMATION".equals(o.getStatus()):!Set.of("PENDING_CONFIRMATION","CONFIRMED").contains(o.getStatus()))throw new BizException(ErrorCode.ORDER_NOT_REFUNDABLE);
        if(!reject&&!now().isBefore(boundary(id,false)))throw new BizException(ErrorCode.ORDER_NOT_REFUNDABLE,"已到最早场次开始时间");
        PaymentAttempt paid=m.payments(id).stream().filter(p->"SUCCESS".equals(p.getStatus())).filter(p->{RefundRecord r=m.paymentRefund(p.getId());return r==null||!Boolean.TRUE.equals(r.getCompensation());}).findFirst().orElseThrow(()->new BizException(ErrorCode.ORDER_NOT_REFUNDABLE));
        transition(o,"REFUNDING",reject?"STAFF":"USER",reject?actor:uid,reason,null);
        return ensureRefund(o,paid,false,reject?actor:uid,reason).getId();
    });}
    private RefundRecord ensureRefund(BookingOrder o,PaymentAttempt p,boolean compensation,Long actor,String reason){
        RefundRecord existing=m.paymentRefund(p.getId());if(existing!=null)return existing;
        RefundRecord r=new RefundRecord();r.setRefundNo("REF"+UUID.randomUUID().toString().replace("-",""));r.setOrderId(o.getId());r.setPaymentAttemptId(p.getId());r.setAmountCents(p.getAmountCents());r.setStatus("PENDING");r.setReason(reason);r.setRequestedBy(actor);r.setCompensation(compensation);r.setCreatedAt(now());r.setUpdatedAt(now());m.insertRefund(r);return r;
    }
    public RefundRecord retryRefund(Long id){RefundRecord r=m.refund(id);if(r==null)throw new BizException(ErrorCode.NOT_FOUND);driveRefund(id);return m.refund(id);}
    public void driveRefund(Long id){
        RefundRecord r=m.refund(id);if(r==null||"SUCCESS".equals(r.getStatus()))return;
        PaymentAttempt paid=m.payments(r.getOrderId()).stream().filter(p->p.getId().equals(r.getPaymentAttemptId())).findFirst().orElseThrow();
        Result result;try{result=gateway.refund(request(r),paid.getRequestNo());}catch(RuntimeException e){
            tx(()->{locked(r.getOrderId(),null);RefundRecord current=m.lockRefund(id);if(!"SUCCESS".equals(current.getStatus()))m.refundResult(id,"FAILED","渠道退款未确认，可重试",null,now(),now().plusSeconds(60));return null;});return;
        }
        settleRefund(id,result);
    }
    public void settleRefundByNo(Result result){RefundRecord r=m.refundNo(result.requestNo());if(r==null)throw new BizException(ErrorCode.NOT_FOUND);settleRefund(r.getId(),result);}
    public void settleRefund(Long id,Result result){tx(()->{
        RefundRecord before=m.refund(id);if(before==null)throw new BizException(ErrorCode.NOT_FOUND);
        BookingOrder o=locked(before.getOrderId(),null);RefundRecord r=m.lockRefund(id);validate(result,request(r));
        if("SUCCESS".equals(r.getStatus()))return null;
        if(!Objects.equals(r.getAmountCents(),o.getTotalCents()))throw new BizException(ErrorCode.REFUND_AMOUNT_MISMATCH);
        boolean success="SUCCESS".equals(result.status());
        m.refundResult(id,success?"SUCCESS":("FAILED".equals(result.status())?"FAILED":"PROCESSING"),success?null:"渠道退款未确认，可重试",success?result.occurredAt():null,now(),now().plusSeconds(60));
        if(success){
            if(Boolean.TRUE.equals(r.getCompensation()))m.resolveAnomaly(r.getPaymentAttemptId());
            else{if(!"REFUNDING".equals(o.getStatus()))throw new BizException(ErrorCode.INVALID_STATE_TRANSITION);transition(o,"REFUNDED","SYSTEM",null,"整单全额退款成功",null);release(o.getId());}
        }return null;
    });}
    public BookingService.Detail confirm(Long actor,Long id){return staffTransition(actor,id,"CONFIRMED");}
    public BookingService.Detail complete(Long actor,Long id){return staffTransition(actor,id,"COMPLETED");}
    private BookingService.Detail staffTransition(Long actor,Long id,String to){Long uid=tx(()->{
        BookingOrder o=locked(id,null);if(to.equals(o.getStatus()))return o.getUserId();
        if("CONFIRMED".equals(to)){if(!"PENDING_CONFIRMATION".equals(o.getStatus()))throw new BizException(ErrorCode.ORDER_NOT_CONFIRMABLE);}
        else if(!"CONFIRMED".equals(o.getStatus())||now().isBefore(boundary(id,true)))throw new BizException(ErrorCode.ORDER_NOT_COMPLETABLE);
        transition(o,to,"STAFF",actor,"CONFIRMED".equals(to)?"运营确认":"整单到场核销",null);if("COMPLETED".equals(to))fulfillment.manualCheckin(id,actor,now());return o.getUserId();
    });return booking.detail(uid,id);}
    private LocalDateTime boundary(Long id,boolean latest){
        List<LocalDateTime> times=new ArrayList<>();for(OrderItem item:bookings.items(id))try{
            String current=fulfillment.snapshot(item.getId());JsonNode root=json.readTree("SESSION".equals(item.getItemType())?(current==null?item.getSessionSnapshot():current):item.getPackageSnapshot());
            if("SESSION".equals(item.getItemType()))times.add(OffsetDateTime.parse(root.get("startTime").asText()).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime());
            else for(JsonNode s:root.get("sessions"))times.add(OffsetDateTime.parse(s.get("startTime").asText()).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime());
        }catch(Exception e){throw new BizException(ErrorCode.INTERNAL_ERROR,"订单场次快照不完整");}
        return (latest?times.stream().max(Comparator.naturalOrder()):times.stream().min(Comparator.naturalOrder())).orElseThrow(()->new BizException(ErrorCode.INTERNAL_ERROR));
    }
    private void transition(BookingOrder o,String to,String actor,Long actorId,String reason,LocalDateTime occurred){
        publicCache.afterBusinessCommit();
        String from=o.getStatus();if(m.transition(o.getId(),from,to,now(),occurred)!=1)throw new BizException(ErrorCode.INVALID_STATE_TRANSITION);
        m.event(o.getId(),from,to,actor,actorId,reason==null?"":reason,now());
        try{m.outboxStatus(json.writeValueAsString(Map.of("orderId",o.getId(),"userId",o.getUserId(),"status",to)),"ORDER_STATUS:"+o.getId()+":"+to,now());}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException(e);}
        o.setStatus(to);
    }
    private void release(Long id){
        List<Map<String,Object>> allocations=m.allocations(id);
        for(var a:allocations)m.lockSession(((Number)a.get("session_id")).longValue());
        for(var a:allocations)if(m.release(((Number)a.get("session_id")).longValue(),((Number)a.get("quantity")).intValue(),now())!=1)throw new IllegalStateException("Inventory release invariant failed");
        m.released(id,now());
    }
    public void reconcilePayments(){for(PaymentAttempt p:m.duePayments(now()))try{Long refund=settlePayment(gateway.query(request(p)));if(refund!=null)driveRefund(refund);}catch(RuntimeException e){failure("payment-reconcile",p.getId(),e);}}
    public void reconcileRefunds(){for(RefundRecord r:m.dueRefunds(now()))try{Result result=gateway.queryRefund(request(r));if("SUCCESS".equals(result.status()))settleRefund(r.getId(),result);else driveRefund(r.getId());}catch(RuntimeException e){failure("refund-reconcile",r.getId(),e);}}
    public void closeExpired(){for(Long id:m.dueOrders(now()))try{expire(id);}catch(RuntimeException e){failure("expire-orders",id,e);}}
    private void failure(String job,Long id,RuntimeException e){m.jobFailure(job,id,e instanceof BizException b?"BIZ_"+b.getErrorCode().getCode():e.getClass().getSimpleName(),now());}
}
