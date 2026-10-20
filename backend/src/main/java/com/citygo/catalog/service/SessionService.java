package com.citygo.catalog.service;

import com.citygo.catalog.dto.SessionCreateRequest;
import com.citygo.catalog.dto.SessionStatusRequest;
import com.citygo.catalog.dto.SessionUpdateRequest;
import com.citygo.catalog.dto.SessionView;
import com.citygo.catalog.entity.Activity;
import com.citygo.catalog.entity.Session;
import com.citygo.catalog.mapper.ActivityMapper;
import com.citygo.catalog.mapper.SessionMapper;
import com.citygo.common.api.ErrorCode;
import com.citygo.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 场次管理。服务端校验起止/截止时间与名额；
 * 已有有效订单的场次不允许修改或删除（实施书 §6），新场次应另建。
 */
@Service
@RequiredArgsConstructor
public class SessionService {

    public static final String ON_SALE = "ON_SALE";

    private final SessionMapper sessionMapper;
    private final ActivityMapper activityMapper;
    private final Clock clock;

    public List<SessionView> listByActivity(Long activityId, boolean publicOnly) {
        requireActivity(activityId);
        if (publicOnly && !ON_SALE.equals(activityMapper.findById(activityId).getStatus())) {
            throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        return sessionMapper.findByActivityId(activityId, publicOnly ? ON_SALE : null).stream()
                .map(SessionView::from)
                .toList();
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public SessionView create(Long activityId, SessionCreateRequest request) {
        requireActivity(activityId);
        validate(request.startTime(), request.endTime(), request.bookingDeadline(),
                request.priceCents(), request.capacity());
        LocalDateTime now = LocalDateTime.now(clock);
        Session session = new Session();
        session.setActivityId(activityId);
        session.setStartTime(request.startTime());
        session.setEndTime(request.endTime());
        session.setBookingDeadline(request.bookingDeadline());
        session.setPriceCents(request.priceCents());
        session.setCapacity(request.capacity());
        session.setAvailable(request.capacity()); // 新建时剩余=总容量
        session.setStatus(ON_SALE);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        sessionMapper.insert(session);
        return SessionView.from(session);
    }

    /**
     * 有效订单引用时禁止任何修改（时间/容量/价格均不动），避免与快照/名额不一致；
     * 下架另行接口（下架不取消已有订单）。
     */
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public SessionView update(Long id, SessionUpdateRequest request) {
        Session session = requireSession(id);
        requireNoValidOrders(id);
        validate(request.startTime(), request.endTime(), request.bookingDeadline(),
                request.priceCents(), request.capacity());
        sessionMapper.update(id, request.startTime(), request.endTime(), request.bookingDeadline(),
                request.priceCents(), request.capacity(), request.capacity(),
                LocalDateTime.now(clock));
        return SessionView.from(sessionMapper.findById(id));
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public SessionView setStatus(Long id, SessionStatusRequest request) {
        requireSession(id);
        sessionMapper.updateStatus(id, request.status(), LocalDateTime.now(clock));
        return SessionView.from(sessionMapper.findById(id));
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void delete(Long id) {
        requireSession(id);
        requireNoValidOrders(id);
        if(sessionMapper.countAnyRefs(id)>0)throw new BizException(ErrorCode.CONFLICT,"场次被套餐或历史订单引用，请下架代替删除");
        sessionMapper.deleteById(id);
    }

    private void validate(LocalDateTime start, LocalDateTime end, LocalDateTime deadline,
                          Integer priceCents, Integer capacity) {
        if (!start.isBefore(end)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "开始时间必须早于结束时间");
        }
        if (deadline.isAfter(start)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "预约截止时间不能晚于开始时间");
        }
    }

    private void requireNoValidOrders(Long sessionId) {
        if (sessionMapper.countValidOrderRefs(sessionId) > 0) {
            throw new BizException(ErrorCode.ACTIVITY_HAS_VALID_ORDERS);
        }
    }

    private Session requireSession(Long id) {
        Session before = sessionMapper.findById(id);
        if (before == null) throw new BizException(ErrorCode.NOT_FOUND);
        activityMapper.shareById(before.getActivityId());
        Session session = sessionMapper.lockById(id);
        if (session == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "场次不存在");
        }
        return session;
    }

    private void requireActivity(Long activityId) {
        Activity activity = activityMapper.shareById(activityId);
        if (activity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        }
    }
}
