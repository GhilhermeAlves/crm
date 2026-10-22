package com.becommerce.crm.sales.recall.application.port.out;

import com.becommerce.crm.sales.recall.domain.Recall;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecallRepository {

    Recall save(Recall recall);

    Optional<Recall> findById(UUID id);

    PageResult findByCompany(UUID companyId, UUID hostId, String tag, UUID tagId, String q,
                             LocalDate dueFromInclusive, LocalDate dueToExclusive,
                             int page, int pageSize);

    List<SuggestionCandidate> findSuggestionCandidates(UUID companyId, Instant cutoff, Instant now);

    record PageResult(List<Recall> content, long totalElements) {}

    record SuggestionCandidate(
            UUID contactId,
            String firstName,
            String lastName,
            String mobile,
            String phone,
            UUID hostId,
            Instant lastAppointmentAt,
            List<String> recentProcedures,
            boolean hasFutureAppointment,
            boolean hasPendingRecall,
            boolean dismissed
    ) {}
}
