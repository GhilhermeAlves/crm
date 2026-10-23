package com.becommerce.crm.clinical.procedure.domain;

import com.becommerce.crm.masterdata.contact.domain.exception.ContactConflictException;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Procedimento odontológico do paciente (ficha clínica, Fase 3a). {@code PLANNED}
 * e {@code PRE_EXISTING} são livremente editáveis; {@code DONE} só nasce com data
 * de realização (criação direta ou {@link #complete}) e depois só aceita notas.
 * Sem dentes = procedimento da boca toda.
 */
public class PatientProcedure {

    static final int MAX_NOTES_LENGTH = 5000;

    private final UUID id;
    private final UUID companyId;
    private final UUID contactId;
    private final UUID catalogItemId;
    private final String nameSnapshot;
    private ProcedureState state;
    private Dentition dentition;
    private long priceCents;
    private UUID professionalId;
    private String notes;
    private LocalDate performedAt;
    private UUID financialTransactionId;
    private boolean generateTransactionOnDone;
    private List<ProcedureTooth> teeth;
    private final UUID createdBy;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private PatientProcedure(UUID id, UUID companyId, UUID contactId, UUID catalogItemId, String nameSnapshot,
                             ProcedureState state, Dentition dentition, long priceCents, UUID professionalId,
                             String notes, LocalDate performedAt, UUID financialTransactionId,
                             List<ProcedureTooth> teeth, UUID createdBy, LocalDateTime createdAt,
                             LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.contactId = contactId;
        this.catalogItemId = catalogItemId;
        this.nameSnapshot = nameSnapshot;
        this.state = state;
        this.dentition = dentition;
        this.priceCents = priceCents;
        this.professionalId = professionalId;
        this.notes = notes;
        this.performedAt = performedAt;
        this.financialTransactionId = financialTransactionId;
        this.teeth = teeth;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PatientProcedure create(UUID companyId, UUID contactId, UUID catalogItemId, String name,
                                          ProcedureState state, Dentition dentition, long priceCents,
                                          UUID professionalId, String notes, LocalDate performedAt,
                                          List<ProcedureTooth> teeth, UUID createdBy) {
        if (state == null || dentition == null) {
            throw new ContactValidationException("Estado e dentição são obrigatórios.");
        }
        if (state == ProcedureState.DONE && performedAt == null) {
            throw new ContactValidationException("Informe a data de realização do procedimento.");
        }
        LocalDateTime now = LocalDateTime.now();
        return new PatientProcedure(UUID.randomUUID(), companyId, contactId, catalogItemId, name, state,
                dentition, validPrice(priceCents), professionalId, normalizeNotes(notes),
                state == ProcedureState.DONE ? performedAt : null, null, validTeeth(dentition, teeth),
                createdBy, now, now);
    }

    public static PatientProcedure reconstitute(UUID id, UUID companyId, UUID contactId, UUID catalogItemId,
                                                String nameSnapshot, ProcedureState state, Dentition dentition,
                                                long priceCents, UUID professionalId, String notes,
                                                LocalDate performedAt, UUID financialTransactionId,
                                                List<ProcedureTooth> teeth, UUID createdBy,
                                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        return reconstitute(id, companyId, contactId, catalogItemId, nameSnapshot, state, dentition, priceCents,
                professionalId, notes, performedAt, financialTransactionId, false, teeth, createdBy, createdAt,
                updatedAt);
    }

    public static PatientProcedure reconstitute(UUID id, UUID companyId, UUID contactId, UUID catalogItemId,
                                                String nameSnapshot, ProcedureState state, Dentition dentition,
                                                long priceCents, UUID professionalId, String notes,
                                                LocalDate performedAt, UUID financialTransactionId,
                                                boolean generateTransactionOnDone, List<ProcedureTooth> teeth,
                                                UUID createdBy, LocalDateTime createdAt, LocalDateTime updatedAt) {
        PatientProcedure p = new PatientProcedure(id, companyId, contactId, catalogItemId, nameSnapshot, state,
                dentition, priceCents, professionalId, notes, performedAt, financialTransactionId,
                List.copyOf(teeth), createdBy, createdAt, updatedAt);
        p.generateTransactionOnDone = generateTransactionOnDone;
        return p;
    }

    /**
     * Guarda a intenção de gerar o lançamento financeiro quando o procedimento for
     * marcado como realizado. Só vale para {@code PLANNED}; nos demais estados fica falso.
     */
    public void setGenerateTransactionOnDone(boolean generate) {
        this.generateTransactionOnDone = generate && state == ProcedureState.PLANNED;
    }

    public void edit(ProcedureState newState, Dentition newDentition, long newPriceCents, UUID newProfessionalId,
                     String newNotes, List<ProcedureTooth> newTeeth) {
        if (isDone()) {
            throw new ContactConflictException("Procedimento realizado só permite alterar as observações.");
        }
        if (newState != ProcedureState.PLANNED && newState != ProcedureState.PRE_EXISTING) {
            throw new ContactValidationException("Use \"Marcar como realizado\" para concluir o procedimento.");
        }
        if (newDentition == null) {
            throw new ContactValidationException("Estado e dentição são obrigatórios.");
        }
        this.teeth = validTeeth(newDentition, newTeeth);
        this.state = newState;
        this.dentition = newDentition;
        this.priceCents = validPrice(newPriceCents);
        this.professionalId = newProfessionalId;
        this.notes = normalizeNotes(newNotes);
        touch();
    }

    public void editNotes(String newNotes) {
        this.notes = normalizeNotes(newNotes);
        touch();
    }

    public void complete(LocalDate date, UUID professional) {
        if (state != ProcedureState.PLANNED) {
            throw new ContactConflictException("Só procedimentos \"A realizar\" podem ser marcados como realizados.");
        }
        if (date == null) {
            throw new ContactValidationException("Informe a data de realização do procedimento.");
        }
        this.state = ProcedureState.DONE;
        this.performedAt = date;
        if (professional != null) {
            this.professionalId = professional;
        }
        touch();
    }

    public void linkTransaction(UUID transactionId) {
        this.financialTransactionId = transactionId;
        touch();
    }

    public boolean isDone() {
        return state == ProcedureState.DONE;
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    private static long validPrice(long priceCents) {
        if (priceCents < 0) {
            throw new ContactValidationException("O valor do procedimento não pode ser negativo.");
        }
        return priceCents;
    }

    private static List<ProcedureTooth> validTeeth(Dentition dentition, List<ProcedureTooth> teeth) {
        if (teeth == null) {
            return List.of();
        }
        Set<Integer> seen = new HashSet<>();
        for (ProcedureTooth tooth : teeth) {
            if (!dentition.isValidTooth(tooth.tooth())) {
                throw new ContactValidationException("Dente " + tooth.tooth() + " não existe nesta dentição.");
            }
            if (!seen.add(tooth.tooth())) {
                throw new ContactValidationException("Dente " + tooth.tooth() + " informado mais de uma vez.");
            }
        }
        return List.copyOf(teeth);
    }

    private static String normalizeNotes(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_NOTES_LENGTH) {
            throw new ContactValidationException(
                    "As observações devem ter no máximo " + MAX_NOTES_LENGTH + " caracteres.");
        }
        return trimmed;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getContactId() { return contactId; }
    public UUID getCatalogItemId() { return catalogItemId; }
    public String getNameSnapshot() { return nameSnapshot; }
    public ProcedureState getState() { return state; }
    public Dentition getDentition() { return dentition; }
    public long getPriceCents() { return priceCents; }
    public UUID getProfessionalId() { return professionalId; }
    public String getNotes() { return notes; }
    public LocalDate getPerformedAt() { return performedAt; }
    public UUID getFinancialTransactionId() { return financialTransactionId; }
    public boolean isGenerateTransactionOnDone() { return generateTransactionOnDone; }
    public List<ProcedureTooth> getTeeth() { return teeth; }
    public UUID getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
