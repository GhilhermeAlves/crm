# Relatórios — impressão e exportação via notificação — Plano de implementação

> Executor: tasks em ordem, um commit por task, sem push. Worktree `.worktrees/feat-report-exports`, branch `feat/report-exports` (base `crm-improvements-deploy-phase` @ 389f099f).
> **Trabalhe SOMENTE nesse diretório**; antes de cada commit confira `git branch --show-current` = `feat/report-exports`.

## Objetivo

1. Em **Relatórios** do menu lateral, um submenu com os relatórios que podem ser **impressos (PDF)** ou **exportados (Excel/XLSX)**:
   - Contábil mensal (DRE)
   - Fluxo de caixa
   - Extrato de lançamentos
   - Agenda / atendimentos
2. **Todo arquivo impresso ou exportado passa pela notificação:** o usuário pede o arquivo, o backend gera em segundo plano, guarda em Arquivos (storage) e cria uma notificação no **sino** com o botão **Baixar** (URL de download própria).
3. Layout próprio inspirado no modelo `monthly_accounting_2026-06.pdf` (não copiar): cabeçalho com título, período, empresa e "Emitido em", tabela com faixas de grupo, subtotais, barra de resultado e rodapé com página X/Y.

## Arquitetura

```
Tela /reports/export/[tipo]  ──POST /report-exports {type, format, from, to}──►  ReportExportService.request
   (período + PDF/Excel)          ◄── 202 {id, status: PENDING} + toast "você será avisado no sino"
                                                    │ @Async (após commit)
                                                    ▼
                                   ReportExportGenerator → ReportRenderer (dataset do tipo)
                                     → PDF (OpenPDF) | XLSX (Apache POI)
                                     → StorageUseCase.upload → report_exports.status = READY
                                     → Notification(user, "Relatório pronto", metadata {kind: REPORT_EXPORT, exportId, fileName})
Sino (NotificationBell / /notifications) ── "Baixar" ──► GET /report-exports/{id}/download (stream do storage)
```

## Global Constraints

- Backend hexagonal no padrão do módulo `finance/accounting` (port/in, port/out, service, web, infrastructure/persistence). Pacote novo: `com.becommerce.crm.reports.export`.
- Multi-tenant: tabela com `company_id` + RLS FORCE (`company_id = app.current_tenant_id()`) + `GRANT ... TO crm_app`, como a V093. O job assíncrono define `TenantContext.setCompanyId` antes de acessar o banco e limpa no `finally`.
- Permissões por tipo (checadas no `request` e no `download`): financeiros → `finance:read`; agenda → `appointment:read`. Download só pelo **mesmo usuário** que pediu (e da mesma empresa); senão 404.
- Valores em centavos formatados pt-BR (`R$ 1.234,56`), datas `dd/MM/yyyy`, fuso `America/Sao_Paulo`. Reaproveite `FinanceLabels` (rótulos de categoria/forma de pagamento) e o `MoneyFormat`/formatadores existentes quando houver.
- Período máximo: 12 meses (DRE: 1 mês fechado por vez, `month = yyyy-MM`).
- Verificação backend `cd backend && mvn -q test` (sem Docker → não rodar `mvn verify`; ITs rodam no CI). Frontend: `npx vitest run --pool=threads <arquivos>`, `npx tsc --noEmit`, `npx prettier --check --end-of-line auto <arquivos alterados>`. Se `frontend-refine/node_modules` não existir no worktree, crie a junção:
  `New-Item -ItemType Junction -Path "<worktree>\frontend-refine\node_modules" -Target "C:\Users\ghilh\Desktop\PROJETO\crm\frontend-refine\node_modules"`.

Caminhos: `B/` = `backend/src/main/java/com/becommerce/crm/`, `T/` = `backend/src/test/java/com/becommerce/crm/`, `F/` = `frontend-refine/src/`.

---

## Task 1 — Migration V096 `report_exports`

`backend/src/main/resources/db/migration/V096__report_exports.sql` (siga o estilo de comentários/seções da V093):

```sql
CREATE TABLE report_exports (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id        UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    requested_by      UUID NOT NULL,
    type              VARCHAR(40) NOT NULL,
    format            VARCHAR(5)  NOT NULL,
    period_from       DATE NOT NULL,
    period_to         DATE NOT NULL,
    status            VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    file_name         VARCHAR(200),
    storage_object_id UUID,
    error_message     VARCHAR(500),
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at       TIMESTAMP,
    CONSTRAINT ck_report_exports_type   CHECK (type IN ('MONTHLY_DRE','CASH_FLOW','TRANSACTIONS','AGENDA')),
    CONSTRAINT ck_report_exports_format CHECK (format IN ('PDF','XLSX')),
    CONSTRAINT ck_report_exports_status CHECK (status IN ('PENDING','READY','FAILED')),
    CONSTRAINT ck_report_exports_period CHECK (period_to >= period_from)
);
CREATE INDEX idx_report_exports_company_user ON report_exports (company_id, requested_by, created_at DESC);
-- + ENABLE/FORCE RLS, policy tenant_isolation_policy e GRANT ao crm_app (igual V093)
```

Commit: `feat(reports): report_exports table (V096)`.

## Task 2 — Domínio, porta e persistência

- `domain/ReportExportType` (`MONTHLY_DRE`, `CASH_FLOW`, `TRANSACTIONS`, `AGENDA`) com `label()` pt-BR ("Contábil mensal (DRE)", "Fluxo de caixa", "Extrato de lançamentos", "Agenda / atendimentos"), `requiredPermission()` e `fileSlug()` (`contabil-mensal`, `fluxo-de-caixa`, `extrato-lancamentos`, `agenda`).
- `domain/ExportFormat` (`PDF`, `XLSX`) com `contentType()` e `extension()`.
- `domain/ReportExport` (status `PENDING|READY|FAILED`; métodos `markReady(storageObjectId, fileName)`, `markFailed(message)`).
- `application/port/out/ReportExportRepository` + JPA entity/repository/impl (padrão `AccountingReportRequestRepositoryImpl`).
- Teste de domínio: transições de status (não sai de READY/FAILED).

Commit: `feat(reports): report export domain and persistence`.

## Task 3 — Datasets (dados de cada relatório)

`application/port/out/ReportDataGateway` + `infrastructure/persistence/ReportDataGatewayImpl` (queries nativas/JPQL **com `CAST` explícito ou filtros só quando preenchidos** — nada de `:x IS NULL OR ...`, que quebra no PostgreSQL). Records em `application/dto`:

1. **MonthlyDreDataset** — regime de caixa (lançamentos `PAID` com `paid_at` no mês; cancelados fora). Grupos e mapeamento das nossas categorias (`TransactionCategory`):
   | Grupo | Categorias |
   |---|---|
   | Receitas | PROCEDURE, CONSULTATION, PRODUCT_SALE, OTHER_INCOME |
   | Deduções sobre receita | TAXES |
   | Despesas com pessoal | PAYROLL |
   | Despesas administrativas | RENT, UTILITIES, EQUIPMENT, OTHER_EXPENSE |
   | Divulgação e marketing | MARKETING |
   | Despesas clínicas | SUPPLIES, LAB |
   | Resultado financeiro | "Taxas de cartão/maquininha" = soma de `fee_cents` dos lançamentos pagos |
   Cada linha: rótulo da categoria (`FinanceLabels`), nº de lançamentos, valor. Mostrar todas as categorias do grupo, mesmo com 0. `Resultado do mês = Receitas − Deduções − Pessoal − Administrativas − Marketing − Clínicas − Taxas`.
2. **CashFlowDataset** — por dia do período: entradas pagas, saídas pagas, saldo do dia e saldo acumulado; totais. Reaproveite a lógica do relatório de fluxo de caixa existente (`finance/report`) se ela já entregar esses números.
3. **TransactionsDataset** — lançamentos com vencimento no período (todos os status exceto cancelados, opção futura): vencimento, pagamento, descrição, paciente, categoria, forma de pagamento, tipo, valor (despesa negativa), status; totais de receitas, despesas e saldo.
4. **AgendaDataset** — consultas do período: por profissional, contagem por status (agendadas, confirmadas, realizadas, faltas, canceladas) e lista detalhada (data/hora, paciente, tipo, profissional, status, origem).

Testes unitários do mapeamento DRE (grupos, subtotais, resultado com taxa) usando dados em memória.

Commit: `feat(reports): datasets for DRE, cash flow, transactions and agenda`.

## Task 4 — Renderizadores PDF e XLSX

- `pom.xml`: adicionar `org.apache.poi:poi-ooxml` (versão estável atual da linha 5.x).
- `application/service/render/PdfReportLayout` (OpenPDF, A4 retrato; Agenda e Extrato em paisagem): cabeçalho (título em caixa alta na cor primária, período, nome/CNPJ da empresa, "Emitido em dd/MM/yyyy - HH:mm:ss" à direita), tabela com cabeçalho colorido, **faixa clara para grupos**, linhas zebradas finas, **linha de subtotal em negrito**, **barra final de resultado** (fundo na cor primária, texto branco), valores à direita, zeros em cinza, negativos com "−". Rodapé em todas as páginas: empresa · "Página X/Y" · emitido em. Cabeçalho da tabela repetido em quebra de página.
- `application/service/render/XlsxReportWriter` (POI): mesma estrutura em planilha — linha de título, período/emissão, cabeçalho em negrito com fundo, grupos/subtotais em negrito, valores como **número** com formato `R$ #,##0.00` (não texto), datas como data, colunas com largura ajustada, painel congelado no cabeçalho.
- Um `ReportRenderer` por tipo que converte o dataset em linhas genéricas (`ReportTable`: colunas, linhas com tipo GROUP/ITEM/SUBTOTAL/TOTAL) consumidas pelos dois writers — layout escrito uma vez só.
- Testes: PDF gerado não vazio e começa com `%PDF`; XLSX abre com POI e a célula de valor é numérica; DRE contém "Resultado do mês".

Commit: `feat(reports): PDF and XLSX renderers with shared table layout`.

## Task 5 — Serviço assíncrono, notificação e endpoints

- `ReportExportService.request(companyId, userId, permissions, type, format, from, to)`: valida permissão e período, grava `PENDING`, publica evento; um listener `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` chama `ReportExportGenerator.generate(exportId)`.
- `generate`: TenantContext → dataset → render → `StorageUseCase.upload(companyId, fileName, contentType, bytes, userId)` → `markReady` → `NotificationUseCase.create(companyId, new CreateNotificationRequest(userId, NotificationType.SYSTEM, "Relatório pronto para download", "<label> · <período> · PDF|Excel", metadata), ...)` com `metadata = {"kind":"REPORT_EXPORT","exportId":"…","fileName":"…","format":"PDF"}`. Em erro: `markFailed` + notificação "Não foi possível gerar o relatório" (sem link). Nunca deixa o registro em PENDING.
- Nome do arquivo: `<fileSlug>_<yyyy-MM-dd>_a_<yyyy-MM-dd>.<ext>` (DRE: `contabil-mensal_2026-06.pdf`).
- `web/ReportExportController` em `/api/v1/companies/{companyId}/report-exports`:
  - `POST` → 202 com `ReportExportResponse {id, type, format, status, fileName, createdAt}`;
  - `GET` → últimos 20 do usuário;
  - `GET /{id}` → status;
  - `GET /{id}/download` → `Content-Disposition: attachment`, content-type do formato, bytes do storage (404 se não for do usuário/empresa ou não estiver READY).
- Permissão de entrada no controller: `isAuthenticated()`; a checagem fina por tipo fica no service (usa as authorities do `CurrentUser`).
- Testes: service (permissão negada, período inválido, fluxo feliz chama upload + notificação com metadata, falha marca FAILED e notifica); controller download de outro usuário → 404.

Commit: `feat(reports): async report export with notification and download URL`.

## Task 6 — Frontend: submenu e tela de exportação

- `F/lib/constants.ts`: `REPORTS_EXPORT: "/reports/export"`.
- `F/components/layout/navigation.ts`: **Relatórios** passa a ter `children`: "Painel de relatórios" (`/reports`), "Contábil mensal (DRE)" (`/reports/export/monthly-dre`), "Fluxo de caixa" (`/reports/export/cash-flow`), "Extrato de lançamentos" (`/reports/export/transactions`), "Agenda / atendimentos" (`/reports/export/agenda`). Esconder itens sem permissão (mesmo mecanismo usado nos outros itens com permissão).
- `F/app/(dashboard)/reports/export/[type]/page.tsx` + `F/features/reports/export/` (types, service, hooks com React Query, componente `ReportExportForm`): título e descrição do relatório, seletor de período (DRE: mês; demais: intervalo com atalhos "Este mês", "Mês passado", "Últimos 3 meses"), botões **Imprimir (PDF)** e **Exportar (Excel)**. Ao enviar: toast "Gerando o relatório… você será avisado no sino quando estiver pronto." e lista "Exportações recentes" (status Gerando/Pronto/Falhou, botão Baixar quando pronto) com polling a cada 5 s enquanto houver PENDING.
- Testes (vitest): envio chama o service com tipo/formato/período; botão Baixar aparece só em READY.

Commit: `feat(reports): export submenu and report export page`.

## Task 7 — Sino com link de download

- `F/features/communication/notifications`: quando `metadata.kind === "REPORT_EXPORT"`, a notificação mostra o botão **Baixar** (ícone Download). Clique: `GET /report-exports/{exportId}/download` com o cliente HTTP autenticado (blob) → salva com `fileName`; marca a notificação como lida. Mesmo comportamento na página `/notifications`.
- Helper compartilhado `downloadReportExport(exportId, fileName)` usado no sino, na página de notificações e na lista de exportações recentes.
- Testes: renderiza "Baixar" só para REPORT_EXPORT; clique chama o download e marca como lida.

Commit: `feat(notifications): download link for report exports`.

## Task 8 — Exportações existentes passam pela notificação

"Todo arquivo impresso ou exportado tem que passar pela notificação":
- `F/features/finance/components/AccountingReportDialog.tsx` (relatório contábil atual): passar a usar `POST /report-exports` (Contábil mensal → `MONTHLY_DRE`; Extrato/Lançamentos → `TRANSACTIONS`) e avisar pelo sino, em vez de baixar direto.
- Botões de CSV dos relatórios da tela (`F/features/finance/components/reports/report-csv.ts` usado em Overview/Agenda/CashFlow/Revenue): trocar por "Exportar" que chama o tipo equivalente (`CASH_FLOW`, `AGENDA`; Overview/Revenue → `TRANSACTIONS` do período) em XLSX. Se algum não tiver equivalente, deixe o botão levando para `/reports/export/<tipo>` mais próximo.
- Não remover o endpoint antigo `/finance/accounting-reports` nesta entrega (só deixa de ser usado pelo frontend); anote no PR.

Commit: `refactor(reports): route existing exports through report export + notification`.

## Task 9 — Verificação final

- Backend `mvn -q test`; frontend vitest dos módulos tocados, `tsc`, prettier.
- Teste manual local (se possível): gerar cada relatório em PDF e Excel, ver a notificação no sino, baixar e abrir o arquivo.
- Relatar arquivos, testes e hashes. Sem push.

## Fora de escopo (próximas entregas)

- Expiração/limpeza automática dos arquivos gerados (ex.: 30 dias) e contagem na quota de Arquivos.
- Envio do relatório por e-mail ao contador.
- Novas categorias contábeis mais detalhadas (impostos separados por tributo, pró-labore etc.) — exigiria ampliar `TransactionCategory`.
