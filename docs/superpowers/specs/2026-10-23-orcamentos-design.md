# Ficha do Paciente — Fase 3b: Orçamentos

**Status:** Implementada (decisões tomadas pelo agente com autorização do usuário em 2026-10-23: "não precisa me pedir permissão, faça o recomendado"). Branch `feature/pacientes-fase3b` (base `crm-improvements-deploy-phase` @ d215ac8a, já com a 3a).

Depende da 3a (`docs/superpowers/specs/2026-10-22-ficha-clinica-design.md`): procedimentos do paciente em `patient_procedures`.

## 1. Objetivo

Aba **Orçamentos** na ficha do paciente: montar um orçamento a partir de procedimentos "A realizar", aplicar desconto, aprovar (gerando parcelas em `financial_transactions`) ou marcar como perdido, e visualizar/imprimir escolhendo quais blocos aparecem.

## 2. Decisões

1. **Itens = procedimentos.** Um item de orçamento é sempre um procedimento `PLANNED` do mesmo paciente; o valor do item é o `price_cents` do procedimento no momento em que entra no orçamento (snapshot, editável enquanto rascunho). Um procedimento pertence a no máximo **um** orçamento não perdido.
2. **Status:** `DRAFT` (editável) → `APPROVED` ou `LOST`. `APPROVED` → `LOST` permitido só sem parcela paga (parcelas pendentes são canceladas). Não há volta para `DRAFT`. "Aprovado sem assinatura" do Capim vira apenas o rótulo "Aprovado" (assinatura eletrônica fora de escopo).
3. **Desconto:** valor em centavos (`discount_cents`, 0 ≤ desconto ≤ subtotal). Sem desconto percentual (YAGNI).
4. **Aprovação gera parcelas:** `FinancialTransaction.createSeries` existente: tipo `INCOME`, categoria `PROCEDURE`, total = subtotal − desconto, 1 a 24 parcelas mensais a partir do 1º vencimento, forma de pagamento escolhida, contato = paciente, descrição `Orçamento: <título>`. Total zero → aprova sem parcelas. Exige `finance:create`; sem ela, aprovar dá 403.
5. **Vínculo com lançamentos:** tabela `budget_transactions (budget_id, transaction_id)`.
6. **Interação com a 3a:**
   - Procedimento em orçamento `APPROVED`: ao concluir, a opção "gerar lançamento" some no front e o backend recusa `generateTransaction=true` (400 "Este procedimento já é cobrado pelo orçamento aprovado.") — evita cobrança dupla.
   - Excluir procedimento em orçamento `DRAFT`: remove o item (e recalcula). Em orçamento `APPROVED`: 409.
   - Editar preço de procedimento não altera o item do orçamento (snapshot).
   - A resposta do procedimento ganha `budget: {id, title, status} | null` (orçamento não perdido) e a lista "Ficha geral" mostra a coluna **Orçamento**.
7. **Excluir orçamento:** só `DRAFT` ou `LOST`. `APPROVED` → 409 (marque como perdido antes).
8. **Visualizar:** diálogo com checkboxes dos blocos — valor total, valor por procedimento, dentistas, desconto, observações, forma de pagamento (todos marcados por padrão) — que abre uma página de impressão `/patients/[id]/orcamentos/[budgetId]/imprimir?blocos=total,procedures,dentists,discount,notes,payment` com `window.print()`. As chaves dos blocos estão em `utils/blocks.ts`; sem `blocos` na URL, todos aparecem. A página fica no grupo de rotas `app/(print)/` (layout próprio, com `ProtectedRoute`, sem sidebar/header do painel); a URL é a mesma. Sem PDF no servidor. A escolha fica em `localStorage` (conveniência).
9. **Permissões:** leitura `contact:read`, escrita `contact:update`, aprovar também `finance:create`. Nenhuma permissão nova.
10. **Fora de escopo:** assinatura eletrônica, envio por WhatsApp/e-mail (o botão "enviar" do Capim), desconto percentual, edição de orçamento aprovado, análise de crédito, múltiplas versões, aba Financeiro com cards por orçamento (a aba Financeiro existente já lista os lançamentos gerados).

## 3. Modelo de dados — `V107__budgets.sql`

RLS ENABLE + FORCE + `tenant_isolation_policy` + GRANT em todas as tabelas (padrão V105/V106).

`budgets`: `id`, `company_id`, `contact_id` (FK contacts CASCADE), `title VARCHAR(160) NOT NULL`, `status VARCHAR(10)` CHECK (`DRAFT`,`APPROVED`,`LOST`), `discount_cents BIGINT NOT NULL DEFAULT 0 CHECK >= 0`, `notes TEXT`, `payment_method VARCHAR(20)` (mesmos valores do financeiro, NULL até aprovar), `installments INT` (1–24, NULL até aprovar), `first_due_date DATE`, `approved_at TIMESTAMP`, `approved_by UUID`, `lost_at TIMESTAMP`, `created_by`, `updated_by`, `created_at`, `updated_at`. CHECK: `status <> 'APPROVED' OR approved_at IS NOT NULL`.

`budget_items`: `id`, `company_id`, `budget_id` (FK budgets CASCADE), `procedure_id` (FK patient_procedures CASCADE), `price_cents BIGINT NOT NULL CHECK >= 0`, `position INT NOT NULL`, UNIQUE `(budget_id, procedure_id)`.

`budget_transactions`: `budget_id` (FK budgets CASCADE), `transaction_id` (FK financial_transactions CASCADE), `company_id`, PK `(budget_id, transaction_id)`.

"Um procedimento em no máximo um orçamento não perdido" é garantido no service (o status mora em `budgets`).

## 4. API

Base `/api/v1/companies/{companyId}/patients/{contactId}/budgets`:

| Método | Rota | Descrição |
|---|---|---|
| GET | `/` | lista (mais recente primeiro): id, título, status, subtotal, desconto, total, updatedAt, updatedByName |
| GET | `/{id}` | detalhe com itens (procedimento: nome, dentes, profissional, preço), parcelas (`id, dueDate, amountCents, status`) |
| POST | `/` | cria `DRAFT`: `{title, procedureIds[], discountCents, notes}` |
| PUT | `/{id}` | edita `DRAFT` (mesmo corpo; itens substituídos, preço do item novo = preço atual do procedimento, item mantido conserva o preço salvo; aceita `itemPrices: {procedureId: cents}` para editar) |
| POST | `/{id}/approve` | `{paymentMethod, installments, firstDueDate}` |
| POST | `/{id}/lose` | marca perdido |
| DELETE | `/{id}` | exclui (`DRAFT`/`LOST`) |

Erros: validação 400 (`ContactValidationException`), estado 409 (`ContactConflictException`), não encontrado 404 (`BudgetNotFoundException`), permissão 403.

Validações de criação/edição: título obrigatório; ≥ 1 procedimento; cada procedimento do mesmo paciente, `PLANNED`, sem outro orçamento não perdido; desconto ≤ subtotal.

## 5. Frontend

- Aba **Orçamentos** em `PatientTabs`, logo após "Ficha clínica" → `/patients/[id]/orcamentos`.
- `src/features/clinical/budgets/`: types, service, hooks, `BudgetList` (tabela Título, Última edição, Editado por, Total, Status; ações Visualizar, Editar (DRAFT), Aprovar (DRAFT), Marcar como perdido, Excluir), `BudgetFormDialog` (título, checklist dos procedimentos "A realizar" livres com valor editável, desconto em R$, observações, subtotal/total ao vivo), `ApproveBudgetDialog` (forma de pagamento, parcelas 1–24, 1º vencimento; prévia das parcelas com resto na 1ª), `BudgetViewDialog` (checkboxes dos blocos) e a página de impressão.
- Ficha clínica: coluna "Orçamento" na lista, com o texto "título · status" (ou "—"); `CompleteProcedureDialog` esconde "gerar lançamento" quando `procedure.budget?.status === "APPROVED"`.
- Diálogos de confirmação usam os botões "Confirmar" (perder orçamento) e "Confirmar exclusão" (excluir), para não colidir com os textos dos botões da linha.

## 6. Testes

Backend: domínio do orçamento (transições, desconto, totais), service (validações de itens, aprovação gera N parcelas com soma correta e resto na 1ª, 403 sem `finance:create`, perder com paga → 409 e com pendentes → canceladas, excluir), regra nova da 3a (concluir com orçamento aprovado + gerar → 400; excluir procedimento em orçamento aprovado → 409, em rascunho → item removido), controller unitário, `BudgetIsolationIT`. Frontend: lista (ações por status), formulário (totais e validação), aprovação (prévia das parcelas), visualização (blocos), coluna Orçamento e diálogo de conclusão. Verificação: `mvn test`, lint, tsc, vitest, Prettier; CI (E2E aplica a V107).
