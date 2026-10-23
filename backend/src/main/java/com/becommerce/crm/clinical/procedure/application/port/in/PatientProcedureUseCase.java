package com.becommerce.crm.clinical.procedure.application.port.in;

import com.becommerce.crm.clinical.procedure.application.dto.CompleteProcedureRequest;
import com.becommerce.crm.clinical.procedure.application.dto.PatientProcedureRequest;
import com.becommerce.crm.clinical.procedure.application.dto.PatientProcedureResponse;
import com.becommerce.crm.clinical.procedure.domain.ProcedureState;

import java.util.List;
import java.util.UUID;

public interface PatientProcedureUseCase {

    List<PatientProcedureResponse> list(UUID companyId, UUID contactId, ProcedureState state, String category,
                                        Integer tooth);

    PatientProcedureResponse create(UUID companyId, UUID contactId, PatientProcedureRequest request, UUID actorId,
                                    boolean canCreateFinance);

    PatientProcedureResponse update(UUID companyId, UUID contactId, UUID procedureId,
                                    PatientProcedureRequest request, UUID actorId);

    PatientProcedureResponse complete(UUID companyId, UUID contactId, UUID procedureId,
                                      CompleteProcedureRequest request, UUID actorId, boolean canCreateFinance);

    void delete(UUID companyId, UUID contactId, UUID procedureId, UUID actorId);
}
