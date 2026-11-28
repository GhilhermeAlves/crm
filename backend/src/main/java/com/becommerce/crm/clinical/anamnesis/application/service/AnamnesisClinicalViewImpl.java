package com.becommerce.crm.clinical.anamnesis.application.service;

import com.becommerce.crm.clinical.anamnesis.application.dto.PatientAnamnesisAnswer;
import com.becommerce.crm.clinical.anamnesis.application.dto.PatientAnamnesisResponse;
import com.becommerce.crm.clinical.anamnesis.application.port.in.PatientAnamnesisUseCase;
import com.becommerce.crm.clinical.anamnesis.domain.PatientAnamnesisStatus;
import com.becommerce.crm.clinical.trichology.application.port.AnamnesisClinicalView;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisModelResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisOptionResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisQuestionResponse;
import com.becommerce.crm.masterdata.anamnesis.application.dto.AnamnesisSectionResponse;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisBinding;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class AnamnesisClinicalViewImpl implements AnamnesisClinicalView {

    private final PatientAnamnesisUseCase anamneses;

    public AnamnesisClinicalViewImpl(PatientAnamnesisUseCase anamneses) {
        this.anamneses = anamneses;
    }

    @Override
    public Optional<ClinicalAnamnesis> latestCompleted(UUID companyId, UUID contactId) {
        return keepTenant(companyId, () -> anamneses.current(companyId, contactId)).map(AnamnesisClinicalViewImpl::toView);
    }

    @Override
    public Optional<ClinicalAnamnesis> byId(UUID companyId, UUID contactId, UUID anamnesisId) {
        if (anamnesisId == null) {
            return Optional.empty();
        }
        try {
            PatientAnamnesisResponse r = keepTenant(companyId, () -> anamneses.get(companyId, contactId, anamnesisId));
            return r.status() == PatientAnamnesisStatus.COMPLETED ? Optional.of(toView(r)) : Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /** O caso de uso limpa o TenantContext ao sair; o exame continua no mesmo tenant. */
    private static <T> T keepTenant(UUID companyId, Supplier<T> call) {
        UUID previous = TenantContext.getCompanyId();
        try {
            return call.get();
        } finally {
            if (previous != null) {
                TenantContext.setCompanyId(previous);
            } else {
                TenantContext.setCompanyId(companyId);
            }
        }
    }

    static ClinicalAnamnesis toView(PatientAnamnesisResponse r) {
        return new ClinicalAnamnesis(r.id(), r.completedAt(), quizLike(r.snapshot(), r.answers()),
                readable(r.snapshot(), r.answers()));
    }

    /** Respostas vinculadas no formato das regras do Copiloto (booleanos como "true"/"false"). */
    static Map<String, Object> quizLike(AnamnesisModelResponse snapshot, Map<UUID, PatientAnamnesisAnswer> answers) {
        Map<String, Object> out = new LinkedHashMap<>();
        Map<AnamnesisBinding, Object> values = AnamnesisBindings.values(snapshot, answers);
        // EnumMap segue a ordem do enum: TRICHO_SEX vem depois e vence PERSON_SEX
        values.forEach((binding, value) -> {
            if (binding.quizKey() != null) {
                out.put(binding.quizKey(), normalize(value));
            }
        });
        return out;
    }

    private static Object normalize(Object value) {
        if (value instanceof Boolean b) {
            return b.toString();
        }
        if (value instanceof Collection<?> c) {
            return c.stream().map(String::valueOf).toList();
        }
        return String.valueOf(value);
    }

    /** Perguntas do paciente respondidas, sem as vinculadas ao cadastro (nada que identifique a pessoa). */
    static List<QuestionAnswer> readable(AnamnesisModelResponse snapshot, Map<UUID, PatientAnamnesisAnswer> answers) {
        List<QuestionAnswer> out = new ArrayList<>();
        if (snapshot == null || snapshot.sections() == null || answers == null) {
            return out;
        }
        for (AnamnesisSectionResponse section : snapshot.sections()) {
            if (section.professional() || section.questions() == null) {
                continue;
            }
            for (AnamnesisQuestionResponse q : section.questions()) {
                if (q.binding() != null && q.binding().group() == AnamnesisBinding.Group.PERSON) {
                    continue;
                }
                PatientAnamnesisAnswer a = answers.get(q.id());
                String text = answerText(q, a);
                if (text != null) {
                    out.add(new QuestionAnswer(section.title(), q.text(), text));
                }
            }
        }
        return out;
    }

    private static String answerText(AnamnesisQuestionResponse q, PatientAnamnesisAnswer a) {
        if (a == null || a.value() == null) {
            return null;
        }
        Object v = a.value();
        String base;
        if (v instanceof Boolean b) {
            base = b ? "Sim" : "Não";
        } else if (v instanceof Collection<?> c) {
            if (c.isEmpty()) {
                return null;
            }
            base = String.join(", ", c.stream().map(x -> label(q, String.valueOf(x))).toList());
        } else {
            String s = String.valueOf(v).trim();
            if (s.isEmpty()) {
                return null;
            }
            base = label(q, s);
        }
        if (a.complement() != null && !a.complement().isBlank()) {
            base = base + " (" + a.complement().trim() + ")";
        }
        return base;
    }

    private static String label(AnamnesisQuestionResponse q, String value) {
        if (q.options() == null) {
            return value;
        }
        for (AnamnesisOptionResponse o : q.options()) {
            if (value.equals(o.value()) || value.equals(o.label())) {
                return o.label();
            }
        }
        return value;
    }
}
