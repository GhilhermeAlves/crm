package com.becommerce.crm.infrastructure.omnichannel;

import com.becommerce.crm.application.omnichannel.port.output.OmnichannelCompanyResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolve a empresa de um canal via a função SECURITY DEFINER
 * {@code app.resolve_channel_company} (V044). Usada pelo webhook, que não tem
 * sessão de usuário autenticado e, portanto, não pode confiar no GUC.
 */
@Service
public class OmnichannelCompanyResolverImpl implements OmnichannelCompanyResolver {

    private final JdbcTemplate jdbcTemplate;

    public OmnichannelCompanyResolverImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<UUID> resolveCompanyByChannelReference(String channelExternalId) {
        UUID companyId = jdbcTemplate.query(
                "SELECT app.resolve_channel_company(?)",
                rs -> rs.next() ? Optional.ofNullable(rs.getObject(1, UUID.class)).orElse(null) : null,
                channelExternalId);
        return Optional.ofNullable(companyId);
    }
}
