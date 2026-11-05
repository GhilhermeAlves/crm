# Leo pelo Telegram — plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Conversar com o Leo pelo Telegram (texto e áudio), com vínculo por usuário e confirmação de ações por botões.

**Architecture:** O webhook público em `/api/v1/telegram/webhook/{companyId}` é validado pelo header secreto do Telegram e repassa cada update para um processador assíncrono. O processador resolve o usuário vinculado (permissões do banco, mesma regra do `LocalCurrentUserResolver`) e chama `AiAssistantUseCase.chat` / `AiActionUseCase.confirm|cancel` sem alterar o texto. Tudo fica no pacote novo `communication/telegram`.

**Tech Stack:** Spring Boot 3 (RestClient, JPA, Flyway, `@Async`), PostgreSQL com RLS, Next.js + React Query (frontend-refine), JUnit 5 + Mockito + MockMvc + MockRestServiceServer.

**Spec:** `docs/superpowers/specs/2026-11-04-leo-telegram-design.md`

## Global Constraints

- Pacote backend: `com.becommerce.crm.communication.telegram` (subpacotes `domain`, `application`, `infrastructure`, `web`).
- Migration: `V128__telegram_leo.sql`. Antes de criar, confira se `V128` continua livre (`ls backend/src/main/resources/db/migration | sort -V | tail -3`).
- Todas as tabelas novas: `ENABLE` + `FORCE ROW LEVEL SECURITY`, `tenant_isolation_policy` por `app.current_tenant_id()`, `GRANT SELECT, INSERT, UPDATE, DELETE ... TO crm_app` (padrão V116).
- Token do bot cifrado com `MailPasswordCipher` (AES-GCM). O token nunca é devolvido pela API nem gravado em log.
- O texto do usuário é repassado ao Leo **sem nenhuma alteração** (nem trim).
- Toda guarda nova deixa o `CRM_ADMIN` passar.
- Permissões: configurar o bot exige `company:update`, e vincular o próprio Telegram exige `ai:chat`.
- Testes de backend: **o usuário roda**. Cada passo de teste informa o comando (`cd backend && mvn -q test -Dtest=<Classe>`), e o executor não roda `mvn test`.
- Frontend: depois de alterar, rode `npx prettier --check --end-of-line auto <arquivos>` em `frontend-refine`.
- Commits locais na branch `feat/leo-telegram`. Sem push.
- Textos ao usuário em pt-BR.

## Mapa de arquivos

| Arquivo | Responsabilidade |
|---|---|
| `backend/src/main/resources/db/migration/V128__telegram_leo.sql` | tabelas + RLS |
| `communication/telegram/domain/TelegramBotConfig.java` (+ `TelegramLink`, `TelegramLinkCode`) | entidades JPA |
| `communication/telegram/infrastructure/*JpaRepository.java` | repositórios Spring Data |
| `communication/telegram/infrastructure/TelegramBotClient.java` | chamadas HTTP à Bot API |
| `communication/telegram/application/TelegramConfigService.java` | salvar/remover token, setWebhook |
| `communication/telegram/application/TelegramLinkService.java` | código de vínculo, vincular, desvincular |
| `communication/telegram/application/TelegramUserResolver.java` | vínculo → usuário ativo + permissões |
| `communication/telegram/application/TelegramUpdateProcessor.java` | comandos, texto, voz, callbacks |
| `communication/telegram/application/TelegramRateLimiter.java` | 20 mensagens/min por remetente |
| `communication/telegram/web/TelegramWebhookController.java` | webhook público |
| `communication/telegram/web/TelegramSettingsController.java` | config + vínculo (autenticado) |
| `shared/security/config/SecurityConfig.java` | `permitAll` do webhook |
| `frontend-refine/src/features/communication/telegram/*` | cards de configuração e vínculo |

(Os caminhos Java ficam abaixo de `backend/src/main/java/com/becommerce/crm/`, e os testes, abaixo de `backend/src/test/java/com/becommerce/crm/`.)

---

### Task 1: Migration e entidades

**Files:**
- Create: `backend/src/main/resources/db/migration/V128__telegram_leo.sql`
- Create: `communication/telegram/domain/TelegramBotConfig.java`, `TelegramLink.java`, `TelegramLinkCode.java`
- Create: `communication/telegram/infrastructure/TelegramBotConfigJpaRepository.java`, `TelegramLinkJpaRepository.java`, `TelegramLinkCodeJpaRepository.java`, `TelegramUpdateSeenJpaRepository.java`

**Interfaces:**
- Produces: as entidades e os repositórios abaixo, com estes nomes exatos.

- [ ] **Step 1: Escrever a migration**

```sql
-- V128__telegram_leo.sql
-- Leo pelo Telegram: bot por clínica, vínculo usuário↔Telegram, códigos de
-- vínculo e idempotência de updates. Token cifrado (AES-GCM) pela aplicação.

CREATE TABLE telegram_bot_config (
    company_id          UUID PRIMARY KEY REFERENCES companies(id) ON DELETE CASCADE,
    bot_token_cipher    TEXT NOT NULL,
    webhook_secret_hash VARCHAR(64) NOT NULL,
    bot_username        VARCHAR(64) NOT NULL,
    enabled             BOOLEAN NOT NULL DEFAULT true,
    updated_by          UUID,
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE telegram_link (
    id                 UUID PRIMARY KEY,
    company_id         UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id            UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    telegram_user_id   BIGINT NOT NULL,
    chat_id            BIGINT NOT NULL,
    telegram_name      VARCHAR(255),
    ai_conversation_id UUID,
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_telegram_link_tg UNIQUE (company_id, telegram_user_id),
    CONSTRAINT uq_telegram_link_user UNIQUE (company_id, user_id)
);

CREATE TABLE telegram_link_code (
    code_hash  VARCHAR(64) PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    used_at    TIMESTAMP
);
CREATE INDEX idx_telegram_link_code_user ON telegram_link_code(company_id, user_id);

CREATE TABLE telegram_update_seen (
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    update_id  BIGINT NOT NULL,
    seen_at    TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (company_id, update_id)
);

ALTER TABLE telegram_bot_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE telegram_bot_config FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON telegram_bot_config
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON telegram_bot_config TO crm_app;

ALTER TABLE telegram_link ENABLE ROW LEVEL SECURITY;
ALTER TABLE telegram_link FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON telegram_link
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON telegram_link TO crm_app;

ALTER TABLE telegram_link_code ENABLE ROW LEVEL SECURITY;
ALTER TABLE telegram_link_code FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON telegram_link_code
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON telegram_link_code TO crm_app;

ALTER TABLE telegram_update_seen ENABLE ROW LEVEL SECURITY;
ALTER TABLE telegram_update_seen FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON telegram_update_seen
    USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id());
GRANT SELECT, INSERT, UPDATE, DELETE ON telegram_update_seen TO crm_app;
```

- [ ] **Step 2: Conferir as colisões.** Rode `grep -rniE "telegram_(bot_config|link|link_code|update_seen)" backend/src/main/resources/db/migration`. Só a V128 deve aparecer.

- [ ] **Step 3: Escrever as entidades.** Siga o estilo de `masterdata/company/mail/CompanyMailSettingsJpaEntity.java`: `@Entity`, `@Table`, getters e setters, sem Lombok se o arquivo de referência não usar.

```java
// communication/telegram/domain/TelegramBotConfig.java
@Entity @Table(name = "telegram_bot_config")
public class TelegramBotConfig {
    @Id @Column(name = "company_id") private UUID companyId;
    @Column(name = "bot_token_cipher", nullable = false) private String botTokenCipher;
    @Column(name = "webhook_secret_hash", nullable = false) private String webhookSecretHash;
    @Column(name = "bot_username", nullable = false) private String botUsername;
    @Column(nullable = false) private boolean enabled = true;
    @Column(name = "updated_by") private UUID updatedBy;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt = LocalDateTime.now();
    // getters/setters
}

// communication/telegram/domain/TelegramLink.java
@Entity @Table(name = "telegram_link")
public class TelegramLink {
    @Id private UUID id = UUID.randomUUID();
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "telegram_user_id", nullable = false) private long telegramUserId;
    @Column(name = "chat_id", nullable = false) private long chatId;
    @Column(name = "telegram_name") private String telegramName;
    @Column(name = "ai_conversation_id") private UUID aiConversationId;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    // getters/setters
}

// communication/telegram/domain/TelegramLinkCode.java
@Entity @Table(name = "telegram_link_code")
public class TelegramLinkCode {
    @Id @Column(name = "code_hash") private String codeHash;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Column(name = "used_at") private LocalDateTime usedAt;
    // getters/setters
}
```

- [ ] **Step 4: Escrever os repositórios**

```java
public interface TelegramBotConfigJpaRepository extends JpaRepository<TelegramBotConfig, UUID> {}

public interface TelegramLinkJpaRepository extends JpaRepository<TelegramLink, UUID> {
    Optional<TelegramLink> findByCompanyIdAndTelegramUserId(UUID companyId, long telegramUserId);
    Optional<TelegramLink> findByCompanyIdAndUserId(UUID companyId, UUID userId);
    List<TelegramLink> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
}

public interface TelegramLinkCodeJpaRepository extends JpaRepository<TelegramLinkCode, String> {
    @Modifying @Query("delete from TelegramLinkCode c where c.companyId = :companyId and c.userId = :userId")
    void deleteByCompanyIdAndUserId(UUID companyId, UUID userId);
}

public interface TelegramUpdateSeenJpaRepository extends Repository<Object, Long> {
    /** Retorna 1 se gravou (update novo) e 0 se já existia. */
    @Modifying @Query(value = "INSERT INTO telegram_update_seen(company_id, update_id) VALUES (:companyId, :updateId) ON CONFLICT DO NOTHING", nativeQuery = true)
    int markSeen(UUID companyId, long updateId);

    @Modifying @Query(value = "DELETE FROM telegram_update_seen WHERE seen_at < :before", nativeQuery = true)
    int deleteOlderThan(LocalDateTime before);
}
```

Se o Spring Data recusar `Repository<Object, Long>`, use um `@Repository` com `JdbcTemplate` e esses dois métodos.

- [ ] **Step 5: Compilar.** Peça ao usuário para rodar `cd backend && mvn -q -DskipTests compile`. O esperado é `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/resources/db/migration/V128__telegram_leo.sql backend/src/main/java/com/becommerce/crm/communication/telegram
git commit -m "feat(telegram): tabelas e entidades do Leo pelo Telegram"
```

---

### Task 2: Cliente da Bot API

**Files:**
- Create: `communication/telegram/infrastructure/TelegramBotClient.java`
- Test: `communication/telegram/infrastructure/TelegramBotClientTest.java`

**Interfaces:**
- Produces:
  - `String getMe(String token)`, que retorna o username do bot ou lança `TelegramApiException`
  - `void setWebhook(String token, String url, String secret)`
  - `void deleteWebhook(String token)`
  - `long sendMessage(String token, long chatId, String text, List<List<Button>> keyboard)`, que retorna `message_id`; com `keyboard` nulo, não envia teclado
  - `void editMessage(String token, long chatId, long messageId, String text)`, que remove o teclado
  - `void answerCallback(String token, String callbackId)`
  - `byte[] downloadFile(String token, String fileId)`, que faz `getFile` e depois baixa o arquivo
  - `record Button(String text, String callbackData)`
  - `class TelegramApiException extends RuntimeException`
- Mensagens acima de 4096 caracteres: `sendMessage` divide em partes, e só a última leva o teclado.

- [ ] **Step 1: Escrever o teste que falha**

```java
class TelegramBotClientTest {
    private MockRestServiceServer server;
    private TelegramBotClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TelegramBotClient(builder, "https://api.telegram.org");
    }

    @Test
    void getMeReturnsUsername() {
        server.expect(requestTo("https://api.telegram.org/botTOK/getMe"))
              .andRespond(withSuccess("{\"ok\":true,\"result\":{\"username\":\"leo_bot\"}}", MediaType.APPLICATION_JSON));
        assertEquals("leo_bot", client.getMe("TOK"));
    }

    @Test
    void getMeInvalidTokenThrows() {
        server.expect(requestTo("https://api.telegram.org/botBAD/getMe"))
              .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"ok\":false}").contentType(MediaType.APPLICATION_JSON));
        assertThrows(TelegramBotClient.TelegramApiException.class, () -> client.getMe("BAD"));
    }

    @Test
    void sendMessageSendsTextUnchangedWithKeyboard() {
        server.expect(requestTo("https://api.telegram.org/botTOK/sendMessage"))
              .andExpect(jsonPath("$.chat_id").value(42))
              .andExpect(jsonPath("$.text").value("  marca a Maria  "))
              .andExpect(jsonPath("$.reply_markup.inline_keyboard[0][0].callback_data").value("c:1"))
              .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":7}}", MediaType.APPLICATION_JSON));
        long id = client.sendMessage("TOK", 42, "  marca a Maria  ",
                List.of(List.of(new TelegramBotClient.Button("✅ Confirmar", "c:1"))));
        assertEquals(7, id);
    }

    @Test
    void sendMessageSplitsLongText() {
        String longText = "a".repeat(5000);
        server.expect(ExpectedCount.times(2), requestTo("https://api.telegram.org/botTOK/sendMessage"))
              .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":1}}", MediaType.APPLICATION_JSON));
        client.sendMessage("TOK", 1, longText, null);
        server.verify();
    }

    @Test
    void downloadFileResolvesPathThenDownloads() {
        server.expect(requestTo("https://api.telegram.org/botTOK/getFile?file_id=F1"))
              .andRespond(withSuccess("{\"ok\":true,\"result\":{\"file_path\":\"voice/a.oga\"}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.telegram.org/file/botTOK/voice/a.oga"))
              .andRespond(withSuccess(new byte[]{1, 2, 3}, MediaType.APPLICATION_OCTET_STREAM));
        assertArrayEquals(new byte[]{1, 2, 3}, client.downloadFile("TOK", "F1"));
    }
}
```

- [ ] **Step 2: Peça ao usuário para rodar** `cd backend && mvn -q test -Dtest=TelegramBotClientTest`. O esperado é FAIL, porque a classe ainda não existe.

- [ ] **Step 3: Implementar**

```java
@Component
public class TelegramBotClient {
    static final int MAX_LEN = 4096;
    private final RestClient rest;
    private final String baseUrl;

    public TelegramBotClient(RestClient.Builder builder,
                             @Value("${app.telegram.api-base-url:https://api.telegram.org}") String baseUrl) {
        this.rest = builder.build();
        this.baseUrl = baseUrl;
    }

    public record Button(String text, String callbackData) {}

    public static class TelegramApiException extends RuntimeException {
        public TelegramApiException(String message, Throwable cause) { super(message, cause); }
    }

    public String getMe(String token) {
        JsonNode r = call(token, "getMe", null);
        return r.path("username").asText();
    }

    public void setWebhook(String token, String url, String secret) {
        call(token, "setWebhook", Map.of("url", url, "secret_token", secret,
                "allowed_updates", List.of("message", "callback_query")));
    }

    public void deleteWebhook(String token) { call(token, "deleteWebhook", Map.of()); }

    public long sendMessage(String token, long chatId, String text, List<List<Button>> keyboard) {
        List<String> parts = split(text);
        long last = 0;
        for (int i = 0; i < parts.size(); i++) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("chat_id", chatId);
            body.put("text", parts.get(i));
            if (keyboard != null && i == parts.size() - 1) body.put("reply_markup", markup(keyboard));
            last = call(token, "sendMessage", body).path("message_id").asLong();
        }
        return last;
    }

    public void editMessage(String token, long chatId, long messageId, String text) {
        call(token, "editMessageText", Map.of("chat_id", chatId, "message_id", messageId,
                "text", text.length() > MAX_LEN ? text.substring(0, MAX_LEN) : text));
    }

    public void answerCallback(String token, String callbackId) {
        call(token, "answerCallbackQuery", Map.of("callback_query_id", callbackId));
    }

    public byte[] downloadFile(String token, String fileId) {
        try {
            JsonNode r = rest.get().uri(baseUrl + "/bot" + token + "/getFile?file_id=" + fileId)
                    .retrieve().body(JsonNode.class);
            String path = r.path("result").path("file_path").asText();
            return rest.get().uri(baseUrl + "/file/bot" + token + "/" + path).retrieve().body(byte[].class);
        } catch (RestClientException e) {
            throw new TelegramApiException("Falha ao baixar arquivo do Telegram", e);
        }
    }

    private JsonNode call(String token, String method, Object body) {
        try {
            var spec = body == null
                    ? rest.get().uri(baseUrl + "/bot" + token + "/" + method)
                    : rest.post().uri(baseUrl + "/bot" + token + "/" + method)
                          .contentType(MediaType.APPLICATION_JSON).body(body);
            JsonNode r = spec.retrieve().body(JsonNode.class);
            if (r == null || !r.path("ok").asBoolean()) throw new TelegramApiException("Telegram recusou " + method, null);
            return r.path("result");
        } catch (RestClientException e) {
            // Nunca inclua o token na mensagem.
            throw new TelegramApiException("Falha ao chamar Telegram: " + method, e);
        }
    }

    private static Map<String, Object> markup(List<List<Button>> keyboard) {
        return Map.of("inline_keyboard", keyboard.stream().map(row -> row.stream()
                .map(b -> Map.of("text", b.text(), "callback_data", b.callbackData())).toList()).toList());
    }

    static List<String> split(String text) {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < text.length(); i += MAX_LEN) parts.add(text.substring(i, Math.min(text.length(), i + MAX_LEN)));
        if (parts.isEmpty()) parts.add(text);
        return parts;
    }
}
```

Observação: `rest.get()` e `rest.post()` retornam tipos diferentes (`RequestHeadersUriSpec` e `RequestBodySpec`). Se `var spec` não compilar, separe em dois ramos com um `retrieve()` em cada.

- [ ] **Step 4: Peça ao usuário para rodar o mesmo comando.** O esperado é PASS.

- [ ] **Step 5: Commit** com `git commit -m "feat(telegram): cliente da Bot API"`.

---

### Task 3: Configuração do bot (token + webhook)

**Files:**
- Create: `communication/telegram/application/TelegramConfigService.java`
- Create: `communication/telegram/web/TelegramSettingsController.java` (endpoints de config; a Task 4 adiciona os de vínculo)
- Modify: `backend/src/main/resources/application.yml` (bloco `app.telegram`)
- Test: `communication/telegram/application/TelegramConfigServiceTest.java`, `communication/telegram/web/TelegramSettingsControllerTest.java`

**Interfaces:**
- Consumes: `TelegramBotClient` (Task 2), `TelegramBotConfigJpaRepository` (Task 1), `MailPasswordCipher`
- Produces:
  - `record TelegramConfigResponse(boolean configured, String botUsername, boolean enabled, LocalDateTime updatedAt)`
  - `TelegramConfigResponse get(UUID companyId)`
  - `TelegramConfigResponse save(UUID companyId, String token, UUID userId)`
  - `void delete(UUID companyId)`
  - `Optional<String> tokenIfValidSecret(UUID companyId, String providedSecret)`, que retorna o token decifrado só se o segredo bater e a integração estiver ativa (usado na Task 5)
  - `Optional<String> token(UUID companyId)`
  - `static String sha256(String)`

- [ ] **Step 1: Adicionar a configuração** em `application.yml`, dentro de `app:`:

```yaml
  telegram:
    api-base-url: ${TELEGRAM_API_BASE_URL:https://api.telegram.org}
    # URL pública do backend que o Telegram chama (ex.: https://api.suaclinica.com.br)
    webhook-base-url: ${TELEGRAM_WEBHOOK_BASE_URL:}
```

- [ ] **Step 2: Escrever o teste que falha**

```java
@ExtendWith(MockitoExtension.class)
class TelegramConfigServiceTest {
    static final UUID CO = UUID.randomUUID(), USER = UUID.randomUUID();
    @Mock TelegramBotClient client;
    @Mock TelegramBotConfigJpaRepository repo;
    MailPasswordCipher cipher = new MailPasswordCipher("k", "");
    TelegramConfigService service;

    @BeforeEach void setUp() {
        service = new TelegramConfigService(client, repo, cipher, "https://api.crm.test");
    }

    @Test void saveValidatesTokenRegistersWebhookAndStoresCipher() {
        when(client.getMe("TOK")).thenReturn("leo_bot");
        ArgumentCaptor<String> secret = ArgumentCaptor.forClass(String.class);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        TelegramConfigResponse r = service.save(CO, "TOK", USER);

        verify(client).setWebhook(eq("TOK"), eq("https://api.crm.test/api/v1/telegram/webhook/" + CO), secret.capture());
        ArgumentCaptor<TelegramBotConfig> saved = ArgumentCaptor.forClass(TelegramBotConfig.class);
        verify(repo).save(saved.capture());
        assertNotEquals("TOK", saved.getValue().getBotTokenCipher());
        assertEquals("TOK", cipher.decrypt(saved.getValue().getBotTokenCipher()));
        assertEquals(TelegramConfigService.sha256(secret.getValue()), saved.getValue().getWebhookSecretHash());
        assertEquals("leo_bot", r.botUsername());
    }

    @Test void saveRejectsInvalidToken() {
        when(client.getMe("BAD")).thenThrow(new TelegramBotClient.TelegramApiException("x", null));
        assertThrows(BusinessException.class, () -> service.save(CO, "BAD", USER));
        verify(repo, never()).save(any());
    }

    @Test void saveRequiresWebhookBaseUrl() {
        service = new TelegramConfigService(client, repo, cipher, "");
        assertThrows(BusinessException.class, () -> service.save(CO, "TOK", USER));
    }

    @Test void tokenIfValidSecretChecksHashAndEnabled() {
        TelegramBotConfig c = new TelegramBotConfig();
        c.setCompanyId(CO); c.setBotTokenCipher(cipher.encrypt("TOK"));
        c.setWebhookSecretHash(TelegramConfigService.sha256("S")); c.setEnabled(true);
        when(repo.findById(CO)).thenReturn(Optional.of(c));
        assertEquals(Optional.of("TOK"), service.tokenIfValidSecret(CO, "S"));
        assertEquals(Optional.empty(), service.tokenIfValidSecret(CO, "errado"));
        assertEquals(Optional.empty(), service.tokenIfValidSecret(CO, null));
        c.setEnabled(false);
        assertEquals(Optional.empty(), service.tokenIfValidSecret(CO, "S"));
    }

    @Test void deleteRemovesWebhookAndRow() {
        TelegramBotConfig c = new TelegramBotConfig();
        c.setCompanyId(CO); c.setBotTokenCipher(cipher.encrypt("TOK"));
        when(repo.findById(CO)).thenReturn(Optional.of(c));
        service.delete(CO);
        verify(client).deleteWebhook("TOK");
        verify(repo).delete(c);
    }
}
```

`BusinessException` é a exceção de regra de negócio que o `GlobalExceptionHandler` converte em 400/422. Confirme o nome exato com `grep -rn "class .*BusinessException\|class .*ValidationException" backend/src/main/java/com/becommerce/crm/shared` e use a mesma no teste e na implementação.

- [ ] **Step 3: Peça ao usuário para rodar** `cd backend && mvn -q test -Dtest=TelegramConfigServiceTest`. O esperado é FAIL.

- [ ] **Step 4: Implementar**

```java
@Service
public class TelegramConfigService {
    private static final Logger log = LoggerFactory.getLogger(TelegramConfigService.class);
    private final TelegramBotClient client;
    private final TelegramBotConfigJpaRepository repo;
    private final MailPasswordCipher cipher;
    private final String webhookBaseUrl;
    private final SecureRandom random = new SecureRandom();

    public TelegramConfigService(TelegramBotClient client, TelegramBotConfigJpaRepository repo,
                                 MailPasswordCipher cipher,
                                 @Value("${app.telegram.webhook-base-url:}") String webhookBaseUrl) {
        this.client = client; this.repo = repo; this.cipher = cipher;
        this.webhookBaseUrl = webhookBaseUrl == null ? "" : webhookBaseUrl.replaceAll("/+$", "");
    }

    public record TelegramConfigResponse(boolean configured, String botUsername, boolean enabled, LocalDateTime updatedAt) {
        static TelegramConfigResponse empty() { return new TelegramConfigResponse(false, null, false, null); }
    }

    @Transactional(readOnly = true)
    public TelegramConfigResponse get(UUID companyId) {
        return withTenant(companyId, () -> repo.findById(companyId)
                .map(c -> new TelegramConfigResponse(true, c.getBotUsername(), c.isEnabled(), c.getUpdatedAt()))
                .orElse(TelegramConfigResponse.empty()));
    }

    @Transactional
    public TelegramConfigResponse save(UUID companyId, String token, UUID userId) {
        if (webhookBaseUrl.isBlank()) throw new BusinessException("TELEGRAM_WEBHOOK_BASE_URL não configurada no servidor.");
        if (token == null || token.isBlank()) throw new BusinessException("Informe o token do bot.");
        String username;
        try { username = client.getMe(token.strip()); }
        catch (TelegramBotClient.TelegramApiException e) { throw new BusinessException("Token do bot inválido."); }
        String secret = newSecret();
        try {
            client.setWebhook(token.strip(), webhookBaseUrl + "/api/v1/telegram/webhook/" + companyId, secret);
        } catch (TelegramBotClient.TelegramApiException e) {
            throw new BusinessException("O Telegram recusou o webhook. Confira a URL pública do servidor.");
        }
        return withTenant(companyId, () -> {
            TelegramBotConfig c = repo.findById(companyId).orElseGet(TelegramBotConfig::new);
            c.setCompanyId(companyId);
            c.setBotTokenCipher(cipher.encrypt(token.strip()));
            c.setWebhookSecretHash(sha256(secret));
            c.setBotUsername(username);
            c.setEnabled(true);
            c.setUpdatedBy(userId);
            c.setUpdatedAt(LocalDateTime.now());
            repo.save(c);
            return new TelegramConfigResponse(true, username, true, c.getUpdatedAt());
        });
    }

    @Transactional
    public void delete(UUID companyId) {
        withTenant(companyId, () -> {
            repo.findById(companyId).ifPresent(c -> {
                try { client.deleteWebhook(cipher.decrypt(c.getBotTokenCipher())); }
                catch (RuntimeException e) { log.warn("[TELEGRAM] falha ao remover webhook (company={})", companyId); }
                repo.delete(c);
            });
            return null;
        });
    }

    @Transactional(readOnly = true)
    public Optional<String> tokenIfValidSecret(UUID companyId, String providedSecret) {
        if (providedSecret == null || providedSecret.isBlank()) return Optional.empty();
        return withTenant(companyId, () -> repo.findById(companyId)
                .filter(TelegramBotConfig::isEnabled)
                .filter(c -> MessageDigest.isEqual(c.getWebhookSecretHash().getBytes(StandardCharsets.UTF_8),
                        sha256(providedSecret).getBytes(StandardCharsets.UTF_8)))
                .map(c -> cipher.decrypt(c.getBotTokenCipher())));
    }

    @Transactional(readOnly = true)
    public Optional<String> token(UUID companyId) {
        return withTenant(companyId, () -> repo.findById(companyId).filter(TelegramBotConfig::isEnabled)
                .map(c -> cipher.decrypt(c.getBotTokenCipher())));
    }

    private String newSecret() {
        byte[] b = new byte[32]; random.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    public static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    /** Executa com o tenant definido, para as consultas sob RLS FORCE. */
    static <T> T withTenant(UUID companyId, Supplier<T> body) {
        UUID previous = TenantContext.getCompanyId();
        try { TenantContext.setCompanyId(companyId); return body.get(); }
        finally { if (previous == null) TenantContext.clear(); else TenantContext.setCompanyId(previous); }
    }
}
```

Confira se `TenantContext.getCompanyId()` existe (`grep -n "public static" backend/src/main/java/com/becommerce/crm/shared/tenant/*/TenantContext.java`). Se não existir, use só `set` e `clear`, como no `AiActionService`. O tenant precisa estar definido **antes** de a transação abrir a conexão. Se o projeto aplica o tenant ao iniciar a transação (como no `AiActionService.propose`, que define o tenant dentro de um método `@Transactional`), o padrão acima funciona. Se os testes de integração mostrarem RLS vazio, mova o `@Transactional` para um bean interno chamado depois do `setCompanyId`.

- [ ] **Step 5: Controller e teste dele**

```java
@RestController
@RequestMapping("/api/v1/companies/{companyId}/telegram")
public class TelegramSettingsController {
    private static final String OWN_COMPANY = "Você só pode configurar o Telegram da sua própria empresa.";
    private final TelegramConfigService config;

    public TelegramSettingsController(TelegramConfigService config) { this.config = config; }

    public record SaveTelegramToken(@NotBlank String token) {}

    @GetMapping
    @PreAuthorize("hasAuthority('company:update')")
    public TelegramConfigResponse get(@CurrentCompanyId(OWN_COMPANY) UUID companyId) { return config.get(companyId); }

    @PutMapping
    @PreAuthorize("hasAuthority('company:update')")
    public TelegramConfigResponse save(@CurrentCompanyId(OWN_COMPANY) UUID companyId,
                                       @Valid @RequestBody SaveTelegramToken body,
                                       @AuthenticationPrincipal CurrentUser principal) {
        return config.save(companyId, body.token(), principal.userId());
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('company:update')")
    public ResponseEntity<Void> delete(@CurrentCompanyId(OWN_COMPANY) UUID companyId) {
        config.delete(companyId);
        return ResponseEntity.noContent().build();
    }
}
```

O `@CurrentCompanyId` é o mesmo do `CompanyMailController`: ele valida que o `companyId` é a empresa do usuário e deixa o `CRM_ADMIN` passar. Monte o teste do controller no estilo do `AiAssistantControllerTest` (standalone MockMvc + `AuthenticationPrincipalArgumentResolver`), cobrindo:
- `PUT` com `{"token":"TOK"}` chama `config.save(COMPANY_ID, "TOK", USER_ID)` e retorna 200 com `botUsername`;
- `PUT` com token vazio retorna 400;
- a resposta de `GET` não contém o campo `token` (`jsonPath("$.token").doesNotExist()`).

O standalone não avalia `@PreAuthorize` nem o resolvedor de `@CurrentCompanyId`. Registre o resolvedor como no `CompanyMailController` (confira se existe teste dele com `grep -rln "CompanyMailController" backend/src/test`) e copie a configuração.

- [ ] **Step 6: Peça ao usuário para rodar** `cd backend && mvn -q test -Dtest='TelegramConfigServiceTest,TelegramSettingsControllerTest'`. O esperado é PASS.

- [ ] **Step 7: Commit** com `git commit -m "feat(telegram): configuração do bot por clínica"`.

---

### Task 4: Vínculo usuário ↔ Telegram

**Files:**
- Create: `communication/telegram/application/TelegramLinkService.java`
- Create: `communication/telegram/application/TelegramUserResolver.java`
- Modify: `communication/telegram/web/TelegramSettingsController.java` (endpoints de vínculo)
- Test: `communication/telegram/application/TelegramLinkServiceTest.java`, `TelegramUserResolverTest.java`

**Interfaces:**
- Consumes: os repositórios da Task 1, `PermissionRepository.findEffectivePermissionNamesByUserIdAndCompanyId(UUID, UUID)`, `MembershipRepository.existsActiveByUserIdAndCompanyId(UUID, UUID)`, `UserRoleRepository`, `RoleRepository`, `TenantAuditRecorder.record(...)`
- Produces:
  - `TelegramLinkService.generateCode(UUID companyId, UUID userId) → String`: 6 dígitos, vale 10 min e invalida os códigos anteriores do usuário
  - `TelegramLinkService.link(UUID companyId, String code, long telegramUserId, long chatId, String telegramName) → LinkResult`
  - `enum LinkResult { LINKED, INVALID_CODE, TELEGRAM_ALREADY_LINKED }`
  - `TelegramLinkService.unlinkByTelegram(UUID companyId, long telegramUserId) → boolean`
  - `TelegramLinkService.unlinkUser(UUID companyId, UUID userId)`
  - `TelegramLinkService.myLink(UUID companyId, UUID userId) → Optional<LinkView>` com `record LinkView(UUID userId, String telegramName, LocalDateTime createdAt)`
  - `TelegramLinkService.listLinks(UUID companyId) → List<LinkView>`
  - `TelegramLinkService.setConversation(UUID companyId, long telegramUserId, UUID conversationId)`
  - `TelegramUserResolver.resolve(UUID companyId, long telegramUserId) → Optional<LinkedUser>`, com `record LinkedUser(UUID userId, long chatId, UUID conversationId, List<String> permissions, List<String> roles)`. Retorna vazio se não houver vínculo ou se a membership não estiver ativa.

- [ ] **Step 1: Escrever os testes que falham**

```java
@ExtendWith(MockitoExtension.class)
class TelegramLinkServiceTest {
    static final UUID CO = UUID.randomUUID(), USER = UUID.randomUUID();
    @Mock TelegramLinkJpaRepository links;
    @Mock TelegramLinkCodeJpaRepository codes;
    @Mock TenantAuditRecorder auditor;
    TelegramLinkService service;

    @BeforeEach void setUp() { service = new TelegramLinkService(links, codes, auditor, Clock.systemUTC()); }

    @Test void generateCodeHasSixDigitsAndStoresOnlyHash() {
        ArgumentCaptor<TelegramLinkCode> saved = ArgumentCaptor.forClass(TelegramLinkCode.class);
        String code = service.generateCode(CO, USER);
        assertTrue(code.matches("\\d{6}"));
        verify(codes).deleteByCompanyIdAndUserId(CO, USER);
        verify(codes).save(saved.capture());
        assertEquals(TelegramConfigService.sha256(CO + ":" + code), saved.getValue().getCodeHash());
        assertTrue(saved.getValue().getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(9)));
    }

    @Test void linkWithValidCodeCreatesLinkAndBurnsCode() {
        TelegramLinkCode c = code(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5), null);
        when(codes.findById(TelegramConfigService.sha256(CO + ":123456"))).thenReturn(Optional.of(c));
        when(links.findByCompanyIdAndTelegramUserId(CO, 99L)).thenReturn(Optional.empty());
        when(links.findByCompanyIdAndUserId(CO, USER)).thenReturn(Optional.empty());

        assertEquals(TelegramLinkService.LinkResult.LINKED, service.link(CO, "123456", 99L, 99L, "Gui"));
        assertNotNull(c.getUsedAt());
        verify(links).save(argThat(l -> l.getUserId().equals(USER) && l.getTelegramUserId() == 99L));
    }

    @Test void expiredUsedOrUnknownCodeIsRejected() {
        when(codes.findById(any())).thenReturn(Optional.empty());
        assertEquals(TelegramLinkService.LinkResult.INVALID_CODE, service.link(CO, "000000", 1, 1, "x"));

        TelegramLinkCode expired = code(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1), null);
        when(codes.findById(TelegramConfigService.sha256(CO + ":111111"))).thenReturn(Optional.of(expired));
        assertEquals(TelegramLinkService.LinkResult.INVALID_CODE, service.link(CO, "111111", 1, 1, "x"));

        TelegramLinkCode used = code(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5), LocalDateTime.now());
        when(codes.findById(TelegramConfigService.sha256(CO + ":222222"))).thenReturn(Optional.of(used));
        assertEquals(TelegramLinkService.LinkResult.INVALID_CODE, service.link(CO, "222222", 1, 1, "x"));
        verify(links, never()).save(any());
    }

    @Test void codeFromOtherCompanyDoesNotMatch() {
        // O hash inclui o companyId: o mesmo código em outra clínica gera outro hash.
        assertNotEquals(TelegramConfigService.sha256(CO + ":123456"),
                TelegramConfigService.sha256(UUID.randomUUID() + ":123456"));
    }

    @Test void telegramAlreadyLinkedToOtherUserIsRejected() {
        TelegramLinkCode c = code(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5), null);
        when(codes.findById(any())).thenReturn(Optional.of(c));
        TelegramLink other = new TelegramLink(); other.setUserId(UUID.randomUUID());
        when(links.findByCompanyIdAndTelegramUserId(CO, 99L)).thenReturn(Optional.of(other));
        assertEquals(TelegramLinkService.LinkResult.TELEGRAM_ALREADY_LINKED, service.link(CO, "123456", 99L, 99L, "x"));
    }

    private TelegramLinkCode code(LocalDateTime expires, LocalDateTime used) {
        TelegramLinkCode c = new TelegramLinkCode();
        c.setCompanyId(CO); c.setUserId(USER); c.setExpiresAt(expires); c.setUsedAt(used);
        return c;
    }
}

@ExtendWith(MockitoExtension.class)
class TelegramUserResolverTest {
    static final UUID CO = UUID.randomUUID(), USER = UUID.randomUUID();
    @Mock TelegramLinkJpaRepository links;
    @Mock PermissionRepository permissions;
    @Mock MembershipRepository memberships;
    @Mock UserRoleRepository userRoles;
    @Mock RoleRepository roles;
    TelegramUserResolver resolver;

    @BeforeEach void setUp() { resolver = new TelegramUserResolver(links, permissions, memberships, userRoles, roles); }

    @Test void unlinkedReturnsEmpty() {
        when(links.findByCompanyIdAndTelegramUserId(CO, 5L)).thenReturn(Optional.empty());
        assertTrue(resolver.resolve(CO, 5L).isEmpty());
    }

    @Test void inactiveMemberReturnsEmpty() {
        when(links.findByCompanyIdAndTelegramUserId(CO, 5L)).thenReturn(Optional.of(link()));
        when(memberships.existsActiveByUserIdAndCompanyId(USER, CO)).thenReturn(false);
        assertTrue(resolver.resolve(CO, 5L).isEmpty());
    }

    @Test void activeMemberGetsEffectivePermissions() {
        when(links.findByCompanyIdAndTelegramUserId(CO, 5L)).thenReturn(Optional.of(link()));
        when(memberships.existsActiveByUserIdAndCompanyId(USER, CO)).thenReturn(true);
        when(permissions.findEffectivePermissionNamesByUserIdAndCompanyId(USER, CO)).thenReturn(List.of("ai:chat"));
        when(userRoles.findByUserIdAndCompanyId(USER, CO)).thenReturn(List.of());
        var u = resolver.resolve(CO, 5L).orElseThrow();
        assertEquals(USER, u.userId());
        assertEquals(List.of("ai:chat"), u.permissions());
    }

    private TelegramLink link() {
        TelegramLink l = new TelegramLink(); l.setCompanyId(CO); l.setUserId(USER); l.setTelegramUserId(5L); l.setChatId(5L);
        return l;
    }
}
```

- [ ] **Step 2: Peça ao usuário para rodar** `cd backend && mvn -q test -Dtest='TelegramLinkServiceTest,TelegramUserResolverTest'`. O esperado é FAIL.

- [ ] **Step 3: Implementar o `TelegramLinkService`**

```java
@Service
public class TelegramLinkService {
    private static final Duration TTL = Duration.ofMinutes(10);
    private final TelegramLinkJpaRepository links;
    private final TelegramLinkCodeJpaRepository codes;
    private final TenantAuditRecorder auditor;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public TelegramLinkService(TelegramLinkJpaRepository links, TelegramLinkCodeJpaRepository codes,
                               TenantAuditRecorder auditor, Clock clock) {
        this.links = links; this.codes = codes; this.auditor = auditor; this.clock = clock;
    }

    public enum LinkResult { LINKED, INVALID_CODE, TELEGRAM_ALREADY_LINKED }
    public record LinkView(UUID userId, String telegramName, LocalDateTime createdAt) {}

    @Transactional
    public String generateCode(UUID companyId, UUID userId) {
        return TelegramConfigService.withTenant(companyId, () -> {
            codes.deleteByCompanyIdAndUserId(companyId, userId);
            String code = String.format("%06d", random.nextInt(1_000_000));
            TelegramLinkCode c = new TelegramLinkCode();
            c.setCodeHash(hash(companyId, code));
            c.setCompanyId(companyId); c.setUserId(userId);
            c.setExpiresAt(now().plus(TTL));
            codes.save(c);
            return code;
        });
    }

    @Transactional
    public LinkResult link(UUID companyId, String code, long telegramUserId, long chatId, String telegramName) {
        return TelegramConfigService.withTenant(companyId, () -> {
            Optional<TelegramLinkCode> found = code == null ? Optional.empty() : codes.findById(hash(companyId, code.strip()));
            if (found.isEmpty() || found.get().getUsedAt() != null || found.get().getExpiresAt().isBefore(now())) {
                return LinkResult.INVALID_CODE;
            }
            TelegramLinkCode c = found.get();
            Optional<TelegramLink> byTelegram = links.findByCompanyIdAndTelegramUserId(companyId, telegramUserId);
            if (byTelegram.isPresent() && !byTelegram.get().getUserId().equals(c.getUserId())) {
                return LinkResult.TELEGRAM_ALREADY_LINKED;
            }
            c.setUsedAt(now());
            TelegramLink link = links.findByCompanyIdAndUserId(companyId, c.getUserId()).orElseGet(TelegramLink::new);
            link.setCompanyId(companyId); link.setUserId(c.getUserId());
            link.setTelegramUserId(telegramUserId); link.setChatId(chatId); link.setTelegramName(telegramName);
            links.save(link);
            audit(companyId, c.getUserId(), "TELEGRAM_LINKED", "Telegram vinculado ao usuário");
            return LinkResult.LINKED;
        });
    }

    @Transactional
    public boolean unlinkByTelegram(UUID companyId, long telegramUserId) {
        return TelegramConfigService.withTenant(companyId, () -> links.findByCompanyIdAndTelegramUserId(companyId, telegramUserId)
                .map(l -> { links.delete(l); audit(companyId, l.getUserId(), "TELEGRAM_UNLINKED", "Telegram desvinculado"); return true; })
                .orElse(false));
    }

    @Transactional
    public void unlinkUser(UUID companyId, UUID userId) {
        TelegramConfigService.withTenant(companyId, () -> {
            links.findByCompanyIdAndUserId(companyId, userId).ifPresent(l -> {
                links.delete(l); audit(companyId, userId, "TELEGRAM_UNLINKED", "Telegram desvinculado");
            });
            return null;
        });
    }

    @Transactional(readOnly = true)
    public Optional<LinkView> myLink(UUID companyId, UUID userId) {
        return TelegramConfigService.withTenant(companyId, () -> links.findByCompanyIdAndUserId(companyId, userId).map(this::view));
    }

    @Transactional(readOnly = true)
    public List<LinkView> listLinks(UUID companyId) {
        return TelegramConfigService.withTenant(companyId, () -> links.findByCompanyIdOrderByCreatedAtDesc(companyId).stream().map(this::view).toList());
    }

    @Transactional
    public void setConversation(UUID companyId, long telegramUserId, UUID conversationId) {
        TelegramConfigService.withTenant(companyId, () -> {
            links.findByCompanyIdAndTelegramUserId(companyId, telegramUserId).ifPresent(l -> { l.setAiConversationId(conversationId); links.save(l); });
            return null;
        });
    }

    private LinkView view(TelegramLink l) { return new LinkView(l.getUserId(), l.getTelegramName(), l.getCreatedAt()); }
    private LocalDateTime now() { return LocalDateTime.now(clock.withZone(ZoneOffset.UTC)); }
    private static String hash(UUID companyId, String code) { return TelegramConfigService.sha256(companyId + ":" + code); }

    private void audit(UUID companyId, UUID userId, String event, String description) {
        try {
            auditor.record(companyId, AuditAction.CUSTOM, AuditModule.AI, "TelegramLink", userId.toString(),
                    description, userId, Map.of("event", event, "channel", "telegram"));
        } catch (RuntimeException ignored) { /* auditoria nunca derruba o fluxo */ }
    }
}
```

Confira a assinatura exata de `TenantAuditRecorder.record` (`sed -n 30,45p backend/src/main/java/com/becommerce/crm/analytics/audit/application/service/TenantAuditRecorder.java`) e ajuste os argumentos copiando a chamada usada em `AiActionService.audit`. Se não houver um bean `Clock`, adicione `@Bean Clock clock() { return Clock.systemUTC(); }` numa `@Configuration` do pacote telegram e procure antes se já existe um (`grep -rn "Clock clock()" backend/src/main/java`).

- [ ] **Step 4: Implementar o `TelegramUserResolver`** (mesma regra do `LocalCurrentUserResolver`)

```java
@Service
public class TelegramUserResolver {
    private final TelegramLinkJpaRepository links;
    private final PermissionRepository permissions;
    private final MembershipRepository memberships;
    private final UserRoleRepository userRoles;
    private final RoleRepository roles;

    public TelegramUserResolver(TelegramLinkJpaRepository links, PermissionRepository permissions,
                                MembershipRepository memberships, UserRoleRepository userRoles, RoleRepository roles) {
        this.links = links; this.permissions = permissions; this.memberships = memberships;
        this.userRoles = userRoles; this.roles = roles;
    }

    public record LinkedUser(UUID userId, long chatId, UUID conversationId, List<String> permissions, List<String> roles) {}

    @Transactional(readOnly = true)
    public Optional<LinkedUser> resolve(UUID companyId, long telegramUserId) {
        return TelegramConfigService.withTenant(companyId, () -> links.findByCompanyIdAndTelegramUserId(companyId, telegramUserId)
                .filter(l -> memberships.existsActiveByUserIdAndCompanyId(l.getUserId(), companyId))
                .map(l -> new LinkedUser(l.getUserId(), l.getChatId(), l.getAiConversationId(),
                        permissions.findEffectivePermissionNamesByUserIdAndCompanyId(l.getUserId(), companyId),
                        userRoles.findByUserIdAndCompanyId(l.getUserId(), companyId).stream()
                                .map(ur -> roles.findById(ur.getRoleId())).flatMap(Optional::stream)
                                .map(r -> r.getName()).toList())));
    }
}
```

- [ ] **Step 5: Endpoints de vínculo no `TelegramSettingsController`**

```java
// construtor passa a receber também TelegramLinkService links
public record LinkCodeResponse(String code, String botUsername, int expiresInMinutes) {}

@PostMapping("/link-code")
@PreAuthorize("hasAuthority('ai:chat')")
public LinkCodeResponse linkCode(@CurrentCompanyId(OWN_COMPANY) UUID companyId, @AuthenticationPrincipal CurrentUser principal) {
    TelegramConfigResponse cfg = config.get(companyId);
    if (!cfg.configured() || !cfg.enabled()) throw new BusinessException("O Telegram ainda não foi configurado nesta clínica.");
    return new LinkCodeResponse(links.generateCode(companyId, principal.userId()), cfg.botUsername(), 10);
}

@GetMapping("/me")
@PreAuthorize("hasAuthority('ai:chat')")
public ResponseEntity<TelegramLinkService.LinkView> me(@CurrentCompanyId(OWN_COMPANY) UUID companyId, @AuthenticationPrincipal CurrentUser principal) {
    return links.myLink(companyId, principal.userId()).map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
}

@DeleteMapping("/me")
@PreAuthorize("hasAuthority('ai:chat')")
public ResponseEntity<Void> unlinkMe(@CurrentCompanyId(OWN_COMPANY) UUID companyId, @AuthenticationPrincipal CurrentUser principal) {
    links.unlinkUser(companyId, principal.userId());
    return ResponseEntity.noContent().build();
}

@GetMapping("/links")
@PreAuthorize("hasAuthority('company:update')")
public List<TelegramLinkService.LinkView> listLinks(@CurrentCompanyId(OWN_COMPANY) UUID companyId) { return links.listLinks(companyId); }

@DeleteMapping("/links/{userId}")
@PreAuthorize("hasAuthority('company:update')")
public ResponseEntity<Void> revoke(@CurrentCompanyId(OWN_COMPANY) UUID companyId, @PathVariable UUID userId) {
    links.unlinkUser(companyId, userId);
    return ResponseEntity.noContent().build();
}
```

Inclua estes casos no `TelegramSettingsControllerTest`:
- `POST /link-code` com o bot configurado retorna `code` e `botUsername`;
- sem bot configurado, retorna 4xx;
- `DELETE /me` chama `unlinkUser(COMPANY_ID, USER_ID)`.

- [ ] **Step 6: Peça ao usuário para rodar** `cd backend && mvn -q test -Dtest='TelegramLinkServiceTest,TelegramUserResolverTest,TelegramSettingsControllerTest'`. O esperado é PASS.

- [ ] **Step 7: Commit** com `git commit -m "feat(telegram): vínculo do usuário com o Telegram"`.

---

### Task 5: Webhook público (segredo + idempotência + assíncrono)

**Files:**
- Create: `communication/telegram/web/TelegramWebhookController.java`
- Create: `communication/telegram/application/TelegramUpdateDispatcher.java`
- Modify: `shared/security/config/SecurityConfig.java:80` (nova linha `permitAll`)
- Test: `communication/telegram/web/TelegramWebhookControllerTest.java`

**Interfaces:**
- Consumes: `TelegramConfigService.tokenIfValidSecret` (Task 3), `TelegramUpdateSeenJpaRepository.markSeen` (Task 1)
- Produces: `TelegramUpdateDispatcher.dispatch(UUID companyId, String token, JsonNode update)` é `@Async` e chama `TelegramUpdateProcessor.process(companyId, token, update)` (Task 6).

- [ ] **Step 1: Escrever o teste que falha**

```java
@ExtendWith(MockitoExtension.class)
class TelegramWebhookControllerTest {
    static final UUID CO = UUID.randomUUID();
    @Mock TelegramConfigService config;
    @Mock TelegramUpdateSeenJpaRepository seen;
    @Mock TelegramUpdateDispatcher dispatcher;
    MockMvc mvc;

    @BeforeEach void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new TelegramWebhookController(config, seen, dispatcher, new ObjectMapper())).build();
    }

    @Test void missingOrWrongSecretIs401AndNothingProcessed() throws Exception {
        when(config.tokenIfValidSecret(CO, null)).thenReturn(Optional.empty());
        mvc.perform(post("/api/v1/telegram/webhook/" + CO).contentType(APPLICATION_JSON).content("{\"update_id\":1}"))
           .andExpect(status().isUnauthorized());
        verifyNoInteractions(dispatcher, seen);
    }

    @Test void validNewUpdateIsDispatched() throws Exception {
        when(config.tokenIfValidSecret(CO, "S")).thenReturn(Optional.of("TOK"));
        when(seen.markSeen(CO, 1L)).thenReturn(1);
        mvc.perform(post("/api/v1/telegram/webhook/" + CO).header("X-Telegram-Bot-Api-Secret-Token", "S")
                .contentType(APPLICATION_JSON).content("{\"update_id\":1,\"message\":{\"text\":\"oi\"}}"))
           .andExpect(status().isOk());
        verify(dispatcher).dispatch(eq(CO), eq("TOK"), argThat(n -> n.path("update_id").asLong() == 1));
    }

    @Test void repeatedUpdateIsIgnored() throws Exception {
        when(config.tokenIfValidSecret(CO, "S")).thenReturn(Optional.of("TOK"));
        when(seen.markSeen(CO, 1L)).thenReturn(0);
        mvc.perform(post("/api/v1/telegram/webhook/" + CO).header("X-Telegram-Bot-Api-Secret-Token", "S")
                .contentType(APPLICATION_JSON).content("{\"update_id\":1}"))
           .andExpect(status().isOk());
        verifyNoInteractions(dispatcher);
    }
}
```

- [ ] **Step 2: Peça ao usuário para rodar** `cd backend && mvn -q test -Dtest=TelegramWebhookControllerTest`. O esperado é FAIL.

- [ ] **Step 3: Implementar**

```java
@RestController
public class TelegramWebhookController {
    public static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";
    private final TelegramConfigService config;
    private final TelegramUpdateSeenJpaRepository seen;
    private final TelegramUpdateDispatcher dispatcher;
    private final ObjectMapper mapper;

    public TelegramWebhookController(TelegramConfigService config, TelegramUpdateSeenJpaRepository seen,
                                     TelegramUpdateDispatcher dispatcher, ObjectMapper mapper) {
        this.config = config; this.seen = seen; this.dispatcher = dispatcher; this.mapper = mapper;
    }

    @PostMapping("/api/v1/telegram/webhook/{companyId}")
    public ResponseEntity<Void> receive(@PathVariable UUID companyId,
                                        @RequestHeader(value = SECRET_HEADER, required = false) String secret,
                                        @RequestBody String body) throws JsonProcessingException {
        Optional<String> token = config.tokenIfValidSecret(companyId, secret);
        if (token.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        JsonNode update = mapper.readTree(body);
        long updateId = update.path("update_id").asLong(-1);
        if (updateId < 0) return ResponseEntity.ok().build();
        int fresh = TelegramConfigService.withTenant(companyId, () -> seen.markSeen(companyId, updateId));
        if (fresh == 1) dispatcher.dispatch(companyId, token.get(), update);
        return ResponseEntity.ok().build();
    }
}

@Component
public class TelegramUpdateDispatcher {
    private static final Logger log = LoggerFactory.getLogger(TelegramUpdateDispatcher.class);
    private final TelegramUpdateProcessor processor;
    public TelegramUpdateDispatcher(TelegramUpdateProcessor processor) { this.processor = processor; }

    @Async
    public void dispatch(UUID companyId, String token, JsonNode update) {
        try { processor.process(companyId, token, update); }
        catch (RuntimeException e) { log.error("[TELEGRAM] falha ao processar update (company={})", companyId, e); }
    }
}
```

`markSeen` é `@Modifying` e precisa de transação. Envolva a chamada num método `@Transactional` de um bean (por exemplo, `TelegramConfigService.markSeen(companyId, updateId)`) ou anote `@Transactional` no repositório. No teste, mocke esse método no lugar de `seen.markSeen`, ajustando o construtor e o teste juntos. Para a Task 5 compilar antes da Task 6, crie `TelegramUpdateProcessor` com um `public void process(UUID companyId, String token, JsonNode update) {}` vazio. A Task 6 preenche.

- [ ] **Step 4: Liberar a rota no `SecurityConfig`**, logo abaixo da linha do webhook do WhatsApp (`SecurityConfig.java:80`):

```java
                .requestMatchers(HttpMethod.POST, "/api/v1/telegram/webhook/**").permitAll()
```

Confira também se o `TenantFilter` não bloqueia uma requisição anônima nessa rota. Ele já deixa passar `/api/v1/omnichannel/whatsapp/webhook`, então procure como isso é feito (`grep -n "webhook\|permit\|shouldNotFilter" backend/src/main/java/com/becommerce/crm/shared/tenant/filter/TenantFilter.java`) e adicione `/api/v1/telegram/webhook/` da mesma forma, se necessário.

- [ ] **Step 5: Peça ao usuário para rodar o mesmo comando.** O esperado é PASS.

- [ ] **Step 6: Commit** com `git commit -m "feat(telegram): webhook com segredo e idempotência"`.

---

### Task 6: Processador (comandos, texto, áudio, confirmação)

**Files:**
- Modify: `communication/telegram/application/TelegramUpdateProcessor.java`
- Create: `communication/telegram/application/TelegramRateLimiter.java`
- Test: `communication/telegram/application/TelegramUpdateProcessorTest.java`, `TelegramRateLimiterTest.java`

**Interfaces:**
- Consumes: `TelegramBotClient` (Task 2), `TelegramLinkService` e `TelegramUserResolver` (Task 4), `AiAssistantUseCase.chat(UUID, UUID, List<String>, AiChatRequest) → AiChatResponse`, `AiActionUseCase.confirm(UUID, UUID, List<String>, UUID) → AiActionResponse`, `AiActionUseCase.cancel(UUID, UUID, UUID) → AiActionResponse`, `AiMediaProvider.transcribe(byte[], String) → String`
- Produces: `void process(UUID companyId, String token, JsonNode update)` e `boolean TelegramRateLimiter.tryAcquire(UUID companyId, long telegramUserId)`

Comportamento:

| Entrada | Ação |
|---|---|
| `callback_query` com `data = c:<uuid>` | resolve o remetente, chama `confirm`, `answerCallback`, edita a mensagem para `✅ Feito: <description>` ou `⚠️ Não foi possível: <errorMessage>` |
| `callback_query` com `x:<uuid>` | `cancel`, depois edita para `❌ Cancelado.` |
| `/start` | texto de boas-vindas com instrução de vínculo |
| `/vincular <código>` | `link(...)`; responde `✅ Telegram vinculado. Pode falar comigo.`, `Código inválido ou vencido. Gere outro no seu perfil do CRM.` ou `Este Telegram já está vinculado a outro usuário.` |
| remetente sem vínculo (qualquer outra mensagem) | `Conta não vinculada. Gere o código no seu perfil do CRM (Perfil → Conectar Telegram) e envie /vincular CÓDIGO.` |
| `/desvincular` | `unlinkByTelegram`, depois `Telegram desvinculado.` |
| `/nova` | `setConversation(null)`, depois `Nova conversa iniciada.` |
| texto | `chat(..., new AiChatRequest(text, conversationId, null))` com `text` **idêntico**; salva o `conversationId`; envia `message` e, para cada action `PROPOSED`, envia `description` com os botões |
| `voice` ou `audio` | `downloadFile`, `transcribe(bytes, mime)` e então o mesmo fluxo do texto, com a resposta prefixada por `🎙️ Entendi: "<transcrito>"\n\n` |
| outros tipos | `Por enquanto entendo texto e áudio.` |
| sem `ai:chat` | `Seu usuário não tem acesso ao assistente.` |
| limite estourado | `Muitas mensagens em pouco tempo. Aguarde um minuto.` |
| exceção do Leo | `Não consegui processar agora, tente de novo.` |
| exceção na transcrição | `Não entendi o áudio, pode repetir ou escrever?` |

Mensagens de grupo (`chat.type != "private"`) são ignoradas, para o bot não agir dentro de grupos.

- [ ] **Step 1: Escrever os testes que falham**

```java
@ExtendWith(MockitoExtension.class)
class TelegramUpdateProcessorTest {
    static final UUID CO = UUID.randomUUID(), USER = UUID.randomUUID(), CONV = UUID.randomUUID(), ACTION = UUID.randomUUID();
    static final long TG = 555L;
    @Mock TelegramBotClient bot;
    @Mock TelegramLinkService links;
    @Mock TelegramUserResolver users;
    @Mock AiAssistantUseCase leo;
    @Mock AiActionUseCase actions;
    @Mock AiMediaProvider media;
    @Mock TelegramRateLimiter limiter;
    final ObjectMapper om = new ObjectMapper();
    TelegramUpdateProcessor p;

    @BeforeEach void setUp() {
        p = new TelegramUpdateProcessor(bot, links, users, leo, actions, media, limiter);
        lenient().when(limiter.tryAcquire(any(), anyLong())).thenReturn(true);
    }

    JsonNode msg(String json) throws Exception {
        return om.readTree("{\"update_id\":1,\"message\":{\"chat\":{\"id\":555,\"type\":\"private\"},\"from\":{\"id\":555,\"first_name\":\"Gui\"}," + json + "}}");
    }

    TelegramUserResolver.LinkedUser linked(List<String> perms) {
        return new TelegramUserResolver.LinkedUser(USER, TG, CONV, perms, List.of());
    }

    @Test void textReachesLeoUnchanged() throws Exception {
        when(users.resolve(CO, TG)).thenReturn(Optional.of(linked(List.of("ai:chat"))));
        when(leo.chat(eq(CO), eq(USER), eq(List.of("ai:chat")), any()))
                .thenReturn(new AiChatResponse(CONV, "ok", "openai"));
        p.process(CO, "TOK", msg("\"text\":\"  marca a Maria dia 13 às 14h \""));
        ArgumentCaptor<AiChatRequest> req = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(leo).chat(eq(CO), eq(USER), any(), req.capture());
        assertEquals("  marca a Maria dia 13 às 14h ", req.getValue().message());
        assertEquals(CONV, req.getValue().conversationId());
        verify(bot).sendMessage("TOK", TG, "ok", null);
    }

    @Test void unlinkedSenderNeverReachesLeo() throws Exception {
        when(users.resolve(CO, TG)).thenReturn(Optional.empty());
        p.process(CO, "TOK", msg("\"text\":\"oi\""));
        verifyNoInteractions(leo);
        verify(bot).sendMessage(eq("TOK"), eq(TG), contains("Conta não vinculada"), isNull());
    }

    @Test void linkCommandCallsService() throws Exception {
        when(links.link(CO, "123456", TG, TG, "Gui")).thenReturn(TelegramLinkService.LinkResult.LINKED);
        p.process(CO, "TOK", msg("\"text\":\"/vincular 123456\""));
        verify(bot).sendMessage(eq("TOK"), eq(TG), contains("vinculado"), isNull());
        verifyNoInteractions(leo);
    }

    @Test void voiceIsTranscribedPassedUnchangedAndEchoed() throws Exception {
        when(users.resolve(CO, TG)).thenReturn(Optional.of(linked(List.of("ai:chat"))));
        when(bot.downloadFile("TOK", "F1")).thenReturn(new byte[]{1});
        when(media.transcribe(new byte[]{1}, "audio/ogg")).thenReturn("marca a Maria");
        when(leo.chat(any(), any(), any(), any())).thenReturn(new AiChatResponse(CONV, "Certo.", "openai"));
        p.process(CO, "TOK", msg("\"voice\":{\"file_id\":\"F1\",\"mime_type\":\"audio/ogg\"}"));
        ArgumentCaptor<AiChatRequest> req = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(leo).chat(any(), any(), any(), req.capture());
        assertEquals("marca a Maria", req.getValue().message());
        verify(bot).sendMessage("TOK", TG, "🎙️ Entendi: \"marca a Maria\"\n\nCerto.", null);
    }

    @Test void proposedActionGetsConfirmButtons() throws Exception {
        when(users.resolve(CO, TG)).thenReturn(Optional.of(linked(List.of("ai:chat"))));
        AiActionResponse a = new AiActionResponse(ACTION, CONV, "cancel_appointment", null, null,
                "Cancelar consulta de Maria 13/11 14h", "PROPOSED", Map.of(), null, null, null, null);
        when(leo.chat(any(), any(), any(), any())).thenReturn(new AiChatResponse(CONV, "Confirma?", "openai", List.of(a)));
        p.process(CO, "TOK", msg("\"text\":\"cancela a Maria\""));
        verify(bot).sendMessage("TOK", TG, "Cancelar consulta de Maria 13/11 14h",
                List.of(List.of(new TelegramBotClient.Button("✅ Confirmar", "c:" + ACTION),
                                new TelegramBotClient.Button("❌ Cancelar", "x:" + ACTION))));
    }

    @Test void confirmCallbackUsesClickerIdentity() throws Exception {
        when(users.resolve(CO, TG)).thenReturn(Optional.of(linked(List.of("ai:chat", "appointment:update"))));
        AiActionResponse done = new AiActionResponse(ACTION, CONV, "t", null, null, "Cancelar consulta", "EXECUTED",
                Map.of(), null, null, null, null);
        when(actions.confirm(CO, USER, List.of("ai:chat", "appointment:update"), ACTION)).thenReturn(done);
        JsonNode cb = om.readTree("{\"update_id\":2,\"callback_query\":{\"id\":\"cb1\",\"from\":{\"id\":555},\"data\":\"c:" + ACTION
                + "\",\"message\":{\"message_id\":9,\"chat\":{\"id\":555,\"type\":\"private\"}}}}");
        p.process(CO, "TOK", cb);
        verify(bot).answerCallback("TOK", "cb1");
        verify(bot).editMessage("TOK", 555L, 9L, "✅ Feito: Cancelar consulta");
    }

    @Test void callbackFromOtherUsersActionIsRejectedByUseCase() throws Exception {
        when(users.resolve(CO, TG)).thenReturn(Optional.of(linked(List.of("ai:chat"))));
        when(actions.confirm(any(), any(), any(), any())).thenThrow(new AiActionNotFoundException("Acao nao encontrada ou sem acesso."));
        JsonNode cb = om.readTree("{\"update_id\":2,\"callback_query\":{\"id\":\"cb1\",\"from\":{\"id\":555},\"data\":\"c:" + ACTION
                + "\",\"message\":{\"message_id\":9,\"chat\":{\"id\":555,\"type\":\"private\"}}}}");
        p.process(CO, "TOK", cb);
        verify(bot).editMessage(eq("TOK"), eq(555L), eq(9L), startsWith("⚠️"));
    }

    @Test void withoutAiChatPermissionIsBlocked() throws Exception {
        when(users.resolve(CO, TG)).thenReturn(Optional.of(linked(List.of())));
        p.process(CO, "TOK", msg("\"text\":\"oi\""));
        verifyNoInteractions(leo);
    }

    @Test void groupMessagesAreIgnored() throws Exception {
        JsonNode g = om.readTree("{\"update_id\":3,\"message\":{\"chat\":{\"id\":-1,\"type\":\"group\"},\"from\":{\"id\":555},\"text\":\"oi\"}}");
        p.process(CO, "TOK", g);
        verifyNoInteractions(bot, leo, users);
    }

    @Test void rateLimitedSenderGetsWarning() throws Exception {
        when(limiter.tryAcquire(CO, TG)).thenReturn(false);
        p.process(CO, "TOK", msg("\"text\":\"oi\""));
        verifyNoInteractions(leo);
        verify(bot).sendMessage(eq("TOK"), eq(TG), contains("Muitas mensagens"), isNull());
    }
}

class TelegramRateLimiterTest {
    @Test void allowsTwentyPerMinuteThenBlocks() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-04T10:00:00Z"));
        TelegramRateLimiter l = new TelegramRateLimiter(clock, 20);
        UUID co = UUID.randomUUID();
        for (int i = 0; i < 20; i++) assertTrue(l.tryAcquire(co, 1L));
        assertFalse(l.tryAcquire(co, 1L));
        assertTrue(l.tryAcquire(co, 2L));
        clock.advance(Duration.ofSeconds(61));
        assertTrue(l.tryAcquire(co, 1L));
    }

    static final class MutableClock extends Clock {
        private Instant now;
        MutableClock(Instant now) { this.now = now; }
        void advance(Duration d) { now = now.plus(d); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
```

Confira a exceção que o `AiActionService.requireOwner` lança (`grep -n "requireOwner" -A6 backend/src/main/java/com/becommerce/crm/automation/ai/application/action/AiActionService.java`) e use a classe real no teste.

- [ ] **Step 2: Peça ao usuário para rodar** `cd backend && mvn -q test -Dtest='TelegramUpdateProcessorTest,TelegramRateLimiterTest'`. O esperado é FAIL.

- [ ] **Step 3: Implementar o limitador**

```java
@Component
public class TelegramRateLimiter {
    private final Clock clock;
    private final int perMinute;
    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    @Autowired
    public TelegramRateLimiter(Clock clock) { this(clock, 20); }
    TelegramRateLimiter(Clock clock, int perMinute) { this.clock = clock; this.perMinute = perMinute; }

    public boolean tryAcquire(UUID companyId, long telegramUserId) {
        Instant now = clock.instant(), cutoff = now.minusSeconds(60);
        Deque<Instant> q = hits.computeIfAbsent(companyId + ":" + telegramUserId, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst().isBefore(cutoff)) q.pollFirst();
            if (q.size() >= perMinute) return false;
            q.addLast(now);
            return true;
        }
    }
}
```

- [ ] **Step 4: Implementar o processador**

```java
@Service
public class TelegramUpdateProcessor {
    private static final Logger log = LoggerFactory.getLogger(TelegramUpdateProcessor.class);
    static final String NOT_LINKED = "Conta não vinculada. Gere o código no seu perfil do CRM (Perfil → Conectar Telegram) e envie /vincular CÓDIGO.";

    private final TelegramBotClient bot;
    private final TelegramLinkService links;
    private final TelegramUserResolver users;
    private final AiAssistantUseCase leo;
    private final AiActionUseCase actions;
    private final AiMediaProvider media;
    private final TelegramRateLimiter limiter;

    public TelegramUpdateProcessor(TelegramBotClient bot, TelegramLinkService links, TelegramUserResolver users,
                                   AiAssistantUseCase leo, AiActionUseCase actions, AiMediaProvider media,
                                   TelegramRateLimiter limiter) {
        this.bot = bot; this.links = links; this.users = users; this.leo = leo;
        this.actions = actions; this.media = media; this.limiter = limiter;
    }

    public void process(UUID companyId, String token, JsonNode update) {
        if (update.has("callback_query")) { handleCallback(companyId, token, update.get("callback_query")); return; }
        JsonNode m = update.path("message");
        if (m.isMissingNode() || !"private".equals(m.path("chat").path("type").asText())) return;
        long chatId = m.path("chat").path("id").asLong();
        long from = m.path("from").path("id").asLong();
        if (!limiter.tryAcquire(companyId, from)) { send(token, chatId, "Muitas mensagens em pouco tempo. Aguarde um minuto."); return; }

        String text = m.has("text") ? m.get("text").asText() : null;
        if (text != null && text.startsWith("/")) { if (handleCommand(companyId, token, chatId, from, m, text)) return; }

        Optional<TelegramUserResolver.LinkedUser> user = users.resolve(companyId, from);
        if (user.isEmpty()) { send(token, chatId, NOT_LINKED); return; }
        if (!user.get().permissions().contains("ai:chat") && !user.get().roles().contains(CurrentUser.CRM_ADMIN_ROLE)) {
            send(token, chatId, "Seu usuário não tem acesso ao assistente."); return;
        }

        String prefix = "";
        if (text == null) {
            JsonNode voice = m.has("voice") ? m.get("voice") : m.get("audio");
            if (voice == null) { send(token, chatId, "Por enquanto entendo texto e áudio."); return; }
            try {
                byte[] audio = bot.downloadFile(token, voice.path("file_id").asText());
                text = media.transcribe(audio, voice.path("mime_type").asText("audio/ogg"));
            } catch (RuntimeException e) {
                log.warn("[TELEGRAM] falha na transcrição (company={})", companyId, e);
                send(token, chatId, "Não entendi o áudio, pode repetir ou escrever?"); return;
            }
            if (text == null || text.isBlank()) { send(token, chatId, "Não entendi o áudio, pode repetir ou escrever?"); return; }
            prefix = "🎙️ Entendi: \"" + text + "\"\n\n";
        }
        askLeo(companyId, token, chatId, from, user.get(), text, prefix);
    }

    /** true se o comando foi tratado aqui; false se deve seguir para o Leo. */
    private boolean handleCommand(UUID companyId, String token, long chatId, long from, JsonNode m, String text) {
        String[] parts = text.trim().split("\\s+", 2);
        String cmd = parts[0].toLowerCase(Locale.ROOT).replaceAll("@.*$", "");
        switch (cmd) {
            case "/start" -> send(token, chatId, "Olá! Sou o Leo, assistente do CRM. Para começar, gere o código no seu perfil do CRM e envie /vincular CÓDIGO.");
            case "/vincular" -> {
                String name = m.path("from").path("first_name").asText(null);
                var r = links.link(companyId, parts.length > 1 ? parts[1] : null, from, chatId, name);
                send(token, chatId, switch (r) {
                    case LINKED -> "✅ Telegram vinculado. Pode falar comigo por texto ou áudio.";
                    case INVALID_CODE -> "Código inválido ou vencido. Gere outro no seu perfil do CRM.";
                    case TELEGRAM_ALREADY_LINKED -> "Este Telegram já está vinculado a outro usuário.";
                });
            }
            case "/desvincular" -> send(token, chatId, links.unlinkByTelegram(companyId, from) ? "Telegram desvinculado." : NOT_LINKED);
            case "/nova" -> {
                if (users.resolve(companyId, from).isEmpty()) send(token, chatId, NOT_LINKED);
                else { links.setConversation(companyId, from, null); send(token, chatId, "Nova conversa iniciada."); }
            }
            default -> { return false; }
        }
        return true;
    }

    private void askLeo(UUID companyId, String token, long chatId, long from,
                        TelegramUserResolver.LinkedUser user, String text, String prefix) {
        AiChatResponse r;
        try {
            r = leo.chat(companyId, user.userId(), user.permissions(), new AiChatRequest(text, user.conversationId(), null));
        } catch (RuntimeException e) {
            log.error("[TELEGRAM] falha no Leo (company={})", companyId, e);
            send(token, chatId, "Não consegui processar agora, tente de novo."); return;
        }
        if (r.conversationId() != null && !r.conversationId().equals(user.conversationId())) {
            links.setConversation(companyId, from, r.conversationId());
        }
        send(token, chatId, prefix + (r.message() == null ? "" : r.message()));
        for (AiActionResponse a : r.actions() == null ? List.<AiActionResponse>of() : r.actions()) {
            if (!"PROPOSED".equals(a.status())) continue;
            bot.sendMessage(token, chatId, a.description(), List.of(List.of(
                    new TelegramBotClient.Button("✅ Confirmar", "c:" + a.id()),
                    new TelegramBotClient.Button("❌ Cancelar", "x:" + a.id()))));
        }
    }

    private void handleCallback(UUID companyId, String token, JsonNode cb) {
        String data = cb.path("data").asText("");
        long from = cb.path("from").path("id").asLong();
        long chatId = cb.path("message").path("chat").path("id").asLong();
        long messageId = cb.path("message").path("message_id").asLong();
        safe(() -> bot.answerCallback(token, cb.path("id").asText()));
        if (data.length() < 3 || data.charAt(1) != ':') return;
        UUID actionId;
        try { actionId = UUID.fromString(data.substring(2)); } catch (IllegalArgumentException e) { return; }

        Optional<TelegramUserResolver.LinkedUser> user = users.resolve(companyId, from);
        if (user.isEmpty()) { send(token, chatId, NOT_LINKED); return; }
        String result;
        try {
            if (data.charAt(0) == 'c') {
                AiActionResponse a = actions.confirm(companyId, user.get().userId(), user.get().permissions(), actionId);
                result = "EXECUTED".equals(a.status()) ? "✅ Feito: " + a.description()
                        : "⚠️ Não foi possível: " + (a.errorMessage() != null ? a.errorMessage() : a.status());
            } else {
                actions.cancel(companyId, user.get().userId(), actionId);
                result = "❌ Cancelado.";
            }
        } catch (RuntimeException e) {
            result = "⚠️ Não foi possível: " + (e.getMessage() != null ? e.getMessage() : "erro inesperado");
        }
        String finalResult = result;
        safe(() -> bot.editMessage(token, chatId, messageId, finalResult));
    }

    private void send(String token, long chatId, String text) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try { bot.sendMessage(token, chatId, text, null); return; }
            catch (TelegramBotClient.TelegramApiException e) {
                if (attempt == 3) log.warn("[TELEGRAM] envio falhou após 3 tentativas (chat={})", chatId);
            }
        }
    }

    private static void safe(Runnable r) { try { r.run(); } catch (RuntimeException ignored) { } }
}
```

**Auditoria com origem telegram.** O processador recebe também `TenantAuditRecorder auditor` no construtor (adicione `@Mock TenantAuditRecorder auditor` no teste e passe-o ao construtor). Depois de cada `confirm` ou `cancel` bem-sucedido em `handleCallback`, registre:

```java
private void auditChannel(UUID companyId, UUID userId, UUID actionId, String event, String description) {
    try {
        auditor.record(companyId, AuditAction.CUSTOM, AuditModule.AI, "AiAction", actionId.toString(),
                description + " (via Telegram)", userId,
                Map.of("event", event, "channel", "telegram", "actionId", actionId.toString()));
    } catch (RuntimeException ignored) { }
}
// confirm: auditChannel(companyId, userId, actionId, "AI_ACTION_CONFIRMED_VIA_TELEGRAM", a.description());
// cancel:  auditChannel(companyId, userId, actionId, "AI_ACTION_CANCELLED_VIA_TELEGRAM", "Ação cancelada");
```

Copie os argumentos de `record` do `AiActionService.audit`, porque a assinatura real manda. Adicione ao `confirmCallbackUsesClickerIdentity` a verificação `verify(auditor).record(eq(CO), any(), any(), eq("AiAction"), eq(ACTION.toString()), contains("via Telegram"), eq(USER), argThat(m -> "telegram".equals(m.get("channel"))))`, ajustada à assinatura real.

Use o `CurrentUser.CRM_ADMIN_ROLE` que já existe. Se o `AiAssistantService.chat` validar `ai:chat` por conta própria, a checagem aqui é só para dar uma mensagem amigável.

- [ ] **Step 5: Peça ao usuário para rodar o mesmo comando.** O esperado é PASS.

- [ ] **Step 6: Commit** com `git commit -m "feat(telegram): Leo por texto, áudio e botões de confirmação"`.

---

### Task 7: Limpeza periódica

**Files:**
- Create: `communication/telegram/infrastructure/TelegramCleanupScheduler.java`

- [ ] **Step 1: Implementar** (sem tenant: a limpeza roda como tarefa de sistema, no mesmo padrão do `CampaignScheduler`)

```java
@Component
public class TelegramCleanupScheduler {
    private final TelegramUpdateSeenJpaRepository seen;
    public TelegramCleanupScheduler(TelegramUpdateSeenJpaRepository seen) { this.seen = seen; }

    @Scheduled(cron = "${app.telegram.cleanup-cron:0 30 4 * * *}")
    @Transactional
    public void purge() { seen.deleteOlderThan(LocalDateTime.now().minusDays(2)); }
}
```

Com `FORCE ROW LEVEL SECURITY`, um `DELETE` sem tenant não apaga nada. Veja como o `CampaignScheduler` percorre as empresas (`sed -n 30,70p backend/src/main/java/com/becommerce/crm/automation/campaign/infrastructure/scheduler/CampaignScheduler.java`) e repita o mesmo laço: para cada empresa com `telegram_bot_config`, defina o tenant e apague. Se ele usar outro mecanismo, como um papel de sistema que ignora o RLS, use esse.

- [ ] **Step 2: Compilar.** Peça ao usuário para rodar `cd backend && mvn -q -DskipTests compile`.

- [ ] **Step 3: Commit** com `git commit -m "feat(telegram): limpeza dos updates processados"`.

---

### Task 8: Frontend — configuração e vínculo

**Files:**
- Create: `frontend-refine/src/features/communication/telegram/api.ts`
- Create: `frontend-refine/src/features/communication/telegram/TelegramBotCard.tsx`
- Create: `frontend-refine/src/features/communication/telegram/TelegramLinkCard.tsx`
- Create: `frontend-refine/src/app/(dashboard)/settings/telegram/page.tsx`
- Modify: `frontend-refine/src/lib/constants.ts` (rota `SETTINGS_TELEGRAM`)
- Modify: `frontend-refine/src/components/layout/navigation.ts` (item depois de "E-mail")
- Modify: `frontend-refine/src/app/(dashboard)/profile/page.tsx` (renderiza `TelegramLinkCard`)

**Interfaces:**
- Consumes: os endpoints das Tasks 3 e 4 em `/api/v1/companies/{companyId}/telegram`

- [ ] **Step 1: Cliente da API**

```ts
// features/communication/telegram/api.ts
import api from "@/lib/api";

export type TelegramConfig = {
  configured: boolean;
  botUsername: string | null;
  enabled: boolean;
  updatedAt: string | null;
};
export type TelegramLink = { userId: string; telegramName: string | null; createdAt: string };
export type LinkCode = { code: string; botUsername: string; expiresInMinutes: number };

const base = (companyId: string) => `/companies/${companyId}/telegram`;

export const telegramApi = {
  get: (c: string) => api.get<TelegramConfig>(base(c)).then((r) => r.data),
  save: (c: string, token: string) => api.put<TelegramConfig>(base(c), { token }).then((r) => r.data),
  remove: (c: string) => api.delete(base(c)),
  links: (c: string) => api.get<TelegramLink[]>(`${base(c)}/links`).then((r) => r.data),
  revoke: (c: string, userId: string) => api.delete(`${base(c)}/links/${userId}`),
  me: (c: string) => api.get<TelegramLink | "">(`${base(c)}/me`).then((r) => (r.status === 204 ? null : (r.data as TelegramLink))),
  unlinkMe: (c: string) => api.delete(`${base(c)}/me`),
  linkCode: (c: string) => api.post<LinkCode>(`${base(c)}/link-code`).then((r) => r.data),
};
```

Confira o prefixo do `api` em `frontend-refine/src/lib/api.ts`. Se a `baseURL` não incluir `/api/v1`, copie o formato de URL do `CompanyMailCard.tsx`.

- [ ] **Step 2: `TelegramBotCard`** (administrador). Siga o visual do `CompanyMailCard.tsx`: `Card`, `Input type="password"`, botões e `toast`.
  - Sem configuração: um campo "Token do bot (@BotFather)" e o botão "Conectar bot".
  - Configurado: um `Badge` "Conectado como @{botUsername}" e o botão "Desconectar", que pede confirmação com o `AlertDialog` do projeto (nunca `window.confirm`).
  - Abaixo, a lista "Pessoas vinculadas", com o nome do Telegram, a data e o botão "Revogar".
  - Query keys: `["telegram", companyId]` e `["telegram-links", companyId]`. Cada mutation invalida as duas.
  - Erros: `error.response.data.message`, com o mesmo helper `errorMessage` do `CompanyMailCard`.

- [ ] **Step 3: `TelegramLinkCard`** (qualquer usuário com `ai:chat`).
  - Já vinculado: "Vinculado ao Telegram de {telegramName}" e o botão "Desconectar".
  - Não vinculado: botão "Conectar Telegram". Ao clicar, mostra o código grande, o texto "Envie `/vincular {code}` para @{botUsername} em até 10 minutos" e o link `https://t.me/{botUsername}`. Por 10 minutos, refaz a busca de `me` a cada 5 s e, quando vincular, mostra o toast "Telegram vinculado".
  - Se o bot não estiver configurado (erro 4xx no `linkCode`), mostra a mensagem do backend.
  - Query key: `["telegram-me", companyId]`.

- [ ] **Step 4: Página, rota e menu**

```ts
// lib/constants.ts, logo abaixo de SETTINGS_EMAIL
  SETTINGS_TELEGRAM: "/settings/telegram",
```

```ts
// components/layout/navigation.ts, logo depois do item "E-mail"
      {
        label: "Telegram (Leo)",
        href: ROUTES.SETTINGS_TELEGRAM,
        icon: Send,
        permission: "company:update",
        module: "AI_AGENT",
      },
```

Importe `Send` de `lucide-react` se ainda não estiver importado. A página `settings/telegram/page.tsx` segue a `settings/email/page.tsx`, com `PageTitle` "Telegram (Leo)", o subtítulo "Converse com o Leo pelo Telegram, por texto ou áudio." e o `<TelegramBotCard companyId={companyId} />`. No `profile/page.tsx`, renderize `<TelegramLinkCard companyId={user.companyId} />` abaixo do card de dados pessoais, só quando houver `companyId`.

- [ ] **Step 5: Verificar**

```bash
cd frontend-refine
npx tsc --noEmit
npx eslint src/features/communication/telegram "src/app/(dashboard)/settings/telegram" "src/app/(dashboard)/profile/page.tsx" src/components/layout/navigation.ts src/lib/constants.ts
npx prettier --check --end-of-line auto src/features/communication/telegram "src/app/(dashboard)/settings/telegram/page.tsx" "src/app/(dashboard)/profile/page.tsx" src/components/layout/navigation.ts src/lib/constants.ts
```

O esperado é tudo sem erros. Se o Prettier reclamar, rode `npx prettier --write --end-of-line auto` nos mesmos arquivos e cheque de novo.

- [ ] **Step 6: Commit** com `git commit -m "feat(telegram): telas de configuração do bot e vínculo no perfil"`.

---

### Task 9: Configuração de produção e verificação final

**Files:**
- Modify: o `.env` de exemplo usado no deploy (procure com `grep -rln "OPENAI_API_KEY" --include=*.example --include=*.env* . | grep -v node_modules`) para incluir `TELEGRAM_WEBHOOK_BASE_URL=`
- Modify: `docker-compose.yml` ou o compose de produção usado pelo `deploy.sh`, para repassar `TELEGRAM_WEBHOOK_BASE_URL` ao backend (no mesmo formato de `OPENAI_API_KEY`)

- [ ] **Step 1: Adicionar a variável** nos arquivos acima, com o comentário `# URL pública do backend, ex.: https://api.suaclinica.com.br (Telegram chama /api/v1/telegram/webhook/{companyId})`.

- [ ] **Step 2: Rodar a suíte completa.** Peça ao usuário para rodar `cd backend && mvn -q test`. O esperado é tudo verde. Se falhar, corrija antes de seguir.

- [ ] **Step 3: Revisar o próprio diff.** Rode `git diff origin/crm-improvements-deploy-phase --stat` e confira que:
  - nenhum log contém o token (`grep -rn "token" backend/src/main/java/com/becommerce/crm/communication/telegram | grep -i "log\."`);
  - o texto do usuário não passa por `trim`, `strip` ou `replace` antes do `chat` (o comando `/vincular` pode usar `strip` no código, o texto livre não).

- [ ] **Step 4: Commit** com `git commit -m "chore(telegram): variável de webhook para produção"`.

- [ ] **Step 5: Roteiro de teste manual** (depois do deploy, feito pelo usuário):
  1. Defina `TELEGRAM_WEBHOOK_BASE_URL` na VPS e faça o deploy.
  2. Em Configurações → Telegram (Leo), cole o token do bot "Leo". O esperado é "Conectado como @…".
  3. Em Perfil → Conectar Telegram, envie `/vincular CÓDIGO` ao bot. O esperado é "✅ Telegram vinculado".
  4. Envie "quais consultas tenho amanhã?". O Leo responde.
  5. Envie um áudio pedindo para cancelar uma consulta de teste. O esperado é "🎙️ Entendi: …" seguido dos botões; clique em ✅ e confira no CRM e na auditoria.
  6. Envie mensagem de outra conta do Telegram, sem vínculo. O esperado é "Conta não vinculada".
