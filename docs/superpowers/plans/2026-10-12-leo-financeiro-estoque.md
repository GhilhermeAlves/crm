# Léo (assistente interno do CRM) — Financeiro e Estoque — Plano de implementação

> Executor: siga as tasks em ordem, um commit por task, sem push. Worktree: `.worktrees/feat-leo-fases`, branch `feat/leo-fases-2-4` (nome histórico; escopo: só Léo) (base `crm-improvements-deploy-phase` @ 29332937, já contém o PR #33 da Fase 1 e o #34 do estoque).
> **Trabalhe SOMENTE nesse diretório.** Antes de cada commit confira `git branch --show-current` = `feat/leo-fases-2-4`.

**Contexto (Fase 1, já em produção no código):** o Léo interno usa tools `AiTool` registradas no `AiToolRegistry`. Leitura estende `AbstractAiReadTool` (ex.: `tool/tools/SearchAppointmentsTool.java`). Escrita segue `tool/write/CancelAppointmentTool.java`: valida, chama `AiActionService.propose(...)` e o `AiActionExecutor` executa só após o usuário clicar em Confirmar. Horários/datas para o modelo usam `AgendaFormat` (fuso São Paulo). O prompt está em `AiAssistantService.systemPrompt(...)` e o catálogo de telas em `AiScreenCatalog`.

## Global Constraints

- Escrita do Léo interno: SEMPRE proposta + confirmação (nunca grava direto). Parâmetros executados vêm da proposta persistida.
- Tenant/usuário/permissões só do `AiToolContext`. Permissões reais: `finance:read`, `finance:manage`, `inventory:read`, `inventory:manage`.
- Valores em dinheiro: o modelo envia reais como número decimal (`150.50`); converta para `amountCents` (Long) com `BigDecimal.movePointRight(2).longValueExact()`; rejeite ≤ 0 ou mais de 2 casas. Na saída para o modelo, formate em `R$ 1.234,56`.
- Datas: `yyyy-MM-dd` (LocalDate). Textos ao usuário em pt-BR.
- Testes unitários com Mockito no padrão de `AppointmentWriteToolsTest` e `SearchAppointmentsToolTest`. Verificação: `cd backend && mvn -q test` (sem Docker → não rodar `mvn verify`). Frontend (só se tocar): `npx vitest run --pool=threads <arquivos>`, `npx tsc --noEmit` e **`npx prettier --check --end-of-line auto <arquivos alterados>`** (o CI quebra em Prettier).

Caminhos: `B/` = `backend/src/main/java/com/becommerce/crm/`, `T/` = `backend/src/test/java/com/becommerce/crm/`.

---

> As melhorias do **agente do WhatsApp** (outro agente, não o Léo) estão em plano separado: `docs/superpowers/plans/2026-10-12-whatsapp-agente-agenda.md`.

---

## FASE A — Financeiro

Use `FinancialTransactionService` (confira se há interface `...UseCase` em `B/finance/transaction/application/port/in`; se houver, injete a interface).

### Task 1 — read tools financeiras
1. `B/automation/ai/application/tool/tools/SearchTransactionsTool.java` — `search_transactions`, permissão `finance:read`. Args: `type` (INCOME/EXPENSE), `status` (use os valores aceitos por `list(...)` — confira no service/controller), `from`/`to` (`yyyy-MM-dd`; padrão: mês atual), `category` (enum `TransactionCategory`), `limit` (máx 50). Chama `list(companyId, type, status, from, to, null, category, page 0, size limit)` (confira a assinatura completa). Saída enxuta: `id, description, type, category, amount ("R$ …"), dueDate, paidAt, status`.
2. `FinanceSummaryTool` — `finance_summary`, `finance:read`. Args `from`/`to` (padrão mês atual). Soma receitas e despesas (pagas e em aberto) e devolve `{ receitasPagas, receitasAbertas, despesasPagas, despesasAbertas, saldoRealizado }` formatados. Se já existir um endpoint/serviço de resumo no módulo `finance/report` (ex.: `FinanceReportService` overview/cash-flow), REUTILIZE-o em vez de somar à mão.
3. Testes para as duas (período padrão = mês corrente com `Clock` fixo; filtros repassados; formatação de moeda).
4. Commit: `feat(ai): finance read tools for Léo`.

### Task 2 — write tools financeiras (proposta + confirmação)
1. `CreateTransactionTool` — `create_transaction`, permissão `finance:manage`. Args: `type`, `description` (obrigatório, ≤255), `category` (obrigatória e compatível com o `type` — `category.type() == type`, senão erro claro), `amount` (reais), `paymentMethod` (enum `PaymentMethod`, padrão PIX), `dueDate` (padrão hoje), `paid` (boolean; se true, `paidAt = dueDate`), `contactId` (opcional; valide que é da empresa como o `CreateTaskTool` faz), `installments` (1–24, padrão 1). Descrição da proposta: "Lançar receita: Consulta Maria — R$ 150,00 (PIX, venc. 12/10/2026)" / "Lançar despesa: …" + " em 3x" se parcelado.
2. `PayTransactionTool` — `pay_transaction`, `finance:manage`. Args `transactionId`, `paidAt` (padrão hoje). Valida que existe (`getById`) e está em aberto. Descrição: "Dar baixa: <descrição> — R$ X em dd/MM/yyyy".
3. `AiActionExecutor`: `case "create_transaction"` → `create(companyId, new CreateFinancialTransactionRequest(...), action.getUserId())`; `case "pay_transaction"` → `pay(companyId, id, new PayTransactionRequest(paidAt), action.getUserId())`. Guarde nos parâmetros `amountCents` (Long) e datas como `yyyy-MM-dd`.
4. Testes: proposta criada sem chamar o service; categoria incompatível → erro; valor inválido (0, negativo, 3 casas) → erro; executor chama o service com os valores persistidos.
5. Commit: `feat(ai): Léo proposes financial entries and payments`.

---

## FASE B — Estoque

Use `InventoryService` (ou interface em `B/masterdata/inventory/application/port/in`).

### Task 3 — read + write de estoque
1. `SearchStockTool` — `search_stock`, `inventory:read`. Args `query` (nome/SKU), `lowStock` (boolean), `limit` (máx 50). Chama `listProducts(companyId, query, null, lowStock, true, 0, limit)`. Saída: `id, name, sku, quantity, unit, minStock, lowStock`.
2. `RegisterStockMovementTool` — `register_stock_movement`, `inventory:manage`. Args `itemId` (obrigatório), `type` (`StockMovementType`: confira os valores, ex. ENTRADA/SAIDA/AJUSTE), `quantity` (≥1; em AJUSTE é a contagem real), `note` (≤500). Valida o item (existe, é da empresa, produto ativo). Em SAIDA, se `quantity` > saldo atual, rejeite com "Saldo insuficiente: há N <unidade>". Descrição: "Entrada de estoque: 10 un de Luva A (saldo 5 → 15)" / "Saída…" / "Ajuste de estoque: Luva A para 12 un".
3. Executor: `case "register_stock_movement"` → `registerMovement(companyId, itemId, new RegisterMovementRequest(type, quantity, note))`.
4. Testes no mesmo padrão (proposta sem execução; saída maior que saldo rejeitada; executor).
5. Commit: `feat(ai): Léo checks stock and proposes stock movements`.

---

### Task 4 — Prompt, telas e frontend
1. `AiScreenCatalog`: adicione `/estoque` → "Estoque" ("saldo dos produtos, entradas, saídas e ajustes") — confira o rótulo real em `frontend-refine/src/components/layout/navigation.ts`. Teste em `AiScreenCatalogTest`.
2. `AiAssistantService.systemPrompt`: na frase de ações, inclua "lançar receitas/despesas, dar baixa em lançamentos e registrar movimentações de estoque". Atualize o teste do prompt.
3. Frontend `useAiConfirmAction` (`frontend-refine/src/features/automation/ai/hooks/useAi.ts`): após confirmar, invalide também as queries do financeiro quando `action.tool` for `create_transaction`/`pay_transaction` e do estoque quando for `register_stock_movement` (descubra as query keys em `src/features/finance/**/hooks` e `src/features/masterdata/inventory/hooks/useInventory.ts`). Rode prettier/tsc/vitest dos arquivos tocados.
4. Commit: `feat(ai): Léo knows finance and inventory actions`.

### Task 5 — Verificação final
- `cd backend && mvn -q test` verde; frontend tsc + vitest + prettier (`--end-of-line auto`) verdes.
- Relatar arquivos, testes e hashes. Sem push.
