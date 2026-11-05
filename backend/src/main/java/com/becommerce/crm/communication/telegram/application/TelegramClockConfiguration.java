package com.becommerce.crm.communication.telegram.application;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class TelegramClockConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
