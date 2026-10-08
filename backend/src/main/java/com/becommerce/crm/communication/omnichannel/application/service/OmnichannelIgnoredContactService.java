package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.communication.omnichannel.application.dto.IgnoredContactRequest;
import com.becommerce.crm.communication.omnichannel.application.port.input.OmnichannelIgnoredContactUseCase;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelIgnoredContactRepository;
import com.becommerce.crm.communication.omnichannel.domain.IgnoredContact;
import com.becommerce.crm.communication.omnichannel.domain.OmnichannelNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OmnichannelIgnoredContactService implements OmnichannelIgnoredContactUseCase {

    private final OmnichannelIgnoredContactRepository repository;

    public OmnichannelIgnoredContactService(OmnichannelIgnoredContactRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<IgnoredContact> list(UUID companyId) {
        return repository.findByCompany(companyId);
    }

    @Override
    @Transactional
    public IgnoredContact add(UUID companyId, IgnoredContactRequest request) {
        String phone = IgnoredContact.normalizePhone(request.phone());
        if (phone.length() < 10) {
            throw new IllegalArgumentException("Telefone inválido");
        }
        return repository.findByCompany(companyId).stream()
                .filter(c -> c.phone().equals(phone))
                .findFirst()
                .orElseGet(() -> repository.save(new IgnoredContact(UUID.randomUUID(), companyId, phone,
                        request.label() == null || request.label().isBlank() ? null : request.label().trim(),
                        LocalDateTime.now())));
    }

    @Override
    @Transactional
    public IgnoredContact update(UUID companyId, UUID id, IgnoredContactRequest request) {
        List<IgnoredContact> contacts = repository.findByCompany(companyId);
        IgnoredContact current = contacts.stream().filter(c -> c.id().equals(id)).findFirst()
                .orElseThrow(() -> new OmnichannelNotFoundException(id, "IgnoredContact"));
        String phone = IgnoredContact.normalizePhone(request.phone());
        if (phone.length() < 10) {
            throw new IllegalArgumentException("Telefone inválido");
        }
        String label = request.label() == null || request.label().isBlank() ? null : request.label().trim();
        // Número já cadastrado em outro item: une os dois (o telefone é único por empresa).
        IgnoredContact other = contacts.stream()
                .filter(c -> !c.id().equals(id) && c.phone().equals(phone)).findFirst().orElse(null);
        if (other != null) {
            repository.delete(companyId, id);
            return repository.save(new IgnoredContact(other.id(), companyId, phone,
                    label != null ? label : other.label(), other.createdAt()));
        }
        return repository.save(new IgnoredContact(current.id(), companyId, phone, label, current.createdAt()));
    }

    @Override
    @Transactional
    public void remove(UUID companyId, UUID id) {
        boolean exists = repository.findByCompany(companyId).stream().anyMatch(c -> c.id().equals(id));
        if (!exists) {
            throw new OmnichannelNotFoundException(id, "IgnoredContact");
        }
        repository.delete(companyId, id);
    }
}
