package com.becommerce.crm.infrastructure.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração da camada de mensageria (Sprint 23). RabbitMQ permanece um
 * detalhe de INFRAESTRUTURA: o domínio/aplicação só conhecem as portas
 * ({@code WhatsAppEventPublisher}, {@code FollowUpEventPublisher}) e os eventos
 * tipados; nenhum {@code RabbitTemplate}/{@code @RabbitListener} vaza para fora.
 *
 * <p>O converter Jackson tipado é o único bean necessário — o auto-config do
 * Spring Boot aplica {@link MessageConverter} ao {@code RabbitTemplate} e às
 * factories de listener ({@code __TypeId__} para desserialização segura em
 * {@code com.becommerce.crm}). Retry/backoff dos consumers vêm de
 * {@code spring.rabbitmq.listener.*} (application.yml).
 */
@Configuration
public class RabbitConfig {

    /** Pacotes confiáveis para desserialização JSON tipada dos eventos.
     *  O sufixo {@code .*} cobre TODOS os subpacotes (os eventos ficam em
     *  {@code com.becommerce.crm.application.*.event}); sem o wildcard apenas o
     *  pacote exato seria confiável e a desserialização do consumer falharia. */
    private static final String TRUSTED_PACKAGE = "com.becommerce.crm.*";

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages(TRUSTED_PACKAGE);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}