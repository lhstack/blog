package com.blog.domain.common;

import java.util.List;

/**
 * 分页结果值对象，与具体存储实现无关。
 */
public record PageResult<T>(List<T> items, long total, int page, int size) {

    public static <T> PageResult<T> of(List<T> items, long total, int page, int size) {
        return new PageResult<>(List.copyOf(items), total, page, size);
    }

    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(List.of(), 0L, page, size);
    }

    public long totalPages() {
        return size <= 0 ? 0L : (total + size - 1) / size;
    }
}
