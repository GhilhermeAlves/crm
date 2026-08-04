package com.becommerce.auth.presentation.rest;

import com.becommerce.auth.infrastructure.gateway.GatewayApiRelay;
import com.becommerce.auth.infrastructure.gateway.GatewayCookieFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * BFF relay (Sprint 6.4): expõe {@code /api/**} no mesmo domínio do browser e
 * repassa ao crm-backend autenticando com o access token da sessão de gateway.
 *
 * <p>A autenticação é feita exclusivamente pelo cookie HttpOnly
 * {@code crm_session}; nenhum token trafega entre browser e auth-service.
 * Requisições sem sessão (ou com sessão expirada/revogada) respondem 401 no
 * padrão do projeto ({@code status, code, error, message, timestamp}).
 */
@RestController
public class ApiRelayController {

    private final GatewayApiRelay relay;
    private final GatewayCookieFactory cookieFactory;

    public ApiRelayController(GatewayApiRelay relay, GatewayCookieFactory cookieFactory) {
        this.relay = relay;
        this.cookieFactory = cookieFactory;
    }

    @RequestMapping("/api/**")
    public ResponseEntity<byte[]> relay(HttpServletRequest request) throws IOException {
        String sessionToken = cookieFactory.readSessionToken(request.getCookies()).orElse(null);
        Map<String, String> headers = new HashMap<>();
        for (String name : GatewayApiRelay.FORWARD_REQUEST_HEADERS) {
            String value = request.getHeader(name);
            if (value != null) {
                headers.put(name, value);
            }
        }
        return relay.forward(
                HttpMethod.valueOf(request.getMethod()),
                request.getRequestURI(),
                request.getQueryString(),
                sessionToken,
                headers,
                request.getInputStream().readAllBytes());
    }
}
