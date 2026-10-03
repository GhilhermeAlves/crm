package com.becommerce.crm.sales.scheduling.application.dto;

import java.time.Instant;

public record SlotResponse(Instant start, Instant end) {}
