# CRM Improvements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Corrigir posturas de segurança em produção, higienizar o repositório e a VPS, melhorar a UX/qualidade do frontend, consolidar autorização no backend e adicionar testes E2E — em 4 fases independentes.

**Architecture:** Plano em fases. Fase 1 (segurança/hardening/higiene: webhook, OTP, validação, exceções, CORS, VPS), Fase 2 (UX frontend: busca server-side, estados, rotas, refactor de componentes, mutation helper), Fase 3 (autorização centralizada no backend), Fase 4 (Playwright E2E). Cada fase é executável e revisada isoladamente, com testes verdes ao final.

**Tech Stack:** Backend Java 25 / Spring Boot 3.5.16 (Clean+DDD, Checkstyle, JaCoCo, Testcontainers), auth-service Java 25, frontend Next.js 14 / React 18 / TypeScript 5 / Tailwind 3 / shadcn-ui / TanStack Query 5 / Vitest 4, VPS Ubuntu 24.04 Docker Compose + Nginx TLS, Playwright.

**Spec:** `docs/superpowers/specs/2026-09-17-crm-improvements-design.md` — o plano argumenta a partir do spec; executores leem ambos.

---

## Global Constraints

Copiado do spec; TODAS as tasks herdam isto. Valores exatos, verbatim:

- **Idioma da UI:** pt-BR (strings de UI, toasts e mensagens continuam em pt-BR).
- **Fora de escopo (NÃO tocar):** reorganização de `docs/`, `docs-ai/`, `contexts/`, `docker/` vs raiz, `infra/` vs `infrastructure/`; e a dupla fonte de identidade (PostgreSQL + Keycloak). Manter `SPRING_FLYWAY_OUT_OF_ORDER=true` e manter os 2 stacks compose da VPS (`crm-infrastructure` + app).
- **Autorização:** RLS FORCE no PostgreSQL é a última barreira; manter `@PreAuthorize` de permissões nos controllers; não remover verificação de tenant de serviços.
- **Sem novas dependências** não justificadas no spec (i18n e socket.io ficam de fora; `socket.io-client`/`cmdk` continuam no package.json até tarefa dedicada, não é responsabilidade deste plano removê-los — mas `deals.mock.ts` deve sair).
- **Segredos:** nunca commitar `.env`, tokens, senhas ou chaves. Arquivos `.env*` são untracked na VPS e no repo.
- **Formatação frontend:** rodar `npm run format` (prettier) antes de commit (o CI falha se o formato divergir).
- **Comandos de teste:** backend/auth: `./mvnw verify` (Windows usa `.\mvnw.cmd verify`); frontend: `npm run lint`, `npm run typecheck`, `npm test`. Testes de integração `*IT.java` exigem Docker (Testcontainers).
- **Estilo de commit:** escopo curto em português/inglês, ex.: `fix(backend): rejeitar webhook não autenticado`, `feat(frontend): adicionar busca server-side de leads`.
- **Etiqueta na VPS:** `crm-vps` (root). Qualquer alteração em produção é depois da fase de código passar nos testes e com backup antes (backup automático criado na Task 1.8; para a Task 1.1, um `pg_dump` manual antes do rollout é obrigatório).

---

## FASE 1 — Segurança, hardening e higiene

### Task 1.1: Autenticar webhook UAZAPI (crítico — produção hoje aceita POST anônimo)

**Contexto verificado em produção:** `CRM_WHATSAPP_APP_SECRET` está **vazio** e `WHATSAPP_WEBHOOK_ALLOW_UNSIGNED=true`. O provider é `uazapi`. No estado atual, qualquer requisição POST anônima a `/api/v1/omnichannel/whatsapp/webhook` (público, sem sessão) é processada e injeta mensagens no inbox. O endpoint é protegido apenas por `WhatsAppWebhookSignatureVerifier`, que exige HMAC **somente quando** um `app-secret` existe — e o UAZAPI não envia HMAC.

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/infrastructure/omnichannel/whatsapp/WhatsAppWebhookTokenVerifier.java`
- Modify: `backend/src/main/java/com/becommerce/crm/presentation/rest/omnichannel/WhatsAppWebhookController.java`
- Modify: `backend/src/test/java/com/becommerce/crm/presentation/rest/omnichannel/WhatsAppWebhookControllerTest.java`
- Create: `backend/src/test/java/com/becommerce/crm/infrastructure/omnichannel/whatsapp/WhatsAppWebhookTokenVerifierTest.java`
- Modify: `backend/src/main/resources/application.yml` (linha 132 — default já é `false`, adicionar property `webhook-token`)
- Modify: `docker/docker-compose.yml` (passagem de env `OMNICHANNEL_WHATSAPP_WEBHOOK_TOKEN`) e `.env.example`
- Modify: `docker/nginx/crm.conf` (comentário/roteamento — sem mudança de rota)

**Interfaces:**
- Consumes: `WhatsAppWebhookSignatureVerifier.isValid(signature, payload)` (existe).
- Produces: `WhatsAppWebhookTokenVerifier` com método `boolean isAuthenticated(String rawPayload, String signatureHeader, String tokenParam)`. O controller passa a chamar `tokenVerifier.isAuthenticated(...)`.

- [ ] **Step 1: Investigar como o UAZAPI autentica o callback**
      Executar na VPS e decidir o método que a instância UAZAPI consegue enviar na URL do webhook:

      ```bash
      ssh crm-vps "docker logs crm-backend 2>&1 | grep -i 'Webhook recebido' | tail -5"
      ssh crm-vps "docker exec crm-postgres psql -U crm_admin -d crm -tAc \"select channel_ref,secrets_ref from omnichannel_channels\" 2>/dev/null"
      ```
      Verificar na dashboard da instância UAZAPI se a URL do webhook registrada aceita query param (`?token=...`) ou header. **Decisão tomada aqui define a coleta do token** (query param → propriedade `webhook-token`; header `X-Uazapi-Token` → mesmo valor). Se a UAZAPI não suportar nenhum dos dois, o fallback é restringir a `location` do webhook no nginx por IP (registrado no Step 7). Em qualquer caso, `WHATSAPP_WEBHOOK_ALLOW_UNSIGNED` passa a `false`.
      Expected: método de transporte do token identificado.

- [ ] **Step 2: Escrever o teste que falha (novo verifier)**

```java
package com.becommerce.crm.infrastructure.omnichannel.whatsapp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhatsAppWebhookTokenVerifierTest {

    private static final String TOKEN = "segredo-webhook-abc";

    private static WhatsAppWebhookTokenVerifier newVerifier(boolean allowUnsigned, String token) {
        return new WhatsAppWebhookTokenVerifier(allowUnsigned, token);
    }

    @Test
    void semTokenConfigurado_eAllowUnsignedFalse_deveRejeitar() {
        WhatsAppWebhookTokenVerifier v = newVerifier(false, "");
        assertFalse(v.isAuthenticated("{}", null, null));
    }

    @Test
    void tokenConfigurado_aceitaQueryCorreta() {
        WhatsAppWebhookTokenVerifier v = newVerifier(false, TOKEN);
        assertTrue(v.isAuthenticated("{}", null, TOKEN));
    }

    @Test
    void tokenConfigurado_rejeitaAusenteOuErrado() {
        WhatsAppWebhookTokenVerifier v = newVerifier(false, TOKEN);
        assertFalse(v.isAuthenticated("{}", null, null));
        assertFalse(v.isAuthenticated("{}", null, "errado"));
    }

    @Test
    void modoDesenvolvimento_allowUnsignedTrue_aceita() {
        WhatsAppWebhookTokenVerifier v = newVerifier(true, "");
        assertTrue(v.isAuthenticated("{}", null, null));
    }
}
```

- [ ] **Step 3: Rodar para ver falhar**

Run: `.\mvnw.cmd test -Dtest=WhatsAppWebhookTokenVerifierTest -pl .` (na raiz `backend/`)
Expected: FAIL — `WhatsAppWebhookTokenVerifier` não existe / não compila.

- [ ] **Step 4: Implementar o verifier**

```java
package com.becommerce.crm.infrastructure.omnichannel.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Autenticação de webhook para providers que NÃO usam o HMAC da Meta
 * (ex.: UAZAPI). Segue a mesma política fail-safe do verifier HMAC:
 * <ul>
 *   <li>token configurado ({@code omnichannel.whatsapp.webhook-token}):
 *       o payload SÓ é aceito com o token correto (query param ou header);</li>
 *   <li>token vazio: aceita apenas em modo dev explícito
 *       ({@code omnicchannels.whatsapp.webhook-allow-unsigned=true} — default é rejeitar).</li>
 * </ul>
 */
@Component
public class WhatsAppWebhookTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookTokenVerifier.class);

    private final boolean allowUnsigned;
    private final String webhookToken;

    public WhatsAppWebhookTokenVerifier(
            @Value("${omnichannel.whatsapp.webhook-allow-unsigned:false}") boolean allowUnsigned,
            @Value("${omnichannel.whatsapp.webhook-token:}") String webhookToken) {
        this.allowUnsigned = allowUnsigned;
        this.webhookToken = webhookToken == null ? "" : webhookToken.trim();
    }

    /**
     * @param rawPayload     body bruto (para futuro HMAC, mantido por assinatura)
     * @param signatureHeader header X-Hub-Signature-256 (Meta; hoje ignorado quando há token)
     * @param tokenParam      token recebido (query param {@code token} ou header X-Uazapi-Token)
     */
    public boolean isAuthenticated(String rawPayload, String signatureHeader, String tokenParam) {
        if (!webhookToken.isEmpty()) {
            if (tokenParam == null) {
                log.warn("Webhook {} sem token; rejeitando", "uazapi");
                return false;
            }
            boolean ok = MessageDigest.isEqual(
                    webhookToken.getBytes(StandardCharsets.UTF_8),
                    tokenParam.trim().getBytes(StandardCharsets.UTF_8));
            if (!ok) {
                log.warn("Webhook {} com token inválido; rejeitando", "uazapi");
            }
            return ok;
        }
        if (!allowUnsigned) {
            log.warn("Webhook sem token configurado e allow-unsigned desativado; rejeitando");
            return false;
        }
        return true;
    }
}
```

- [ ] **Step 5: Atualizar o controller para usar o verifier**

```java
    private final WhatsAppWebhookUseCase webhookUseCase;
    private final WhatsAppWebhookSignatureVerifier signatureVerifier;
    private final WhatsAppWebhookTokenVerifier tokenVerifier;
    private final ObjectMapper objectMapper;

    public WhatsAppWebhookController(WhatsAppWebhookUseCase webhookUseCase,
                                     WhatsAppWebhookSignatureVerifier signatureVerifier,
                                     WhatsAppWebhookTokenVerifier tokenVerifier,
                                     ObjectMapper objectMapper) {
        this.webhookUseCase = webhookUseCase;
        this.signatureVerifier = signatureVerifier;
        this.tokenVerifier = tokenVerifier;
        this.objectMapper = objectMapper;
    }
```
E o `@PostMapping` passa a aceitar o token:

```java
    @PostMapping
    public ResponseEntity<Void> handle(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestParam(name = "token", required = false) String token,
            @RequestHeader(value = "X-Uazapi-Token", required = false) String uazapiToken,
            @RequestBody String rawPayload) {
        if (!signatureVerifier.isValid(signature, rawPayload)
                && !tokenVerifier.isAuthenticated(rawPayload, signature, token != null ? token : uazapiToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // ... restante inalterado (parse + handleEvent)
    }
```
Nota: a ordem `signatureVerifier.isValid(...) || tokenVerifier.isAuthenticated(...)` mantém o HMAC da Meta (cloud-api) e permite o token do UAZAPI.

- [ ] **Step 6: Corrigir o teste do controller (2 primeiros casos com app-secret continuam; adicionar caso de token)**

No `@BeforeEach`:
```java
mockMvc = MockMvcBuilders.standaloneSetup(new WhatsAppWebhookController(
                useCase,
                new WhatsAppWebhookSignatureVerifier(SECRET, false),
                new WhatsAppWebhookTokenVerifier(false, "segredo-token"),
                new ObjectMapper()))
        .build();
```
Adicionar:
```java
    @Test
    void uazapiSemToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/omnichannel/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"messages\"}"))
                .andExpect(status().isUnauthorized());
        verify(useCase, never()).handleEvent(any());
    }

    @Test
    void uazapiComTokenCorreto_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/omnichannel/whatsapp/webhook")
                        .param("token", "segredo-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"messages\"}"))
                .andExpect(status().isOk());
        verify(useCase).handleEvent(any());
    }
```
Rode `.\mvnw.cmd test -Dtest=WhatsAppWebhookControllerTest` — todos verdes.

- [ ] **Step 7: Ajustar config (application.yml, compose app, .env.example)**

`application.yml` (bloco `omnichannel.whatsapp`), acrescentar após `webhook-allow-unsigned`:
```yaml
    webhook-token: ${OMNICHANNEL_WHATSAPP_WEBHOOK_TOKEN:}
```
Em `docker/docker-compose.yml`, adicionar ao serviço `backend`:
```yaml
      - OMNICHANNEL_WHATSAPP_WEBHOOK_TOKEN=${OMNICHANNEL_WHATSAPP_WEBHOOK_TOKEN:-}
      - WHATSAPP_WEBHOOK_ALLOW_UNSIGNED=${WHATSAPP_WEBHOOK_ALLOW_UNSIGNED:-false}
```
Em `.env.example` (raiz e/ou `docker/.env.example`):
```
OMNICHANNEL_WHATSAPP_WEBHOOK_TOKEN=
WHATSAPP_WEBHOOK_ALLOW_UNSIGNED=false
```

- [ ] **Step 8: Rollout em produção com backup**

```bash
# backup obrigatório antes
ssh crm-vps "docker exec crm-postgres pg_dump -U crm_admin -d crm --format=custom -f /tmp/pre-webhook-auth.dump && docker cp crm-postgres:/tmp/pre-webhook-auth.dump /root/pre-webhook-auth.dump"
```
Gerar token forte: `openssl rand -hex 32`. Registrar a URL do webhook na instância UAZAPI acrescentando `?token=<valor>` (ou configurar o header suportado — do Step 1). Atualizar `/opt/crm/docker/.env`:
```
WHATSAPP_WEBHOOK_ALLOW_UNSIGNED=false
OMNICHANNEL_WHATSAPP_WEBHOOK_TOKEN=<valor>
```
Reiniciar apenas backend: `ssh crm-vps "cd /opt/crm/docker && docker compose up -d backend"`. Validar: (a) webhook do UAZAPI (enviar mensagem real de teste) entra; (b) `curl -X POST https://srv1348261.hstgr.cloud/api/v1/omnichannel/whatsapp/webhook -d '{}'` retorna **401** (sem token).
Se o UAZAPI não suportar token, aplicar fallback nginx: na `location /api/v1/omnichannel/whatsapp/webhook`, restringir por IP de origem com `allow <ip>; deny all;` e documentar; MAS o código acima garante que mesmo assim o endpoint rejeita sem token quando token configurado.

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/becommerce/crm/infrastructure/omnichannel/whatsapp/WhatsAppWebhookTokenVerifier.java \
        backend/src/main/java/com/becommerce/crm/presentation/rest/omnichannel/WhatsAppWebhookController.java \
        backend/src/main/resources/application.yml \
        backend/src/test/java/com/becommerce/crm/infrastructure/omnichannel/whatsapp/WhatsAppWebhookTokenVerifierTest.java \
        backend/src/test/java/com/becommerce/crm/presentation/rest/omnichannel/WhatsAppWebhookControllerTest.java \
        docker/docker-compose.yml docker/.env.example .env.example
git commit -m "fix(omnichannel): exigir token no webhook UAZAPI (allow-unsigned=false por padrão)"

---

### Task 1.2: Rate limit no fluxo OTP `/auth/phone/*`

**Contexto:** `OtpService` já tem cooldown de reenvio por telefone e limite de tentativas de verificação por OTP, mas o endpoint público `POST /auth/phone/send-otp` não tem limite por IP — um atacante pode disparar OTPs (custo de SMS) ou forçar OTPs para telefones arbitrários. Reutilizar o padrão `InvitationRateLimiter` (janela fixa atômica via Lua + Redis).

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/infrastructure/otp/rate/OtpRateLimiter.java`
- Modify: `backend/src/main/java/com/becommerce/crm/presentation/rest/identity/PhoneAuthController.java`
- Create: `backend/src/test/java/com/becommerce/crm/infrastructure/otp/rate/OtpRateLimiterTest.java`
- Modify: `backend/src/test/java/com/becommerce/crm/presentation/rest/identity/PhoneAuthControllerTest.java` (se existir; senão criar controller test novo seguindo `WhatsAppWebhookControllerTest`)

**Interfaces:**
- Consumes: `StringRedisTemplate` (bean do Spring Data Redis) — mesmo tipo do `InvitationRateLimiter`.
- Produces: `OtpRateLimiter.trySend(String clientIp): boolean` e `OtpRateLimiter.tryVerify(String phoneE164): boolean`.

- [ ] **Step 1: Escrever o teste que falha**

```java
package com.becommerce.crm.infrastructure.otp.rate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OtpRateLimiterTest {

    private static final int MAX_SEND = 5;       // por IP a cada minuto
    private static final int MAX_VERIFY = 5;     // por telefone a cada minuto

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private OtpRateLimiter limiter;

    @BeforeEach
    void setup() {
        limiter = new OtpRateLimiter(redis, MAX_SEND, MAX_VERIFY);
    }

    @Test
    void primeiraChamada_permite() {
        when(redis.execute(any(RedisScript.class), any(List.class), any(String.class)))
                .thenReturn(1L);
        assertTrue(limiter.trySend("1.2.3.4"));
    }

    @Test
    void acimaDoLimite_deveBloquear() {
        when(redis.execute(any(RedisScript.class), any(List.class), any(String.class)))
                .thenReturn((long) (MAX_SEND + 1));
        assertFalse(limiter.trySend("1.2.3.4"));
    }

    @Test
    void redisIndisponivel_permitePorFailOpen() {
        when(redis.execute(any(RedisScript.class), any(List.class), any(String.class)))
                .thenThrow(new RuntimeException("redis down"));
        assertTrue(limiter.trySend("1.2.3.4"));
    }
}
```

- [ ] **Step 2: Rodar para ver falhar**

Run: `.\mvnw.cmd test -Dtest=OtpRateLimiterTest`
Expected: FAIL — classe não existe.

- [ ] **Step 3: Implementar o limiter**

```java
package com.becommerce.crm.infrastructure.otp.rate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Rate limiter distribuído para o fluxo de autenticação por telefone/OTP.
 * Mesma política do {@code InvitationRateLimiter}: janela fixa atômica via
 * Lua (INCR + EXPIRE) no Redis e fail-open controlado quando o Redis está
 * indisponível (nunca derruba o fluxo de login legítimo).
 */
@Component
public class OtpRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(OtpRateLimiter.class);

    private static final int DEFAULT_MAX_SEND_PER_MINUTE = 5;
    private static final int DEFAULT_MAX_VERIFY_PER_MINUTE = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final String KEY_PREFIX = "crm:ratelimit:otp:";

    private static final DefaultRedisScript<Long> INCR_EXPIRE = new DefaultRedisScript<>("""
            local count = redis.call("incr", KEYS[1])
            if count == 1 then
                redis.call("expire", KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redis;
    private final int maxSendPerWindow;
    private final int maxVerifyPerWindow;

    public OtpRateLimiter(StringRedisTemplate redis,
                          @org.springframework.beans.factory.annotation.Value(
                                  "${app.otp.rate-limit.send-per-minute:" + DEFAULT_MAX_SEND_PER_MINUTE + "}") int maxSend,
                          @org.springframework.beans.factory.annotation.Value(
                                  "${app.otp.rate-limit.verify-per-minute:" + DEFAULT_MAX_VERIFY_PER_MINUTE + "}") int maxVerify) {
        this.redis = redis;
        this.maxSendPerWindow = maxSend;
        this.maxVerifyPerWindow = maxVerify;
    }

    public OtpRateLimiter(StringRedisTemplate redis, int maxSend, int maxVerify) {
        this.redis = redis;
        this.maxSendPerWindow = maxSend;
        this.maxVerifyPerWindow = maxVerify;
    }

    public boolean trySend(String clientIp) {
        return permit("send", clientIp, maxSendPerWindow);
    }

    public boolean tryVerify(String phoneE164) {
        return permit("verify", phoneE164, maxVerifyPerWindow);
    }

    private boolean permit(String bucket, String key, int max) {
        long windowSeconds = Math.max(WINDOW.toSeconds(), 1);
        long bucketStart = Instant.now().getEpochSecond() / windowSeconds;
        String redisKey = KEY_PREFIX + bucket + ":" + key + ":" + bucketStart;
        Long count;
        try {
            count = redis.execute(INCR_EXPIRE, List.of(redisKey), String.valueOf(windowSeconds));
        } catch (Exception e) {
            log.warn("OTP rate limiter unavailable (Redis down), allowing request: bucket={} error={}",
                    bucket, e.getClass().getSimpleName());
            return true;
        }
        return count == null || count <= max;
    }
}
```

- [ ] **Step 4: Integrar no controller**

```java
import com.becommerce.crm.infrastructure.otp.rate.OtpRateLimiter;
import org.springframework.web.bind.annotation.RequestHeader;

private static final int TOO_MANY_REQUESTS = 429;

    private final OtpRateLimiter otpRateLimiter;

    public PhoneAuthController(PhoneAuthUseCase phoneAuthUseCase, OtpService otpService, OtpRateLimiter otpRateLimiter) {
        this.phoneAuthUseCase = phoneAuthUseCase;
        this.otpService = otpService;
        this.otpRateLimiter = otpRateLimiter;
    }

    @PostMapping(value = "/send-otp", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> sendOtp(
            @Valid @RequestBody SendOtpRequest request,
            @RequestHeader(value = "X-Real-IP", required = false) String realIp) {
        String ip = realIp != null && !realIp.isBlank() ? realIp : "unknown";
        if (!otpRateLimiter.trySend(ip)) {
            return ResponseEntity.status(TOO_MANY_REQUESTS).build();
        }
        PhoneAuthUseCase.SendOtpResult result = phoneAuthUseCase.sendOtp(request.phone());
        return result.sent() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    @PostMapping(value = "/verify-otp", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        if (!otpRateLimiter.tryVerify(request.phone())) {
            return ResponseEntity.status(TOO_MANY_REQUESTS).build();
        }
        PhoneAuthUseCase.VerifyOtpResult result = phoneAuthUseCase.verifyOtp(request.phone(), request.otp());
        return result.success() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }
```
Nota: `X-Real-IP` já é repassado pelo nginx na `location /api/v1/auth/phone/` (confirmado no nginx da VPS). O teste de controller deve mockar `OtpRateLimiter` retornando `true`.

- [ ] **Step 5: Rodar testes e commit**

Run: `.\mvnw.cmd test -Dtest=OtpRateLimiterTest,PhoneAuthControllerTest`
Expected: PASS.
```bash
git add backend/src/main/java/com/becommerce/crm/infrastructure/otp/rate/OtpRateLimiter.java \
        backend/src/main/java/com/becommerce/crm/presentation/rest/identity/PhoneAuthController.java \
        backend/src/test/java/com/becommerce/crm/infrastructure/otp/rate/OtpRateLimiterTest.java \
        backend/src/test/java/com/becommerce/crm/presentation/rest/identity/PhoneAuthControllerTest.java
git commit -m "fix(auth): rate limit distribuído no fluxo OTP por IP/telefone"
```

---

### Task 1.3: Validação `@Valid` nos endpoints de auth

**Contexto:** `AuthController` só valida em `register`. `forgot-password`, `reset-password` e `change-password` aceitam body sem Bean Validation. `users/invite` idem (verificar). Alinhar com o padrão existente.

**Files:**
- Modify: `backend/src/main/java/com/becommerce/crm/presentation/rest/identity/AuthController.java`
- Modify: `backend/src/main/java/com/becommerce/crm/presentation/rest/identity/UserController.java` (método invite)
- Modify: DTOs (`application/identity/dto/*.java`): `ForgotPasswordRequest`, `ResetPasswordRequest`, `ChangePasswordRequest`, `InviteUserRequest`
- Modify tests: `backend/src/test/java/com/becommerce/crm/presentation/rest/identity/*ControllerTest.java`

- [ ] **Step 1: Verificar DTOs atuais**

Run: `rg -n "record (ForgotPasswordRequest|ResetPasswordRequest|ChangePasswordRequest|InviteUserRequest)" backend/src/main/java/com/becommerce/crm/application/identity/dto/`
Expected: localizar os records. Eles não têm anotações `@NotBlank`/`@Email` hoje (confirmar e anotar).

- [ ] **Step 2: Anotar os DTOs** (exemplo — aplicar o mesmo padrão aos 4 records listados)

```java
    public record ForgotPasswordRequest(
            @NotBlank @Email String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }
```
Ajustar conforme o domínio: o value object `domain/identity/valueobject/Password.java` já valida senha (força BCrypt strength=12 no uso); o `@Size` do DTO é camada de validação rápida — não duplicar regra conflitante com o `Password`.

- [ ] **Step 3: Adicionar `@Valid` nos controllers**

```java
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal CurrentUser principal,
                                               @Valid @RequestBody ChangePasswordRequest request) {
```

- [ ] **Step 4: Teste que valida 400 em payload inválido**

Em `AuthControllerTest` (ou criar, seguindo `WhatsAppWebhookControllerTest` como base standalone):
```java
    @Test
    void forgotPassword_comEmailInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nao-e-email\"}"))
                .andExpect(status().isBadRequest());
    }
```
- [ ] **Step 5: Rodar todos os testes de auth e commit**

Run: `.\mvnw.cmd test -Dtest='*Auth*Test,*User*Test'`
Expected: PASS (com os mocks de `AuthUseCase` — lembrar que `@Valid` falha ANTES de chamar o use case).
```bash
git add backend/src/main/java/com/becommerce/crm/presentation/rest/identity/AuthController.java \
        backend/src/main/java/com/becommerce/crm/presentation/rest/identity/UserController.java \
        backend/src/main/java/com/becommerce/crm/application/identity/dto/
git commit -m "fix(auth): adicionar Bean Validation nos fluxos forgot/reset/change-password e invite"
```

---

### Task 1.4: Não vazar mensagens internas no `GlobalExceptionHandler`

**Contexto:** o handler devolve `ex.getMessage()` na maioria dos endpoints (ex.: `IllegalStateException`, `InvalidCredentialsException`, `IdentityServiceUnavailableException`, `UserProvisioningException`). Mensagens internas (detalhes de infra/causas) vazam para o cliente. A regra: exceções de **domínio** continuam retornando sua mensagem (ex.: "Lead não encontrado"), pois são intencionais e seguras; exceções de **infra/gerais** (sem código de domínio) devolvem mensagem genérica e logam o detalhe server-side.

**Files:**
- Modify: `backend/src/main/java/com/becommerce/crm/presentation/rest/handler/GlobalExceptionHandler.java`
- Modify: `backend/src/test/java/com/becommerce/crm/presentation/rest/handler/GlobalExceptionHandlerTest.java`

- [ ] **Step 1: Escrever teste que falha (mensagem genérica em infra)**

```java
    @Test
    void identityServiceUnavailable_deveRetornarMensagemGenerica() throws Exception {
        mockMvc.perform(get("/test/identity-unavailable"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Serviço de identidade indisponível, tente novamente."));
    }
```
- [ ] **Step 2: Adicionar helper seguro e aplicar**

Introduzir no handler:
```java
    /** Mensagem controlada para exceções de infraestrutura — nunca expõe causa interna. */
    private static String safeInfraMessage(String generic) {
        return generic;
    }
```
Trocar nas respostas dos handlers de **infra** (`IllegalStateException`, `IdentityServiceUnavailableException`, `UserProvisioningException`, `InvalidCredentialsException`, `Exception`):
- `IllegalStateException` → `"Operação não pode ser concluída."` + `log.warn("IllegalState: {}", ex.getMessage(), ex);`
- `IdentityServiceUnavailableException` → `"Serviço de identidade indisponível, tente novamente."` + log ERROR com o `ex.getMessage()` original.
- `InvalidCredentialsException` → `"Credenciais inválidas."`
- `UserProvisioningException` → `"Não foi possível concluir o cadastro, tente novamente."` + log.
- `Exception` (geral) já está genérico — manter.

As exceções de domínio (NotFound/Validation/etc.) **permanecem** com `ex.getMessage()` — são DTOs controlados.

- [ ] **Step 3: Rodar testes e commit**

Run: `.\mvnw.cmd test -Dtest=GlobalExceptionHandlerTest`
Expected: PASS.
```bash
git add backend/src/main/java/com/becommerce/crm/presentation/rest/handler/GlobalExceptionHandler.java \
        backend/src/test/java/com/becommerce/crm/presentation/rest/handler/GlobalExceptionHandlerTest.java
git commit -m "fix(api): não vazar mensagens internas em respostas de erro"
```

---

### Task 1.5: Restringir default do CORS

**Contexto:** `application.yml:100` tem `allowed-origins: ${CORS_ALLOWED_ORIGINS:*}`. Sem override, qualquer origem é aceita (embora os cookies HttpOnly não sejam lidos cross-origin, headers como `Authorization` em dev/outros envs poderiam). Tornar o default explícito.

**Files:**
- Modify: `backend/src/main/resources/application.yml` (linha 100)
- Modify: `backend/src/main/resources/application-prod.yml` (se definir CORS, manter o valor atual)
- Modify: `backend/src/main/resources/application-test.yml` (se necessário)

- [ ] **Step 1: Mudar o default**

```yaml
app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:}
```
- [ ] **Step 2: Confirmar que o código de CORS trata lista vazia como "sem origens" (não como "*")**

Run: `rg -rn "allowed-origins|AllowedOriginPatterns|CorsConfiguration" backend/src/main/java/com/becommerce/crm/infrastructure/security/`
Expected: localizar a classe de configuração CORS. Se ela fizer `allowedOriginPatterns("*")` quando vazio, ajustar para: lista vazia → nenhum padrão permitido (bloqueia cross-origin sem configuração; o frontend Next.js dev via proxy NÃO precisa de CORS pois é mesma origem).

- [ ] **Step 3: Teste de configuração**

Run: `.\mvnw.cmd test -Dtest='*SecurityConfig*Test,*Cors*Test'` — se inexistente, rodar a suíte principal `.\mvnw.cmd verify` (sem ITs: `-DskipITs`).
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/resources/application.yml backend/src/main/resources/application-prod.yml backend/src/main/resources/application-test.yml backend/src/main/java/com/becommerce/crm/infrastructure/security/
git commit -m "fix(security): CORS sem origem wildcard por padrão"
```

---

### Task 1.6: Logar exceções no `KeycloakJwtAuthenticationConverter`

**Contexto:** `catch (Exception ignored) {}` engole falhas de parsing de roles — sem log, diagnóstico impossível e autorização vazia silenciosa.

**Files:**
- Modify: `backend/src/main/java/com/becommerce/crm/infrastructure/security/config/KeycloakJwtAuthenticationConverter.java`
- Modify: `backend/src/test/java/com/becommerce/crm/infrastructure/security/config/KeycloakJwtAuthenticationConverterTest.java` (se existir; senão criar)

- [ ] **Step 1: Adicionar logger e trocar catch**

```java
    private static final Logger log = LoggerFactory.getLogger(KeycloakJwtAuthenticationConverter.class);

        } catch (Exception e) {
            log.warn("Falha ao extrair realm roles do JWT: {}", e.getMessage(), e);
        }
        return Collections.emptyList();
```
(Idem para `extractClientRoles`.)

- [ ] **Step 2: Teste que valida log (sem ter que mockar logger — verificar comportamento inalterado)**

Garantir que o comportamento público não muda:
```java
    @Test
    void convert_comTokenSemRoles_retornaUsuarioSemAutoridades() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").claim("sub", "u").build();
        when(currentUserResolver.resolve(any())).thenReturn(currentUser);
        KeycloakJwtAuthenticationConverter converter = new KeycloakJwtAuthenticationConverter(currentUserResolver);
        UsernamePasswordAuthenticationToken auth = converter.convert(jwt);
        assertNotNull(auth);
    }
```
- [ ] **Step 3: Rodar testes e commit**

Run: `.\mvnw.cmd test -Dtest=KeycloakJwtAuthenticationConverterTest`
Expected: PASS.
```bash
git add backend/src/main/java/com/becommerce/crm/infrastructure/security/config/KeycloakJwtAuthenticationConverter.java \
        backend/src/test/java/com/becommerce/crm/infrastructure/security/config/KeycloakJwtAuthenticationConverterTest.java
git commit -m "fix(security): logar falhas de extração de roles no converter JWT"

---

### Task 1.7: Higiene do repositório (lixo + `.github` + `.gitignore`)

**Files:**
- Delete: `backend/fix_test.py`, `backend/fix_webhook.py`, `backend/tmp_apply.sh`, `backend/hs_err_pid14156.log`, `backend/hs_err_pid3680.log`, `backend/replay_pid14156.log` (validar quais existem primeiro)
- Delete: arquivos de tooling de IA em `.github/` (UUIDs `*.json` e scripts `recordToolUse.*`)
- Modify: `.gitignore` (raiz e `backend/.gitignore` se houver)

- [ ] **Step 1: Inventariar**

Run: `git status --short` e `git ls-files | rg -i "hs_err|replay_pid|fix_test|fix_webhook|tmp_apply|recordToolUse|\.json$" | rg -i "backend|\.github" | head -50`
Expected: lista exata de arquivos a remover (confirmar untracked vs tracked — se untracked, basta excluir do disco; se tracked, `git rm`).

- [ ] **Step 2: Remover**

```bash
# untracked (existem no status como ??): apenas excluir do disco
Remove-Item backend/fix_test.py,backend/fix_webhook.py -ErrorAction Continue
# tracked/committed (se listados por git ls-files): remover do índice
git rm --ignore-unmatch backend/tmp_apply.sh 'backend/hs_err_pid*.log' 'backend/replay_pid*.log' .github/*.json .github/recordToolUse.ps1 .github/recordToolUse.sh
```

- [ ] **Step 3: Reforçar `.gitignore`**

Adicionar ao `.gitignore` raiz:
```gitignore
# Lixo de debug/JVM
hs_err_pid*.log
replay_pid*.log

# Scripts ad-hoc locais
fix_test.py
fix_webhook.py
tmp_apply.sh

# Tooling de IA local
.github/recordToolUse.*
```

- [ ] **Step 4: Verificar que nada quebrou**

Run: `git status --short` (somente os arquivos pretendidos) e `git commit`:
```bash
git add .gitignore
git add -u
git commit -m "chore: remover artefatos de debug e tooling de IA; reforçar .gitignore"
```

---

### Task 1.8: Hardening operacional da VPS (env, Keycloak, backup, swap, healthcheck, sessão, limpeza)

**Files (repo):**
- Modify: `scripts/` — criar `scripts/backup-db.sh` e `scripts/restore-db.sh`
- Modify: `docker/docker-compose.yml` — healthcheck do backend
- Modify: `.env.example` — documentar `AUTH_GATEWAY_SESSION_IDLE_TIMEOUT`

**Files (VPS, via ssh crm-vps):**
- `/opt/crm/.env`, `/opt/crm/docker/.env` (permissões 600, `WHATSAPP_WEBHOOK_ALLOW_UNSIGNED=false` já na Task 1.1)
- `/opt/crm/docker-compose.yml` (raiz da infra: remover `KC_BOOTSTRAP_ADMIN_*`)
- `/opt/crm/docker/docker-compose.yml` (healthcheck backend, `AUTH_GATEWAY_SESSION_IDLE_TIMEOUT=4h`)
- Crontab (backup diário)

- [ ] **Step 1: `.env` com permissão 600 (VPS)**

```bash
ssh crm-vps "chmod 600 /opt/crm/.env /opt/crm/docker/.env && ls -l /opt/crm/.env /opt/crm/docker/.env"
```
Expected: `-rw------- 1 root root`.

- [ ] **Step 2: Remover bootstrap do Keycloak (VPS)**

```bash
ssh crm-vps "rg -n 'KC_BOOTSTRAP' /opt/crm/docker-compose.yml"
```
Editar `/opt/crm/docker-compose.yml`: remover as duas linhas `KC_BOOTSTRAP_ADMIN_USERNAME` e `KC_BOOTSTRAP_ADMIN_PASSWORD` (a senha de admin já foi definida e fica no console do Keycloak). Aplicar: `cd /opt/crm && docker compose up -d keycloak` (aceitar recreate com healthcheck). Validar com `docker ps | grep keycloak` → healthy.

- [ ] **Step 3: Healthcheck do backend (repo + VPS)**

No repo, `docker/docker-compose.yml`, serviço `backend`:
```yaml
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://localhost:8080/actuator/health >/dev/null 2>&1 || exit 1"]
      interval: 30s
      timeout: 5s
      retries: 3
```
Aplicar na VPS em `/opt/crm/docker/docker-compose.yml` (mesmo bloco) e `cd /opt/crm/docker && docker compose up -d backend`. Validar `docker ps` → `crm-backend` healthy.

- [ ] **Step 4: Sessões ociosas do gateway (VPS)**

No `.env` da infra ou no compose app, definir:
```
AUTH_GATEWAY_SESSION_IDLE_TIMEOUT=4h
```
No repo, documentar no `docker/.env.example` e `docker-compose.yml` do app com comentário de intenção. Aplicar e reiniciar o auth-service: `cd /opt/crm/docker && docker compose up -d auth-service`. Validar login/logout manual rápido.

- [ ] **Step 5: Criar scripts de backup (repo)**

`scripts/backup-db.sh`:
```bash
#!/usr/bin/env bash
# Backup diário dos bancos do CRM (crm + keycloak_db) — postgres:17-alpine.
# Uso: BACKUP_DIR=/opt/crm/backups RETENTION_DAYS=7 ./backup-db.sh
set -euo pipefail
POSTGRES_CONTAINER=${POSTGRES_CONTAINER:-crm-postgres}
POSTGRES_USER=${POSTGRES_USER:-crm_admin}
BACKUP_DIR=${BACKUP_DIR:-/opt/crm/backups}
RETENTION_DAYS=${RETENTION_DAYS:-7}
STAMP=$(date +%Y%m%d-%H%M%S)
mkdir -p "$BACKUP_DIR"
for DB in crm keycloak_db; do
  docker exec "$POSTGRES_CONTAINER" pg_dump -U "$POSTGRES_USER" -d "$DB" --format=custom -f "/tmp/${DB}.dump"
  docker cp "$POSTGRES_CONTAINER:/tmp/${DB}.dump" "$BACKUP_DIR/${DB}-${STAMP}.dump"
  docker exec "$POSTGRES_CONTAINER" rm -f "/tmp/${DB}.dump"
done
# Rotação
find "$BACKUP_DIR" -name '*.dump' -mtime "+${RETENTION_DAYS}" -delete
echo "Backup concluído em $BACKUP_DIR ($(date))"
```
`scripts/restore-db.sh`:
```bash
#!/usr/bin/env bash
# Restore pontual: ./restore-db.sh <banco> <arquivo.dump>
set -euo pipefail
DB=${1:?uso: restore-db.sh <banco> <arquivo.dump>}
DUMP=${2:?uso: restore-db.sh <banco> <arquivo.dump>}
docker cp "$DUMP" crm-postgres:/tmp/restore.dump
docker exec crm-postgres pg_restore -U crm_admin -d "$DB" --clean --if-exists /tmp/restore.dump
docker exec crm-postgres rm -f /tmp/restore.dump
echo "Restore de $DB concluído."
```
Tornar executáveis (`chmod +x`) e adicionar ao `.gitignore` se o diretório `backups/` for gerado no repo (não é — executa na VPS).

- [ ] **Step 6: Instalar na VPS + cron + swap**

```bash
# instalar scripts
scp scripts/backup-db.sh scripts/restore-db.sh crm-vps:/opt/crm/scripts/ && ssh crm-vps "chmod +x /opt/crm/scripts/backup-db.sh /opt/crm/scripts/restore-db.sh"

# primeiros backup manual + teste de restore (container temporário) — UMA vez
ssh crm-vps "/opt/crm/scripts/backup-db.sh"
ssh crm-vps "docker run --rm -v crm-backup-test:/tmp/postgres -e POSTGRES_PASSWORD=x postgres:17-alpine ... " # ou: criar banco crm_restore_test e rodar restore-db.sh crm_restore_test <dump>
# Validar que SELECT count(*) das tabelas principais > 0 no banco restaurado.

# cron diário (0h30)
ssh crm-vps "(crontab -l 2>/dev/null; echo '30 0 * * * /opt/crm/scripts/backup-db.sh >> /var/log/crm-backup.log 2>&1') | crontab -"

# swap 2G persistente
ssh crm-vps "fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile && echo '/swapfile none swap sw 0 0' >> /etc/fstab && free -h"
```
Expected: `free -h` mostra swap; `swapon --show` lista; crontab contém o job.

- [ ] **Step 7: Limpeza de árvores órfãs (VPS)**

```bash
ssh crm-vps "
  cd /root && tar czf /root/crm-backup-forest-$(date +%Y%m%d).tar.gz \
    local-main-tree local-main-tree.old crm-pre8.4-untracked-moved crm-backup-pre8.4 local-main.tar index-sync.tar.gz 2>/dev/null || true
  rm -rf /opt/crm-backup- /root/crm-deploy-backup /root/local-main-tree /root/local-main-tree.old \
         /root/crm-pre8.4-untracked-moved /root/crm-backup-pre8.4 /root/local-main.tar /root/index-sync.tar.gz
  rm -f '/root/NUL' '/root/=' '/root/\.lesshst' /root/vps-hotfix-*.patch
  df -h /
"
```
Expected: disco liberado; `/root` contém apenas `out/`, `.docker/`, `.m2/`, `crm-backup-forest-*.tar.gz` e o ambiente normal.

- [ ] **Step 8: Rodar `git status` e commit do repo**

Run: `git status --short` (deve conter apenas `scripts/backup-db.sh`, `scripts/restore-db.sh`, `docker/docker-compose.yml`, `.env.example`)
```bash
git add scripts/backup-db.sh scripts/restore-db.sh docker/docker-compose.yml .env.example docker/.env.example
git commit -m "chore(ops): backup/restore scripts, healthcheck e default de sessão ociosa"

---

## FASE 2 — Qualidade UX do frontend

> **Contexto de build:** todos os passos frontend rodam na pasta `frontend/`: `npm run typecheck`, `npm run lint`, `npm test` (Vitest) e `npm run format` (prettier) antes de commitar.

### Task 2.1: Busca server-side de leads (corrigir bug de página)

**Contexto:** em `src/app/(dashboard)/leads/page.tsx:93-108`, `filteredLeads` filtra apenas `data?.content` (página corrente). Buscar em `page > 0` não encontra leads de outras páginas. A API jpa já rotula por status/source/classification; falta o termo `search`. Decisão (spec 2.1): busca no servidor, de ponta a ponta.

**Files (backend):**
- Modify: `backend/src/main/java/com/becommerce/crm/presentation/rest/lead/LeadController.java` (adicionar `@RequestParam(required=false) String search`)
- Modify: `backend/src/main/java/com/becommerce/crm/application/lead/port/input/LeadUseCase.java` (assinatura `list`)
- Modify: `backend/src/main/java/com/becommerce/crm/application/lead/service/LeadService.java:141-155`
- Modify: `backend/src/main/java/com/becommerce/crm/application/lead/port/output/LeadRepository.java` (`findByCompanyWithFilters`)
- Modify: `backend/src/main/java/com/becommerce/crm/infrastructure/lead/persistence/LeadRepositoryImpl.java:46-61`
- Modify: `backend/src/main/java/com/becommerce/crm/infrastructure/lead/persistence/LeadJpaRepository.java:17-26`
- Modify test: `backend/src/test/java/com/becommerce/crm/infrastructure/lead/persistence/LeadIsolationIT.java`

**Files (frontend):**
- Modify: `src/features/leads/types/lead.types.ts` (`ListLeadsParams` adicionar `search?`)
- Modify: `src/features/leads/hooks/useLeads.ts` (pass through — já aceita params)
- Modify: `src/app/(dashboard)/leads/page.tsx` (remover `filteredLeads` client-side; passar `search` ao `useLeads`)

- [ ] **Step 1 (backend): TDD da busca na query JPA**

Escrever teste de integração em `LeadIsolationIT`:
```java
    @Test
    void list_comSearch_retornaLeadsQueBatemComContato() {
        // cria contato "João da Silva" + lead; e outro contato "Maria" + lead
        PageResponse<LeadResponse> page = leadUseCase.list(companyId, null, null, null, "joão", 0, 10, "createdAt", "desc");
        assertEquals(1, page.totalElements());
    }
```
Run: `.\mvnw.cmd test -Dtest=LeadIsolationIT` → FAIL (search não é passado).

- [ ] **Step 2 (backend): Implementar**

`LeadJpaRepository`:
```java
    @Query("SELECT l FROM LeadJpaEntity l LEFT JOIN ContactJpaEntity c ON c.id = l.contactId " +
            "WHERE l.companyId = :companyId " +
            "AND (:status IS NULL OR :status = '' OR l.status = :status) " +
            "AND (:source IS NULL OR :source = '' OR l.source = :source) " +
            "AND (:classification IS NULL OR :classification = '' OR l.classification = :classification) " +
            "AND (:search IS NULL OR :search = '' OR LOWER(COALESCE(c.firstName,'')) LIKE LOWER(:like) " +
            "     OR LOWER(COALESCE(c.lastName,'')) LIKE LOWER(:like) " +
            "     OR LOWER(COALESCE(c.email,'')) LIKE LOWER(:like) " +
            "     OR LOWER(COALESCE(c.phone,'')) LIKE LOWER(:like))")
    Page<LeadJpaEntity> findByCompanyWithFilters(
            @Param("companyId") UUID companyId,
            @Param("status") String status,
            @Param("source") String source,
            @Param("classification") String classification,
            @Param("search") String search,
            Pageable pageable);
```
`LeadRepositoryImpl.findByCompanyWithFilters`: adicionar parâmetro `String search` e montar `like` no `Specification`/`PageRequest`: `String like = "%" + (search == null ? "" : search.trim().toLowerCase()) + "%";` e chamar `jpaRepository.findByCompanyWithFilters(companyId, status, source, classification, like, pageRequest)`. Propagar `search` nas assinaturas de `LeadRepository`, `LeadUseCase.list` e `LeadService.list` (passando `like` já formatado para a query).
`LeadController.list`: adicionar `@RequestParam(required = false) String search` e repassar.

- [ ] **Step 3 (backend): rodar e passar**

Run: `.\mvnw.cmd verify` (ou `-DskipITs` para unit) — Expected: PASS.

- [ ] **Step 4 (frontend): tipos + página**

`lead.types.ts` — `ListLeadsParams`:
```ts
export interface ListLeadsParams {
  page?: number;
  pageSize?: number;
  status?: LeadStatus;
  source?: LeadSource;
  classification?: LeadClassification;
  sortBy?: string;
  sortDirection?: "asc" | "desc";
  search?: string;
}
```
`leads/page.tsx`: remover `filteredLeads` (linhas 93-108), o agrupamento para usar `data?.content ?? []`, e incluir no `useLeads(...)`:
```ts
  const { data, isLoading, error, refetch } = useLeads(companyId, {
    page,
    pageSize: 10,
    search: search.trim() || undefined,
    status: status !== "all" ? (status as LeadStatus) : undefined,
    source: source !== "all" ? (source as LeadSource) : undefined,
    classification: classification !== "all" ? (classification as LeadClassification) : undefined,
    sortBy: "createdAt",
    sortDirection: "desc",
  });
```
Trocar todas as referências a `filteredLeads` por `data?.content ?? []`. Manter `setPage(0)` ao digitar.

- [ ] **Step 5 (frontend): testes + format + commit**

Criar/ajustar teste em `src/features/leads/hooks/useLeads.test.ts` para o novo parâmetro `search` (mock de `LeadService.list` retorna página com 1 item) e em `src/features/leads/services/lead.service.test.ts` validar que `api.get` recebe `params.search`. Rodar `npm test` (filtrado a leads), `npm run typecheck`, `npm run lint`, `npm run format`.
```bash
git add -A
git commit -m "fix(leads): busca server-side de leads por nome/email/telefone"
```

---

### Task 2.2: Remover busca decorativa do Header

**Files:**
- Modify: `src/components/layout/Header.tsx` (remover o Button "Pesquisar..." e imports `Search`, `Command`)

- [ ] **Step 1: Remover**

Apagar o bloco `{/* Search (UI only) */} <Button ...>...</Button>` e os imports não usados (`Search`, `Command`).

- [ ] **Step 2: Verificar**

Run: `npm run typecheck`, `npm test` (componente Header não tem teste próprio; `ProtectedRoute`/layout não dependem dele). Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add src/components/layout/Header.tsx
git commit -m "chore(header): remover busca decorativa (UI only)"
```

---

### Task 2.3: Remover `deals.mock.ts`

**Files:**
- Delete: `src/features/pipeline/data/deals.mock.ts`
- Modify: dependentes encontrados por grep de `deals.mock` / `dealsMock` / `mockDeals`

- [ ] **Step 1: Achar dependências**

Run: `rg -n "deals.mock|dealsMock|mockDeals|from.*pipeline/data/deals" src/`
Expected: lista de imports. Se `pipeline/page.tsx` usa dados mock como seed em vez da API, substituir por dados reais vindo de `useOpportunities`/`usePipelines` (indicar em que trecho) para validar que a página não regride.

- [ ] **Step 2: Remover e atualizar**

`Remove-Item src/features/pipeline/data/deals.mock.ts` e ajustar os imports/uso encontrados (se um componente usa mock como fallback de UI, trocar por `EmptyState`).

- [ ] **Step 3: Verificar e commit**

Run: `npm run typecheck`, `npm test`, `npm run lint`.
```bash
git add -A
git commit -m "chore(pipeline): remover mock de deals deixado no código"
```

---

### Task 2.4: Estados de loading/error/empty em 4 páginas

**Contexto:** `channels`, `storage`, `pipeline` e `follow-up-sequences` hoje só renderizam `Loader2`/`EmptyState` sem tratamento de erro. Aplicar o padrão das páginas-modelo (leads/tenants/contacts), usando componentes compartilhados existentes.

**Files:**
- Modify: `src/app/(dashboard)/channels/page.tsx`
- Modify: `src/app/(dashboard)/storage/page.tsx`
- Modify: `src/app/(dashboard)/pipeline/page.tsx`
- Modify: `src/app/(dashboard)/follow-up-sequences/page.tsx`

- [ ] **Step 1: Padrão a aplicar (exemplo concreto para `channels/page.tsx`)**

Substituir o bloco de renderização por:
```tsx
  const { data, isLoading, error, refetch } = useChannels(companyId);

  if (isLoading) return <SkeletonTable rows={5} />;
  if (error) return <ErrorCard message={error.message} onRetry={() => refetch()} />;
  if (!data || data.length === 0) {
    return (
      <EmptyState
        icon={<MessageSquareOff className="h-8 w-8" />}
        title="Nenhum canal configurado"
        description="Conecte um canal de WhatsApp para começar a receber mensagens."
      />
    );
  }
```
Imports: `SkeletonTable` de `@/components/feedback/SkeletonTable`, `ErrorCard` de `@/components/common/ErrorCard`, `EmptyState` de `@/components/common/EmptyState` (todos já existem). Manter `PageTitle`/header existente fora do bloco condicional.

- [ ] **Step 2: Aplicar o mesmo em `storage`, `pipeline`, `follow-up-sequences`**

Mesmo padrão, adaptando título/ícone/descrição de cada empty state e o hook usado (`useStorageObjects`, `usePipelines`/`useOpportunities`, `useFollowUpSequences`).

- [ ] **Step 3: Verificar e commit**

Run: `npm run typecheck`, `npm test`, `npm run lint`, `npm run format`.
```bash
git add -A
git commit -m "feat(pages): padronizar loading/error/empty em channels/storage/pipeline/follow-up-sequences"
```

---

### Task 2.5: Consolidar rotas legadas `(authenticated)` → `(dashboard)/settings`

**Contexto:** duas vias para RBAC: `(authenticated)/roles*` + `/permissions` e `(dashboard)/settings/roles` (+ `settings/users`). Spec 2.5: mover para `(dashboard)/settings/*`, eliminar o route group legado, atualizar `ROUTES`.

**Files:**
- Move: `src/app/(authenticated)/roles/` → `src/app/(dashboard)/settings/roles/` (e `roles/new`, `roles/[id]`, `roles/[id]/edit`)
- Move: `src/app/(authenticated)/permissions/` → `src/app/(dashboard)/settings/permissions/`
- Delete: `src/app/(authenticated)/layout.tsx`
- Modify: `src/lib/constants.ts` (`ROUTES.ROLES`, `ROUTES.ROLES_NEW`, `ROUTES.PERMISSIONS` → apontar para `/settings/roles`, `/settings/roles/new`, `/settings/permissions`)
- Modify: `src/components/layout/Sidebar.tsx` (links existentes para `/roles` → `ROUTES.SETTINGS_ROLES` já existente ou novo)
- Modify: quaisquer `router.push(ROUTES.ROLES...)` encontrados

- [ ] **Step 1: Inventariar referências**

Run: `rg -n "ROUTES\.ROLES|ROUTES\.PERMISSIONS|/roles|/permissions" src/ --glob '!**/*.test.*' | rg -v 'settings/roles|settings/users|settings/agent'`
Expected: lista de pontos a atualizar.

- [ ] **Step 2: Mover páginas**

Usar `git mv` (preserva histórico):
```bash
git mv "src/app/(authenticated)/roles" "src/app/(dashboard)/settings/roles"
git mv "src/app/(authenticated)/permissions" "src/app/(dashboard)/settings/permissions"
Remove-Item "src/app/(authenticated)/layout.tsx" -ErrorAction Continue
git rm --ignore-unmatch "src/app/(authenticated)/layout.tsx"
```
Lembrar: `(dashboard)/settings/roles/page.tsx` já existe? Confirmar — `settings/roles` aparece no mapa (settings/roles). Se JÁ existir página em `(dashboard)/settings/roles`, então **mover o que falta** (new/[id]/[id]/edit), mesclar e remover duplicado. O executor deve `ls src/app/(dashboard)/settings/` primeiro e ajustar (isso é a decisão da task — eliminar a duplicação escolhendo o caminho canônico `/settings/roles`).

- [ ] **Step 3: Atualizar constantes**

```ts
  ROLES: "/settings/roles",
  ROLES_NEW: "/settings/roles/new",
  PERMISSIONS: "/settings/permissions",
  SETTINGS_ROLES: "/settings/roles",
```
Ajustar `ROUTES.ROLES_NEW` e usos de `ROLES.ROLES_NEW` para `/settings/roles/new` (o grupo edit/[id] mantém `/settings/roles/[id]`).

- [ ] **Step 4: Atualizar referências e verificar**

Trocar `ROUTES.ROLES`, `ROUTES.ROLES_NEW`, `ROUTES.PERMISSIONS` nos pontos do Step 1 para os novos valores. Rodar `npm run typecheck`, `npm test`, `npm run lint`. Adicionar teste de navegação se o Sidebar tiver (verificar `Sidebar`/`navigation` já testados).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor(routes): consolidar RBAC em /settings/roles e /settings/permissions"
```

---

### Task 2.6: Quebrar `design-system/page.tsx` (1199 linhas)

**Files:**
- Create: `src/app/(dashboard)/design-system/sections/` com `TypographySection.tsx`, `ColorsSection.tsx`, `ButtonSection.tsx`, `FormSection.tsx`, `FeedbackSection.tsx`, `DataDisplaySection.tsx`, `OverlaySection.tsx`
- Modify: `src/app/(dashboard)/design-system/page.tsx` (compor as seções)

- [ ] **Step 1: Estruturar**

Mover cada bloco atual da página (identificável pelos `SectionTitle`/comentários de seção) para um componente `*Section.tsx` com a mesma marcação. A página passa a:
```tsx
export default function DesignSystemPage() {
  return (
    <div className="space-y-16">
      <TypographySection />
      <ColorsSection />
      <ButtonSection />
      <FormSection />
      <FeedbackSection />
      <DataDisplaySection />
      <OverlaySection />
    </div>
  );
}
```
- [ ] **Step 2: Verificar**

Run: `npm run typecheck` e navegação manual em `/design-system` (dev) para conferir que as seções renderizam (sem teste visual automático; existe página, não componente). `npm test` para não regredir o resto.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "refactor(design-system): fatiar showcases em seções por categoria"
```

---

### Task 2.7: Quebrar `TenantForm.tsx` (479 linhas)

**Files:**
- Create: `src/features/tenants/components/tenant-form/CompanyInfoFields.tsx`, `SettingsFields.tsx`, `QuotaFields.tsx`, `TenantFormSection.tsx`
- Modify: `src/features/tenants/components/TenantForm.tsx` (compor seções)

- [ ] **Step 1: Fatiar**

Extrair 3 grupos de campos (dados da empresa, settings/planos, quota/billing) em componentes de campos controlados (`Controller` do react-hook-form já usado). Cada componente expõe props `control` (tal como o hook usa). `TenantFormSection` encapsula título + `Card`. Verificar o `tenant.schema` para nomes de campos exatos.

- [ ] **Step 2: Verificar**

Run: `npm test` (se houver teste de TenantForm, atualizar imports), `npm run typecheck`, `npm run lint`.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "refactor(tenants): quebrar TenantForm em seções de campos"
```

---

### Task 2.8: Quebrar `WorkflowForm.tsx` (419 linhas)

**Files:**
- Create: `src/features/workflows/components/workflow-form/ConditionBuilder.tsx`, `ActionBuilder.tsx`, `ExecutionList.tsx`, `WorkflowFormSection.tsx`
- Modify: `src/features/workflows/components/WorkflowForm.tsx` (compor)

- [ ] **Step 1: Fatiar**

Extrair: (a) builder de condições (condições atuais, add/remove, validação), (b) builder de ações (tipo+ação, input dinâmico por ação), (c) lista de execuções recentes, (d) wrapper de seção. Conferir `workflow.schema.ts` para nomes de campos e tipos. Manter o `handleSubmit`/validação na página.

- [ ] **Step 2: Verificar**

Run: `npm test` (testes de schema/hook de workflow), `npm run typecheck`, `npm run lint`.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "refactor(workflows): extrair builders de condições/ações e lista de execuções"
```

---

### Task 2.9: Quebrar `Sidebar.tsx` (426 linhas)

**Files:**
- Create: `src/components/layout/SidebarGroup.tsx`, `src/components/layout/SidebarItem.tsx`, `src/components/layout/navigation.ts`
- Modify: `src/components/layout/Sidebar.tsx`

- [ ] **Step 1: Extrair dados + subcomponentes**

`navigation.ts` exporta a estrutura de navegação copiada do corpo atual do `Sidebar`:
```ts
import type { LucideIcon } from "lucide-react";
import { Users, ... } from "lucide-react";
import { ROUTES } from "@/lib/constants";

export interface NavItem { label: string; href: string; icon: LucideIcon; permission?: string; }
export interface NavGroup { title: string; items: NavItem[]; permission?: string; }

export const NAVIGATION: NavGroup[] = [ /* conteúdo extraído do Sidebar */ ];
```
`SidebarItem.tsx`: item único (ícone + label + active). `SidebarGroup.tsx`: título + itens (controla `aria-expanded`, estado colapsado). `Sidebar.tsx` itera `NAVIGATION` renderizando `SidebarGroup`.

- [ ] **Step 2: Verificar**

Run: `npm test` (existe `ProtectedRoute.test` e testes de layout; se houver `Sidebar.test`, atualizar imports), `npm run typecheck`.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "refactor(layout): extrair SidebarGroup/SidebarItem e dados de navegação"
```

---

### Task 2.10: Quebrar `data-table.tsx` (402 linhas)

**Files:**
- Create: `src/components/ui/data-table/DataTablePagination.tsx`, `DataTableSkeleton.tsx`, `DataTableRowActions.tsx`, `DataTableEmpty.tsx`
- Modify: `src/components/ui/data-table.tsx` (recompor usando os subcomponentes)

- [ ] **Step 1: Extrair**

Mover para subcomponentes: paginação (a seção "Anterior/Próximo"/páginas), o skeleton de carregamento, o dropdown de ações por linha e o empty state. **API pública do componente não muda** (export `DataTable` com as props atuais usadas por `LeadTable`, `ContactTable`, `DealTable` etc.).

- [ ] **Step 2: Verificar**

Run: `npm test` — `src/components/ui/data-table.test.tsx` existe e valida a API pública. Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "refactor(ui): desmembrar data-table em subcomponentes"
```

---

### Task 2.11: Mutation helper único (padrão de hooks)

**Contexto:** todos os hooks repetem `onSuccess → invalidateQueries + toast.success` e `onError → toast.error(error.message || fallback)`; `aiErrorMessage`/`aiAnalysisErrorMessage` duplicam o mapeamento de erro por status.

**Files:**
- Create: `src/lib/query/mutation-utils.ts`
- Modify: hooks de features (começar por leads — `useLeads.ts`; replicar em users, tenants, tasks, workflows, contacts, campaigns)
- Modify: `src/features/ai/services/ai.service.ts` (unificar mapeamento de erro)

- [ ] **Step 1: Criar helper**

```ts
import { QueryClient, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

export interface MutationOptions<TData = unknown, TError = Error, TVariables = void, TContext = unknown> {
  successMessage?: string;
  invalidateKeys?: unknown[][];
  extraInvalidate?: (queryClient: QueryClient, data: TData, variables: TVariables) => void;
  silentError?: boolean;
}

export function useMutationDefaults<
  TData,
  TVariables = void,
  TError = { response?: { data?: { message?: string } } } & Error = never,
>() {
  const queryClient = useQueryClient();
  return {
    queryClient,
    onSuccess: (opts: MutationOptions<TData, TError, TVariables>) => (data: TData, variables: TVariables) => {
      opts.invalidateKeys?.forEach((key) => queryClient.invalidateQueries({ queryKey: key }));
      opts.extraInvalidate?.(queryClient, data, variables);
      if (opts.successMessage) toast.success(opts.successMessage);
    },
    onError: (opts: MutationOptions<TData, TError, TVariables>) => (error: TError) => {
      if (opts.silentError) return;
      const message = (error as { response?: { data?: { message?: string } } }).response?.data?.message
        ?? (error as Error).message
        ?? "Algo deu errado";
      toast.error(message);
    },
  };
}
```

- [ ] **Step 2: Aplicar em `useLeads.ts` (exemplo completo)**

```ts
export function useCreateLead(companyId: string | null) {
  const { queryClient, onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (data: CreateLeadRequest) => LeadService.create(companyId as string, data),
    onSuccess: onSuccess({
      successMessage: "Lead criado com sucesso",
      invalidateKeys: [["leads", companyId]],
    }),
    onError: onError({}),
  });
}

export function useUpdateLead(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateLeadRequest }) =>
      LeadService.update(companyId as string, id, data),
    onSuccess: onSuccess({
      successMessage: "Lead atualizado com sucesso",
      invalidateKeys: [["leads", companyId]],
      extraInvalidate: (qc, _d, { id }) => qc.invalidateQueries({ queryKey: ["leads", companyId, id] }),
    }),
    onError: onError({}),
  });
}
```

- [ ] **Step 3: Replicar nos demais hooks**

O mesmo esqueleto nos hooks de `users`, `tenants`, `tasks`, `workflows`, `contacts`, `campaigns` (arquivos `src/features/<feature>/hooks/*.ts`), substituindo as mensagens de sucesso atuais (mantendo o texto pt-BR atual de cada toast).

- [ ] **Step 4: Unificar mapeamento de erro de IA**

Em `ai.service.ts`, extrair um único `aiErrorMessage(status)` usado por `aiErrorMessage` e `aiAnalysisErrorMessage`.

- [ ] **Step 5: Verificar e commit**

Run: `npm test`, `npm run typecheck`, `npm run lint`, `npm run format` (ajustar asserts de toast/hooks nos testes existentes — `useLeads.test.ts` etc. validam toast e invalidação).
```bash
git add -A
git commit -m "refactor(hooks): mutation helper único para onSuccess/invalidate/toast"

---

## FASE 3 — Arquitetura / backend

> **Contexto de build:** comandos rodam em `backend/` com `.\mvnw.cmd`. Suítes de guarda: `*IsolationIT` (integração) e checkstyle (`.\mvnw.cmd verify` executa ambos). Não tocar identidade (PostgreSQL/Keycloak) nem estrutura de `docs/`.

### Task 3.1: Centralizar autorização de tenant (remover tríplice duplicação)

**Contexto:** hoje o acesso ao tenant de um recurso é verificado em até 3 lugares: `requireCompanyAccess` no controller (ex.: `LeadController`), o `TenantContext` no service, e RLS no Postgres. O objetivo é ter UMA verificação de tenant no service (via `TenantContext`), `@PreAuthorize` por permissão no método, e RLS como última barreira. Nenhuma duplicação nova deve ser introduzida e `*IsolationIT` permanece verde.

**Files (candidatos — inventariar no Step 1):**
- Modify: controllers que hoje usam `requireCompanyAccess` (ex.: `backend/src/main/java/com/becommerce/crm/presentation/rest/lead/LeadController.java`)
- Modify: `backend/src/main/java/com/becommerce/crm/infrastructure/security/config/TenantContext.java` (se necessário para propagar companyId consistente)
- Modify: services sem `TenantContext` (adicionar escopo de companyId)

- [ ] **Step 1: Inventariar (as-is, antes de codar)**

```bash
rg -rn "requireCompanyAccess|TenantContext|@PreAuthorize" backend/src/main/java --glob '*.java' | Cut -d: -f1 | Sort-Object -Unique
```
Expected: lista de controllers vs services vs gateways. Registrar: (a) recursos com tríplice verificação, (b) recursos com apenas 1-2 verificações (vendor lock potencial), (c) controllers onde `@PreAuthorize` já cobre a rota (ex.: leads usam `lead:read`).

- [ ] **Step 2: Decidir o mecanismo único (anotar no commit) e garantir guardas**

Baseline de guarda ANTES de qualquer mudança:
```bash
.\mvnw.cmd verify -DskipTests && .\mvnw.cmd test -Dtest='*IsolationIT'
```
Expected: verde. Se alguma `*IsolationIT` já falhar, registrar no commit como débito e seguir só após env. verde.
Mecanismo: adicionar `@CurrentCompanyId` + resolver que lê o `companyId` do JWT (uma única fonte) em vez de ler do corpo/path em cada controller. O service faz `tenantContext.require(companyId, recurso)` (mesma semântica do `requireCompanyAccess` atual, movida para o service). Controllers deixam de chamar `requireCompanyAccess` e passam a depender do `@PreAuthorize` + argumento `@CurrentCompanyId`.

- [ ] **Step 3: Aplicar recurso a recurso (TDD por iteração)**

Para cada recurso `X` do Step 1:
1. Escrever/determinar teste de isolamento em `XIsolationIT` que tenta acessar recurso de OUTRA empresa → hoje já deve estar protegido por alguma das 3 camadas.
2. Mover a checagem de tenant do controller para o service (via `TenantContext./@CurrentCompanyId`), mantendo o teste verde.
3. Garantir `@PreAuthorize('<recurso>:<ação>')` no método (ex.: se `lead:read` existe, usar; se faltar permissão alguma, anotar débito no commit e documentar).
4. Rodar `.\mvnw.cmd test -Dtest='*IsolationIT'` — Expected: verde a cada passo.
Começar por leads (mais coberto), seguir para contacts, opportunities, tasks, workflows, e os demais listados. Parar ao esgotar a lista — commits em lotes de ~1-3 recursos.

- [ ] **Step 4: Verificação e commits**

```bash
.\mvnw.cmd verify
git status --short
git add -A && git commit -m "refactor(authn): centralizar verificação de tenant no service (via TenantContext + @CurrentCompanyId)"
```
Critério: sem duplicação nova (na listagem do Step 1, nenhum controller novo passa a replicar checagem de tenant), `*IsolationIT` verde, checkstyle verde.

---

### Task 3.2: Consistência na casa (validação, mensagens, logs)

**Contexto:** durante a Task 3.1 serão notadas inconsistências (validations ausentes, mensagens de erro divergentes, logs sem contexto). Curar o que for encontrado, SEM tocar identidade/docs e sem fugir do escopo.

**Files:** os apontados pela varredura.

- [ ] **Step 1: Varredura de inconsistências**

```bash
rg -rn "throw new (IllegalArgumentException|IllegalStateException|BusinessRuleException)\(" backend/src/main/java --glob '*.java'
rg -rn "ResponseStatusException|@Valid|@Validated" backend/src/main/java/com/becommerce/crm/presentation/rest --glob '*.java'
rg -rn "logger\.(error|warn)" backend/src/main/java --glob '*.java' | rg -v "companyId|tenantId|userId"
```
Expected: lista de (a) mensagens de regra em pt-BR com grafia inconsistente (ex.: "nao" vs "não", "registro"/"recurso"), (b) rotas PUT/PATCH sem `@Valid`, (c) logs sem contexto de empresa/usuário.

- [ ] **Step 2: Curar por categoria**

1. **Validação:** adicionar `@Valid` a rotas PUT/PATCH que recebem `@RequestBody` (dentro do grupo de segurança — sem tocar identidade). Teste: nenhum teste de validação existente quebra.
2. **Mensagens:** normalizar mensagens de regra business para o padrão do `GlobalExceptionHandler`/`BusinessRuleException` já existente (memo do padrão que já domina; unificar o restante).
3. **Logs:** adicionar `companyId={}` / `userId={}` aos logs de erro/warn que tratam recursos de tenant.

- [ ] **Step 3: Verificar e commit**

```bash
.\mvnw.cmd verify
git status --short
git add -A && git commit -m "refactor(business): consistentes validações (@Valid), mensagens e logs de tenant"
```

---

## FASE 4 — Testes E2E (Playwright)

> **Contexto:** rodar em `frontend/`. Ambiente: stack completa local (docker compose de dev ou os 8 containers da VPS via `ngrok` não — usar o `webServer` do Playwright). Base: `http://localhost:3000` (frontend) gerido pelo `webServer` do playwright.config; backend `http://localhost:8081/backend`. Dados: seeds/fixtures via API ou script, nunca produção.

### Task 4.1: Setup do Playwright (config + scripts + CI)

**Files:**
- Create: `frontend/playwright.config.ts`
- Create: `frontend/e2e/` (specs das próximas tasks), `frontend/e2e/fixtures/`
- Modify: `frontend/package.json` (devDependencies `@playwright/test`, scripts)
- Modify: workflow CI existente (`.github/workflows/*.yml`) — adicionar job e2e

- [ ] **Step 1: Instalar e configurar**

```bash
npm install -D @playwright/test
npx playwright install chromium
```
`frontend/playwright.config.ts`:
```ts
import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1, // JPA/Keycloak e VITE_MODE contrato: um usuário por vez evita corrida de OTP/2FA
  retries: process.env.CI ? 2 : 0,
  reporter: process.env.CI ? [["html", { open: "never" }]] : "list",
  use: {
    baseURL: "http://localhost:3000",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  webServer: {
    command: "npm run build && npm run start",
    url: "http://localhost:3000",
    reuseExistingServer: !process.env.CI,
    timeout: 180_000,
  },
  projects: [
    { name: "chromium", use: { ...devices["Desktop Chrome"] } },
  ],
});
```
`package.json`:
```json
  "scripts": {
    "e2e": "playwright test",
    "e2e:ui": "playwright test --ui"
  }
```
CI (.github/workflows/ci-e2e.yml): job `e2e` com `npm ci`, `npx playwright install --with-deps chromium`, `npm run e2e`, upload do HTML report em falha.

- [ ] **Step 2: Smoke spec de prova**

`frontend/e2e/smoke.spec.ts`: carrega `/login`, espera o título "Entrar"/"Login" (confirmar o texto real na página de login) e navega a `/health` (ou rota pública real). Rodar `npm run e2e` — Expected: 1 pass.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "test(e2e): setup Playwright com webServer e job no CI"
```

---

### Task 4.2: Flows de login/logout

**Files:**
- Create: `frontend/e2e/auth.spec.ts`, `frontend/e2e/fixtures/users.json`

- [ ] **Step 1: Seed de usuário E2E**

Criar usuário de teste (via API do backend ou script de seed) com e-mail fixo `e2e.admin@crm.local` e senha conhecida — ALTA prioridade definir via env `E2E_ADMIN_*` para não colidir com dados reais. `fixtures/users.json` guarda os valores usados em testes.

- [ ] **Step 2: Specs**

`auth.spec.ts`:
```ts
test("login com credenciais válidas redireciona para o dashboard", async ({ page }) => {
  await page.goto("/login");
  await page.getByLabel("E-mail").fill(process.env.E2E_ADMIN_EMAIL!);
  await page.getByLabel("Senha").fill(process.env.E2E_ADMIN_PASSWORD!);
  await page.getByRole("button", { name: "Entrar" }).click();
  await expect(page).toHaveURL(/\/dashboard/);
});

test("credenciais inválidas mostram erro e não autentica", async ({ page }) => {
  await page.goto("/login");
  await page.getByLabel("E-mail").fill("nao-existe@crm.local");
  await page.getByLabel("Senha").fill("errada");
  await page.getByRole("button", { name: "Entrar" }).click();
  await expect(page.getByText(/credenciais|inválid/i)).toBeVisible();
});

test("logout retorna ao login", async ({ page }) => {
  await login(page); // helper em fixtures/auth.ts
  await page.getByRole("button", { name: /sair/i }).click();
  await expect(page).toHaveURL(/\/login/);
});
```
Criar helper `frontend/e2e/fixtures/auth.ts` com `login(page)` reutilizável. Ajustar labels/roles ao real (verificar `src/app/(auth)/login/page.tsx`).

- [ ] **Step 3: Rodar e estabilizar**

`npm run e2e -- e2e/auth.spec.ts` — Expected: 3 pass. Se o fluxo requer OTP e o gateway exige 2FA só com `SESSION_IDLE_TIMEOUT` zerado — anotar env `AUTH_GATEWAY_SESSION_IDLE_TIMEOUT` para o teste não depender de exceção.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "test(e2e): fluxos de login, erro e logout"
```

---

### Task 4.3: Specs de negócio (leads, pipeline, inbox, members/roles)

**Files:**
- Create: `frontend/e2e/leads.spec.ts`, `frontend/e2e/pipeline.spec.ts`, `frontend/e2e/inbox.spec.ts`, `frontend/e2e/settings-roles.spec.ts`

- [ ] **Step 1: Seed de dados (fixture)**

Seed por API (`POST /backend/...` com token do E2E) ou via DB; `frontend/e2e/fixtures/seed.ts` cria: 1 empresa, 5 contatos, 3 leads em estágios variados. Limpar ao fim (afterAll) para isolamento.

- [ ] **Step 2: Specs**

`leads.spec.ts`:
- Lista renderiza leads do seed (contar linhas da tabela)
- Criar lead pelo formulário → toast de sucesso e linha nova na tabela
- Buscar por termo no campo de busca → só linhas correspondentes (valida a Task 2.1 server-side)
- Filtro por status → contagem correta

`pipeline.spec.ts`:
- Kanban renderiza colunas dos estágios
- Arrastar/mover oportunidade de coluna → toast confirma mudança de estágio (ou via modal se drag for frágil)

`inbox.spec.ts`:
- Inbox lista conversas do seed; abrir conversa mostra mensagens; envio de mensagem replica no thread (verificar se inbox é realtime — sem realtime, validar resposta visual do envio)

`settings-roles.spec.ts`:
- Navegar a `/settings/roles` renderiza a lista; criar role com permissão; editar nome; remover

- [ ] **Step 3: Rodar e estabilizar**

`npm run e2e` — Expected: todos pass. Tratar flakiness com `getByRole`/`toBeVisible` (nunca sleep), `expect.poll`/`toBeVisible({ timeout })` para estados pós-mutação.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "test(e2e): leads, pipeline, inbox e members/roles"
```

---

### Task 4.4: Estabilidade e isolamento

**Files:**
- Modify: `frontend/e2e/fixtures/*` e specs (ajustar conforme achados)

- [ ] **Step 1: Auditoria de isolamento**

Garantir: nunca atingir produção (baseURL fixo local; nenhum `page.goto` com URL externa), `workers: 1`, seeds criados/limpos por spec, e nenhum teste que depende da ordem (executar com `--shard` ou ordem aleatória no CI: `npm run e2e -- --shard=1/1 --forbid-only`).

- [ ] **Step 2: Pipeline no CI com artefatos**

Roda full suite no PR job `e2e` (CI): retries 2, HTML report uploaded em falha. Validar rodando o job no CI (ou local com `process.env.CI=1 npm run e2e`).

- [ ] **Step 3: Commit final**

```bash
npm run typecheck && npm run lint && npm test
git add -A
git commit -m "test(e2e): estabilidade e isolamento dos specs"
```

---

## Critério de conclusão global do plano

- Fase 1 aplicada e verificada na VPS (Task 1.1 rollout + Task 1.8) com ratings de segurança repetíveis.
- Fases 2-4 com suítes verdes: backend `.\mvnw.cmd verify` (≈700 unit + `*IsolationIT`), auth-service (≈280), frontend `npm test` + `typecheck` + `lint` (≈128), e2e `npm run e2e` verde.
- VPS saudável após cada deploy: `docker ps` todos healthy, backup diário rodando (1 execução validada por restore de teste).
- Nenhuma feature nova além do escopo aprovado (realtime/i18n ficam no roadmap).