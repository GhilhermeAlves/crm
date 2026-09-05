package com.becommerce.crm.application.ai.service;

import com.becommerce.crm.application.ai.port.output.AiProvider;
import com.becommerce.crm.domain.ai.AiProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Failover de geração de IA (Sprint 2 — IA autônoma do WhatsApp). Reutiliza os
 * {@link AiProvider}s já cadastrados no projeto (arquitetura atual, NÃO um novo
 * sistema de IA): o PRIMEIRO provider da lista é o primário e os demais são
 * fallbacks na ordem — no deploy atual há um único provider (OpenAI), então no
 * máximo 1 tentativa; o mecanismo já suporta N providers sem alteração.
 *
 * <p>Regras:</p>
 * <ul>
 *   <li>Erro NÃO recuperável ({@link AiProviderException#isRecoverable()} == false
 *       — config/chave inválida, request estruturalmente inválido) NUNCA dispara
 *       fallback: tentativas limitadas a 1, sem chamadas infinitas.</li>
 *   <li>Erro recuperável (timeout, 429, 5xx, indisponibilidade temporária) tenta
 *       o próximo provider; se todos falharem, a exceção recuperável do último é
 *       relançada (o chamador decide — p.ex. nenhuma resposta, sem envio duplicado).</li>
 *   <li>Retorna no máximo um {@code ChatResult} por chamada — o envio/duplicação
 *       da mensagem é responsabilidade do chamador (uma única persistência/envio).</li>
 * </ul>
 */
@Service
public class AiChatFailover {

    private static final Logger log = LoggerFactory.getLogger(AiChatFailover.class);

    private final List<AiProvider> providers;

    public AiChatFailover(List<AiProvider> providers) {
        this.providers = providers == null ? List.of() : List.copyOf(providers);
    }

    public AiProvider.ChatResult chat(AiProvider.ChatRequest request) {
        if (providers.isEmpty()) {
            throw new AiProviderException("Nenhum provider de IA configurado.", false);
        }

        AiProviderException failure = null;
        for (int i = 0; i < providers.size(); i++) {
            AiProvider provider = providers.get(i);
            long start = System.nanoTime();
            try {
                AiProvider.ChatResult result = provider.chatWithTools(request);
                log.info("Auto-resposta gerada (provider={}, elapsedMs={})",
                        provider.providerName(), elapsedMs(start));
                return result;
            } catch (AiProviderException e) {
                failure = e;
                boolean canFallback = i + 1 < providers.size();
                if (!e.isRecoverable()) {
                    log.warn("Provider {} falhou de forma NÃO recuperável; sem fallback ({}ms)",
                            provider.providerName(), elapsedMs(start));
                    break;
                }
                log.warn("Provider {} falhou de forma recuperável ({}ms); fallback={}",
                        provider.providerName(), elapsedMs(start), canFallback);
            }
        }
        throw failure;
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}