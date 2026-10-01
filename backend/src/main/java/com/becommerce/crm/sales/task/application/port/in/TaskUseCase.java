package com.becommerce.crm.sales.task.application.port.in;

import com.becommerce.crm.sales.task.application.dto.CreateTaskRequest;
import com.becommerce.crm.sales.task.application.dto.TaskResponse;
import com.becommerce.crm.sales.task.application.dto.UpdateTaskRequest;
import com.becommerce.crm.sales.task.domain.TaskStatus;

import java.util.List;
import java.util.UUID;

public interface TaskUseCase {

    TaskResponse create(UUID companyId, CreateTaskRequest request, UUID createdBy);

    TaskResponse getById(UUID companyId, UUID taskId);

    TaskResponse update(UUID companyId, UUID taskId, UpdateTaskRequest request);

    TaskResponse changeStatus(UUID companyId, UUID taskId, TaskStatus status);

    void delete(UUID companyId, UUID taskId);

    List<TaskResponse> listByCompany(UUID companyId, TaskStatus status);

    List<TaskResponse> listByOpportunity(UUID companyId, UUID opportunityId);

    List<TaskResponse> listDueToday(UUID companyId);
}