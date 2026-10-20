package com.citygo.catalog.dto;

import com.citygo.catalog.entity.Category;

public record CategoryView(Long id, String categoryType, String name, Integer sortOrder, Boolean status) {

    public static CategoryView from(Category c) {
        return new CategoryView(c.getId(), c.getCategoryType(), c.getName(), c.getSortOrder(), c.getStatus());
    }
}
