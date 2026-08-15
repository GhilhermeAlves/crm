package com.becommerce.crm.infrastructure.config.scheduler;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita o agendamento cron do backend (varredura de oportunidades paradas).
 */
@Configuration
@EnableScheduling
public class WorkflowSchedulingConfig {
}