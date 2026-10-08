package com.becommerce.crm.automation.ai.application.agent.context;

import com.becommerce.crm.communication.omnichannel.domain.IgnoredContact;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.contact.domain.Contact;
import com.becommerce.crm.sales.scheduling.application.port.out.AppointmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Carrega o contexto do paciente do CRM: contato (nome) e próximos
 * agendamentos (agenda = fonte de verdade). Só o necessário, com limite.
 */
@Component
public class PatientContextProvider {

    private static final Logger log = LoggerFactory.getLogger(PatientContextProvider.class);

    static final int MAX_UPCOMING = 3;

    private final ContactRepository contactRepository;
    private final AppointmentRepository appointmentRepository;

    public PatientContextProvider(ContactRepository contactRepository, AppointmentRepository appointmentRepository) {
        this.contactRepository = contactRepository;
        this.appointmentRepository = appointmentRepository;
    }

    /** Sem CRM (fallback/testes): paciente sempre não identificado. */
    public static PatientContextProvider none() {
        return new PatientContextProvider(null, null);
    }

    /**
     * @param contactId contato já vinculado à conversa; quando nulo, tenta pelo
     *                  telefone (com e sem o nono dígito) dentro da MESMA empresa
     */
    public PatientContext load(UUID companyId, UUID contactId, String phone, String profileName,
                               ClinicContext clinic) {
        if (contactRepository == null || appointmentRepository == null) {
            return PatientContext.unknown(profileName);
        }
        try {
            Contact contact = (contactId != null ? contactRepository.findById(contactId) : byPhone(companyId, phone))
                    .filter(c -> companyId.equals(c.getCompanyId()))
                    .orElse(null);
            if (contact == null) {
                return PatientContext.unknown(profileName);
            }
            contactId = contact.getId();
            ZoneId zone = clinic.zone();
            UUID resolvedId = contactId;
            List<PatientContext.UpcomingAppointment> upcoming = appointmentRepository
                    .findUpcomingByContact(companyId, resolvedId, clinic.now().toInstant(), MAX_UPCOMING).stream()
                    .filter(a -> companyId.equals(a.getCompanyId()))
                    .map(a -> new PatientContext.UpcomingAppointment(a.getTitle(), a.getStartAt().atZone(zone),
                            a.getStatus() != null ? a.getStatus().name() : null))
                    .toList();
            return new PatientContext(resolvedId, fullName(contact), profileName, upcoming);
        } catch (RuntimeException e) {
            log.warn("Contexto do paciente indisponível (company={}): {}", companyId, e.getMessage());
            return PatientContext.unknown(profileName);
        }
    }

    private Optional<Contact> byPhone(UUID companyId, String phone) {
        if (phone == null || phone.isBlank()) {
            return Optional.empty();
        }
        for (String variant : IgnoredContact.phoneVariants(phone)) {
            for (String candidate : List.of(variant, "+" + variant)) {
                Optional<Contact> found = contactRepository.findByCompanyIdAndPhone(companyId, candidate);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    private static String fullName(Contact c) {
        String name = Stream.of(c.getFirstName(), c.getLastName()).filter(s -> s != null && !s.isBlank())
                .map(String::trim).collect(Collectors.joining(" "));
        return name.isBlank() ? null : name;
    }
}
