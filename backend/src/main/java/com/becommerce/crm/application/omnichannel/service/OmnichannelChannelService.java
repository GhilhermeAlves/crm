package com.becommerce.crm.application.omnichannel.service;

import com.becommerce.crm.application.omnichannel.dto.ChannelRequest;
import com.becommerce.crm.application.omnichannel.dto.ChannelResponse;
import com.becommerce.crm.application.omnichannel.port.input.OmnichannelChannelUseCase;
import com.becommerce.crm.application.omnichannel.port.output.OmnichannelChannelRepository;
import com.becommerce.crm.domain.omnichannel.Channel;
import com.becommerce.crm.domain.omnichannel.ChannelStatus;
import com.becommerce.crm.domain.omnichannel.OmnichannelNotFoundException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Gestão de canais omnichannel (Sprint 16). CRUD scoped à empresa ativa;
 * o isolamento por tenant é garantido pelo RLS FORCE (GUC) — não há filtro
 * adicional no serviço.
 */
@Service
public class OmnichannelChannelService implements OmnichannelChannelUseCase {

    private final OmnichannelChannelRepository channelRepository;

    public OmnichannelChannelService(OmnichannelChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    @Override
    @Transactional
    public ChannelResponse create(UUID companyId, ChannelRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
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
