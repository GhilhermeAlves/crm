package com.becommerce.crm.sales.recall.application.port.in;

import com.becommerce.crm.sales.recall.application.dto.CreateRecallRequest;
import com.becommerce.crm.sales.recall.application.dto.RecallResponse;
import com.becommerce.crm.sales.recall.application.dto.RecallSuggestionResponse;
import com.becommerce.crm.sales.recall.application.dto.ScheduleRecallRequest;
import com.becommerce.crm.sales.recall.application.dto.UpdateRecallRequest;
import com.becommerce.crm.shared.application.dto.PageResponse;

import java.util.List;
import java.util.UUID;

public interface RecallUseCase {

    RecallResponse create(UUID companyId, CreateRecallRequest request, UUID createdBy);

    PageResponse<RecallResponse> list(UUID companyId, UUID hostId, String tag, UUID tagId, String q, String window,
                                      int page, int pageSize);

    RecallResponse update(UUID companyId, UUID recallId, UpdateRecallRequest request);

    RecallResponse dismiss(UUID companyId, UUID recallId);

    RecallResponse schedule(UUID companyId, UUID recallId, ScheduleRecallRequest request);

    com.becommerce.crm.shared.application.dto.PageResponse<RecallSuggestionResponse> suggestions(UUID companyId, int sinceMonths, int page, int pageSize);

    List<RecallSuggestionResponse> suggestions(UUID companyId, int sinceMonths);

    void dismissSuggestion(UUID companyId, UUID contactId, UUID dismissedBy);
}
