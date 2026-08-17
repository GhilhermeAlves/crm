package com.becommerce.crm.application.notification.port.output;

import com.becommerce.crm.application.notification.dto.NotificationResponse;

/**
 * Publica uma notificação recém-criada para o destinatário via canal de tempo
 * real (ex.: WebSocket/STOMP). Implementação em infraestrutura; a camada de
 * aplicação depende apenas deste contrato.
 */
public interface NotificationPusher {

    /**
     * Envia a notificação ao usuário {@code NotificationResponse.userId()}.
     * A implementação deve garantir que apenas sessões da MESMA empresa recebam.
     */
    void push(NotificationResponse notification);
}