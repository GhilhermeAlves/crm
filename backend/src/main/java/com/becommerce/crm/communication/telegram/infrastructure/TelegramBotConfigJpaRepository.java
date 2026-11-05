package com.becommerce.crm.communication.telegram.infrastructure;

import com.becommerce.crm.communication.telegram.domain.TelegramBotConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TelegramBotConfigJpaRepository extends JpaRepository<TelegramBotConfig, UUID> {
}
