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
    public void remove(UUID companyId, UUID id) {
        boolean exists = repository.findByCompany(companyId).stream().anyMatch(c -> c.id().equals(id));
        if (!exists) {
            throw new OmnichannelNotFoundException(id, "IgnoredContact");
        }
        repository.delete(companyId, id);
    }
}
