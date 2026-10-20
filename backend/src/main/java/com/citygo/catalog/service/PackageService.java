package com.citygo.catalog.service;
import com.citygo.catalog.dto.*;
import com.citygo.catalog.entity.*;
import com.citygo.catalog.mapper.*;
import com.citygo.booking.mapper.CheckoutLockMapper;
import com.citygo.common.api.*;
import com.citygo.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class PackageService {
    private final PackageMapper mapper;
    private final SessionMapper sessions;
    private final ActivityMapper activities;
    private final CheckoutLockMapper locks;
    private final ProductQuoteService quotes;
    private final Clock clock;
    public record PackageDetail(TravelPackage product,List<SessionView> sessions,List<ProductQuoteService.SessionSnapshot> composition) {}
    public PageResult<TravelPackage> page(boolean publicOnly,Long categoryId,String keyword,int page,int pageSize) {
        String status=publicOnly?"ON_SALE":null;
        return PageResult.of(mapper.page(status,categoryId,keyword,(page-1)*pageSize,pageSize),mapper.count(status,categoryId,keyword),page,pageSize);
    }
    public PackageDetail detail(Long id,boolean publicOnly) {
        TravelPackage p=required(id);
        if(publicOnly && !"ON_SALE".equals(p.getStatus())) throw new BizException(ErrorCode.NOT_FOUND);
        List<Session> children=mapper.sessions(id).stream().map(sessions::findById).toList();
        List<ProductQuoteService.SessionSnapshot> composition=children.stream().map(s->{
            Activity a=activities.findById(s.getActivityId());
            return new ProductQuoteService.SessionSnapshot(s.getId(),a.getId(),a.getName(),a.getAddress(),s.getStartTime(),s.getEndTime(),s.getBookingDeadline(),s.getPriceCents());
        }).toList();
        return new PackageDetail(p,children.stream().map(SessionView::from).toList(),composition);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public PackageDetail create(Long actor,PackageRequest r,Long sourceId) {
        TreeSet<Long> ids=new TreeSet<>(r.sessionIds());
        if(ids.size()!=r.sessionIds().size()) throw new BizException(ErrorCode.BAD_REQUEST,"套餐内场次不能重复");
        TreeSet<Long> activityIds=new TreeSet<>(),categoryIds=new TreeSet<>(); categoryIds.add(r.categoryId());
        for(Long id:ids) {
            Session s=sessions.findById(id);if(s==null) throw new BizException(ErrorCode.NOT_FOUND,"场次不存在");
            activityIds.add(s.getActivityId());
        }
        for(Long id:activityIds) { Activity a=activities.findById(id);if(a==null)throw new BizException(ErrorCode.NOT_FOUND);categoryIds.add(a.getCategoryId()); }
        // Same order as checkout: categories -> activities -> package family -> sessions.
        categoryIds.forEach(locks::category); activityIds.forEach(locks::activity);
        Long family=null;int version=1;
        if(sourceId!=null) {
            TravelPackage source=required(sourceId);family=source.getFamilyId()==null?source.getId():source.getFamilyId();
            if(mapper.lock(family)==null)throw new BizException(ErrorCode.NOT_FOUND);
            version=Math.addExact(mapper.maxVersion(family),1);
        }
        ids.forEach(locks::session);
        quotes.requireCategory(r.categoryId(),"PACKAGE");
        for(Long id:ids)quotes.resolve(new com.citygo.booking.dto.CheckoutRequest.Line("SESSION",id,1));
        LocalDateTime now=LocalDateTime.now(clock);
        TravelPackage p=new TravelPackage();p.setCategoryId(r.categoryId());p.setName(r.name());p.setDescription(r.description());
        p.setPackagePriceCents(r.packagePriceCents());p.setVersion(version);p.setFamilyId(family);p.setStatus("OFF_SALE");
        p.setCreatedBy(actor);p.setCreatedAt(now);p.setUpdatedAt(now);mapper.insert(p);
        if(family==null)mapper.initFamily(p.getId());
        int sort=0;for(Long id:r.sessionIds())mapper.addSession(p.getId(),id,sort++,now);
        return detail(p.getId(),false);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public PackageDetail status(Long id,String status) {
        if(mapper.lock(id)==null)throw new BizException(ErrorCode.NOT_FOUND);
        // Composition/price never change in place. Publishing still rechecks at checkout.
        mapper.status(id,status,LocalDateTime.now(clock));return detail(id,false);
    }
    private TravelPackage required(Long id){TravelPackage p=mapper.find(id);if(p==null)throw new BizException(ErrorCode.NOT_FOUND);return p;}
}
