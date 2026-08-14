package com.becommerce.crm.application.activity.port.input;

import com.becommerce.crm.application.activity.dto.ActivityResponse;
import com.becommerce.crm.application.activity.dto.CreateActivityRequest;
import com.becommerce.crm.application.activity.dto.UpdateActivityRequest;

import java.util.List;
import java.util.UUID;

public interface ActivityUseCase {

    ActivityResponse create(UUID companyId, CreateActivityRequest request, UUID createdBy);

    ActivityResponse getById(UUID companyId, UUID activityId);

    ActivityResponse update(UUID companyId, UUID activityId, UpdateActivityRequest request);

    void delete(UUID companyId, UUID activityId);

    List<ActivityResponse> listByCompany(UUID companyId);

    List<ActivityResponse> listByContact(UUID companyId, UUID contactId);

    List<ActivityResponse> listByOpportunity(UUID companyId, UUID opportunityId);

    List<ActivityResponse> recent(UUID companyId, int limit);
}