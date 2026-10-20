package com.citygo.catalog.dto;

import com.citygo.catalog.entity.Session;

import java.time.LocalDateTime;

public record SessionView(Long id, Long activityId, LocalDateTime startTime, LocalDateTime endTime,
                          LocalDateTime bookingDeadline, Integer priceCents, Integer capacity,
                          Integer available, String status) {

    public static SessionView from(Session s) {
        return new SessionView(s.getId(), s.getActivityId(), s.getStartTime(), s.getEndTime(),
                s.getBookingDeadline(), s.getPriceCents(), s.getCapacity(), s.getAvailable(), s.getStatus());
    }
}
