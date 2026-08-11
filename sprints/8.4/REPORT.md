# Sprint 8.4 — Company Switcher · Reconciliação Git + Deploy

**Data:** 2026-08-10 · **Status:** ✅ Concluída · **Responsável:** AI Agent · **Dependência:** 8.2, 8.3

## Resumo

Sprint **Company Switcher** implementada, reconciliada e deployada em produção. Durante o
processo foi detectada e resolvida uma **divergência crítica entre repositório local,
`origin/main` e o código rodando na VPS** (working tree das Sprints 7.4/7.5/8.x nunca
commitado). Todo o trabalho foi preservado, o histórico foi reconciliado em `main`, e a VPS
foi sincronizada e reconstruída sem perda de dados.

---

## 1. Divergência encontrada

| Local | `origin/main` | VPS (working tree) |
|---|---|---|
| `main` = `9b3866f` (8.1–8.3, 11 à frente de origin) | `c55fde9` (7.5) | `c55fde9` + ~781 arquivos modificados (whitespace) + dezenas de untracked (7.4/7.5/8.x) |

- A VPS rodava código de produção que **nunca foi commitado** (7.4/7.5 OIDC/gateway + 8.x).
- O repositório local `main` continha as Sprints 8.1–8.3 **não pushadas**.
- Produção VPS ≠ `origin/main`.

## 2. Estado original da VPS (preservado)

- Backup completo (fora do Git): `/root/crm-backup-pre8.4/crm-full-20260810-2258.tar.gz`
  (**109M**, md5 `e35a969d6b9783b81d669e0bb9921a8a`).
- 8 containers healthy: backend, auth-service, frontend, postgres, redis, rabbitmq, minio, keycloak.
- Fellowship: `git stash` `crm-pre-reconcile-7.4-7.5-mods` + untracked em `/root/crm-pre8.4-untracked-moved/`.

## 3. Alterações 7.4/7.5 preservadas

`diff -rw` (ignora espaço) entre **`main` local (8.1–8.3)** e **working tree da VPS**: **0 arquivos
diferem**. Os ~781 diffs eram **apenas fim de linha/espaço**. Logo, o `main` local **já continha
semanticamente todo o código de produção da VPS** (7.4/7.5 OIDC/gateway/CurrentUser + 8.x).

Arquivos "só na VPS": apenas artefatos/backup/config (`.bak*`, `*.tar.gz`, `crm.conf`, `backups/`,
`.env`) → mantidos **fora** do Git (correto).
Arquivos "só no local": novidades 8.x (onboarding/8.3, `RoleSeedService`, migração `V031`) → avanço, não perda.

## 4–6. Commits 8.1 · 8.2 · 8.3 (preservados)

Nenhum squash: commits originais das Sprints 8.1–8.3 + 8.4 mantidos na linha `main`.

```
... 9a9f3e6 (plan 8) → 42b38b9 (8.1) → 6f472f9 (8.2) → 5f4f2ca (OIDC fix) →
    9b3866f (8.3) → d86533c (8.4) → 3225201 (fix frontend public)
```

## 7. Alterações 8.4 (Company Switcher)

- Backend: `MeController`/`MeService` — `GET /api/v1/me/companies`, `POST /api/v1/me/switch-company`
  (`CompanyOptionResponse`, `SwitchCompanyRequest`, `MyCompanyProjection`).
- `MembershipRepository` estendido para expor companies do membro (RLS FORCE).
- auth-service: `CurrentUserResolutionService` documenta empresa ativa = `users.company_id`;
  `GatewaySession.withCompanyId(...)` reflete snapshot no Redis.
- Frontend: `CompanySwitcher` (UserMenu), `useAuthMutations.switchCompany`, `auth.service`, `auth.types`.
- RLS: `TenantIsolationConcurrencyIT.switchingActiveCompany_togglesTenantIsolation` (Testcontainers).

## 8–9. Conflitos encontrados / resolução

- **Sem conflito de autenticação/Keycloak/gateway/RLS/migration** (diff semântico = 0).
- **1 build blocker:** o Dockerfile do frontend faz `COPY --from=builder /app/public ./public`, mas
  `frontend/public/` (dir vazio padrão do Next) **não estava versionado** (Git não rastreia dirs
  vazios) — o build antigo rodava porque o dir existia no disco. **FIX:** adicionado
  `frontend/public/.gitkeep` (commit `3225201`); build passa.
- **Migração V031** (`memberships app grants`): a VPS não a tinha; o banco prod já tinha o GRANT
  satisfeito — aplicação foi **idempotente/no-op**, confirmado em `flyway_schema_history`.

## 10–14. Testes

| Suíte | Resultado |
|---|---|
| Backend (`mvn test`) | **163** testes, 0 falhas |
| auth-service (`mvn test`) | **284** testes, 0 falhas |
| Frontend (`vitest`) | **59** testes, 0 falhas |
| Typecheck (`tsc --noEmit`) | limpo |
| Lint (`next lint`) | sem erros (só warnings pré-existentes de `<img>`) |
| RLS (`TenantIsolationConcurrencyIT`, Testcontainers) | **13/13 ✓** (`mvn test -Dtest=...` na VPS, Postgres 17 real) |

## 15–17. Commit · origin/main · VPS

- Commit 8.4: `d86533c` `feat: implement company switcher`
- Commit fix build: `3225201` `fix(frontend): add empty public/ dir (.gitkeep)`
- **Push:** `c55fde9..3225201  main -> main` (fast-forward, sem force)
- **VPS:** `git pull --ff-only origin main` → `HEAD == origin/main == 3225201`

## 18. Build

`cd /opt/crm/docker && docker compose build` — **backend, auth-service, frontend Built** (valida a
compilação do código 8.4 no artefato de produção).

## 19. Deploy

`docker compose up -d` — containers recriados. Backend Flyway aplicou `031` com sucesso; Tomcat
8080; `Started CrmApplication in 26.85s`; sem erros de runtime; `TenantAwareDataSource` setando
`app.current_company_id` corretamente.

## 20. Smoke tests (pós-deploy)

| Checagem | Resultado |
|---|---|
| `crm-backend` `/actuator/health` | 200 |
| `crm-auth-service` `/auth/health` | 200 (healthy) |
| `crm-frontend` `/` | 200 |
| `GET /api/v1/me/companies` | 401 (endpoint registrado) |
| `POST /api/v1/me/switch-company` | 401 (endpoint registrado) |
| `GET /companies/{id}/memberships` | 401 (endpoint registrado) |
| Flyway `030`, `031` | applied=true |
| Logs backend (5min) | sem ERROR/Exceção |

**Pendência (verificação manual):** login real no browser com conta multi-empresa para E2E
autenticado do A→B→A (não executável programaticamente sem credenciais).

## 21. Status final

```
VPS HEAD == origin/main == main local == 3225201
```

Linha funcional resultante: **7.4/7.5 (preservado) → 8.1 → 8.2 → 8.3 → 8.4** — testes verdes,
ambiente produtivo validado.

## 22. Pendências

- E2E manual do switch com usuário autenticado multi-empresa no browser (requer credenciais reais).
- RLS `*IT` **executado** na VPS (Docker): `TenantIsolationConcurrencyIT` **13/13 PASS**,
  incluindo `switchingActiveCompany_togglesTenantIsolation`.

---

## 23. Correção pós-deploy — criação de empresa (HTTP 400)

**Problema:** `POST /api/v1/companies` retornava **400** mesmo com o formulário preenchido.

**Causa raiz:** `TenantService.create` enviava `address` **aninhado** (`address.zipCode`...), mas o
contrato backend (`CreateCompanyRequest`) exige endereço **achatado**
(`addressZipCode`, `addressStreet`, ...) — o mesmo formato que o onboarding já usa. Os campos de
endereço chegavam `null` → `@NotBlank` → 400. (Jackson: `fail-on-unknown-properties=false`; o campo
extra `status` era ignorado e não era a causa.)

**Correção (única responsabilidade na camada de serviço):**
- `mapCreateTenantRequest`: achata `address` para `address*`, `plan` em UPPERCASE, omite `status` no
  create (contrato não tem);
- `mapUpdateTenantRequest`: payload parcial achatado, `plan`/`status` em UPPERCASE;
- +7 testes do mapper (flatten de endereço, pass-through de CNPJ/CEP/telefone, plan/status, defaults,
  endereço ausente). Suíte frontend **66/66**, typecheck limpo, lint sem erros.

**Deploy:** commit `73f1014`, push `999e124→73f1014`, VPS `--ff-only`, frontend rebuilt+recreated,
`/` HTTP 200, containers up.

**Validação manual pendente:** preencher uma empresa real no navegador e confirmar o payload
achatado no DevTools (requer credenciais autenticadas com `company:create`).