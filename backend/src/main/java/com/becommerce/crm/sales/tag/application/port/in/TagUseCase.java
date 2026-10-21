package com.becommerce.crm.sales.tag.application.port.in;

import com.becommerce.crm.sales.tag.application.dto.CreateTagRequest;
import com.becommerce.crm.sales.tag.application.dto.TagResponse;
import com.becommerce.crm.sales.tag.application.dto.UpdateTagRequest;

import java.util.List;
import java.util.UUID;

public interface TagUseCase {

    TagResponse create(UUID companyId, CreateTagRequest request);

    TagResponse getById(UUID companyId, UUID tagId);

    TagResponse update(UUID companyId, UUID tagId, UpdateTagRequest request);

    void delete(UUID companyId, UUID tagId);

    List<TagResponse> listByCompany(UUID companyId);

    boolean existsInCompany(UUID companyId, UUID tagId);
}
