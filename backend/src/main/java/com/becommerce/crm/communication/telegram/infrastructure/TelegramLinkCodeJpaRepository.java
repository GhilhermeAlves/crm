package com.becommerce.crm.communication.telegram.infrastructure;

import com.becommerce.crm.communication.telegram.domain.TelegramLinkCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface TelegramLinkCodeJpaRepository extends JpaRepository<TelegramLinkCode, String> {

    @Modifying
    @Query("delete from TelegramLinkCode c where c.companyId = :companyId and c.userId = :userId")
    void deleteByCompanyIdAndUserId(@Param("companyId") UUID companyId, @Param("userId") UUID userId);

    @Modifying
    @Query("update TelegramLinkCode c set c.usedAt = :now where c.codeHash = :hash and c.usedAt is null")
    int markUsed(@Param("hash") String hash, @Param("now") LocalDateTime now);
}
