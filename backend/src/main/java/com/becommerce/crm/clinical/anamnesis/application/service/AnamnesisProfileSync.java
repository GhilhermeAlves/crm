package com.becommerce.crm.clinical.anamnesis.application.service;

import com.becommerce.crm.clinical.anamnesis.application.dto.PatientAnamnesisAnswer;
import com.becommerce.crm.clinical.anamnesis.divergence.ProfileDivergenceJpaEntity;
import com.becommerce.crm.clinical.anamnesis.divergence.ProfileDivergenceJpaRepository;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisModelResponse;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisBinding;
import com.becommerce.crm.masterdata.person.PersonException;
import com.becommerce.crm.masterdata.person.PersonService;
import com.becommerce.crm.masterdata.person.PersonService.ProfileField;
import com.becommerce.crm.masterdata.person.TaxIdValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Ao concluir a anamnese: campo vazio no cadastro é preenchido; campo diferente vira
 * divergência para a recepção decidir (nunca sobrescreve). Falha num campo não impede os demais.
 */
@Component
public class AnamnesisProfileSync {

    private static final Logger log = LoggerFactory.getLogger(AnamnesisProfileSync.class);

    private final PersonService persons;
    private final ProfileDivergenceJpaRepository divergences;

    public AnamnesisProfileSync(PersonService persons, ProfileDivergenceJpaRepository divergences) {
        this.persons = persons;
        this.divergences = divergences;
    }

    public record SyncResult(int applied, int divergences) {
    }

    public SyncResult sync(UUID companyId, UUID personId, UUID anamnesisId, AnamnesisModelResponse snapshot,
                           Map<UUID, PatientAnamnesisAnswer> answers, UUID userId) {
        Map<AnamnesisBinding, Object> values = AnamnesisBindings.values(snapshot, answers);
        if (values.isEmpty()) {
            return new SyncResult(0, 0);
        }
        PersonService.ProfileValues current = persons.profileValues(companyId, personId);
        if (current == null) {
            return new SyncResult(0, 0);
        }
        String origin = userId == null ? "Anamnese (paciente)" : "Anamnese";
        int applied = 0;
        int diverged = 0;
        for (Map.Entry<AnamnesisBinding, Object> e : values.entrySet()) {
            ProfileField field = fieldOf(e.getKey());
            if (field == null) {
                continue;
            }
            String proposed = asText(field, e.getValue());
            if (proposed == null) {
                continue;
            }
            String existing = currentOf(field, current);
            try {
                if (field == ProfileField.CPF && !TaxIdValidator.isValidCpf(proposed)) {
                    diverged += diverge(companyId, personId, anamnesisId, field, existing, proposed, "CPF inválido.");
                } else if (existing == null || existing.isBlank()) {
                    persons.applyAnamnesisPatch(companyId, personId, field, proposed, userId, origin);
                    applied++;
                } else if (!normalize(field, existing).equals(normalize(field, proposed))) {
                    diverged += diverge(companyId, personId, anamnesisId, field, existing, proposed, null);
                }
            } catch (PersonException ex) {
                diverged += diverge(companyId, personId, anamnesisId, field, existing, proposed, ex.getMessage());
            } catch (RuntimeException ex) {
                log.warn("Falha ao aplicar {} da anamnese {} no cadastro {}", field, anamnesisId, personId, ex);
            }
        }
        return new SyncResult(applied, diverged);
    }

    static ProfileField fieldOf(AnamnesisBinding binding) {
        return switch (binding) {
            case PERSON_NAME -> ProfileField.NAME;
            case PERSON_BIRTH_DATE -> ProfileField.BIRTH_DATE;
            case PERSON_SEX -> ProfileField.GENDER;
            case PERSON_CPF -> ProfileField.CPF;
            case PERSON_MOBILE -> ProfileField.MOBILE;
            case PERSON_EMAIL -> ProfileField.EMAIL;
            case PERSON_ADDRESS -> ProfileField.ADDRESS;
            default -> null;
        };
    }

    static String asText(ProfileField field, Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) {
            return null;
        }
        return switch (field) {
            case CPF, MOBILE -> {
                String digits = s.replaceAll("\\D", "");
                yield digits.isEmpty() ? null : digits;
            }
            case GENDER -> AnamnesisBindings.fromSexCode(AnamnesisBindings.toSexCode(s));
            case BIRTH_DATE -> s.length() >= 10 ? s.substring(0, 10) : s;
            default -> s;
        };
    }

    static String currentOf(ProfileField field, PersonService.ProfileValues p) {
        return switch (field) {
            case NAME -> p.name();
            case BIRTH_DATE -> p.birthDate() == null ? null : p.birthDate().toString();
            case GENDER -> p.gender();
            case CPF -> p.cpf();
            case MOBILE -> p.mobile();
            case EMAIL -> p.email();
            case ADDRESS -> p.address();
        };
    }

    static String normalize(ProfileField field, String value) {
        if (value == null) {
            return "";
        }
        return switch (field) {
            case CPF, MOBILE -> {
                String digits = value.replaceAll("\\D", "");
                // celular com ou sem o 55 do país
                yield field == ProfileField.MOBILE && digits.length() == 13 && digits.startsWith("55")
                        ? digits.substring(2) : digits;
            }
            case GENDER -> String.valueOf(AnamnesisBindings.toSexCode(value));
            case BIRTH_DATE -> {
                try {
                    yield LocalDate.parse(value.length() >= 10 ? value.substring(0, 10) : value).toString();
                } catch (RuntimeException e) {
                    yield value.trim();
                }
            }
            default -> value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        };
    }

    private int diverge(UUID companyId, UUID personId, UUID anamnesisId, ProfileField field, String current,
                        String proposed, String reason) {
        if (divergences.existsByCompanyIdAndPersonIdAndFieldAndProposedValueAndStatus(companyId, personId,
                field.name(), proposed, "PENDING")) {
            return 0;
        }
        ProfileDivergenceJpaEntity d = new ProfileDivergenceJpaEntity();
        d.id = UUID.randomUUID();
        d.companyId = companyId;
        d.personId = personId;
        d.anamnesisId = anamnesisId;
        d.field = field.name();
        d.currentValue = current;
        d.proposedValue = proposed;
        d.reason = reason == null ? null : (reason.length() > 255 ? reason.substring(0, 255) : reason);
        divergences.save(d);
        return 1;
    }
}
