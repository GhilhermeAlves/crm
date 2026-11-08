package com.becommerce.crm.billing.infrastructure;

import com.becommerce.crm.billing.domain.BillingAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface BillingAccountJpaRepository extends JpaRepository<BillingAccount, UUID> {}
