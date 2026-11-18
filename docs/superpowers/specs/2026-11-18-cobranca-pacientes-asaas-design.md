# Cobrança de pacientes pelo Asaas — desenho

Data: 2026-11-18 · Status: aprovado pelo usuário no chat

## Objetivo

Na empresa do dono do CRM, receitas a receber de pacientes/contatos podem virar
cobranças no Asaas (link de pagamento). O paciente recebe o link por e-mail (Asaas)
e WhatsApp (CRM) e escolhe Pix, boleto ou cartão. Pagamentos pelo link baixam o
lançamento sozinhos; pagamentos na recepção baixam no CRM e são informados ao Asaas.

Fora do escopo: conta Asaas por empresa cliente (cada empresa decidirá o meio de
recebimento depois), split, parcelamento no cartão pelo Asaas, negativação.

## Decisões do usuário

1. Só a empresa onde o CRM_ADMIN ligar o recurso (no começo, só a dele).
   Usa a mesma conta/chave Asaas da cobrança de licença e o mesmo webhook.
2. Gatilhos: (a) ao aprovar orçamento, pergunta "Enviar cobrança pelo Asaas?";
   (b) botão "Cobrar pelo Asaas" em qualquer receita pendente com contato.
3. Envio: e-mail pelo Asaas (notificações do cliente ligadas); WhatsApp pelo CRM
   (Evolution, `WhatsAppConnectionService.sendText`) com o link, se a empresa tiver
   WhatsApp conectado e o contato tiver celular.
4. Forma de pagamento: o paciente escolhe (`billingType=UNDEFINED`).
5. Pagou na recepção (dinheiro/cartão/Pix da clínica): ao baixar no CRM, chama
   `POST /v3/payments/{id}/receiveInCash` (sem crédito de saldo, sem taxa documentada,
   `notifyCustomer=false`). O link passa a constar como pago.
6. Lançamento cancelado/excluído no CRM: `DELETE /v3/payments/{id}`.
7. CPF obrigatório: sem CPF o botão fica desativado com aviso.

## Modelo de dados (migration V141)

`financial_transactions` (RLS por tenant já existente) ganha:
- `asaas_payment_id VARCHAR(100)` (único quando não nulo)
- `asaas_invoice_url VARCHAR(500)`
- `asaas_status VARCHAR(30)` — `PENDING`, `RECEIVED`, `RECEIVED_IN_CASH`, `CANCELLED`, `FAILED`
- `asaas_error VARCHAR(500)`

`contacts` ganha `asaas_customer_id VARCHAR(100)`.

`companies` ganha `patient_billing_enabled BOOLEAN NOT NULL DEFAULT false`
(só o CRM_ADMIN altera).

Conferir colisão de número de migration antes do merge (base atual: V140).

## Componentes

- `PatientBillingService` (novo, pacote `finance.patientbilling`):
  - `charge(companyId, transactionId)` — valida (recurso ligado, receita, PENDING,
    contato com CPF, sem cobrança ativa); cria/reaproveita cliente Asaas do contato
    (grava `asaas_customer_id` logo após criar); cria cobrança
    (`value`, `dueDate` = vencimento ou hoje se vencido, `billingType=UNDEFINED`,
    `externalReference="tx:<transactionId>"`, `description` do lançamento); grava
    id/link/status; envia WhatsApp. Sem @Transactional em volta das chamadas HTTP.
  - `onPaidInClinic(transaction)` → `receiveInCash` quando há cobrança PENDING.
  - `onCancelled(transaction)` → `DELETE` quando há cobrança PENDING.
  - `applyWebhook(payment)` → se `externalReference` começa com `tx:`, baixa o
    lançamento (PAID, data do pagamento, forma conforme `billingType`: PIX, BOLETO,
    CREDIT_CARD) ou marca CANCELLED/estornado.
  - `resendWhatsApp(companyId, transactionId)`.
- `AsaasClient`: novos métodos `createPayment`, `receiveInCash`, `deletePayment`;
  `createCustomer` passa a aceitar `notificationDisabled=false` e celular.
- `BillingPaymentService.apply` / `AsaasWebhookService`: antes de tratar como
  licença, desvia pagamentos com referência `tx:` para `PatientBillingService`
  (o reprocessamento da V140 vale para os dois).
- `FinancialTransactionService.pay/cancel/delete`: chamam os ganchos após o commit
  (evento AFTER_COMMIT), sem bloquear a baixa se o Asaas falhar (fica `asaas_error`).
- `BudgetService.approve`: novo campo opcional `chargeViaAsaas` no request; após o
  commit, chama `charge` para cada parcela gerada.

## Telas

- Aprovar orçamento: checkbox "Enviar cobrança pelo Asaas" (só se o recurso estiver
  ligado), desativado com aviso se o paciente não tiver CPF.
- Financeiro (lista/detalhe do lançamento): selo do status Asaas; ações
  "Cobrar pelo Asaas", "Copiar link", "Reenviar WhatsApp", "Tentar de novo".
- Cartão de cobrança do CRM_ADMIN na empresa: interruptor
  "Cobrança de pacientes pelo Asaas".

## Erros

- Asaas fora/recusou: lançamento intacto, `asaas_status=FAILED` + `asaas_error`,
  botão "Tentar de novo". Nada de 500 para o usuário.
- WhatsApp desconectado ou contato sem celular: cobrança criada mesmo assim; aviso
  "WhatsApp não enviado" e o link fica para copiar.
- Webhook de cobrança de paciente com lançamento já pago/cancelado: ignora (idempotente).

## Testes

- Unitários: `PatientBillingService` (criar, reaproveitar cliente, sem CPF, recurso
  desligado, falha do Asaas, receiveInCash, delete, webhook pago/estornado,
  idempotência), desvio `tx:` no webhook, `BudgetService.approve` com e sem
  `chargeViaAsaas`.
- Frontend: botões/estados no lançamento e checkbox na aprovação.
- Produção: teste real com um lançamento de R$ 5,00 para um contato do próprio dono.
