package com.citygo.common.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一分页结构：page 从 1 开始，pageSize ≤ 100。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> items;
    private long total;
    private int page;
    private int pageSize;

    public static <T> PageResult<T> of(List<T> items, long total, int page, int pageSize) {
        return new PageResult<>(items, total, page, pageSize);
    }
}
