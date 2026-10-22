# Ficha do Paciente — Fase 3: Ficha clínica e Orçamentos

Data: 2026-10-22 · Base: `feature/pacientes-fase2` (V105) · Referência de fluxo: Capim (observado em 2026-10-22)

## Objetivo

Dar à ficha do paciente o odontograma com procedimentos por dente e o orçamento que, ao ser aprovado, libera o financeiro do tratamento. Entrega em duas partes, cada uma com seu PR:

- **3A — Catálogo de procedimentos + Ficha clínica**
- **3B — Orçamentos (aprovação → parcelas no financeiro)**

## Regras de negócio

1. **Financeiro de tratamento só nasce de orçamento aprovado.** Procedimento na ficha nunca gera lançamento (diferente do Capim, que tem o checkbox "Gerar lançamento financeiro" — não teremos).
2. O **próprio atendente aprova** o orçamento no sistema (sem assinatura do paciente). Precisa de `budget:approve`.
3. Orçamento aprovado é **imutável**. Para mudar: cancelar (estorna parcelas pendentes) e criar outro.
4. Orçamento **Perdido**: procedimentos continuam na ficha como "A realizar", sem vínculo.
5. Um procedimento pertence a no máximo um orçamento ativo (rascunho ou aprovado).

## 3A — Catálogo de procedimentos padrão

- Lista de 341 procedimentos extraída do Capim da clínica, com preços: `docs/superpowers/specs/2026-10-22-procedimentos-padrao.tsv` (nome `\t` preço). Copiada para `backend/src/main/resources/catalog/procedimentos-odontologicos.tsv`.
- Endpoint `POST /api/v1/companies/{companyId}/catalog/import-default-procedures` (`catalog:manage`): insere como `SERVICE`, categoria `Procedimento odontológico`; preço `0.00` vira `NULL`. **Idempotente**: pula nomes já existentes na empresa (comparação case-insensitive, trim). Retorna `{created, skipped}`.
- Página Catálogo: botão "Importar procedimentos padrão" (só com `catalog:manage`), com confirmação e toast do resultado.

## 3A — Ficha clínica

### Dados (V106)

`patient_procedures` (tenant, RLS FORCE, GRANT `crm_app`):

| coluna | tipo | nota |
|---|---|---|
| id, company_id | UUID | padrão |
| contact_id | UUID NOT NULL → contacts ON DELETE CASCADE | |
| catalog_item_id | UUID → catalog_items ON DELETE SET NULL | |
| name | VARCHAR(160) NOT NULL | snapshot do nome |
| status | VARCHAR(15) | `TO_DO`, `DONE`, `PRE_EXISTING` |
| dentition | VARCHAR(10) | `PERMANENT`, `DECIDUOUS` |
| teeth | SMALLINT[] | FDI (11–48 / 51–85); vazio = procedimento geral |
| faces | VARCHAR(5)[] | `M`,`D`,`O`/`I`,`V`,`L`/`P` — opcional |
| price_cents | BIGINT NULL ≥ 0 | |
| professional_user_id | UUID → users ON DELETE SET NULL | |
| notes | TEXT | |
| budget_id | UUID NULL (FK adicionada na V107) | |
| performed_at | DATE | preenchido ao marcar `DONE` |
| created_by, created_at, updated_at | | padrão |

Permissões: `clinical_record:read` (todos os papéis), `clinical_record:manage` (ADMIN, MANAGER, AGENT), concedidas por empresa no padrão da V105.

### API

`/api/v1/companies/{companyId}/contacts/{contactId}/procedures` — `GET` (filtro `status`), `POST`, `PUT /{id}`, `POST /{id}/done`, `DELETE /{id}` (proibido se vinculado a orçamento aprovado).

### Tela (aba "Ficha clínica")

- Odontograma com alternância Permanente/Decídua; cada dente colorido pelo estado mais relevante (A realizar > Realizado > Pré-existente). Clicar num dente abre o drawer com o dente pré-selecionado.
- Drawer "Adicionar procedimento": estado, procedimento (busca no catálogo; preço sugerido), valor, dentes, faces, profissional, observações.
- Lista "Ficha geral" com filtros por estado: procedimento, dente/faces, estado, orçamento vinculado, ações (marcar realizado, editar, excluir).
- Coluna lateral Anotações reaproveita as anotações da Fase 1.

## 3B — Orçamentos

### Dados (V107)

`budgets`: id, company_id, contact_id, title, valid_until, status (`DRAFT`, `APPROVED`, `LOST`, `CANCELLED`), discount_type (`AMOUNT`/`PERCENT`), discount_value, subtotal_cents, total_cents, notes, payment_plan (JSONB, opcional), approved_by, approved_at, lost_reason, created_by, created_at, updated_at, updated_by.

`budget_items`: id, company_id, budget_id, patient_procedure_id (→ patient_procedures), name, teeth, price_cents, professional_user_id.

`payment_plan`: `{ downPaymentCents, downPaymentMethod, installments, firstDueDate, method }`.

FK `patient_procedures.budget_id → budgets` e `financial_transactions.budget_id` (nova coluna, ON DELETE SET NULL). Permissões `budget:read`, `budget:manage`, `budget:approve` (ADMIN, MANAGER, AGENT).

### Fluxo

- **Etapa 1 — Procedimentos:** escolher procedimentos "A realizar" da ficha ou adicionar novos (que nascem como `TO_DO` na ficha); título, validade, desconto R$/%, observações; saldo total calculado no backend.
- **Etapa 2 — Pagamento (opcional):** entrada + parcelas + 1º vencimento + meio (`BOLETO`, `CREDIT_CARD`, `DEBIT_CARD`, `PIX`, `CASH`, `TRANSFER`, `CHECK`, `OTHER`). Sem plano, a aprovação gera 1 receita à vista vencendo hoje.
- **Aprovar** (`POST .../budgets/{id}/approve`, transacional): status `APPROVED`, gera `financial_transactions` `INCOME`/`PROCEDURE`/`PENDING` com `contact_id`, `budget_id`, `installment_group_id` comum; soma das parcelas = total (centavos residuais na última).
- **Marcar como perdido**: `LOST` + motivo opcional; desvincula procedimentos.
- **Cancelar aprovado**: `CANCELLED`; parcelas `PENDING` viram `CANCELLED`; parcelas pagas permanecem (aviso na tela).
- Aba "Orçamentos": tabela Título, Última edição, Editado por, Saldo total, Status, ações (Visualizar, Aprovar, Marcar como perdido, Cancelar, Excluir rascunho).
- **Visualizar/Imprimir**: escolher blocos (valor total, valor por procedimento, dentistas, desconto, observações, forma de pagamento); impressão pelo navegador, no padrão dos documentos da Fase 2.
- Aba Financeiro do paciente passa a agrupar lançamentos por orçamento (card com barra pago/total).

## Erros e validações

- Total ≥ 0; desconto não pode exceder subtotal; parcelas 1–36; `firstDueDate` obrigatório se houver parcelas.
- Aprovar orçamento sem itens → 422. Aprovar/editar não-rascunho → 409.
- Procedimento já vinculado a outro orçamento ativo → 409.

## Testes

- Backend: domínio (cálculo de total/desconto/parcelas e centavos residuais), serviços (aprovar gera N lançamentos; cancelar estorna pendentes; perder desvincula), import idempotente, integração com RLS.
- Frontend: schemas zod, odontograma (seleção de dente), fluxo do orçamento em duas etapas.
- E2E do CI valida as migrations V106/V107 no Postgres real.

## Fora de escopo

Comissão de profissional, análise de crédito, assinatura eletrônica, envio do orçamento por WhatsApp/e-mail, plano/especialidade nos procedimentos.
