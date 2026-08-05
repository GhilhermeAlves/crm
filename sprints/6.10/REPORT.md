# Sprint 6.10 — Production Infrastructure Hardening & Final Closure

**Data:** 2026-08-05 · **Ambiente:** `https://srv1348261.hstgr.cloud` (VPS `76.13.237.238`) · **Status:** ✅ Concluída

## Identificação

- **Sprint:** 6.10
- **Nome:** Production Infrastructure Hardening & Final Closure (etapa auth/Gateway)
- **Responsável:** AI Agent
- **Fase:** Segurança — Access Gateway
- **Dependência:** Sprint 6.9

## Objetivo

Eliminar as exposições desnecessárias da infraestrutura de produção, endurecer os
`docker-compose.yml` da VPS (bind `127.0.0.1:` nas portas publicadas + remoção de credenciais
default hardcoded), desabilitar a documentação da API (`/docs`) em produção, restringir o
actuator a `/actuator/health`, ativar o profile `prod` nos serviços de aplicação e executar a
validação final completa (local → commit → deploy controlado → validação externa/interna →
regressão 6.6–6.9 → GO/NO-GO), encerrando a etapa auth/Gateway.

## Escopo

- Endurecimento dos composes de **infraestrutura** (`docker-compose.yml`) e de **aplicação**
  (`docker/docker-compose.yml`).
- Remoção de credenciais/defaults inseguros dos arquivos (sem rotação de senhas vivas).
- Recondicionamento do compose de aplicação à configuração real em execução na VPS (drift).
- Backend: `SPRING_PROFILES_ACTIVE=prod`, springdoc desabilitado em prod, actuator
  restrito a `health`.
- Nginx de produção: remoção de `/docs/`, restrição do `/actuator/` a `/actuator/health`,
  `server_tokens off`, config gerenciada no repositório (`docker/nginx/crm.conf`).
- Backup obrigatório, deploy controlado e validação completa com regressão 6.6–6.9.
- Documentação de fechamento (`sprints/6.10/REPORT.md`) + índice.

## Decisões de escopo (confirmadas pelo usuário)

1. **"Rotação de credenciais" = remoção de credenciais hardcoded dos composes.** Nenhuma senha
   viva dos datastores foi alterada (trocar `POSTGRES_PASSWORD` via env não altera a senha real
   do Postgres já inicializado e causaria indisponibilidade total; Keycloak/RabbitMQ/MinIO
   exigiriam `ALTER`/`change_password` coordenados). Mantidas as credenciais reais do `.env`.
2. **Reconciliar o compose local à VPS + endurecer.** O `docker/docker-compose.yml` local estava
   desatualizado em relação à configuração em execução (`AUTH_GATEWAY_*`, `extra_hosts`,
   healthcheck do auth-service). Foi reconciliado e endurecido, depois deployado.
3. **Trocar `SPRING_PROFILES_ACTIVE` de `dev` para `prod`** no backend e auth-service.
4. **Restringir o actuator** a `/actuator/health` (nginx) + exposição `health` no profile prod.

## Auditoria pré-execução (estado da VPS)

- Containers em execução (todos `Up`): `crm-frontend`, `crm-auth-service` (healthy),
  `crm-backend`, `crm-keycloak` (healthy), `crm-minio` (healthy), `crm-rabbitmq` (healthy),
  `crm-postgres` (healthy), `crm-redis` (healthy). Todos na rede `crm-network`.
- Rede `docker_crm-network` existe mas **não é usada** por nenhum container (sobra).
- Firewall: `ufw` ativo (22/80/443); script `crm-docker-port-block.sh` + unit systemd
  `crm-docker-port-block.service` (enabled) com DROP em INPUT/FORWARD/DOCKER-USER para as portas
  internas. Contador DOCKER-USER = 21 pacotes dropados (varredura externa real).
- Nginx `sites-available/crm`: `/api/`→8082, `/docs/`→8081, `/actuator/`→8081,
  `/auth/`→8082, `/realms//resources//js/`→8080, `/`→3000.

## ACHADO — drift do compose de aplicação (local × VPS)

O `docker/docker-compose.yml` local **não** correspondia à configuração em execução na VPS:

- A VPS tinha `AUTH_GATEWAY_ENABLED/CLIENT_ID/CLIENT_SECRET/REDIRECT_URI/...`, `extra_hosts`
  (`srv1348261.hstgr.cloud:76.13.237.238`), healthcheck do auth-service e frontend **sem**
  `NEXT_PUBLIC_API_URL` (a VPS roda sem essas envs; o frontend usa base relativa `/api/v1`).
- A VPS tinha `CRM_DATABASE_USERNAME=postgres` / `CRM_DATABASE_PASSWORD=postgres` hardcoded no
  backend (inócuos pois `SPRING_DATASOURCE_*` sobrescrevem via env, porém perigosos como fallback).
- O compose local da infraestrutura (`docker-compose.yml` raiz) estava atrás do da VPS
  (keycloak publicava `8080:8080` em vez de `127.0.0.1:8080:8080` e healthcheck antigo).

## ACHADO — serviços datastore vestigiais com credenciais default

O compose de aplicação (`/opt/crm/docker/docker-compose.yml`) declarava serviços
`postgres`/`redis`/`rabbitmq`/`minio` **que não são os containers reais** (os reais pertencem ao
projeto `crm-infrastructure`), mas com **credenciais default hardcoded**:

- `POSTGRES_USER/PASSWORD=postgres/postgres`
- `RABBITMQ_DEFAULT_USER/PASS=guest/guest`
- `MINIO_ROOT_USER/PASSWORD=minioadmin/minioadmin`
- Redis sem senha

Confirmado em runtime que os containers reais não usavam esses defaults: RabbitMQ mgmt
retornou `401` para `guest:guest`; MinIO usa `MINIO_ROOT_USER` do `.env`; Postgres usa
`crm_admin`; Redis exige senha (`NOAUTH Authentication required.`). Mesmo assim, o bloco era um
risco de configuração (basta um `docker compose up -d` no diretório errado para tentar
recriar esses serviços com credenciais inseguras). **Removido.**

## ACHADO — portas publicadas em 0.0.0.0

Apesar do firewall (que já bloqueava o caminho externo desde a 6.9), os composes publicavam os
serviços na interface pública:

| Porta | Serviço | Compose 6.10 (antes) |
|-------|---------|----------------------|
| 5432 | PostgreSQL | `0.0.0.0` |
| 6379 | Redis | `0.0.0.0` |
| 5672/15672 | RabbitMQ | `0.0.0.0` |
| 9000/9001 | MinIO | `0.0.0.0` |
| 8081 | backend | `0.0.0.0` |
| 8082 | auth-service | `0.0.0.0` |
| 3000 | frontend | `0.0.0.0` |
| 8080 | Keycloak | `127.0.0.1` (já correto na VPS) |

## ACHADO — springdoc e actuator ativos em produção

- Backend rodava com `SPRING_PROFILES_ACTIVE=dev` na VPS → springdoc **habilitado**, servindo
  o OpenAPI em `/docs` (JSON) na porta interna 8081 (confirmado `curl :8081/docs` → `200`).
- `/actuator/**` permitAll no backend e exposto pelo nginx (`/actuator/health` → `200` externo;
  `info/metrics/prometheus` também expostos pela configuração base).
- Nginx mantinha `location /docs/` (que, na prática, já 404ava por mismatch de caminho, mas
  permanecia como superfície de exposição).

## Backup (obrigatório, antes de qualquer alteração)

Caminho: `/opt/crm/backups/sprint-6.10-pre-20260805-104558/infra/`

- `docker-compose.yml` (infra), `docker-compose.app.yml`, `docker-compose.prod.yml`,
  `docker-compose.dev.yml`
- `env.root`, `env.docker` (chmod 600 — conteúdo **não** registrado; apenas caminho)
- `nginx.crm.conf`, `nginx.nginx.conf`, `crm-docker-port-block.service`,
  `crm-docker-port-block.sh`, `ufw.status.txt`, `docker.ps.txt`
- `application-prod.yml.bak-6.10-pre` (adicionado antes do scp do backend)
- Adicionais de rollback: `/etc/nginx/sites-available/crm.bak-6.10-pre`

## Alterações locais — compose de infraestrutura (`docker-compose.yml`)

- Reconciliado com a VPS: keycloak `127.0.0.1:8080:8080` + healthcheck `GET /health` em :9000.
- **Todas as portas publicadas restritas ao loopback:**
  - postgres `127.0.0.1:5432`, redis `127.0.0.1:6379`,
    rabbitmq `127.0.0.1:5672` + `127.0.0.1:15672`,
    minio `127.0.0.1:9000` + `127.0.0.1:9001`.
- Credenciais permanecem 100% via `.env` (`${VAR:?required}`) — nenhum default inseguro.

## Alterações locais — compose de aplicação (`docker/docker-compose.yml`)

- **Removidos** os serviços datastore vestigiais (`postgres`/`redis`/`rabbitmq`/`minio`),
  seus volumes e `depends_on` (os datastores reais vivem no projeto `crm-infrastructure`).
- **Portas restritas ao loopback:** backend `127.0.0.1:8081`, auth-service `127.0.0.1:8082`,
  frontend `127.0.0.1:3000`.
- **`SPRING_PROFILES_ACTIVE=prod`** no backend e auth-service.
- `CRM_DATABASE_USERNAME/PASSWORD` agora derivados do `.env` (`${SPRING_DATASOURCE_USERNAME}` /
  `${SPRING_DATASOURCE_PASSWORD}`) — removido `postgres/postgres` hardcoded.
- Reconciliado com a VPS: bloco completo `AUTH_GATEWAY_*`, `extra_hosts`, healthcheck do
  auth-service, frontend sem envs `NEXT_PUBLIC_*`.

## Alterações locais — backend (`application-prod.yml`)

- `springdoc.api-docs.enabled: false` e `springdoc.swagger-ui.enabled: false` (prod → `/docs` 404).
- `management.endpoints.web.exposure.include: health` (prod → só health exposto).
- Nenhuma mudança em código Java nem no `SecurityConfig` (o permitAll de `/docs/**` fica inócuo
  com o springdoc desabilitado; a exposição do actuator é limitada no Spring + nginx).

## Alterações locais — nginx (config gerenciada no repositório)

Novo `docker/nginx/crm.conf` (fonte da verdade para `/etc/nginx/sites-available/crm`):

- Removida a `location /docs/`.
- `location = /actuator/health` (exata, proxy→8081) + `location /actuator/ { return 404; }`.
- `server_tokens off`.
- Demais rotas (`/api/`, `/auth/`, `/realms/`, `/resources/`, `/js/`, `/`) inalteradas.

## Testes locais (antes do commit/deploy)

- **Backend:** `mvn clean verify` → **87 testes, 0 falhas, BUILD SUCCESS** (config-only).
- **Auth-service:** `mvn clean verify` → **227 testes, 0 falhas, BUILD SUCCESS**.
- **Frontend:** lint PASS (0 erros; warnings pré-existentes) · typecheck PASS ·
  tests **30/30 PASS** · build PASS.
- Composes validados com `docker compose config -q` (env placeholder local) e, na VPS, com o
  `.env` real — todos os `host_ip: 127.0.0.1` confirmados no resolved config.

## Commits

- `d5fb3da` — `chore(infra): harden production docker exposure`
  (composes + `docker/nginx/crm.conf`; remove defaults inseguros e exposições).
- `1bd9ca6` — `chore(auth): disable production api docs and restrict actuator`
  (`application-prod.yml`).
- `docs: document sprint 6.10 final closure` (este relatório + índice — ver "Resultado").

## Deploy (controlado, na VPS)

Ordem executada (com backups e validação a cada passo):

1. **Backup** pré-execução (caminho acima).
2. `scp` dos 4 arquivos: `application-prod.yml`, `docker-compose.yml` (raiz),
   `docker/docker-compose.yml`, `crm.conf` → staging.
3. Validação na VPS: `docker compose config -q` (app + infra, com `.env` real) — OK.
4. **Nginx**: `nginx -t` OK → `systemctl reload nginx` (sem downtime).
5. **Infra**: `docker compose --env-file .env up -d` → recriados apenas `crm-postgres`,
   `crm-redis`, `crm-rabbitmq`, `crm-minio` (mudança de bind); `crm-keycloak` **não** recriado
   (bind já era loopback). Dados preservados (bind volumes `./infrastructure/...`).
6. **Aplicação**: `docker compose build backend` (nova `application-prod.yml`) →
   `docker compose up -d --no-deps backend auth-service frontend` (recriados com binds loopback
   e profile prod).

## Validação — containers e healthchecks

```
crm-frontend      Up      (sem healthcheck — imagem sem wget; igual ao estado anterior)
crm-auth-service  Up healthy
crm-backend       Up      (sem healthcheck — runtime JRE sem wget/curl; igual ao anterior)
crm-rabbitmq      Up healthy
crm-postgres      Up healthy
crm-redis         Up healthy
crm-minio         Up healthy
crm-keycloak      Up healthy
```

- Backend: log confirma `The following 1 profile is active: "prod"` e
  `Tomcat started on port 8080 ... with context path '/'`; RabbitMQ conectado como
  `crm_rabbitmq@172.28.0.5:5672`; seeder de roles OK; nenhum erro de startup.
- Banco íntegro após recriação: `crm_main`, `keycloak_db` e demais bancos presentes.

## Validação — portas/loopback (evidência no host)

`ss -tlnp` na VPS — **todas** as publicações via docker-proxy agora em `127.0.0.1`:

```
127.0.0.1:5432  127.0.0.1:6379  127.0.0.1:5672  127.0.0.1:15672
127.0.0.1:9000  127.0.0.1:9001  127.0.0.1:8080  127.0.0.1:8081
127.0.0.1:8082  127.0.0.1:3000
```

Nenhum listener em `0.0.0.0`. `docker port <container>` confirma o bind loopback de cada um.

## Validação — reachability externa (cliente)

Do ambiente local contra `76.13.237.238`:

- `22`, `80`, `443` → **OPEN** (únicos serviços públicos).
- `5432`, `6379`, `5672`, `15672`, `9000`, `9001`, `8081`, `8082`, `3000` → **closed/filtered**.

Conclusão em duas camadas independentes: (1) firewall do host (ufw + INPUT/FORWARD/DOCKER-USER
DROP, persistido pelo unit systemd) e (2) containers sem publicação na interface pública —
mesmo que o firewall falhasse, as portas não estariam alcançáveis externamente.

## Validação — firewall

- `ufw status`: **active**, permitidos somente `22/tcp`, `80/tcp`, `443/tcp`.
- `iptables`: DROP `eth0` para as portas internas em INPUT posição 1, FORWARD posição 1 e
  DOCKER-USER; contador DOCKER-USER = 21 (varredura externa dropada); FORWARD drop = 0
  (nenhum pacote externo chega a essas portas).

## Validação — credenciais e defaults removidos

- **Redis**: `redis-cli ping` sem senha → `NOAUTH Authentication required.` (senha exigida).
- **RabbitMQ**: `guest:guest` no mgmt API → `401`; `rabbitmqctl authenticate_user crm_rabbitmq`
  → `Success` (credencial real do `.env`).
- **Postgres**: conexões usam `crm_admin` + senha do `.env` (nenhum `postgres/postgres` em
  compose).
- **MinIO**: `MINIO_ROOT_USER/PASSWORD` do `.env` (sem `minioadmin/minioadmin`).
- Nenhum arquivo de compose contém mais credenciais hardcoded.

## Validação — /docs desabilitado (produção)

- **Interno** (`curl localhost:8081/docs`) → **404** (springdoc desabilitado no profile prod).
- **Externo** (`https://.../docs/`) → **308** (normalização de trailing slash do Next.js) →
  `/docs` → **404**. Nenhuma rota serve a documentação.
- `/v3/api-docs` interno → 401 (protegido; não é o caminho do springdoc).

## Validação — /actuator restrito

- `https://.../actuator/health` → **200** `{"status":"UP"}` (mantido intencionalmente).
- `https://.../actuator/info` → **404** (bloqueado no nginx) e **404** interno (exposição
  restrita a `health` no profile prod). `metrics`/`prometheus`/demais → 404.

## Validação — login real (fluxo OIDC completo)

`repro69.py` (fluxo HTTP com cookie jar simulando o browser) — **todos os passos OK**:

1. `GET /auth/authorize?redirect=/dashboard` → 302 para o Keycloak (PKCE S256, `client_id=crm-gateway`).
2. Form de login do Keycloak renderizado (200).
3. POST de credenciais → 302 callback com `code` → troca no servidor → sessão criada
   (`crm_session` HttpOnly+Secure) → 302 `/dashboard`.
4. `GET /dashboard` → 200 (login **termina em /dashboard**).
5. `GET /api/v1/auth/me` → 200 (`e2e.tester@crm.local`, empresa `11111111-...`).
6. `POST /auth/refresh` sem header CSRF → 403 `CSRF_INVALID` (esperado).

## Validação — relay/BFF

- `/api/v1/health` sem sessão → 401 `SESSION_NOT_FOUND` (gateway bloqueia sem cookie).
- `/api/v1/auth/me` com sessão (via repro69) → 200. Relay com token da sessão íntegro.

## Regressão 6.6–6.9 (VPS)

| Suite | Resultado | Observação |
|-------|-----------|------------|
| 6.6 (`e2e_66.py`) | **22/22 PASS** | health/readiness, rate limit, login, relay, logout |
| 6.7 (`e2e_67.py`) | **17/17 PASS** | rate limit por usuário autenticado, redis down/up |
| 6.8 (`e2e_68.py`) | **20/20 PASS** | 1ª execução 16/20 com as mesmas 4 falhas de contaminação de bucket de rate limit documentadas na 6.9 (58/20 aceitas em vez de 60/20 por janela compartilhada) → **re-run isolado 20/20**; **não é regressão** |
| 6.9 (`repro69.py`) | **PASS** | login manual termina em `/dashboard`; `/me` 200; refresh sem CSRF 403 |

## Redis down/up

Coberto pelas suites: `e2e_67` e `e2e_68` executam cenários de Redis indisponível sob carga
concorrente (503/401 controlados, sem loop de erro) e recuperação (`redis-cli ping` → OK após
restart). Os WARNs de `RedisGatewaySessionStore`/`GatewayRateLimiter` nos logs do auth-service
correspondem a essa fase intencional de teste. Após o restart, `crm-redis` voltou healthy e a
sessão/rate limit voltaram a funcionar (re-login e relay OK nas suites subsequentes).

## Performance

- Sem degradação: `/auth/health` UP, `/actuator/health` UP, login completo OK, todas as
  regressões verdes.
- Mudança de bind (0.0.0.0 → 127.0.0.1) não altera o caminho legítimo (nginx no host acessa via
  loopback); custo nulo.
- Springdoc desabilitado remove o processamento de `/docs` no prod (leve redução de superfície).

## Observações residuais (registradas, sem ação nesta sprint)

- **Secrets visíveis em `docker inspect`**: os valores de `POSTGRES_PASSWORD`, `REDIS_PASSWORD`
  etc. aparecem no inspect dos containers (criptografia de secrets do Docker não habilitada).
  Registrado como risco conhecido; resolução (rotação + Docker secrets) exige janela dedicada.
- **`KC_BOOTSTRAP_ADMIN_USERNAME/PASSWORD`** ainda presentes no compose de infra (env-driven;
  o admin já foi criado no primeiro start). O próprio arquivo recomenda remoção após o primeiro
  start — recomendação para janela futura.
- **Backend/frontend sem healthcheck**: o runtime JRE do backend não possui wget/curl (adicionar
  exigiria mudança de Dockerfile); o auth-service mantém o healthcheck da 6.6. Sem alteração.
- **`/docs/` externo**: 308 (Next.js) → 404 antes de qualquer resposta útil; inócuo.
- **Rede `docker_crm-network` órfã**: não utilizada por nenhum container; remoção opcional
  (`docker network rm docker_crm-network`).
- **Firewall continua sendo a primeira camada** (ufw + iptables + unit systemd); o bind loopback
  é defesa em profundidade.

## Resultado

STATUS: **CONCLUÍDA** — etapa auth/Gateway **encerrada** (Sprint 6 e 6.1–6.10).

- Exposição de produção eliminada: nenhuma porta publicada na interface pública; firewall
  verificado; `/docs` desabilitado; actuator restrito a health.
- Compose da VPS endurecido: sem defaults inseguros, credenciais 100% via `.env`, profile prod.
- Regressão completa verde e login real validado em produção.

## Pendências / recomendações

- Rotação de senhas vivas + Docker secrets (endereça a exposição em `docker inspect`) — janela
  dedicada com downtime planejado.
- Remover `KC_BOOTSTRAP_ADMIN_*` do compose de infra após o primeiro start.
- Adicionar healthchecks ao backend/frontend (Dockerfile com `wget`/`curl` no runtime).
- Limpeza da rede órfã `docker_crm-network`.
- Migração para o backend de produção com `SPRING_PROFILES_ACTIVE=prod` já concluída nesta
  sprint — monitorar logs por 1 semana (logging INFO em vez de DEBUG).
