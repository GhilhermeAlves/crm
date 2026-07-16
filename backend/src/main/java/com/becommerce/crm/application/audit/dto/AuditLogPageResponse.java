package com.becommerce.crm.application.audit.dto;

import com.becommerce.crm.application.identity.dto.PageResponse;

import java.util.List;

public record AuditLogPageResponse(
    List<AuditLogResponse> content,
    int page,
    int pageSize,
    long totalElements,
    int totalPages
) {
    public static AuditLogPageResponse of(PageResponse<AuditLogResponse> pageResponse) {
        return new AuditLogPageResponse(
            pageResponse.content(),
            pageResponse.page(),
            pageResponse.pageSize(),
            pageResponse.totalElements(),
            pageResponse.totalPages()
        );
    }
}
