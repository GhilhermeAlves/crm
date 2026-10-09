package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.communication.omnichannel.application.dto.ChannelRequest;
import com.becommerce.crm.communication.omnichannel.application.dto.ChannelResponse;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelCompanyResolver;
import com.becommerce.crm.communication.omnichannel.domain.Channel;
import com.becommerce.crm.communication.omnichannel.domain.ChannelProvider;
import com.becommerce.crm.communication.omnichannel.domain.ChannelStatus;
import com.becommerce.crm.communication.omnichannel.domain.ChannelType;
import com.becommerce.crm.communication.omnichannel.domain.OmnichannelChannelConflictException;
import com.becommerce.crm.communication.omnichannel.domain.OmnichannelNotFoundException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OmnichannelChannelServiceTest {

    private final UUID companyId = UUID.randomUUID();

    @Mock OmnichannelChannelRepository channelRepository;
    @Mock OmnichannelCompanyResolver companyResolver;

    @InjectMocks OmnichannelChannelService service;

    @BeforeEach
    @AfterEach
    void cleanTenant() {
        TenantContext.clear();
    }

    private ChannelRequest request(ChannelStatus status) {
        return new ChannelRequest("Comercial", ChannelType.WHATSAPP, ChannelProvider.FAKE,
                "espaco-123", null, "vault:token", status);
    }

    private Channel channel() {
        return Channel.reconstitute(UUID.randomUUID(), companyId, ChannelType.WHATSAPP, ChannelProvider.FAKE,
                "Comercial", ChannelStatus.ACTIVE, "espaco-123", null, "vault:token",
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
    }

    @Test
    void create_shouldSaveAndReturnResponse() {
        Channel c = channel();
        when(channelRepository.save(any(Channel.class))).thenReturn(c);
        ChannelResponse r = service.create(companyId, request(ChannelStatus.ACTIVE));
        assertEquals(c.getId(), r.id());
        assertEquals(ChannelStatus.ACTIVE, r.status());
    }

    @Test
    void create_withExternalIdUsedByAnyCompany_shouldThrowConflictAndNotSave() {
        when(companyResolver.resolveCompanyByChannelReference("espaco-123"))
                .thenReturn(Optional.of(UUID.randomUUID()));
        assertThrows(OmnichannelChannelConflictException.class,
                () -> service.create(companyId, request(ChannelStatus.ACTIVE)));
        verify(channelRepository, never()).save(any());
    }

    @Test
    void update_changingToExternalIdInUse_shouldThrowConflictAndNotSave() {
        Channel c = channel();
        when(channelRepository.findById(c.getId())).thenReturn(Optional.of(c));
        when(companyResolver.resolveCompanyByChannelReference("outra-instancia"))
                .thenReturn(Optional.of(UUID.randomUUID()));
        ChannelRequest req = new ChannelRequest("Comercial", ChannelType.WHATSAPP, ChannelProvider.FAKE,
                "outra-instancia", null, "vault:token", ChannelStatus.ACTIVE);
        assertThrows(OmnichannelChannelConflictException.class, () -> service.update(companyId, c.getId(), req));
        verify(channelRepository, never()).save(any());
    }

    @Test
    void getById_shouldReturnOwnedChannel() {
        Channel c = channel();
        when(channelRepository.findById(c.getId())).thenReturn(Optional.of(c));
        assertEquals(c.getId(), service.getById(companyId, c.getId()).id());
    }

    @Test
    void getById_fromOtherCompany_shouldThrowNotFound() {
        Channel other = Channel.reconstitute(UUID.randomUUID(), UUID.randomUUID(), ChannelType.WHATSAPP,
                ChannelProvider.FAKE, "X", ChannelStatus.ACTIVE, "e", null, null,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        when(channelRepository.findById(other.getId())).thenReturn(Optional.of(other));
        assertThrows(OmnichannelNotFoundException.class, () -> service.getById(companyId, other.getId()));
    }

    @Test
    void listByCompany_shouldMapAll() {
        Channel c = channel();
        when(channelRepository.findByCompanyId(companyId)).thenReturn(List.of(c));
        assertEquals(1, service.listByCompany(companyId).size());
    }

    @Test
    void update_shouldApplyRequestAndSave() {
        Channel c = channel();
        when(channelRepository.findById(c.getId())).thenReturn(Optional.of(c));
        when(channelRepository.save(any(Channel.class))).thenReturn(c);
        ChannelResponse r = service.update(companyId, c.getId(), request(ChannelStatus.INACTIVE));
        assertEquals(ChannelStatus.INACTIVE, r.status());
        assertEquals("Comercial", c.getName());
    }

    @Test
    void setStatus_shouldChangeStatusAndSave() {
        Channel c = channel();
        when(channelRepository.findById(c.getId())).thenReturn(Optional.of(c));
        when(channelRepository.save(any(Channel.class))).thenReturn(c);
        ChannelResponse r = service.setStatus(companyId, c.getId(), ChannelStatus.ERROR);
        assertEquals(ChannelStatus.ERROR, r.status());
    }

    @Test
    void delete_shouldRemoveOwnedChannel() {
        Channel c = channel();
        when(channelRepository.findById(c.getId())).thenReturn(Optional.of(c));
        service.delete(companyId, c.getId());
        verify(channelRepository).delete(c);
    }

    @Test
    void delete_fromOtherCompany_shouldThrowAndNotDelete() {
        Channel other = Channel.reconstitute(UUID.randomUUID(), UUID.randomUUID(), ChannelType.WHATSAPP,
                ChannelProvider.FAKE, "X", ChannelStatus.ACTIVE, "e", null, null,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        when(channelRepository.findById(other.getId())).thenReturn(Optional.of(other));
        assertThrows(OmnichannelNotFoundException.class, () -> service.delete(companyId, other.getId()));
        verify(channelRepository, never()).delete(any());
    }
}