package com.becommerce.crm.communication.telegram.application;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TelegramUpdateDispatcher {

    private static final Logger log = LoggerFactory.getLogger(TelegramUpdateDispatcher.class);

    private final TelegramUpdateProcessor processor;

    public TelegramUpdateDispatcher(TelegramUpdateProcessor processor) {
        this.processor = processor;
    }

    @Async
    public void dispatch(UUID companyId, String token, JsonNode update) {
        try {
            processor.process(companyId, token, update);
        } catch (RuntimeException e) {
            log.error("[TELEGRAM] falha ao processar update (company={})", companyId, e);
        }
    }
}
