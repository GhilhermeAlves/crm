# Cobrança Asaas — Entrega 4 (produção) Implementation Plan

> **For agentic workers:** a Task 1 é código (pode ser executada com superpowers:subagent-driven-development). As Tasks 2–6 são operação na VPS e nos painéis do Asaas: executadas com o usuário, passo a passo, sem subagentes. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Colocar a cobrança no ar: o backend recebe as variáveis do Asaas, o nginx entrega o webhook ao backend, tudo é testado de ponta a ponta no sandbox e só então a chave de produção é ligada e as clínicas são ativadas uma a uma.

**Architecture:** Duas mudanças de repositório (variáveis no `docker/docker-compose.yml` e uma `location` no `docker/nginx/crm.conf`) seguem o fluxo normal (PR → CI → merge em `crm-improvements-deploy-phase` → `scripts/deploy.sh`). O nginx e o `.env` da VPS são aplicados à mão, como já é feito hoje. Primeiro o sistema roda contra o **sandbox** do Asaas; a troca para **produção** é uma mudança só de `.env` + reinício do backend, depois de limpar os dados de teste.

**Tech Stack:** Docker Compose na VPS (`/opt/crm`), nginx (`/etc/nginx/sites-available/crm`), Postgres (`crm-postgres`, banco `crm_main`), painel do Asaas (sandbox `https://sandbox.asaas.com`, produção `https://www.asaas.com`).

**Spec:** `docs/superpowers/specs/2026-11-08-cobranca-asaas-design.md`

**Base:** branch `feature/cobranca-asaas-entrega-4`, empilhada sobre a entrega 3 (PRs #123 e #124 abertos). As Tasks 2–6 só começam depois do merge de #123, #124 e do PR desta entrega em `crm-improvements-deploy-phase`.

## Global Constraints

- Env no backend (VPS: `/opt/crm/.env` e `/opt/crm/docker/.env`): `ASAAS_BASE_URL` (sandbox `https://api-sandbox.asaas.com/v3`, produção `https://api.asaas.com/v3`), `ASAAS_API_KEY`, `ASAAS_WEBHOOK_TOKEN`, `BILLING_ENABLED` (desligado por padrão: sem chave, nada é cobrado).
- Nginx: location própria para `/api/v1/public/billing/` → 8081 (aplicar manualmente na VPS, como as demais rotas públicas).
- Chaves só em env; nunca em código, log, commit ou na conversa. O token do webhook é gerado na VPS e copiado para o painel do Asaas pelo usuário.
- Webhook: `POST https://clin3dpro.cloud/api/v1/public/billing/asaas/webhook`, header `asaas-access-token`.
- Empresas existentes não são cobradas até o CRM_ADMIN clicar em "Ativar cobrança". Empresa sem CPF/CNPJ não pode ser ativada.
- Produção não tem staging: deploy só com CI verde, a partir de `crm-improvements-deploy-phase`, rodado pelo usuário (`! cd .../crm-deploy && bash scripts/deploy.sh <serviços>`). Toda escrita no banco de produção: backup da linha antes.
- Sandbox e produção do Asaas são **contas diferentes**, com chaves e painéis diferentes. Ids de cliente/assinatura do sandbox não existem em produção.

---

### Task 1: Variáveis da cobrança no compose e location do webhook no nginx (código)

**Files:**
- Modify: `docker/docker-compose.yml` (serviço `backend`, bloco `environment`, logo depois de `TELEGRAM_WEBHOOK_BASE_URL`)
- Modify: `docker/nginx/crm.conf` (nova `location` logo depois da do Telegram, antes de `location /api/`)

- [ ] **Step 1: Compose** — acrescentar:

```yaml
      # Cobrança (Asaas) — desligada por padrão; sandbox até a troca para produção.
      - BILLING_ENABLED=${BILLING_ENABLED:-false}
      - ASAAS_BASE_URL=${ASAAS_BASE_URL:-https://api-sandbox.asaas.com/v3}
      - ASAAS_API_KEY=${ASAAS_API_KEY:-}
      - ASAAS_WEBHOOK_TOKEN=${ASAAS_WEBHOOK_TOKEN:-}
      - BILLING_DAILY_CRON=${BILLING_DAILY_CRON:-0 30 3 * * *}
```

- [ ] **Step 2: Nginx** — acrescentar depois do bloco `location ^~ /api/v1/telegram/webhook/ { ... }`:

```nginx
    # Webhook de cobrança do Asaas - publico, SEM sessao do gateway.
    # O Asaas nao envia cookie de sessao; roteado direto ao crm-backend (8081).
    # A autenticidade e garantida pelo header asaas-access-token (comparado no backend).
    location = /api/v1/public/billing/asaas/webhook {
        limit_except POST { deny all; }
        proxy_pass http://127.0.0.1:8081;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
```

- [ ] **Step 3: Conferir** — `docker compose -f docker/docker-compose.yml config --quiet` (sem `.env` local pode avisar variáveis vazias; o que importa é não dar erro de sintaxe). Conferir que nenhum outro compose do repo é usado pelo `scripts/deploy.sh` (ele usa só `docker/docker-compose.yml`).

- [ ] **Step 4: Commit e PR**

```bash
git add docker/docker-compose.yml docker/nginx/crm.conf
git commit -m "chore(cobranca): variáveis do Asaas no compose e location do webhook no nginx"
```

PR para `crm-improvements-deploy-phase` (empilhado sobre o #124 até ele ser mergeado). CI verde → merge pelo usuário.

---

### Task 2: Deploy com a cobrança desligada

Pré-requisito: #123, #124 e o PR da Task 1 mergeados em `crm-improvements-deploy-phase`, CI verde.

- [ ] **Step 1: Deploy (usuário)** — `! cd C:/Users/ghilh/Desktop/PROJETO/crm/crm-deploy && git pull && bash scripts/deploy.sh backend frontend`. Aplica as migrations V130 e V131. Ler a saída (health OK, Flyway sem erro).

- [ ] **Step 2: Nginx (VPS)** — backup, cópia, teste e reload:

```bash
ssh crm-vps 'sudo cp /etc/nginx/sites-available/crm /etc/nginx/sites-available/crm.bak.$(date +%Y%m%d%H%M%S)'
scp docker/nginx/crm.conf crm-vps:/tmp/crm.conf
ssh crm-vps 'sudo cp /tmp/crm.conf /etc/nginx/sites-available/crm && sudo nginx -t && sudo systemctl reload nginx'
```

Antes de copiar, comparar (`diff`) o arquivo da VPS com o do repo: se a VPS tiver algo que o repo não tem, levar para o repo primeiro.

- [ ] **Step 3: Verificar que nada mudou para as clínicas**
  - `curl -s -o /dev/null -w "%{http_code}" -X POST https://clin3dpro.cloud/api/v1/public/billing/asaas/webhook -H 'Content-Type: application/json' -d '{}'` → **401** (chegou ao backend; token vazio no `.env` → sempre 401). Se vier 404/502, o nginx não está certo; se vier a página de login ou 401 do gateway com outro corpo, a location não pegou.
  - `GET` no mesmo caminho → 403 (só POST).
  - Login normal, navegar por pacientes/agenda: tudo como antes (nenhuma empresa tem conta de cobrança ainda).
  - `ssh crm-vps "docker exec crm-postgres psql -U \$POSTGRES_USER -d crm_main -c 'select count(*) from billing_accounts'"` → 0.

---

### Task 3: Sandbox do Asaas — chave, token e webhook

O sandbox é uma conta separada, criada em `https://sandbox.asaas.com` (a conta "normal" do Asaas é a de produção).

- [ ] **Step 1 (usuário, painel sandbox):** Integrações → Chaves de API → gerar chave. Guardar fora da conversa.

- [ ] **Step 2 (VPS): gerar o token do webhook e preencher os dois `.env`** — o usuário roda por SSH (os valores não passam pela conversa):

```bash
ssh crm-vps
TOKEN=$(openssl rand -hex 32); echo "$TOKEN"     # copiar para o painel no Step 3
for f in /opt/crm/.env /opt/crm/docker/.env; do
  sudo cp "$f" "$f.bak.$(date +%Y%m%d%H%M%S)"
  sudo sed -i '/^BILLING_ENABLED=/d;/^ASAAS_BASE_URL=/d;/^ASAAS_API_KEY=/d;/^ASAAS_WEBHOOK_TOKEN=/d' "$f"
  printf 'BILLING_ENABLED=true\nASAAS_BASE_URL=https://api-sandbox.asaas.com/v3\nASAAS_WEBHOOK_TOKEN=%s\n' "$TOKEN" | sudo tee -a "$f" >/dev/null
done
sudo nano /opt/crm/docker/.env    # acrescentar a linha ASAAS_API_KEY=<chave do sandbox> (e no /opt/crm/.env)
cd /opt/crm/docker && docker compose up -d backend
```

Conferir: `docker exec crm-backend env | grep -c ASAAS_` → 3 (sem imprimir os valores) e `docker logs crm-backend --tail 50` sem erro.

- [ ] **Step 3 (usuário, painel sandbox):** Integrações → Webhooks → novo webhook:
  - URL: `https://clin3dpro.cloud/api/v1/public/billing/asaas/webhook`
  - Token de autenticação: o `$TOKEN` do Step 2
  - Versão da API: v3; fila de sincronização ativada; tipo de envio: sequencial
  - Eventos: `PAYMENT_CREATED`, `PAYMENT_CONFIRMED`, `PAYMENT_RECEIVED`, `PAYMENT_OVERDUE`, `PAYMENT_REFUNDED`, `PAYMENT_DELETED`
  - E-mail para avisos de falha: o do dono

- [ ] **Step 4: Webhook com token certo** — o painel do Asaas mostra o histórico de envios do webhook; depois do primeiro evento (Task 4), conferir status 200. Pela VPS: `ssh crm-vps "docker exec crm-postgres psql -U \$POSTGRES_USER -d crm_main -c 'select event, received_at, processed_at from billing_webhook_events order by received_at desc limit 5'"`.

---

### Task 4: Teste de ponta a ponta no sandbox

Usar uma **empresa de teste** criada só para isso (não uma clínica real), com um CPF válido gerado para teste e o e-mail do dono.

- [ ] **Step 1: Ativação automática** — como CRM_ADMIN, criar a empresa de teste (plano Essencial, R$ 49,90) com CPF. Esperado: cartão "Cobrança" no detalhe da empresa com "Em dia" e próximo vencimento = hoje + 7; no painel sandbox, cliente e assinatura Pix mensal com `externalReference` = id da empresa; webhook `PAYMENT_CREATED` → fatura "Aberta" no histórico.

- [ ] **Step 2: Pagar** — no painel sandbox, abrir a cobrança e usar "Confirmar recebimento" (simula o Pix). Esperado: webhook `PAYMENT_RECEIVED` → fatura "Paga"; conta continua "Em dia".

- [ ] **Step 3: Atraso, faixa e bloqueio** — o sandbox não deixa forçar vencimento no passado, então simular no banco **só na empresa de teste** (backup da linha antes):

```sql
-- substituir <ID> pelo id da empresa de teste
select set_config('app.current_company_id', '<ID>', false);
update billing_accounts set status = 'PAST_DUE', overdue_since = current_date - 3 where company_id = '<ID>';
```

Entrar com um usuário admin da empresa de teste: faixa "Mensalidade venceu em …" com "Pagar com Pix". Depois `overdue_since = current_date - 11` e rodar o job: temporariamente `BILLING_DAILY_CRON=0 */5 * * * *` no `/opt/crm/docker/.env` + `docker compose up -d backend`, esperar até 5 min, **voltar o cron para `0 30 3 * * *`** e reiniciar. Esperado: status "Bloqueada"; ao navegar, cai em `/pagamento-pendente` (valor, Pix, "Já paguei", "Sair"). CRM_ADMIN continua navegando normalmente.

(O RLS lê `app.current_company_id` — `app.current_tenant_id()` da V013 — por isso o `set_config` antes do `update`; como as tabelas têm RLS FORCE, sem ele o `update` não altera nenhuma linha. Conferir "UPDATE 1" na saída.)

- [ ] **Step 4: Liberar e "Já paguei"** — como CRM_ADMIN, "Liberar por 3 dias" → a empresa de teste volta a navegar com a faixa "Liberada até …". Depois, no sandbox, "Confirmar recebimento" de uma fatura em aberto (ou criar uma cobrança avulsa da assinatura) e clicar "Já paguei" na tela de pagamento → volta ao sistema, status "Em dia".

- [ ] **Step 5: Mudar valor e plano** — no cartão, editar o valor para R$ 39,90 → no sandbox a assinatura muda de valor (só para as próximas cobranças). Trocar o plano da empresa → valor padrão do novo plano (porque não havia valor negociado — o passo anterior criou um valor negociado; voltar para R$ 49,90 antes, ou conferir que o valor negociado foi mantido).

- [ ] **Step 6: Webhook com token errado** — `curl -s -o /dev/null -w "%{http_code}" -X POST https://clin3dpro.cloud/api/v1/public/billing/asaas/webhook -H 'asaas-access-token: errado' -H 'Content-Type: application/json' -d '{"id":"evt_x","event":"PAYMENT_RECEIVED","payment":{"id":"pay_x"}}'` → 401, e nenhuma linha nova em `billing_webhook_events`.

- [ ] **Step 7: Anotar** os resultados (o que passou e o que não passou) antes de seguir. Qualquer falha volta para correção em código (PR próprio) antes da Task 5.

---

### Task 5: Troca para produção

- [ ] **Step 1 (usuário):** conta de produção do Asaas aprovada (documentos do dono enviados e conta liberada para receber Pix). Gerar a chave de API de produção.

- [ ] **Step 2: Limpar os dados do sandbox** — os ids de cliente/assinatura do sandbox não existem em produção. Com backup antes:

```bash
ssh crm-vps 'docker exec crm-postgres pg_dump -U $POSTGRES_USER -d crm_main -t billing_accounts -t billing_invoices -t billing_webhook_events > /opt/crm/backups/billing-sandbox-$(date +%Y%m%d%H%M%S).sql'
```

Apagar a empresa de teste pelo próprio sistema (CRM_ADMIN → excluir empresa; `billing_accounts`/`billing_invoices` têm `ON DELETE CASCADE`) e limpar `billing_webhook_events` (`delete from billing_webhook_events;` — tabela sem RLS). Conferir que `billing_accounts` ficou vazia. Desativar o webhook no painel do **sandbox**.

- [ ] **Step 3: `.env` de produção** — mesmo procedimento da Task 3 Step 2, com `ASAAS_BASE_URL=https://api.asaas.com/v3`, a chave de produção e um **novo** token (`openssl rand -hex 32`). `docker compose up -d backend` e conferir os logs.

- [ ] **Step 4 (usuário, painel de produção):** criar o webhook igual ao da Task 3 Step 3, com o token novo.

- [ ] **Step 5: Teste real com valor mínimo** — ativar a cobrança de uma empresa do próprio dono (ou de teste) com valor de R$ 5,00 (mínimo do Asaas para Pix), pagar o Pix de verdade, conferir fatura "Paga" via webhook. Depois isentar ou apagar essa empresa.

---

### Task 6: Ativar as clínicas

- [ ] **Step 1: Inventário** — lista de empresas com plano, valor combinado e CPF/CNPJ (a coluna "Cobrança" da lista de empresas mostra todas como "Não iniciada"). Marcar quais são da casa/teste (→ "Marcar como isenta") e quais pagam.

- [ ] **Step 2: Avisar os clientes** antes de ativar: valor, vencimento (7 dias após a ativação), Pix por e-mail 5 dias antes, e que após 10 dias de atraso o sistema fica bloqueado até o pagamento (dados guardados).

- [ ] **Step 3: Ativar uma a uma** no cartão "Cobrança": ajustar o valor (se negociado) **antes** de "Ativar cobrança"; conferir CPF/CNPJ e e-mail da empresa; ativar; conferir no painel do Asaas o cliente e a assinatura.

- [ ] **Step 4: Acompanhar** a primeira semana: `billing_webhook_events` com `processed_at` preenchido, log do job diário às 03:30 (`docker logs crm-backend | grep "job diário"`), coluna "Cobrança" da lista sem "Pendente de envio".

- [ ] **Step 5: Atualizar a memória/documentação** com o estado final (cobrança ligada em produção, data, quem está isento).
