# Gestão financeira e relatórios — plano de construção (prompts para o opencode)

Data: 2026-10-10 · Branch sugerida: `feat/finance` (a partir de `crm-improvements-deploy-phase`)

Referência visual: capturas do Capim em `C:\Users\ghilh\Pictures\Screenshots\financeiro`.
**O opencode não lê imagens.** Por isso cada prompt abaixo descreve a tela inteira em
texto. Os prompts são autocontidos: cole um de cada vez, na ordem, e só passe para o
próximo depois que o anterior estiver verde e commitado.

## Escopo

| Entra | Fica de fora (há capturas, mas é outro plano) |
|---|---|
| Gestão financeira → **Controle financeiro** (gráfico, KPIs, lançamentos) | Estoque → Controle de estoque |
| Novo lançamento, Enviar tabela (CSV), Filtrar | Estoque → Solicitação de prótese (kanban) |
| **Gerar relatórios contábeis** (modal PDF/CSV) | Comunicação → Campanhas |
| **Relatórios da clínica** (hub) + Visão geral, Agenda, Fluxo de caixa, Distribuição de receita | |
| Sugerir um relatório (modal em 2 passos) | |
| Itens de menu Conciliação e Comissões como página "em breve" | Conciliação e Comissões de verdade (não há capturas) |

Fora de escopo também: a "maquininha"/Capininha (banner de produto do Capim), integração
com adquirentes e emissão de nota fiscal.

## Mapeamento Capim → CRM

| Capim | CRM |
|---|---|
| Paciente | Contato (`masterdata/contact`) |
| Dentista / Profissional | Usuário membro da empresa (`identity`) |
| Procedimento | Item do catálogo (`masterdata/catalog`) |
| Consulta / Agendamento | `sales/scheduling` (agenda) |
| Orçamento | Oportunidade (`sales/pipeline`) |

## Sequência de prompts

| # | Prompt | Camada |
|---|---|---|
| 1 | Módulo `finance` — lançamentos (migration V092, CRUD, permissões) | backend |
| 2 | Resumos, série mensal e relatórios agregados | backend |
| 3 | Importação CSV e exportação de relatórios contábeis (PDF/CSV) | backend |
| 4 | Fundação do frontend (tipos, service, hooks, menu, rotas) | frontend |
| 5 | Página Controle financeiro | frontend |
| 6 | Diálogos: Novo lançamento, Enviar tabela, Filtrar, ações da linha | frontend |
| 7 | Modal Gerar relatórios contábeis | frontend |
| 8 | Relatórios da clínica (hub + Sugerir relatório) | frontend |
| 9 | Relatórios: Visão geral e Agenda | frontend |
| 10 | Relatórios: Fluxo de caixa e Distribuição de receita | frontend |

---

## Regras comuns (vão repetidas dentro de cada prompt)

- Repositório Git em `C:\Users\ghilh\Desktop\PROJETO\crm`; trabalhar na branch `feat/finance`.
- Backend Spring Boot em `backend/`, frontend Next.js em `frontend-refine/`.
- **Modelo a copiar:** o módulo de anamnese recém-feito.
  - Backend: `backend/src/main/java/com/becommerce/crm/masterdata/anamnesis/`
    (`domain/`, `application/{dto,port/output,service}/`, `infrastructure/persistence/`, `web/`)
    e a migration `backend/src/main/resources/db/migration/V091__anamnesis_models.sql`
    (RLS FORCE por `company_id`, permissões semeadas na própria migration).
  - Frontend: `frontend-refine/src/features/masterdata/anamnesis/` (`types/`, `services/`,
    `hooks/`, `components/`) e as páginas em `src/app/(dashboard)/settings/documents/anamnesis/`.
- Textos visíveis em pt-BR. Valores monetários guardados em centavos (`BIGINT`) e exibidos com
  `toLocaleString("pt-BR", { style: "currency", currency: "BRL" })`.
- Visual: usar os componentes de `frontend-refine/src/components/ui/` (card, button, dialog,
  tabs, select, popover, calendar, table, data-table, badge, checkbox, radio-group, textarea,
  sheet, skeleton) e os tokens de cor do projeto. **Não** copiar o roxo do Capim: onde a
  descrição diz "cor primária", usar `primary` do tema. Gráficos com `recharts` (já instalado).
- Migrations antigas não são editadas (checksum do Flyway). Checkstyle: linhas até 200 caracteres.
- Verificação backend: `cd backend && mvn -q verify`. Frontend: `cd frontend-refine && npm run lint
  && npx tsc --noEmit && npx prettier --check "src/**/*.{ts,tsx,css}" && npx vitest run`.
- Um commit por prompt (Conventional Commits). Não fazer push.

---

## Prompt 1 — Backend: módulo `finance` (lançamentos)

```text
Contexto: CRM multi-tenant (Spring Boot em backend/, Postgres com RLS). Vamos criar o módulo
financeiro. Trabalhe na branch feat/finance (crie a partir de crm-improvements-deploy-phase se
não existir). Copie a estrutura e as convenções do módulo de anamnese:
backend/src/main/java/com/becommerce/crm/masterdata/anamnesis/ e da migration
backend/src/main/resources/db/migration/V091__anamnesis_models.sql (leia os dois antes).

Crie o pacote com.becommerce.crm.finance.transaction com domain/, application/{dto,port/output,
service}/, infrastructure/persistence/ e web/.

1) Migration V092__financial_transactions.sql (próxima versão livre; confira com ls):
   tabela financial_transactions:
   - id UUID PK default gen_random_uuid()
   - company_id UUID NOT NULL FK companies ON DELETE CASCADE
   - type VARCHAR(10) NOT NULL CHECK (type IN ('INCOME','EXPENSE'))
   - description VARCHAR(255) NOT NULL
   - category VARCHAR(40) NOT NULL  (receitas: PROCEDURE, CONSULTATION, PRODUCT_SALE, OTHER_INCOME;
     despesas: RENT, PAYROLL, SUPPLIES, LAB, TAXES, MARKETING, UTILITIES, EQUIPMENT, OTHER_EXPENSE)
   - amount_cents BIGINT NOT NULL CHECK (amount_cents > 0)
   - fee_cents BIGINT NOT NULL DEFAULT 0  (taxa da maquininha/banco)
   - payment_method VARCHAR(20) NOT NULL CHECK IN ('BOLETO','CREDIT_CARD','DEBIT_CARD','PIX',
     'CASH','TRANSFER','CHECK','OTHER')
   - due_date DATE NOT NULL  (vencimento / data prevista)
   - paid_at DATE NULL  (preenchido = pago/recebido)
   - status VARCHAR(10) NOT NULL DEFAULT 'PENDING' CHECK IN ('PENDING','PAID','CANCELLED')
   - contact_id UUID NULL FK contacts (paciente)
   - professional_user_id UUID NULL FK users (profissional responsável)
   - catalog_item_id UUID NULL FK do item de catálogo (procedimento) — confira o nome da tabela
     em masterdata/catalog/infrastructure/persistence/CatalogItemJpaEntity
   - installment_group_id UUID NULL, installment_number INT NULL, installment_total INT NULL
   - notes TEXT NULL, created_by UUID NULL, created_at/updated_at TIMESTAMP NOT NULL default now
   Índices: (company_id, due_date), (company_id, paid_at), (company_id, type, status).
   RLS: ENABLE + FORCE e a mesma policy por company_id usada na V091.
   Permissões (semeadas como na V091): finance:read para ADMIN e MANAGER;
   finance:manage para ADMIN e MANAGER. Adicione as duas também em
   identity/infrastructure/persistence/RoleSeedService.java nos mesmos papéis.
   "Atrasado" NÃO é status gravado: é PENDING com due_date < hoje (calculado na leitura).

2) Domínio FinancialTransaction (+ enums TransactionType, TransactionCategory, PaymentMethod,
   TransactionStatus), regras: categoria compatível com o tipo; paid_at só com status PAID;
   cancelar não apaga. Exceções FinancialTransactionNotFoundException (404) e
   FinancialTransactionValidationException (400) registradas no
   shared/web/handler/GlobalExceptionHandler.java como a anamnese fez. Adicione FINANCE em
   analytics/audit/domain/AuditModule.java e audite criar/editar/pagar/cancelar/excluir.

3) API em /api/v1/companies/{companyId}/finance/transactions (mesmo padrão de
   AnamnesisController, @PreAuthorize finance:read / finance:manage):
   - GET    lista paginada. Filtros: type, status (PENDING|PAID|CANCELLED|OVERDUE), from, to
     (aplicados a due_date), paymentMethod, category, professionalUserId, contactId, q (busca em
     description, nome do contato e nome do profissional). Ordenação padrão due_date desc.
   - GET    /{id}
   - POST   cria. Aceita "installments" (1..24): se > 1, gera N lançamentos com o mesmo
     installment_group_id, valor dividido (o resto de centavos vai na 1ª parcela) e vencimentos
     mensais a partir de due_date.
   - PUT    /{id} edita
   - POST   /{id}/pay  body { paidAt } → status PAID
   - POST   /{id}/cancel → status CANCELLED
   - DELETE /{id}
   A resposta traz os nomes resolvidos (contactName, professionalName, catalogItemName) e o
   campo calculado overdue (boolean).

4) Testes: serviço (parcelamento e divisão de centavos, validação categoria×tipo, pagar,
   cancelar, overdue) e um teste de isolamento por tenant no estilo
   AnamnesisIsolationIT. Rode cd backend && mvn -q verify até ficar verde.

Commit: feat(finance): financial transactions module with installments and RLS
```

---

## Prompt 2 — Backend: resumos, série mensal e relatórios agregados

```text
Contexto: o módulo com.becommerce.crm.finance.transaction já existe (lançamentos financeiros,
tabela financial_transactions, valores em centavos, status PENDING/PAID/CANCELLED, "atrasado" =
PENDING com due_date < hoje). Branch feat/finance. Siga as convenções do próprio módulo.
Nenhum endpoint novo altera dados; todos exigem finance:read, exceto os de agenda que exigem a
permissão de leitura que a agenda (sales/scheduling) já usa.

Crie um serviço FinanceSummaryService e um controller FinanceReportController em
/api/v1/companies/{companyId}/finance, com:

1) GET /summary?from&to&type&<mesmos filtros da listagem>
   Responde os cards da tela "Controle financeiro":
   { incomeTotalCents, expenseTotalCents, balanceCents, incomeForecastCents, expenseForecastCents }
   - incomeTotal/expenseTotal = soma de PAID com paid_at no período.
   - forecast = soma de PENDING com due_date no período.
   - balance = incomeTotal - expenseTotal. CANCELLED nunca entra.

2) GET /health?months=6|12
   Série do gráfico "Saúde financeira da clínica": um item por mês, do mais antigo ao mês atual,
   incluindo meses sem movimento com zero:
   [{ month: "2026-05", incomeCents, expenseCents, balanceCents }]
   (income/expense = PAID por mês de paid_at.)

3) GET /cash-flow?from&to&granularity=DAY|WEEK|MONTH   (relatório Fluxo de caixa)
   { series: [{ period, incomeCents, openReceivablesCents, expenseCents, openPayablesCents,
                periodBalanceCents, accumulatedBalanceCents }],
     incomeByMethod: { BOLETO, CREDIT_CARD, DEBIT_CARD, PIX, CASH, TRANSFER, CHECK, OTHER },
     incomeHistory: [{ period, forecastCents, incomeCents, defaultCents, openCents }],
     expenseHistory: [{ period, forecastCents, expenseCents, defaultCents, openCents }],
     currentMonth: { income: {paidCents, openCents, defaultCents},
                     expense: {paidCents, openCents, defaultCents} } }
   Definições: open = PENDING com vencimento >= hoje; default (inadimplência) = PENDING vencido;
   forecast = tudo que vence no período (pago ou não).

4) GET /revenue-distribution?from&to&granularity&catalogItemIds   (Distribuição de receita)
   Só receitas PAID (por paid_at):
   { byProcedure: [{ period, catalogItemId, name, amountCents }],
     averageByProcedure: [{ catalogItemId, name, averageCents, averageNetCents }],
     byProfessional: [{ period, professionalUserId, name, amountCents }] }
   averageNetCents = média de (amount_cents - fee_cents).

5) GET /overview?from&to&granularity&professionalUserIds   (Visão geral da clínica)
   { professionals: [{ id, name }],
     totals: { appointments, confirmationRate, attendanceRate, proceduresDone, revenueCents },
     appointmentsByAttendance: [{ period, attended, notAttended, pending }],
     confirmed: [{ period, rate }], attended: [{ period, rate }],
     procedures: [{ period, done, pending }], revenue: [{ period, amountCents }] }
   Consultas vêm da agenda (leia sales/scheduling para achar entidade, status e campos de
   confirmação/comparecimento; se algum conceito não existir, devolva 0 e deixe um TODO claro).
   Procedimentos = lançamentos de receita com catalog_item_id (done = PAID, pending = PENDING).

6) GET /agenda-report?from&to&granularity&appointmentType&statuses&professionalUserIds
   { total, attended, notAttended, attendanceRate, byStatus: [{ status, count }] }
   Status disponíveis no filtro: CANCELADO, CONFIRMADO, PENDENTE (mapeie para os do scheduling).

7) GET /headline   (os 3 cards do topo de "Relatórios da clínica")
   { revenueThisMonthCents, attendancesThisWeek, budgetsCreatedThisMonth }
   budgets = oportunidades criadas no mês (sales/pipeline).

Todas as agregações em SQL (GROUP BY com date_trunc), respeitando RLS (rodam no contexto do
tenant). Testes de serviço cobrindo summary, health com meses vazios, cash-flow (open × default)
e revenue-distribution. cd backend && mvn -q verify verde.

Commit: feat(finance): summaries, monthly health series and report aggregations
```

---

## Prompt 3 — Backend: importação CSV e relatórios contábeis

```text
Contexto: módulo com.becommerce.crm.finance já tem lançamentos (financial_transactions) e
agregações. Branch feat/finance.

1) Importação — POST /api/v1/companies/{companyId}/finance/transactions/import (multipart,
   campo "file", finance:manage, até 5 MB, CSV UTF-8 separado por ";" ou ",").
   Colunas (cabeçalho obrigatório, nomes em pt-BR, ordem livre):
   tipo (receita|despesa), descricao, categoria, valor (aceita "1.234,56" e "1234.56"),
   forma_pagamento, vencimento (dd/MM/yyyy), pago_em (opcional), paciente (opcional, casa por
   nome exato do contato), profissional (opcional, casa por nome/e-mail do membro), observacoes.
   Resposta: { imported, errors: [{ line, message }] }. Linha inválida não derruba as outras.
   GET .../transactions/import/template devolve o CSV modelo com o cabeçalho e 2 linhas exemplo.

2) Relatórios contábeis — POST /api/v1/companies/{companyId}/finance/accounting-reports
   body { type: MONTHLY_ACCOUNTING | STATEMENT | TRANSACTIONS, month: "2026-09", format: PDF|CSV }
   devolve o arquivo (Content-Disposition attachment, nome
   relatorio-<tipo>-<yyyy-MM>.<ext>).
   - MONTHLY_ACCOUNTING ("Relatório Contábil Mensal"): receitas e despesas PAID do mês somadas
     por categoria, com totais e resultado — formato pensado para DRE / Carnê-Leão.
   - STATEMENT ("Extrato Financeiro"): tudo que entrou e saiu (PAID) no mês, em ordem de data,
     com saldo corrido.
   - TRANSACTIONS ("Detalhamento por Transação"): cada lançamento do mês (qualquer status exceto
     CANCELLED) com vencimento, pagamento, valor, taxa, líquido, forma, parcela x/y, paciente,
     profissional.
   Regra de negócio da tela: 1 relatório por tipo+formato+mês por empresa. Guarde em uma tabela
   accounting_report_requests (migration V093, com RLS como as demais) e, se já existir, devolva
   o mesmo arquivo gerado de novo em vez de bloquear (o limite é só registro/auditoria).
   PDF: use a biblioteca de PDF que o backend já tiver no pom.xml; se não houver nenhuma,
   adicione OpenPDF (com.github.librepdf:openpdf) e diga isso no resumo.
   GET .../accounting-reports/available-months devolve os meses com lançamentos
   (["2026-09","2026-08",...]) para o select "Período contábil".

3) Testes: parser do CSV (formatos de valor/data, linha inválida), os 3 relatórios em CSV
   (conteúdo) e um teste de fumaça do PDF (gera bytes com assinatura %PDF).
   cd backend && mvn -q verify verde.

Commit: feat(finance): CSV import and monthly accounting reports (PDF/CSV)
```

---

## Prompt 4 — Frontend: fundação

```text
Contexto: frontend Next.js em frontend-refine/. O backend já expõe a API financeira em
/api/v1/companies/{companyId}/finance (transactions CRUD + pay/cancel + import + template,
summary, health, cash-flow, revenue-distribution, overview, agenda-report, headline,
accounting-reports, accounting-reports/available-months). Leia os controllers em
backend/src/main/java/com/becommerce/crm/finance/ para os contratos exatos.
Branch feat/finance. Copie as convenções de frontend-refine/src/features/masterdata/anamnesis/
(types, services com o cliente `api` de @/lib/api, hooks com TanStack Query) e os testes em
src/__tests__/features/masterdata/anamnesis/.

1) Crie src/features/finance/ com:
   - types/finance.types.ts: tipos dos contratos + mapas de rótulos pt-BR:
     tipo (Receita/Despesa), categorias (Procedimento, Consulta, Venda de produto, Outras
     receitas; Aluguel, Folha de pagamento, Materiais, Laboratório, Impostos, Marketing,
     Contas de consumo, Equipamentos, Outras despesas), formas de pagamento (Boleto, Cartão de
     crédito, Cartão de débito, Pix, Dinheiro, Transferência, Cheque, Outro), status (Pendente,
     Pago, Cancelado, Atrasado). Helpers: centsToBRL(cents), parseBRLToCents("1.234,56").
   - services/finance.service.ts: uma função por endpoint (download de relatório via blob).
   - hooks/useFinance.ts: queries e mutations; mutations invalidam ["finance", companyId, ...].
2) Rotas em src/lib/constants.ts (ROUTES): FINANCE = "/finance",
   FINANCE_CONTROL = "/finance/control", FINANCE_RECONCILIATION = "/finance/reconciliation",
   FINANCE_COMMISSIONS = "/finance/commissions", e para relatórios REPORTS_OVERVIEW =
   "/reports/overview", REPORTS_AGENDA = "/reports/agenda", REPORTS_CASH_FLOW =
   "/reports/cash-flow", REPORTS_REVENUE = "/reports/revenue".
3) Menu (src/components/layout/navigation.ts): nova seção "Gestão" com o item
   "Gestão financeira" (ícone de cifrão, lucide `CircleDollarSign`) com filhos
   "Controle financeiro", "Conciliação", "Comissões"; visível só com finance:read (veja como os
   outros itens checam permissão). "Relatórios" continua na seção Análise.
4) Páginas "em breve" para Conciliação e Comissões: título da página, uma frase
   ("Em breve você poderá conciliar os recebimentos com o extrato bancário." /
   "Em breve você poderá calcular as comissões dos profissionais.") e um ícone grande suave.
   /finance redireciona para /finance/control.
5) Testes unitários: helpers de dinheiro (centsToBRL e parseBRLToCents com "1.234,56",
   "1234.56", "0,05") e o service (URLs e params), no estilo dos testes da anamnese.
   npm run lint && npx tsc --noEmit && npx prettier --check "src/**/*.{ts,tsx,css}" &&
   npx vitest run verdes.

Commit: feat(finance): frontend foundation (types, service, hooks, menu, routes)
```

---

## Prompt 5 — Frontend: página Controle financeiro

```text
Contexto: frontend-refine/, branch feat/finance. Já existem src/features/finance/ (types,
service, hooks) e a rota ROUTES.FINANCE_CONTROL. Crie a página
src/app/(dashboard)/finance/control/page.tsx e os componentes em
src/features/finance/components/. Use os componentes de src/components/ui e recharts.
Não copie cores de outra marca: "cor primária" = token primary do tema.

DESCRIÇÃO DA TELA (de cima para baixo, conteúdo centralizado com largura máxima ~ 780px):

1. Breadcrumb pequeno e cinza: "Início > Controle financeiro" (o último item em negrito).
2. Título "Controle financeiro" (h1, ~20px, negrito).
3. Linha com dois cards lado a lado (≈ 70% / 30%; empilham no mobile):
   3a. Card "Saúde financeira da clínica" (título 13px negrito). Abaixo do título, legenda
       inline: bolinha verde "Receitas", bolinha vermelha "Despesas", traço cinza "Saldo".
       No canto superior direito, dois botões pequenos de alternância "6 meses" e "12 meses"
       (o ativo com borda e fundo primary suave). Gráfico combinado (ComposedChart): barras
       finas verdes = receitas e vermelhas = despesas por mês; linha cinza-escura com pontos
       brancos = saldo. Eixo Y com valores em reais ("R$ 0", "R$ 1.500", ...), eixo X com mês
       abreviado + ano curto ("mai 26", "jun 26"). Abaixo do gráfico, paginação do carrossel:
       seta "<", dois pontinhos (o ativo preenchido), seta ">" — o slide 2 mostra o mesmo
       gráfico porém só a linha do saldo acumulado. Dados: GET /finance/health?months=6|12.
   3b. Card de ações: lista de links com ícone à esquerda e chevron ">" à direita, separados por
       divisória: "Gerar relatório contábil" (ícone de gráfico de linha; abre o modal do
       prompt 7) e "Enviar tabela de lançamentos" (ícone de upload; abre o diálogo de
       importação do prompt 6). (No original havia "Gerenciar maquininhas" e um banner de
       produto — NÃO implementar.)
4. Barra de controles: à esquerda, um segmented control em pílula com "Tudo", "Receitas",
   "Despesas" (o ativo com fundo primary e texto branco; padrão "Tudo"). À direita: um campo de
   data com ícone de calendário mostrando "Hoje (10/10/2026)" que abre um popover com atalhos
   (Hoje, Esta semana, Este mês, Mês passado, Últimos 3 meses, Personalizado → calendário de
   intervalo) e, ao lado, botão contornado "Filtrar" com ícone de funil (abre o Sheet de
   filtros do prompt 6; mostre um badge com a quantidade de filtros ativos).
5. Cards de KPI (ícone redondo à esquerda, rótulo pequeno cinza, valor em negrito):
   - aba Tudo: 3 cards — "Entradas totais" (ícone verde), "Saídas totais" (ícone vermelho),
     "Saldo" (ícone cinza).
   - aba Receitas: 2 cards — "Entradas totais" e "Total previsto" (ícone de calendário).
   - aba Despesas: 2 cards — "Saídas totais" e "Total previsto".
   Sem dados no período o valor mostra "Sem registros". Dados: GET /finance/summary.
6. Card de lançamentos:
   - Campo de busca no topo à esquerda (placeholder "Busque por paciente, profissional ou
     descrição", ícone de lupa à direita), com debounce de 300 ms → parâmetro q.
   - Botão primário "+ Novo lançamento" no topo à direita.
   - Tabela (data-table do projeto): Vencimento, Descrição (com o paciente embaixo em cinza),
     Categoria, Forma de pagamento, Profissional, Parcela ("2/6" ou "—"), Valor (verde com "+"
     para receita, vermelho com "−" para despesa), Status (badge: Pago verde, Pendente amarelo,
     Atrasado vermelho, Cancelado cinza) e menu "⋯" de ações (prompt 6). Paginação no rodapé.
   - Estado vazio (sem lançamentos no filtro): ilustração/ícone grande centralizado (lucide
     `Calculator` dentro de um círculo suave), título "Nenhum lançamento financeiro encontrado",
     texto "Envie sua tabela com essas informações ou adicione um lançamento manualmente." e
     dois botões: contornado "+ Novo lançamento" e primário "Enviar tabela" (ícone de upload).
   - Carregando: skeletons; erro: mensagem com botão "Tentar novamente".
7. A aba, o período e os filtros ficam na URL (searchParams) para o link ser compartilhável.

Os diálogos citados podem ser placeholders que serão feitos no prompt 6/7 — deixe os pontos de
abertura prontos. Teste de componente: troca de aba altera os cards exibidos; estado vazio
aparece com lista vazia; busca chama o hook com q. Rode lint, tsc, prettier --check e vitest.

Commit: feat(finance): financial control page (health chart, KPIs, transactions)
```

---

## Prompt 6 — Frontend: Novo lançamento, Enviar tabela, Filtrar e ações

```text
Contexto: frontend-refine/, branch feat/finance. A página src/app/(dashboard)/finance/control
já existe e tem os pontos de abertura destes diálogos. Use dialog, sheet, form (react-hook-form
+ zod, como o resto do projeto), select, calendar/popover, radio-group, textarea.

1) Diálogo "Novo lançamento" (e "Editar lançamento" com os mesmos campos), largura ~ 560px,
   título à esquerda e "X" à direita, campos em grade de 2 colunas; rótulos acima dos campos;
   campos opcionais com a palavra "Opcional" alinhada à direita do rótulo:
   - Tipo: segmented "Receita" / "Despesa" (linha inteira).
   - Descrição (texto, obrigatório) | Categoria (select; opções mudam conforme o tipo).
   - Valor (máscara "R$ 0,00", obrigatório) | Forma de pagamento (select).
   - Vencimento (data dd/MM/yyyy com ícone de calendário) | Parcelas (select 1x..24x; só para
     criação; mostra abaixo "6x de R$ 150,00").
   - Paciente (Opcional; combobox com busca nos contatos, lista rolável de nomes) |
     Profissional (Opcional; select com os membros da empresa).
   - Procedimento (Opcional; combobox do catálogo; só para Receita) | Taxa (Opcional; R$).
   - Checkbox "Já foi pago" → quando marcado aparece "Data do pagamento" (padrão hoje).
   - Observações (Opcional; textarea, linha inteira).
   - Rodapé: botões de mesma largura "Cancelar" (contornado) e "Salvar" (primário; desabilitado
     até o formulário ser válido; mostra spinner ao salvar). Sucesso: toast "Lançamento salvo".

2) Diálogo "Enviar tabela": título "Enviar tabela de lançamentos", texto explicando o formato,
   link "Baixar modelo (.csv)" (GET .../import/template), área de arrastar-e-soltar com ícone de
   upload ("Arraste o arquivo CSV ou clique para escolher", até 5 MB). Após enviar, mostra o
   resultado: "N lançamentos importados" e, se houver, uma lista rolável "Linha X: mensagem".
   Botões "Fechar" e "Enviar".

3) Sheet lateral "Filtrar" (abre pela direita): Status (checkboxes: Pendente, Pago, Atrasado,
   Cancelado), Forma de pagamento (checkboxes), Categoria (select múltiplo conforme a aba),
   Profissional (select), Paciente (combobox). Rodapé: "Limpar filtros" (link) e "Aplicar"
   (primário). Os filtros vão para a URL.

4) Menu "⋯" de cada linha: "Editar", "Marcar como pago" (pede a data do pagamento num popover
   com calendário; só se não pago), "Cancelar lançamento" (alert-dialog de confirmação) e
   "Excluir" (alert-dialog, texto em vermelho). Ações de escrita só aparecem com finance:manage.

Testes: validação do formulário (valor zero, categoria incompatível, data de pagamento
obrigatória quando "Já foi pago"), resumo das parcelas e o fluxo de importação com resposta
contendo erros. Rode lint, tsc, prettier --check e vitest.

Commit: feat(finance): transaction form, CSV import dialog, filters and row actions
```

---

## Prompt 7 — Frontend: modal Gerar relatórios contábeis

```text
Contexto: frontend-refine/, branch feat/finance. Backend: POST
/api/v1/companies/{companyId}/finance/accounting-reports (body { type, month, format }, devolve
o arquivo) e GET .../accounting-reports/available-months. Crie
src/features/finance/components/AccountingReportDialog.tsx e ligue-o ao link "Gerar relatório
contábil" da página /finance/control.

DESCRIÇÃO DO MODAL (largura ~ 400px):
- Título "Gerar relatórios contábeis" e botão "X".
- Texto: "Baixe o resultado mensal da sua clínica em um dos três formatos abaixo. É possível
  gerar 1 relatório, em 1 formato de arquivo, por mês."
- Seção "Tipo de relatório" (subtítulo em negrito): três cartões selecionáveis empilhados,
  cada um com título em negrito, descrição pequena embaixo e um radio à direita; o cartão
  selecionado fica com borda e fundo primary suaves:
  1. "Relatório Contábil Mensal" — "Receitas e despesas do mês por categoria, para DRE ou
     Carnê-Leão." (MONTHLY_ACCOUNTING)
  2. "Extrato Financeiro" — "Mostra o que entrou e saiu do caixa da clínica no período."
     (STATEMENT)
  3. "Detalhamento por Transação" — "Cada lançamento em tabela, com datas, valores, taxas e
     antecipações." (TRANSACTIONS)
- Seção "Período contábil": select com os meses disponíveis no formato "Setembro 2026"
  (padrão: mês anterior ao atual, se existir).
- Seção "Formato do relatório": radios inline "PDF" e "CSV".
- Botão de largura total "Gerar e baixar": desabilitado (cinza) até escolher tipo, mês e
  formato; ao clicar mostra spinner, baixa o arquivo e fecha o modal com toast
  "Relatório gerado".

Teste: botão desabilitado até completar as 3 escolhas; chama o service com o body certo.
Rode lint, tsc, prettier --check e vitest.

Commit: feat(finance): accounting report dialog (monthly, statement, transactions)
```

---

## Prompt 8 — Frontend: Relatórios da clínica (hub) e Sugerir relatório

```text
Contexto: frontend-refine/, branch feat/finance. Hoje src/app/(dashboard)/reports/page.tsx é
uma página de KPIs de analytics. Ela vira um hub de relatórios; mova o conteúdo atual para
/reports/overview só se ele não tiver outro uso — senão mantenha-o como um card "Analytics do
CRM" dentro do hub. Endpoint dos cards do topo: GET /finance/headline.

DESCRIÇÃO DA TELA "Relatórios da clínica" (largura total):
1. Título "Relatórios da clínica".
2. Três cards de métrica em linha (empilham no mobile), cada um com: ícone redondo colorido +
   rótulo ("Receita gerada" ícone $ verde; "Comparecimentos" ícone ciano; "Orçamentos" ícone
   amarelo), valor grande em negrito ("R$ 0,00"; "0 consultas"; "1 criados") e rodapé cinza com
   ícone de calendário ("Dados deste mês"; "Dados desta semana"; "Dados deste mês").
3. Grade de cards de relatório (3 por linha no desktop, 1 no mobile). Cada card: faixa
   superior com fundo primary bem suave e um ícone ilustrativo centralizado; título em
   negrito; descrição de uma linha; link sublinhado "Visualizar >" no rodapé.
   - "Visão geral" — "Resumo de consultas, orçamentos, procedimentos e receita gerada." →
     /reports/overview
   - "Agenda" — "Veja os compromissos da clínica por período, profissional e status." →
     /reports/agenda
   - "Fluxo de caixa" — "Movimentação financeira com entradas, saídas, saldo e previsões." →
     /reports/cash-flow
   - "Distribuição de receita" — "Faturamento por procedimento, profissional e preço médio." →
     /reports/revenue
   - "Sugerir um relatório" — "Compartilhe o que seria mais útil para a sua rotina clínica." —
     link "Quero sugerir >" abre o modal abaixo.
   Os cards financeiros só aparecem com finance:read.

4. Modal "Sugerir relatório", 2 passos:
   Passo 1 — título "Compartilhe sua sugestão de relatório", texto "Escolha os relatórios mais
   relevantes para o seu dia a dia.", grade 2×2 de opções em caixa com checkbox à direita:
   "Ficha clínica", "Estoque", "Financeiro", "Outros" (múltipla escolha; a selecionada ganha
   borda primary). Botões "Cancelar" e "Continuar" (desabilitado sem seleção).
   Passo 2 — título "Mais contexto para sua escolha", rótulo "Deseja incluir alguma
   observação?" com "Opcional" à direita, textarea (placeholder "Descreva o que você espera
   ver neste relatório"), botões "Cancelar" e "Enviar".
   Ao enviar: toast de sucesso "Recebemos sua sugestão!" no canto superior direito. Envio: se
   houver endpoint de feedback no backend use-o; senão registre via o mesmo mecanismo da
   página "Sugestão de melhorias", se existir; caso nenhum exista, crie
   POST /api/v1/companies/{companyId}/report-suggestions (tabela report_suggestions com RLS,
   próxima migration livre) — diga no resumo qual caminho usou.

Testes: hub renderiza os 5 cards; modal exige seleção no passo 1 e envia no passo 2.
Rode lint, tsc, prettier --check, vitest (e mvn -q verify se mexer no backend).

Commit: feat(reports): clinic reports hub and report suggestion dialog
```

---

## Prompt 9 — Frontend: relatórios Visão geral e Agenda

```text
Contexto: frontend-refine/, branch feat/finance. Backend: GET /finance/overview e
GET /finance/agenda-report (leia o controller para os contratos). Crie as páginas
/reports/overview e /reports/agenda com componentes em src/features/finance/components/reports/.

PADRÃO DAS PÁGINAS DE RELATÓRIO (vale para os prompts 9 e 10; crie um componente
ReportShell reutilizável):
- Link "← Voltar" (pequeno, sublinhado, cor primária) para /reports.
- Título da página + subtítulo cinza de uma linha.
- Um painel branco com borda contendo: cabeçalho (título do painel à esquerda e ícone de
  download à direita, que exporta os dados do relatório em CSV no cliente), uma linha de
  "chips" de filtro e, abaixo, as seções.
- Chip de filtro: caixa pequena com borda, ícone à esquerda, "Rótulo: valor" com o valor em
  cor primária e um "x" para limpar; chips sem valor mostram só o rótulo e um chevron. Clicar
  abre um popover. Chip de lista abre: campo "Pesquisar na lista", checkbox "Selecionar tudo",
  checkboxes das opções e botão "Adicionar filtro" no rodapé.
- Chip "Período": atalhos (Este mês, Mês passado, 2 meses anteriores ou este mês, Últimos 3
  meses, Este ano) + intervalo personalizado. Chip "Dia/Semana/Mês": granularidade.
- Cards de gráfico: título pequeno no topo, menu "⋯" decorativo, legenda com bolinhas
  coloridas. Sem dados: ícone suave e o texto "Sem resultados" centralizado.
- Cards de número: rótulo pequeno no topo (truncado com reticências se não couber) e valor
  grande centralizado.

1) "Relatório geral da clínica" — subtítulo "Visualize consultas realizadas, orçamentos
   aprovados, procedimentos por profissional e receita gerada." Painel "Relatório Geral da
   Clínica"; chips: Período (padrão Este mês), Dia/Semana/Mês (padrão Mês), Profissional.
   Seções:
   - Card "Profissionais da Clínica": tabela de uma coluna "Nome do Profissional" com a
     contagem "N linhas" no rodapé direito.
   - Aviso cinza: "Estão disponíveis somente os dados gerados a partir da data de criação do
     profissional no CRM."
   - "Resumo do Período: <período>": 5 cards de número — Consultas, Taxa de Confirmação (%),
     Taxa de Comparecimento (%), Procedimentos, Receita (R$).
   - "Consultas": 3 cards de gráfico lado a lado — "Consultas por Status de Comparecimento"
     (barras empilhadas: Não compareceu vermelho, Compareceu verde, Comparecimento pendente
     amarelo), "Consultas Confirmadas" (% por período) e "Consultas Comparecidas" (%).
   - "Procedimentos" (barras: Procedimentos Concluídos verde, Pendentes amarelo) e "Receita"
     (barras de receita por período) lado a lado.

2) "Relatório da Agenda" — subtítulo "Veja o total de agendamentos, confirmações e
   comparecimentos." Painel "Relatório da Agenda"; chips: Período, Dia/Semana/Mês, Tipo de
   Agendamento (padrão Consulta), Status do Agendamento (lista: Cancelado, Confirmado,
   Pendente), Profissional. Seções: 4 cards de número — "Agendamentos Totais",
   "Agendamentos Com Comparecimento", "Agendamentos Sem Comparecimento", "Taxa de
   Comparecimento"; e um card largo "Agendamentos por Status" (barras por status).

Testes: ReportShell (voltar, chips, popover de lista com "Selecionar tudo") e renderização de
"Sem resultados". Rode lint, tsc, prettier --check e vitest.

Commit: feat(reports): overview and agenda reports with shared report shell
```

---

## Prompt 10 — Frontend: relatórios Fluxo de caixa e Distribuição de receita

```text
Contexto: frontend-refine/, branch feat/finance. Já existe o componente ReportShell e o padrão
de chips/cards em src/features/finance/components/reports/ (prompt 9) — reutilize. Backend:
GET /finance/cash-flow e GET /finance/revenue-distribution.

1) /reports/cash-flow — título "Relatório de Fluxo de Caixa", subtítulo "Confira entradas,
   saídas, valores em aberto, previsões de recebimentos e pagamentos." Painel "Fluxo de Caixa";
   chips: Período (padrão "2 meses anteriores ou este mês") e Granularidade (padrão Mês).
   Seções:
   - "Fluxo de Caixa": gráfico combinado com legenda Entradas (barra verde-escura),
     Recebimentos em Aberto (barra verde-clara), Saídas (barra vermelha), Pagamentos em Aberto
     (barra rosa), Saldo Mensal (linha amarela), Saldo Acumulado (linha roxa/azul-escura).
     Rótulos de valor sobre as barras e pontos ("R$6.320", "R$0").
   - "Detalhamento de Entradas": 8 cards de número pequenos em linha (quebram no mobile):
     Boleto, Cartão de crédito, Cartão de débito, Pix, Dinheiro, Transferência, Cheque, Outro —
     valores compactos ("R$9k", "R$400"). Abaixo, dois cards: "Histórico de Entrada, Previsão e
     Inadimplência" (linhas: Previsão amarela, Entrada verde, Inadimplência vermelha, Em Aberto
     azul) e "Entrada, Em Aberto e Inadimplência - Mês Atual" (rosca/pizza com as 3 fatias).
   - "Detalhamento de Saídas": os mesmos dois cards para saídas ("Histórico de Saída, Previsão
     e Inadimplência" e "Saída, Em Aberto e Inadimplência - Mês Atual").

2) /reports/revenue — título "Relatório de Distribuição de Receita", subtítulo "Acompanhe o
   faturamento por tipo de procedimento, receita por profissional e preço médio." Painel
   "Segmentação de Receita"; chips: Período, Dia/Semana/Mês, Procedimentos (lista).
   Seções:
   - "Receita por Procedimento": parágrafo explicativo — "São considerados somente os valores
     correspondentes às operações financeiras marcadas como pagas no Controle Financeiro.
     Exemplo: orçamento composto por um único procedimento de R$200, com parcelamento em 2x de
     R$100 e vencimento em junho e julho. Se somente a parcela de junho for paga, a receita
     desse procedimento será de R$100 em junho e R$0 em julho." Gráfico de linhas, uma série
     por procedimento (legenda com nomes truncados), pontos com rótulo de valor.
   - "Preço Médio por Procedimento": barras agrupadas por procedimento — "Preço Médio por
     Procedimento" (verde) e "Preço Médio por Procedimento (com Desconto)" (lilás), valores
     sobre as barras, rótulos do eixo X inclinados ~45°.
   - "Receita por Profissional": mesma nota de "somente valores pagos" e gráfico de linhas por
     profissional (ou "Sem resultados").

Testes: formatação compacta de moeda ("R$9k"), cards por forma de pagamento sempre com as 8
formas (zero quando ausente) e "Sem resultados" nos gráficos vazios. Rode lint, tsc,
prettier --check e vitest.

Commit: feat(reports): cash flow and revenue distribution reports
```

---

## Depois dos 10 prompts

1. Revisar e rodar o E2E local; abrir o PR `feat/finance` → `crm-improvements-deploy-phase`.
2. Deploy com `scripts/deploy.sh` (migrations V092+ rodam no backend; backup é automático).
3. Próximos planos com as capturas que ficaram fora: Estoque (produtos com categoria EPI,
   Instrumento, Material de consumo, Medicamento, Outros; unidades Caixa, Cartela, Mililitros,
   Pacote, Rolo, Unidade; estoque mínimo), Solicitação de prótese (kanban Solicitação criada →
   Enviada para laboratório → Retornada à clínica) e Campanhas.
