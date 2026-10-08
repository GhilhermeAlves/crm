package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.communication.omnichannel.application.dto.IgnoredContactRequest;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelIgnoredContactRepository;
import com.becommerce.crm.communication.omnichannel.domain.IgnoredContact;
import com.becommerce.crm.communication.omnichannel.domain.OmnichannelNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OmnichannelIgnoredContactServiceTest {

    private final OmnichannelIgnoredContactRepository repository = mock(OmnichannelIgnoredContactRepository.class);
    private final OmnichannelIgnoredContactService service = new OmnichannelIgnoredContactService(repository);
    private final UUID companyId = UUID.randomUUID();

    private IgnoredContact contact(String phone, String label) {
        return new IgnoredContact(UUID.randomUUID(), companyId, phone, label, LocalDateTime.now());
    }

    @Test
    void update_trocaNumeroEIdentificacao_mantendoOId() {
        IgnoredContact atual = contact("5534991546422", "Esposo");
        when(repository.findByCompany(companyId)).thenReturn(List.of(atual));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IgnoredContact salvo = service.update(companyId, atual.id(), new IgnoredContactRequest("(34) 98888-7777", "Mãe"));

        assertEquals(atual.id(), salvo.id());
        assertEquals("5534988887777", salvo.phone());
        assertEquals("Mãe", salvo.label());
    }

    @Test
    void update_paraNumeroJaCadastrado_unificaOsDois() {
        IgnoredContact atual = contact("5534911112222", "Errado");
        IgnoredContact existente = contact("5534988887777", "Mãe");
        when(repository.findByCompany(companyId)).thenReturn(List.of(atual, existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IgnoredContact salvo = service.update(companyId, atual.id(), new IgnoredContactRequest("34988887777", null));

        verify(repository).delete(companyId, atual.id());
        assertEquals(existente.id(), salvo.id());
        assertEquals("Mãe", salvo.label());
    }

    @Test
    void update_idInexistente_lancaNaoEncontrado() {
        when(repository.findByCompany(companyId)).thenReturn(List.of());

        assertThrows(OmnichannelNotFoundException.class,
                () -> service.update(companyId, UUID.randomUUID(), new IgnoredContactRequest("34988887777", null)));
        verify(repository, never()).save(argThat(c -> true));
    }
}
