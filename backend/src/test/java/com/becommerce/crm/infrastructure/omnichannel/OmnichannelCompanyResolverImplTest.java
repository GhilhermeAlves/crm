package com.becommerce.crm.infrastructure.omnichannel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OmnichannelCompanyResolverImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private OmnichannelCompanyResolverImpl resolver;

    @BeforeEach
    void setUp() {
        resolver = new OmnichannelCompanyResolverImpl(jdbcTemplate);
    }

    @Test
    void resolveCompany_whenFound_shouldReturnUuid() {
        UUID companyId = UUID.randomUUID();
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<ResultSetExtractor<Object>>any(),
                ArgumentMatchers.<Object>any())).thenReturn(companyId);

        Optional<UUID> result = resolver.resolveCompanyByChannelReference("phone-id-1");

        assertTrue(result.isPresent());
        assertEquals(companyId, result.get());
    }

    @Test
    void resolveCompany_whenNotFound_shouldReturnEmpty() {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<ResultSetExtractor<Object>>any(),
                ArgumentMatchers.<Object>any())).thenReturn(null);

        Optional<UUID> result = resolver.resolveCompanyByChannelReference("unknown-phone");

        assertTrue(result.isEmpty());
    }
}