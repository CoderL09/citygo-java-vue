package com.citygo.fulfillment.service;
import com.citygo.fulfillment.mapper.FulfillmentMapper;
import com.citygo.payment.service.PaymentService;
import com.citygo.booking.service.BookingService;
import com.citygo.common.api.*;
import com.citygo.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
@Service @RequiredArgsConstructor public class CheckinService {
 private final FulfillmentMapper f;private final ScheduleService schedules;private final PaymentService payments;private final Clock clock;
 private static final SecureRandom RANDOM=new SecureRandom();private LocalDateTime now(){return LocalDateTime.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);}
 public record Code(String code,LocalDateTime expiresAt,int version,int ttlSeconds){}
 public record Preview(Long orderId,String orderNo,String status,boolean canComplete,String reason,BookingService.Detail detail,int version,ScheduleService.View fulfillment){}
 private String hash(String code){if(code==null||!code.matches("citygo:checkin:[A-Za-z0-9_-]{43}"))throw new BizException(ErrorCode.BAD_REQUEST,"核销码格式无效");try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 @Transactional(isolation=Isolation.READ_COMMITTED)public Code issue(Long uid,Long id){var o=schedules.required(uid,id,true);if(!"CONFIRMED".equals(o.getStatus()))throw new BizException(ErrorCode.CONFLICT,"仅已确认预约可以生成核销码");byte[] bytes=new byte[32];RANDOM.nextBytes(bytes);String code="citygo:checkin:"+Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);int version=f.version(id);var expires=now().plusMinutes(5);f.invalidate(id,now());f.ticket(new FulfillmentMapper.Ticket(hash(code),id,version,expires,null,null,now()));return new Code(code,expires,version,300);}
 private FulfillmentMapper.Ticket required(String code){var t=f.ticketByHash(hash(code));if(t==null)throw new BizException(ErrorCode.NOT_FOUND,"核销码无效");return t;}
 private Preview locked(FulfillmentMapper.Ticket before){var o=schedules.required(null,before.orderId(),true);var t=f.lockTicket(before.tokenHash());if(t.usedAt()!=null)throw new BizException(ErrorCode.CONFLICT,"此核销码已使用，请勿重复核销");if(!now().isBefore(t.expiresAt()))throw new BizException(ErrorCode.CONFLICT,"核销码已过期，请用户刷新");if(t.orderVersion()!=f.version(o.getId()))throw new BizException(ErrorCode.CONFLICT,"预约已改期，请使用新的核销码");if(!"CONFIRMED".equals(o.getStatus()))throw new BizException(ErrorCode.CONFLICT,"订单当前状态不可核销");var detail=payments.adminDetail(o.getId());boolean ready=!now().isBefore(payments.completionBoundary(o.getId()));return new Preview(o.getId(),o.getOrderNo(),o.getStatus(),ready,ready?null:"尚未到全部场次开始时间",detail,f.version(o.getId()),schedules.view(null,o.getId()));}
 @Transactional(isolation=Isolation.READ_COMMITTED)public Preview preview(String code){return locked(required(code));}
 @Transactional(isolation=Isolation.READ_COMMITTED)public BookingService.Detail redeem(Long actor,String code){var t=required(code);var p=locked(t);if(!p.canComplete())throw new BizException(ErrorCode.ORDER_NOT_COMPLETABLE,p.reason());var result=payments.complete(actor,p.orderId());if(f.consume(t.tokenHash(),actor,now())!=1)throw new BizException(ErrorCode.CONFLICT,"核销码已使用");if(f.markQr(p.orderId(),t.tokenHash())!=1)throw new IllegalStateException("Missing checkin record");return result;}
}
