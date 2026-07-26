package com.becommerce.crm.application.dashboard.dto;

import java.util.List;

public record DashboardKpisResponse(
    long totalUsers,
    long activeUsers,
    long inactiveUsers,
    long newUsersThisMonth,
    long auditEventsThisMonth,
    long totalAuditEvents,
    List<DepartmentCount> usersByDepartment
) {
    public record DepartmentCount(String department, long count) {}
}
