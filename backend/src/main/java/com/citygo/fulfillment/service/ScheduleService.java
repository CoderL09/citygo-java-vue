package com.citygo.fulfillment.service;
import com.citygo.fulfillment.mapper.FulfillmentMapper;
import com.citygo.booking.mapper.*;
import com.citygo.booking.entity.*;
import com.citygo.catalog.service.ProductQuoteService;
import com.citygo.catalog.mapper.*;
import com.citygo.catalog.entity.Session;
import com.citygo.payment.mapper.LifecycleMapper;
import com.citygo.notification.mapper.NotificationMapper;
import com.citygo.common.redis.PublicCache;
import com.citygo.common.api.*;
import com.citygo.common.exception.BizException;
import com.fasterxml.jackson.databind.*;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor public class ScheduleService {
 private final FulfillmentMapper f;private final LifecycleMapper orders;private final BookingMapper bookings;private final CheckoutLockMapper locks;private final SessionMapper sessions;private final ActivityMapper activities;private final ProductQuoteService quotes;private final NotificationMapper notices;private final PublicCache cache;private final ObjectMapper json;private final Clock clock;
 public record Request(@NotNull @Positive Long targetSessionId,@NotNull @Min(0)Integer expectedVersion){}
 public record History(Long id,Long fromSessionId,Long toSessionId,int quantity,JsonNode before,JsonNode after,LocalDateTime createdAt){}
 public record View(int version,boolean canReschedule,String reason,JsonNode current,List<Session> candidates,List<History> history,FulfillmentMapper.Checkin checkin){}
 private LocalDateTime now(){return LocalDateTime.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);}
 public BookingOrder required(Long uid,Long id,boolean lock){var o=lock?orders.lockOrder(id):orders.order(id);if(o==null||(uid!=null&&!uid.equals(o.getUserId())))throw new BizException(ErrorCode.NOT_FOUND);return o;}
 public JsonNode parse(String snapshot){try{return json.readTree(snapshot);}catch(Exception e){throw new IllegalStateException("Invalid fulfillment snapshot",e);}}
 public LocalDateTime start(JsonNode snapshot){return OffsetDateTime.parse(snapshot.get("startTime").asText()).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();}
 public JsonNode current(OrderItem item){String s=f.snapshot(item.getId());return parse(s==null?item.getSessionSnapshot():s);}
 private String reason(BookingOrder o,List<OrderItem> items){if(!"CONFIRMED".equals(o.getStatus()))return "仅已确认预约可以改期";if(items.size()!=1||!"SESSION".equals(items.getFirst().getItemType()))return "首版仅支持单场次订单整单改期，套餐与混合订单暂不支持";if(!now().isBefore(start(current(items.getFirst()))))return "已到当前场次开始时间，不能改期";return null;}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public View view(Long uid,Long id){var o=required(uid,id,false);var items=bookings.items(id);String reason=reason(o,items);JsonNode current=items.size()==1&&"SESSION".equals(items.getFirst().getItemType())?current(items.getFirst()):null;List<Session> candidates=List.of();
 if(reason==null){var item=items.getFirst();candidates=f.candidates(current.get("activityId").asLong(),current.get("sessionId").asLong(),item.getUnitPriceCents(),item.getQuantity(),now());}
 return new View(f.version(id),reason==null,reason,current,candidates,f.history(id).stream().map(h->new History(h.id(),h.fromSessionId(),h.toSessionId(),h.quantity(),parse(h.beforeSnapshot()),parse(h.afterSnapshot()),h.createdAt())).toList(),f.checkin(id));}
 @Transactional(isolation=Isolation.READ_COMMITTED) public View reschedule(Long uid,Long id,String key,Request request){
 if(key==null||!key.matches("[A-Za-z0-9_-]{8,64}"))throw new BizException(ErrorCode.BAD_REQUEST,"改期请求键需为8–64位字母数字、下划线或短横线");
 var order=required(uid,id,true);var replay=f.replay(id,key);if(replay!=null){if(!replay.toSessionId().equals(request.targetSessionId())||replay.expectedVersion()!=request.expectedVersion())throw new BizException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);return view(uid,id);}
 if(f.version(id)!=request.expectedVersion())throw new BizException(ErrorCode.CONFLICT,"预约已变更，请刷新后重新选择");var items=bookings.items(id);String reason=reason(order,items);if(reason!=null)throw new BizException(ErrorCode.CONFLICT,reason);var item=items.getFirst();var before=current(item);Long source=before.get("sessionId").asLong(),activity=before.get("activityId").asLong();
 if(source.equals(request.targetSessionId()))throw new BizException(ErrorCode.CONFLICT,"请选择其他场次");
 if(!"true".equals(locks.platform()))throw new BizException(ErrorCode.PLATFORM_PAUSED);var a=activities.findById(activity);if(a==null)throw new BizException(ErrorCode.NOT_FOUND);locks.category(a.getCategoryId());locks.activity(activity);
 // A concurrent category move can commit between the pre-read and activity lock; never validate an unlocked category.
 if(!a.getCategoryId().equals(activities.findById(activity).getCategoryId()))throw new BizException(ErrorCode.SERVICE_UNAVAILABLE,"活动分类已变化，请刷新后重试");
 new TreeSet<>(List.of(source,request.targetSessionId())).forEach(sid->{if(locks.session(sid)==null)throw new BizException(ErrorCode.NOT_FOUND,"场次不存在");});
 var target=sessions.findById(request.targetSessionId());if(!activity.equals(target.getActivityId()))throw new BizException(ErrorCode.CONFLICT,"只能改到同一活动的其他场次");var quote=quotes.resolve(new com.citygo.booking.dto.CheckoutRequest.Line("SESSION",target.getId(),item.getQuantity()));if(quote.unitPriceCents()!=item.getUnitPriceCents())throw new BizException(ErrorCode.CONFLICT,"首版仅支持与原订单相同单价的场次");
 if(!now().isBefore(start(before)))throw new BizException(ErrorCode.CONFLICT,"已到当前场次开始时间");
 if(locks.reserve(target.getId(),item.getQuantity(),now())!=1)throw new BizException(ErrorCode.SESSION_SOLD_OUT);if(orders.release(source,item.getQuantity(),now())!=1)throw new IllegalStateException("Original allocation release failed");if(f.move(id,source,target.getId(),item.getQuantity())!=1)throw new IllegalStateException("Allocation move failed");
 String after;try{after=json.writeValueAsString(quote.sessions().getFirst());}catch(Exception e){throw new IllegalStateException(e);}int next=request.expectedVersion()+1;
 f.saveCurrent(item.getId(),target.getId(),after,next,now());f.change(new FulfillmentMapper.Change(null,id,key,request.expectedVersion(),source,target.getId(),item.getQuantity(),before.toString(),after,uid,now()));f.advance(id,now());f.invalidate(id,now());orders.event(id,"CONFIRMED","CONFIRMED","USER",uid,"预约改期，版本 "+next,now());notices.enqueue("ORDER_RESCHEDULED","{\"orderId\":"+id+",\"status\":\"CONFIRMED\"}","ORDER_RESCHEDULED:"+id+":"+next,now());cache.afterBusinessCommit();return view(uid,id);
 }
}
