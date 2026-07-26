package com.becommerce.crm.application.dashboard.port.input;

import com.becommerce.crm.application.dashboard.dto.DashboardKpisResponse;
import com.becommerce.crm.application.dashboard.dto.RecentActivityResponse;

import java.util.List;
import java.util.UUID;

public interface DashboardUseCase {
    DashboardKpisResponse getKpis(UUID companyId);
    List<RecentActivityResponse> getRecentActivities(UUID companyId);
}
