package com.becommerce.crm.masterdata.contact.note.application.service;

import com.becommerce.crm.masterdata.contact.note.application.port.out.ContactNoteRepository;
import com.becommerce.crm.masterdata.contact.note.domain.ContactNote;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Registra automaticamente nas anotações do paciente o que acontece com ele
 * (consultas, pagamentos, orçamentos, documentos), para que a aba Anotações seja
 * o histórico completo do paciente.
 *
 * <p>Roda dentro da transação de quem chama. Diferente dos serviços de caso de
 * uso, NÃO limpa o {@link TenantContext} ao final: restaura o valor anterior,
 * para não derrubar o contexto do serviço chamador.
 */
@Component
public class PatientTimelineRecorder {

    private static final Logger log = LoggerFactory.getLogger(PatientTimelineRecorder.class);

    private final ContactNoteRepository noteRepository;

    public PatientTimelineRecorder(ContactNoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public void record(UUID companyId, UUID contactId, String body, UUID actorId) {
        if (companyId == null || contactId == null || body == null || body.isBlank()) {
            return;
        }
        UUID previous = TenantContext.getCompanyId();
        try {
            TenantContext.setCompanyId(companyId);
            String text = body.length() > ContactNote.MAX_BODY_LENGTH
                    ? body.substring(0, ContactNote.MAX_BODY_LENGTH)
                    : body;
            noteRepository.save(ContactNote.create(companyId, contactId, text, actorId));
        } catch (RuntimeException ex) {
            log.warn("Não foi possível registrar a anotação automática do paciente {}", contactId, ex);
        } finally {
            TenantContext.setCompanyId(previous);
        }
    }
}
