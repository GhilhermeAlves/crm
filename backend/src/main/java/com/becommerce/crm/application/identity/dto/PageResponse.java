package com.becommerce.crm.application.identity.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int pageSize,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> of(List<T> content, int page, int pageSize, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        return new PageResponse<>(content, page, pageSize, totalElements, totalPages);
    }
}
