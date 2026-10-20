package com.citygo.catalog.dto;

import com.citygo.catalog.entity.Activity;

public record ActivityView(Long id, Long categoryId, String categoryName, String name,
                           String description, String address, String status) {

    public static ActivityView from(Activity a) {
        return new ActivityView(a.getId(), a.getCategoryId(), a.getCategoryName(), a.getName(),
                a.getDescription(), a.getAddress(), a.getStatus());
    }
}
