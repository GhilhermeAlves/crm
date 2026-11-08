package com.becommerce.crm.billing.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BillingProperties.class)
class BillingConfiguration {}
