package com.becommerce.crm.infrastructure.websocket.config;

import com.becommerce.crm.infrastructure.websocket.security.StompAuthChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuração WebSocket/STOMP (módulo de Notificações).
 *
 * <p>O endpoint fica em {@code /api/v1/ws} (prefixo do servlet {@code /api/v1}),
 * com fallback SockJS. O broker simples publica em destinos {@code /topic/...} e
 * {@code /queue/...}; o prefixo de destino do usuário é {@code /user}, permitindo
 * entrega direta a um usuário autenticado (ex.: {@code /user/{userId}/queue/notifications}).
 *
 * <p>Autenticação: cada frame STOMP CONNECT passa pelo
 * {@link StompAuthChannelInterceptor}, que valida o JWT do cabeçalho
 * {@code Authorization} e anexa o principal {@code CurrentUser} à sessão.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor authInterceptor;

    public WebSocketConfig(@Lazy StompAuthChannelInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }
}