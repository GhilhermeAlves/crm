# Cobrança automática com Asaas — design

Data: 2026-11-08 · Depende de: PR #121 (limites e preços por plano)

## Objetivo

Cobrar a mensalidade das clínicas automaticamente via Asaas (Pix), liberar a
empresa quando o pagamento cai e bloqueá-la após atraso, sem trabalho manual.

## Decisões

| Tema | Decisão |
|---|---|
| Provedor | Asaas, conta no CPF do dono (trocar para CNPJ depois só muda a chave) |
| Meio de pagamento | Pix (`billingType = PIX`); boleto/cartão ficam fora por ora |
| Entrada do cliente | Opção 1: CRM_ADMIN cria empresa/convite com plano; o sistema cria a assinatura. Cadastro self-service fica para depois |
| Recorrência | Assinatura do próprio Asaas (`/v3/subscriptions`, `cycle = MONTHLY`) |
| Primeiro mês | Uso imediato; primeiro vencimento em 7 dias |
| Lembrete | O Asaas envia o Pix por e-mail 5 dias antes do vencimento |
| Atraso | Venceu sem pagar → `PAST_DUE` (faixa de aviso, uso normal). 10 dias de atraso → `BLOCKED` |
| Bloqueio | Login funciona; API responde 402 exceto rotas de cobrança; dados nunca são apagados |
| Exceções | CRM_ADMIN nunca é bloqueado; empresa `exempt` não é cobrada nem bloqueada |
| Empresas existentes | Não são cobradas até o CRM_ADMIN clicar em "Ativar cobrança" |

Preço padrão por plano (centavos), editável por empresa:

| Plano | Mensal |
|---|---|
| STARTER | 19700 |
| PROFESSIONAL | 39700 |
| BUSINESS | 79700 |
| ENTERPRISE | 150000 (sempre negociado) |

## Arquitetura

Novo módulo `backend/.../crm/billing/` (hexagonal, como os demais):

- `domain`: `BillingAccount`, `BillingStatus` (`NOT_STARTED`, `SYNC_PENDING`,
  `ACTIVE`, `PAST_DUE`, `BLOCKED`, `EXEMPT`), `Invoice`, `InvoiceStatus`
  (`PENDING`, `CONFIRMED`, `RECEIVED`, `OVERDUE`, `REFUNDED`, `CANCELLED`).
  Regras puras: `onOverdue`, `onPaid`, `shouldBlock(today)` (vencida há ≥ 10
  dias), `grant(untilDate)`.
- `application`: `BillingService` (ativar, trocar valor/plano, isentar,
  liberar por N dias, reenviar ao Asaas, dados para telas),
  `AsaasWebhookService`, `BillingDailyJob` (`@Scheduled`, 1×/dia).
- `infrastructure`: `AsaasClient` (RestClient; base URL e chave via env;
  timeout curto), adaptadores JPA.
- `web`: `BillingController` (empresa logada e CRM_ADMIN),
  `AsaasWebhookController` (público), `BillingGuardInterceptor`.

`CompanyStatus.SUSPENDED` continua sendo só a suspensão manual (bloqueio total
no login). A cobrança usa `BillingStatus`, independente.

## Dados (migration V130)

`billing_accounts` (1 por empresa; RLS por `company_id` + GRANT ao `crm_app`):
`company_id` PK/FK, `status`, `monthly_price_cents`, `asaas_customer_id`,
`asaas_subscription_id`, `next_due_date`, `overdue_since`, `grace_until`,
`sync_error`, `created_at`, `updated_at`.

`billing_invoices`: `id`, `company_id`, `asaas_payment_id` UNIQUE, `due_date`,
`value_cents`, `status`, `invoice_url` (página Pix do Asaas), `paid_at`,
timestamps. RLS + GRANT.

`billing_webhook_events`: `asaas_event_id` UNIQUE, `event`, `received_at`,
`processed_at` — idempotência. Sem RLS (tabela de plataforma, só o webhook
escreve), sem dados de paciente.

A antiga tabela `subscriptions` (V014) não é usada nem removida.

## Fluxos

**Ativar cobrança** (automático ao criar empresa/convite aceito; manual para
existentes): cria cliente Asaas (nome, CPF/CNPJ, e-mail da empresa) → cria
assinatura PIX com `nextDueDate = hoje + 7` → status `ACTIVE`. Falha no Asaas
→ `SYNC_PENDING` com `sync_error`; a empresa é criada mesmo assim e o
cartão mostra "Tentar novamente". Empresa sem CPF/CNPJ não pode ser ativada
(o Asaas exige documento): o cartão pede o documento.

**Trocar plano/valor**: `PUT /v3/subscriptions/{id}` com
`updatePendingPayments = false` (vale a partir da próxima cobrança). Falha →
marca para sincronizar; o job diário reenvia.

**Webhook** (`POST /api/v1/public/billing/asaas/webhook`):
1. Valida o header `asaas-access-token` (comparação em tempo constante) contra
   `ASAAS_WEBHOOK_TOKEN`; inválido → 401.
2. Ignora evento já processado (`billing_webhook_events`).
3. Eventos tratados: `PAYMENT_CREATED`, `PAYMENT_CONFIRMED`,
   `PAYMENT_RECEIVED`, `PAYMENT_OVERDUE`, `PAYMENT_REFUNDED`,
   `PAYMENT_DELETED`. Outros: registrados e ignorados.
4. Antes de liberar, consulta `GET /v3/payments/{id}` no Asaas e usa o status
   de lá (um aviso forjado não libera nada).
5. Pago → fatura `RECEIVED`, conta `ACTIVE`, limpa `overdue_since`.
   Vencido → fatura `OVERDUE`, conta `PAST_DUE`, `overdue_since` = vencimento.
6. Sempre responde 200 após registrar, para o Asaas não pausar a fila.

**Job diário** (03:30, depois do backup): `PAST_DUE` com
`overdue_since ≤ hoje − 10` e sem `grace_until` vigente → `BLOCKED`;
reenviar contas `SYNC_PENDING`; expirar `grace_until`.

**"Já paguei"**: consulta a fatura aberta no Asaas na hora e libera se paga
(o webhook pode atrasar).

## Bloqueio

`BillingGuardInterceptor` (mesmo padrão do `ModuleGuardInterceptor`): se a
conta da empresa do usuário está `BLOCKED`, responde 402
`{"code":"BILLING_BLOCKED"}` para tudo, exceto `/api/v1/billing/**`,
`/api/v1/me`/identidade e rotas públicas. CRM_ADMIN passa sempre.
O auth-service não muda (login segue permitido).

Frontend: interceptador HTTP trata 402 `BILLING_BLOCKED` → rota
`/pagamento-pendente`.

## Telas

Clínica:
- Faixa no topo quando `PAST_DUE`: "Mensalidade venceu em dd/mm" + "Pagar com
  Pix" (abre `invoice_url`). Só admins da clínica veem o botão; demais veem o aviso.
- `/pagamento-pendente`: valor, botão Pix, "Já paguei", aviso de que os dados
  estão guardados.
- Configurações → "Plano e cobrança": plano, valor, próximo vencimento,
  histórico (admins da clínica).

CRM_ADMIN (detalhe da empresa): cartão "Cobrança" com status, valor editável,
próximo vencimento, histórico, botões "Ativar cobrança", "Marcar como isenta",
"Liberar por X dias", "Tentar novamente". Lista de empresas: coluna de status
da cobrança.

## Configuração

Env no backend (VPS: `/opt/crm/.env` e `/opt/crm/docker/.env`):
`ASAAS_BASE_URL` (sandbox `https://api-sandbox.asaas.com/v3`, produção
`https://api.asaas.com/v3`), `ASAAS_API_KEY`, `ASAAS_WEBHOOK_TOKEN`,
`BILLING_ENABLED` (desligado por padrão: sem chave, nada é cobrado).

Nginx: location própria para `/api/v1/public/billing/` → 8081 (aplicar
manualmente na VPS, como as demais rotas públicas).

## Segurança e privacidade

- Chaves só em env; nunca em código ou log. Logs do webhook sem payload completo.
- Nada de paciente vai ao Asaas: só nome, documento e e-mail da clínica.
- Ações de CRM_ADMIN (isentar, liberar, mudar valor) registradas na auditoria.

## Testes

- Domínio: transições `ACTIVE → PAST_DUE → BLOCKED → ACTIVE`, carência,
  isenção.
- Webhook: token inválido (401), evento repetido, pagamento forjado
  (Asaas diz pendente → não libera).
- Guard: 402 para empresa bloqueada; CRM_ADMIN e rotas de cobrança passam.
- Job diário: bloqueia só após 10 dias; respeita `grace_until`.
- `AsaasClient` com servidor HTTP simulado; nenhum teste chama o Asaas real.
- Frontend: faixa, página de pagamento pendente, cartão de Cobrança.
- Manual no sandbox: criar empresa → pagar Pix de teste no painel → liberar.

## Entrega (PRs separados)

1. Backend: migration, domínio, `AsaasClient`, ativação/troca de valor.
2. Webhook, job diário e bloqueio.
3. Telas (clínica e CRM_ADMIN).
4. Produção: env, nginx, webhook no painel do Asaas, ativar empresas.

## Fora do escopo

Cadastro self-service com página de preços, boleto/cartão, nota fiscal,
usuário extra automático, recursos por plano, cobrança proporcional na troca
de plano.
