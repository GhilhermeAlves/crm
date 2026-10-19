package com.becommerce.crm.communication.omnichannel.infrastructure.persistence;

import com.becommerce.crm.communication.omnichannel.domain.Message;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OmnichannelMessageRepositoryImplTest {

    private final OmnichannelMessageJpaRepository jpa = mock(OmnichannelMessageJpaRepository.class);
    private final OmnichannelMessageRepositoryImpl repository = new OmnichannelMessageRepositoryImpl(jpa);
    private final UUID conversationId = UUID.randomUUID();

    private OmnichannelMessageJpaEntity entity(String body, LocalDateTime createdAt) {
        OmnichannelMessageJpaEntity e = new OmnichannelMessageJpaEntity();
        e.setId(UUID.randomUUID());
        e.setCompanyId(UUID.randomUUID());
        e.setConversationId(conversationId);
        e.setChannelId(UUID.randomUUID());
        e.setDirection("INBOUND");
        e.setType("TEXT");
        e.setBody(body);
        e.setStatus("DELIVERED");
        e.setCreatedAt(createdAt);
        e.setUpdatedAt(createdAt);
        return e;
    }

    @Test
    void primeiraPagina_trazAsMaisRecentesEmOrdemCronologica() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 19, 12, 0);
        PageRequest pageable = PageRequest.of(0, 2);
        // O banco devolve DESC (mais nova primeiro); o total é maior que a página.
        when(jpa.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)).thenReturn(new PageImpl<>(
                List.of(entity("nova", now), entity("anterior", now.minusMinutes(1))), pageable, 40));

        var page = repository.findByConversation(conversationId, 0, 2);

        assertThat(page.content()).extracting(Message::getBody).containsExactly("anterior", "nova");
        assertThat(page.totalElements()).isEqualTo(40);
    }
}
