package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.communication.omnichannel.application.dto.ChannelRequest;
import com.becommerce.crm.communication.omnichannel.application.dto.ChannelResponse;
import com.becommerce.crm.communication.omnichannel.application.port.input.OmnichannelChannelUseCase;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelCompanyResolver;
import com.becommerce.crm.communication.omnichannel.domain.Channel;
import com.becommerce.crm.communication.omnichannel.domain.ChannelStatus;
import com.becommerce.crm.communication.omnichannel.domain.OmnichannelChannelConflictException;
import com.becommerce.crm.communication.omnichannel.domain.OmnichannelNotFoundException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Gestão de canais omnichannel (Sprint 16). CRUD scoped à empresa ativa;
 * o isolamento por tenant é garantido pelo RLS FORCE (GUC) — não há filtro
 * adicional no serviço.
 */
@Service
public class OmnichannelChannelService implements OmnichannelChannelUseCase {

    private final OmnichannelChannelRepository channelRepository;
    private final OmnichannelCompanyResolver companyResolver;

    public OmnichannelChannelService(OmnichannelChannelRepository channelRepository,
                                     OmnichannelCompanyResolver companyResolver) {
        this.channelRepository = channelRepository;
        this.companyResolver = companyResolver;
    }

    @Override
    @Transactional
    public ChannelResponse create(UUID companyId, ChannelRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            requireExternalIdAvailable(request.externalId());
            Channel channel = Channel.create(companyId, request.type(), request.provider(),
                    request.name(), request.externalId(), request.config(), request.secretsRef());
            return toResponse(channelRepository.save(channel));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ChannelResponse getById(UUID companyId, UUID channelId) {
        try {
            TenantContext.setCompanyId(companyId);
            return toResponse(requireOwned(companyId, channelId));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelResponse> listByCompany(UUID companyId) {
        try {
            TenantContext.setCompanyId(companyId);
            return channelRepository.findByCompanyId(companyId).stream()
                    .map(OmnichannelChannelService::toResponse).toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public ChannelResponse update(UUID companyId, UUID channelId, ChannelRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            Channel channel = requireOwned(companyId, channelId);
            if (!Objects.equals(channel.getExternalId(), request.externalId())) {
                requireExternalIdAvailable(request.externalId());
            }
            ChannelStatus status = request.status() != null ? request.status() : channel.getStatus();
            channel.update(request.name(), status, request.externalId(), request.config(), request.secretsRef());
            return toResponse(channelRepository.save(channel));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public ChannelResponse setStatus(UUID companyId, UUID channelId, ChannelStatus status) {
        try {
            TenantContext.setCompanyId(companyId);
            Channel channel = requireOwned(companyId, channelId);
            channel.update(channel.getName(), status, channel.getExternalId(),
                    channel.getConfig(), channel.getSecretsRef());
            return toResponse(channelRepository.save(channel));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID channelId) {
        try {
            TenantContext.setCompanyId(companyId);
            Channel channel = requireOwned(companyId, channelId);
            channelRepository.delete(channel);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * O webhook resolve a empresa só pelo external_id, então ele precisa ser
     * único no sistema todo. A consulta usa o resolver (SECURITY DEFINER) porque
     * o RLS esconde os canais das outras empresas.
     */
    private void requireExternalIdAvailable(String externalId) {
        if (externalId != null && !externalId.isBlank()
                && companyResolver.resolveCompanyByChannelReference(externalId).isPresent()) {
            throw new OmnichannelChannelConflictException(externalId);
        }
    }

    private Channel requireOwned(UUID companyId, UUID channelId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new OmnichannelNotFoundException(channelId, "Canal"));
        if (!channel.getCompanyId().equals(companyId)) {
            throw new OmnichannelNotFoundException(channelId, "Canal");
        }
        return channel;
    }

    private static ChannelResponse toResponse(Channel c) {
        return new ChannelResponse(c.getId(), c.getCompanyId(), c.getType(), c.getProvider(),
                c.getName(), c.getStatus(), c.getExternalId(), c.getConfig(), c.getSecretsRef(),
                c.getCreatedAt(), c.getUpdatedAt());
    }
}
