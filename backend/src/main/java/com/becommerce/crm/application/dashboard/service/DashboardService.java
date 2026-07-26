package com.becommerce.crm.application.dashboard.service;

import com.becommerce.crm.application.audit.port.output.AuditLogRepository;
import com.becommerce.crm.application.dashboard.dto.DashboardKpisResponse;
import com.becommerce.crm.application.dashboard.dto.RecentActivityResponse;
import com.becommerce.crm.application.dashboard.port.input.DashboardUseCase;
import com.becommerce.crm.application.identity.port.output.UserRepository;
import com.becommerce.crm.domain.identity.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DashboardService implements DashboardUseCase {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public DashboardService(UserRepository userRepository, AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardKpisResponse getKpis(UUID companyId) {
        LocalDateTime startOfMonth = LocalDateTime.now()
            .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        long totalUsers = userRepository.countByCompanyId(companyId);
        long activeUsers = userRepository.countByCompanyIdAndStatus(companyId, UserStatus.ACTIVE);
        long inactiveUsers = userRepository.countByCompanyIdAndStatus(companyId, UserStatus.INACTIVE);
        long newUsersThisMonth = userRepository.countByCompanyIdAndCreatedAtAfter(companyId, startOfMonth);

        long totalAuditEvents = auditLogRepository.countByCompanyId(companyId);
        long auditEventsThisMonth = auditLogRepository.countByCompanyIdAndCreatedAtAfter(companyId, startOfMonth);

        Map<String, Long> deptMap = userRepository.countByCompanyIdGroupByDepartment(companyId);
        List<DashboardKpisResponse.DepartmentCount> usersByDepartment = deptMap.entrySet().stream()
            .map(e -> new DashboardKpisResponse.DepartmentCount(e.getKey(), e.getValue()))
            .toList();

        return new DashboardKpisResponse(
            totalUsers, activeUsers, inactiveUsers, newUsersThisMonth,
            auditEventsThisMonth, totalAuditEvents, usersByDepartment
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentActivityResponse> getRecentActivities(UUID companyId) {
        return auditLogRepository.findRecentByCompanyId(companyId, 10).stream()
            .map(log -> new RecentActivityResponse(
                log.getId().toString(),
                log.getUserName(),
                log.getAction().name(),
                log.getModule().name(),
                log.getDescription(),
                log.getCreatedAt()
            ))
            .toList();
    }
}
