package com.becommerce.crm.communication.telegram.infrastructure;

import com.becommerce.crm.communication.telegram.domain.TelegramLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TelegramLinkJpaRepository extends JpaRepository<TelegramLink, UUID> {

    Optional<TelegramLink> findByCompanyIdAndTelegramUserId(UUID companyId, long telegramUserId);

    Optional<TelegramLink> findByCompanyIdAndUserId(UUID companyId, UUID userId);

    List<TelegramLink> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
}
