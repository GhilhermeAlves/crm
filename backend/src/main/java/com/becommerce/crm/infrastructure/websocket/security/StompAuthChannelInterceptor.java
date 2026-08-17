package com.becommerce.crm.infrastructure.websocket.security;

import com.becommerce.crm.infrastructure.security.config.CurrentUserResolver;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Autentica frames STOMP (WebSocket) validando o JWT do cabeçalho
 * {@code Authorization: Bearer <token>} enviado no frame CONNECT.
 *
 * <p>O token é decodificado/validado pelo {@link JwtDecoder} (mesmo JWKS do
 * resource server REST). Em seguida, o {@link CurrentUser} é resolvido com o
 * {@link CurrentUserResolver} — o mesmo que deriva o principal nos endpoints
 * REST — garantindo autorização consistente (RBAC + tenant). O principal é
 * anexado ao {@link StompHeaderAccessor}, ficando disponível para os
 * controllers {@code @MessageMapping} e para o roteamento de destinos
 * {@code /user/{userId}/...}.
 *
 * <p>Sem token (ou token inválido) o frame CONNECT é recusado via
 * {@link IllegalArgumentException}, encerrando a sessão.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;
    private final CurrentUserResolver currentUserResolver;

    public StompAuthChannelInterceptor(JwtDecoder jwtDecoder,
                                       CurrentUserResolver currentUserResolver) {
        this.jwtDecoder = jwtDecoder;
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String token = extractBearerToken(accessor);
        if (token == null) {
            throw new IllegalArgumentException("Conexão STOMP sem token de autenticação.");
        }

        try {
            Jwt jwt = jwtDecoder.decode(token);
            CurrentUser currentUser = currentUserResolver.resolve(jwt);

            List<SimpleGrantedAuthority> authorities = currentUser.permissions().stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(currentUser, jwt, authorities);
            accessor.setUser(authentication);
            return message;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Autenticação STOMP inválida: " + e.getMessage());
        }
    }

    private String extractBearerToken(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}