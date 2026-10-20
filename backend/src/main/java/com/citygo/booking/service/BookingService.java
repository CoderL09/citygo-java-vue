package com.citygo.booking.service;
import com.citygo.booking.dto.CheckoutRequest;
import com.citygo.booking.dto.CheckoutRequest.Line;
import com.citygo.booking.entity.*;
import com.citygo.booking.mapper.*;
import com.citygo.catalog.entity.*;
import com.citygo.catalog.mapper.*;
import com.citygo.catalog.service.ProductQuoteService;
import com.citygo.catalog.service.ProductQuoteService.Quote;
import com.citygo.contact.entity.Contact;
import com.citygo.contact.service.ContactService;
import com.citygo.cart.mapper.CartMapper;
import com.citygo.common.db.UserLockMapper;
import com.citygo.common.api.*;
import com.citygo.common.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class BookingService {
    private final com.citygo.common.redis.PublicCache publicCache;
    private final BookingMapper mapper;private final CheckoutLockMapper locks;private final UserLockMapper users;
    private final SessionMapper sessions;private final ActivityMapper activities;private final PackageMapper packages;
    private final ProductQuoteService quotes;private final ContactService contacts;private final CartMapper cart;
    private final ObjectMapper json;private final PlatformTransactionManager transactions;private final Clock clock;
    public record Summary(Long id,String orderNo,int totalCents,String status,String contactName,String contactPhone,
        String remark,LocalDateTime expireAt,LocalDateTime createdAt){}
    public record Detail(Summary order,List<OrderItem> items){}
    public record PreviewLine(Line request,Quote product,int lineTotalCents){}
    public record Preview(List<PreviewLine> items,int totalCents){}

    public Preview preview(Long uid,CheckoutRequest r){contacts.required(uid,r.contactId());return price(normalize(r).items());}
    public PageResult<Summary> page(Long uid,int page,int size){return PageResult.of(mapper.page(uid,(page-1)*size,size).stream().map(this::summary).toList(),mapper.count(uid),page,size);}
    public Detail detail(Long uid,Long id){BookingOrder o=mapper.own(uid,id);if(o==null)throw new BizException(ErrorCode.NOT_FOUND);return detail(o);}

    public Detail create(Long uid,String key,CheckoutRequest request){
        if(key==null || !key.matches("[A-Za-z0-9_-]{8,64}"))throw new BizException(ErrorCode.BAD_REQUEST,"Idempotency-Key 需为 8 至 64 位字母数字、下划线或短横线");
        CheckoutRequest r=normalize(request);String digest=digest(r);
        TransactionTemplate tx=new TransactionTemplate(transactions);tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        for(int attempt=0;;attempt++){
            try{return tx.execute(status->createTransaction(uid,key,digest,r));}
            catch(DuplicateKeyException e){
                // Read only after the losing transaction has rolled back all stock and writes.
                BookingOrder old=mapper.idempotent(uid,key);if(old!=null)return repeated(old,digest);throw e;
            }catch(PessimisticLockingFailureException e){
                if(attempt>=2)throw new BizException(ErrorCode.SERVICE_UNAVAILABLE,"预约冲突，请稍后重试");
                try{Thread.sleep(20L<<attempt);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new BizException(ErrorCode.SERVICE_UNAVAILABLE);}
            }
        }
    }
    private Detail createTransaction(Long uid,String key,String digest,CheckoutRequest r){
        users.lock(uid);
        BookingOrder old=mapper.idempotent(uid,key);if(old!=null)return repeated(old,digest);
        if(!"true".equals(locks.platform()))throw new BizException(ErrorCode.PLATFORM_PAUSED);
        Contact contact=contacts.required(uid,r.contactId());
        lockCatalog(r.items());
        Preview priced=price(r.items());
        TreeMap<Long,Integer> demand=new TreeMap<>();
        for(PreviewLine line:priced.items())for(var s:line.product().sessions())demand.merge(s.sessionId(),line.request().quantity(),Math::addExact);
        LocalDateTime now=LocalDateTime.now(clock);
        for(var allocation:demand.entrySet())if(locks.reserve(allocation.getKey(),allocation.getValue(),now)!=1)throw new BizException(ErrorCode.SESSION_SOLD_OUT);
        BookingOrder order=new BookingOrder();order.setOrderNo("CG"+UUID.randomUUID().toString().replace("-","").substring(0,30));
        order.setUserId(uid);order.setTotalCents(priced.totalCents());order.setStatus("PENDING_PAYMENT");order.setIdempotencyKey(key);order.setRequestDigest(digest);
        order.setContactName(contact.getName());order.setContactPhone(contact.getPhone());order.setRemark(r.remark());
        order.setExpireAt(now.plusMinutes(15));order.setCreatedAt(now);order.setUpdatedAt(now);mapper.insert(order);
        for(PreviewLine line:priced.items()){
            OrderItem item=new OrderItem();item.setOrderId(order.getId());item.setItemType(line.request().itemType());item.setItemId(line.request().itemId());
            item.setName(line.product().name());item.setQuantity(line.request().quantity());item.setUnitPriceCents(line.product().unitPriceCents());item.setLineTotalCents(line.lineTotalCents());item.setCreatedAt(now);
            if("SESSION".equals(item.getItemType())){item.setSessionId(item.getItemId());item.setSessionSnapshot(serialize(line.product().sessions().getFirst()));}
            else item.setPackageSnapshot(serialize(line.product()));
            mapper.insertItem(item);
        }
        for(var a:demand.entrySet())mapper.allocate(order.getId(),a.getKey(),a.getValue());
        mapper.event(order.getId(),uid,now);
        publicCache.afterBusinessCommit();
        mapper.outbox(serialize(Map.of("orderId",order.getId(),"userId",uid)),"ORDER_CREATED:"+order.getId(),now);
        for(Line line:r.items())cart.consume(uid,line.itemType(),line.itemId(),line.quantity());
        return detail(order);
    }
    private void lockCatalog(List<Line> lines){
        TreeSet<Long> packageIds=new TreeSet<>(),sessionIds=new TreeSet<>(),activityIds=new TreeSet<>(),categoryIds=new TreeSet<>();
        for(Line l:lines){if("PACKAGE".equals(l.itemType())){
            TravelPackage p=packages.find(l.itemId());if(p==null)throw new BizException(ErrorCode.PACKAGE_OFF_SALE);
            packageIds.add(p.getId());if(p.getCategoryId()!=null)categoryIds.add(p.getCategoryId());sessionIds.addAll(packages.sessions(p.getId()));
        }else sessionIds.add(l.itemId());}
        for(Long id:sessionIds){Session s=sessions.findById(id);if(s==null)throw new BizException(ErrorCode.SESSION_OFF_SALE);activityIds.add(s.getActivityId());}
        for(Long id:activityIds){Activity a=activities.findById(id);if(a==null)throw new BizException(ErrorCode.SESSION_OFF_SALE);categoryIds.add(a.getCategoryId());}
        // Shared category/activity/package locks allow concurrent orders, while publication changes wait.
        // Session X locks are always acquired in ascending ID order, including overlapping packages.
        for(Long id:categoryIds)if(locks.category(id)==null)throw new BizException(ErrorCode.SESSION_OFF_SALE);
        for(Long id:activityIds) {
            Activity current=locks.activity(id);
            if(current==null)throw new BizException(ErrorCode.SESSION_OFF_SALE);
            if(!categoryIds.contains(current.getCategoryId()))throw new CannotAcquireLockException("目录分类变更，重建锁集合");
        }
        for(Long id:packageIds)if(packages.share(id)==null)throw new BizException(ErrorCode.PACKAGE_OFF_SALE);
        for(Long id:sessionIds)if(locks.session(id)==null)throw new BizException(ErrorCode.SESSION_OFF_SALE);
    }
    private Preview price(List<Line> lines){
        try{List<PreviewLine> result=new ArrayList<>();int total=0;
            for(Line l:lines){Quote q=quotes.resolve(l);int cents=Math.multiplyExact(q.unitPriceCents(),l.quantity());total=Math.addExact(total,cents);result.add(new PreviewLine(l,q,cents));}
            return new Preview(result,total);
        }catch(ArithmeticException e){throw new BizException(ErrorCode.BAD_REQUEST,"订单金额超过允许范围");}
    }
    private CheckoutRequest normalize(CheckoutRequest r){
        if(r==null || r.items()==null || r.items().isEmpty() || r.items().size()>10)throw new BizException(ErrorCode.BAD_REQUEST);
        List<Line> lines=r.items().stream().sorted(Comparator.comparing(Line::itemType).thenComparing(Line::itemId)).toList();
        HashSet<String> unique=new HashSet<>();for(Line l:lines){if(!unique.add(l.itemType()+":"+l.itemId()))throw new BizException(ErrorCode.BAD_REQUEST,"同一商品不能重复占多行");}
        return new CheckoutRequest(r.contactId(),lines,r.remark()==null?"":r.remark().trim());
    }
    private String digest(CheckoutRequest r){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(serialize(r).getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private String serialize(Object o){try{return json.writeValueAsString(o);}catch(Exception e){throw new IllegalStateException("快照序列化失败",e);}}
    private Detail repeated(BookingOrder old,String digest){if(!digest.equals(old.getRequestDigest()))throw new BizException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);return detail(old);}
    private Summary summary(BookingOrder o){return new Summary(o.getId(),o.getOrderNo(),o.getTotalCents(),o.getStatus(),o.getContactName(),o.getContactPhone(),o.getRemark(),o.getExpireAt(),o.getCreatedAt());}
    private Detail detail(BookingOrder o){return new Detail(summary(o),mapper.items(o.getId()));}
}
