package com.becommerce.crm.communication.telegram.application;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TelegramUpdateProcessor {

    /** Preenchido na Task 6. */
    public void process(UUID companyId, String token, JsonNode update) {
    }
}
