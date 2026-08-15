package com.becommerce.crm.infrastructure.omnichannel.whatsapp;

import com.becommerce.crm.application.omnichannel.port.output.WhatsAppProvider;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provider fake de WhatsApp para desenvolvimento/simulação (Sprint 16, FASE 4/24).
 * NÃO faz chamadas externas: apenas gera um identificador externo sintético.
 * Em produção, o adapter da WhatsApp Cloud API (WhatsAppCloudApiProvider)
 * implementa a mesma porta {@link WhatsAppProvider}, selecionado por config.
 */
@Service
public class FakeWhatsAppProvider implements WhatsAppProvider {

    private static final AtomicLong SEQ = new AtomicLong(1_000_000L);

    @Override
    public SendResult send(SendRequest request) {
        String wamid = "FAKE_" + request.phoneNumberId() + "_" + SEQ.getAndIncrement();
        return new SendResult(wamid);
    }

    @Override
    public String providerName() {
        return "FAKE";
    }
}
