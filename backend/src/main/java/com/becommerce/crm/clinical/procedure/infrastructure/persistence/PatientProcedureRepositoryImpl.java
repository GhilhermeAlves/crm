package com.becommerce.crm.clinical.procedure.infrastructure.persistence;

import com.becommerce.crm.clinical.procedure.application.port.out.PatientProcedureRepository;
import com.becommerce.crm.clinical.procedure.domain.Dentition;
import com.becommerce.crm.clinical.procedure.domain.PatientProcedure;
import com.becommerce.crm.clinical.procedure.domain.ProcedureState;
import com.becommerce.crm.clinical.procedure.domain.ProcedureTooth;
import com.becommerce.crm.clinical.procedure.domain.ToothFace;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class PatientProcedureRepositoryImpl implements PatientProcedureRepository {

    private final PatientProcedureJpaRepository jpaRepository;

    public PatientProcedureRepositoryImpl(PatientProcedureJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PatientProcedure save(PatientProcedure procedure) {
        return toDomain(jpaRepository.save(toEntity(procedure)));
    }

    @Override
    public Optional<PatientProcedure> findById(UUID id) {
        return jpaRepository.findById(id).map(PatientProcedureRepositoryImpl::toDomain);
    }

    @Override
    public List<PatientProcedure> findByCompanyIdAndContactIdOrderByCreatedAtDesc(UUID companyId,
                                                                                  UUID contactId) {
        return jpaRepository.findByCompanyIdAndContactIdOrderByCreatedAtDesc(companyId, contactId).stream()
                .map(PatientProcedureRepositoryImpl::toDomain).toList();
    }

    @Override
    public void delete(PatientProcedure procedure) {
        jpaRepository.deleteById(procedure.getId());
    }

    private static PatientProcedureJpaEntity toEntity(PatientProcedure p) {
        PatientProcedureJpaEntity e = new PatientProcedureJpaEntity();
        e.setId(p.getId());
        e.setCompanyId(p.getCompanyId());
        e.setContactId(p.getContactId());
        e.setCatalogItemId(p.getCatalogItemId());
        e.setNameSnapshot(p.getNameSnapshot());
        e.setState(p.getState().name());
        e.setDentition(p.getDentition().name());
        e.setPriceCents(p.getPriceCents());
        e.setProfessionalId(p.getProfessionalId());
        e.setNotes(p.getNotes());
        e.setPerformedAt(p.getPerformedAt());
        e.setFinancialTransactionId(p.getFinancialTransactionId());
        e.setCreatedBy(p.getCreatedBy());
        e.setCreatedAt(p.getCreatedAt());
        e.setUpdatedAt(p.getUpdatedAt());
        e.setTeeth(p.getTeeth().stream().map(t -> {
            PatientProcedureToothJpaEntity te = new PatientProcedureToothJpaEntity();
            te.setCompanyId(p.getCompanyId());
            te.setTooth((short) t.tooth());
            te.setFaces(t.faces().stream().map(Enum::name).sorted().toArray(String[]::new));
            return te;
        }).collect(Collectors.toCollection(ArrayList::new)));
        return e;
    }

    private static PatientProcedure toDomain(PatientProcedureJpaEntity e) {
        List<ProcedureTooth> teeth = e.getTeeth().stream()
                .map(t -> new ProcedureTooth(t.getTooth(), Arrays.stream(t.getFaces())
                        .map(ToothFace::valueOf).collect(Collectors.toSet())))
                .sorted(Comparator.comparingInt(ProcedureTooth::tooth))
                .toList();
        return PatientProcedure.reconstitute(e.getId(), e.getCompanyId(), e.getContactId(), e.getCatalogItemId(),
                e.getNameSnapshot(), ProcedureState.valueOf(e.getState()), Dentition.valueOf(e.getDentition()),
                e.getPriceCents(), e.getProfessionalId(), e.getNotes(), e.getPerformedAt(),
                e.getFinancialTransactionId(), teeth, e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
