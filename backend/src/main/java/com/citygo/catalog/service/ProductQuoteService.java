package com.citygo.catalog.service;
import com.citygo.booking.dto.CheckoutRequest.Line;
import com.citygo.catalog.entity.*;
import com.citygo.catalog.mapper.*;
import com.citygo.common.api.ErrorCode;
import com.citygo.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class ProductQuoteService {
    private final SessionMapper sessions;
    private final ActivityMapper activities;
    private final CategoryMapper categories;
    private final PackageMapper packages;
    private final Clock clock;
    public record SessionSnapshot(Long sessionId,Long activityId,String activityName,String address,
        LocalDateTime startTime,LocalDateTime endTime,LocalDateTime bookingDeadline,Integer priceCents) {}
    public record Quote(String itemType,Long itemId,String name,int unitPriceCents,Integer packageVersion,
        List<SessionSnapshot> sessions,int available) {}

    /** All values are reread inside the checkout locks. A cart preview never reserves stock. */
    public Quote resolve(Line line) {
        List<Long> ids;
        TravelPackage pkg=null;
        if ("PACKAGE".equals(line.itemType())) {
            pkg=packages.find(line.itemId());
            if(pkg==null || !"ON_SALE".equals(pkg.getStatus())) throw new BizException(ErrorCode.PACKAGE_OFF_SALE);
            requireCategory(pkg.getCategoryId(),"PACKAGE");
            ids=packages.sessions(pkg.getId());
            if(ids.isEmpty()) throw new BizException(ErrorCode.PACKAGE_OFF_SALE);
        } else if ("SESSION".equals(line.itemType())) ids=List.of(line.itemId());
        else throw new BizException(ErrorCode.BAD_REQUEST);
        List<SessionSnapshot> snapshots=new ArrayList<>();
        int available=Integer.MAX_VALUE;
        for(Long id:ids) {
            Session s=sessions.findById(id);
            if(s==null || !"ON_SALE".equals(s.getStatus()) || !s.getBookingDeadline().isAfter(LocalDateTime.now(clock)))
                throw new BizException(ErrorCode.SESSION_OFF_SALE);
            Activity a=activities.findById(s.getActivityId());
            if(a==null || !"ON_SALE".equals(a.getStatus())) throw new BizException(ErrorCode.SESSION_OFF_SALE);
            requireCategory(a.getCategoryId(),"ACTIVITY");
            available=Math.min(available,s.getAvailable());
            snapshots.add(new SessionSnapshot(s.getId(),a.getId(),a.getName(),a.getAddress(),s.getStartTime(),
                s.getEndTime(),s.getBookingDeadline(),s.getPriceCents()));
        }
        if(available<line.quantity()) throw new BizException(ErrorCode.SESSION_SOLD_OUT);
        return new Quote(line.itemType(),line.itemId(),pkg==null?snapshots.getFirst().activityName():pkg.getName(),
            pkg==null?snapshots.getFirst().priceCents():pkg.getPackagePriceCents(),pkg==null?null:pkg.getVersion(),snapshots,available);
    }
    public void requireCategory(Long id,String type) {
        Category c=id==null?null:categories.findById(id);
        if(c==null || !Boolean.TRUE.equals(c.getStatus()) || !type.equals(c.getCategoryType()))
            throw new BizException(ErrorCode.SESSION_OFF_SALE,"分类不存在、已停用或类型不匹配");
    }
}
