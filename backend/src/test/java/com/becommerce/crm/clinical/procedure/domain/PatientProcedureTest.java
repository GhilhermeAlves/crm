package com.becommerce.crm.clinical.procedure.domain;

import com.becommerce.crm.masterdata.contact.domain.exception.ContactConflictException;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PatientProcedureTest {

    private static final UUID COMPANY = UUID.randomUUID();
    private static final UUID CONTACT = UUID.randomUUID();
    private static final UUID ITEM = UUID.randomUUID();

    private static PatientProcedure planned(Dentition dentition, List<ProcedureTooth> teeth) {
        return PatientProcedure.create(COMPANY, CONTACT, ITEM, "Restauração", ProcedureState.PLANNED,
                dentition, 15000, null, null, null, teeth, null);
    }

    @Test
    void validatesTeethAgainstDentition() {
        assertThat(Dentition.PERMANENT.isValidTooth(11)).isTrue();
        assertThat(Dentition.PERMANENT.isValidTooth(19)).isFalse();
        assertThat(Dentition.PERMANENT.isValidTooth(51)).isFalse();
        assertThat(Dentition.DECIDUOUS.isValidTooth(85)).isTrue();
        assertThat(Dentition.DECIDUOUS.isValidTooth(56)).isFalse();
        assertThatThrownBy(() -> planned(Dentition.DECIDUOUS,
                List.of(new ProcedureTooth(11, EnumSet.noneOf(ToothFace.class)))))
                .isInstanceOf(ContactValidationException.class)
                .hasMessageContaining("Dente 11");
    }

    @Test
    void rejectsDuplicatedTooth() {
        assertThatThrownBy(() -> planned(Dentition.PERMANENT, List.of(
                new ProcedureTooth(11, EnumSet.of(ToothFace.M)),
                new ProcedureTooth(11, EnumSet.of(ToothFace.D)))))
                .isInstanceOf(ContactValidationException.class);
    }

    @Test
    void doneRequiresPerformedAt() {
        assertThatThrownBy(() -> PatientProcedure.create(COMPANY, CONTACT, ITEM, "Limpeza",
                ProcedureState.DONE, Dentition.PERMANENT, 0, null, null, null, List.of(), null))
                .isInstanceOf(ContactValidationException.class)
                .hasMessageContaining("data");
    }

    @Test
    void completesPlannedOnly() {
        PatientProcedure procedure = planned(Dentition.PERMANENT, List.of());
        UUID professional = UUID.randomUUID();
        procedure.complete(LocalDate.of(2026, 10, 22), professional);
        assertThat(procedure.isDone()).isTrue();
        assertThat(procedure.getPerformedAt()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(procedure.getProfessionalId()).isEqualTo(professional);
        assertThatThrownBy(() -> procedure.complete(LocalDate.now(), null))
                .isInstanceOf(ContactConflictException.class);
    }

    @Test
    void doneAcceptsOnlyNotes() {
        PatientProcedure procedure = planned(Dentition.PERMANENT, List.of());
        procedure.complete(LocalDate.now(), null);
        procedure.editNotes("  Sem intercorrências  ");
        assertThat(procedure.getNotes()).isEqualTo("Sem intercorrências");
        assertThatThrownBy(() -> procedure.edit(ProcedureState.PLANNED, Dentition.PERMANENT, 100, null,
                null, List.of()))
                .isInstanceOf(ContactConflictException.class);
    }

    @Test
    void editCannotJumpToDone() {
        PatientProcedure procedure = planned(Dentition.PERMANENT, List.of());
        procedure.edit(ProcedureState.PRE_EXISTING, Dentition.PERMANENT, 0, null, null, List.of());
        assertThat(procedure.getState()).isEqualTo(ProcedureState.PRE_EXISTING);
        assertThatThrownBy(() -> procedure.edit(ProcedureState.DONE, Dentition.PERMANENT, 0, null, null,
                List.of()))
                .isInstanceOf(ContactValidationException.class);
    }

    @Test
    void negativePriceIsRejected() {
        assertThatThrownBy(() -> PatientProcedure.create(COMPANY, CONTACT, ITEM, "X", ProcedureState.PLANNED,
                Dentition.PERMANENT, -1, null, null, null, List.of(), null))
                .isInstanceOf(ContactValidationException.class);
    }
}
