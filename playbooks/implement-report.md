# Playbook: Implementação do Módulo Report

## Objetivo
Implementar o sistema de relatórios: consultas analíticas avançadas, exportação em PDF/CSV/Excel, agendamento de relatórios, e geração automática.

## Pré-requisitos
- Módulo Dashboard implementado (queries analíticas base)
- Sistema de cache implementado
- Sistema de armazenamento de arquivos (FileStorage) configurado
- Bibliotecas de geração: PDF (puppeteer ou pdfmake), Excel (exceljs)

## Documentos que DEVEM ser lidos
- `docs/Reports.md`
- `docs/Analytics.md`
- `docs/FileStorage.md`
- `contexts/analytics-context.md`

## Arquivos que poderão ser alterados
### Backend
- `packages/backend/src/application/analytics/` — ReportUseCase, GenerateReportUseCase, ExportReportUseCase, ScheduleReportUseCase
- `packages/backend/src/infrastructure/persistence/` — ReportRepository
- `packages/backend/src/infrastructure/storage/` — FileStorageService (para salvar relatórios gerados)
- `packages/backend/src/infrastructure/scheduler/` — ReportScheduler (agendamento de relatórios)
- `packages/backend/src/presentation/rest/controller/ReportController.ts`

### Frontend
- `packages/frontend/src/components/reports/` — ReportList, ReportBuilder, ReportPreview, ReportExport, ReportScheduler
- `packages/frontend/src/hooks/useReports.ts`
- `packages/frontend/src/app/(auth)/reports/` — Páginas: index, [id], create, preview

## Arquivos proibidos
- `packages/backend/src/domain/` — Nenhuma entidade de domínio deve ser alterada
- `packages/frontend/src/components/dashboard/` — Dashboard components não devem ser alterados
- `packages/backend/src/infrastructure/cache/` — Cache service não deve ser alterado

## Ordem de implementação
1. Definir tipos de relatórios: leads, pipeline, contatos, atividades, performance
2. Implementar ReportRepository com queries parametrizadas
3. Implementar GenerateReportUseCase (gera dados do relatório)
4. Implementar ExportReportUseCase (PDF, CSV, Excel)
5. Implementar FileStorageService para salvar relatórios gerados
6. Implementar ScheduleReportUseCase (agendar relatório diário/semanal)
7. Implementar ReportScheduler com cron jobs
8. Implementar ReportController
9. Criar ReportBuilder no frontend (seletor de tipo, filtros, colunas)
10. Criar ReportPreview (visualização antes de exportar)
11. Criar ReportExport (botões de exportação)
12. Criar ReportScheduler (agendar envio por email)

## Checklist Backend
- [ ] ReportRepository: queries para cada tipo de relatório
- [ ] Relatório de Leads: leads por período, por status, por fonte, por responsável
- [ ] Relatório de Pipeline: oportunidades por stage, valor por stage, previsão de fechamento
- [ ] Relatório de Contatos: contatos por período, por empresa, por tag
- [ ] Relatório de Atividades: mensagens enviadas/recebidas, tempo de resposta
- [ ] Relatório de Performance: taxa de conversão, tempo médio de ciclo, valor total vendido
- [ ] GenerateReportUseCase: gera dados estruturados com filtros (data range, tags, stages)
- [ ] ExportReportUseCase: exporta para CSV (csv-writer), Excel (exceljs), PDF (puppeteer/pdfmake)
- [ ] FileStorageService: salva relatório gerado com URL de download
- [ ] ScheduleReportUseCase: agenda relatório com frequência (diário, semanal, mensal)
- [ ] ReportScheduler: executa relatórios agendados e envia por email
- [ ] ReportController: GET /reports/types, POST /reports/generate, GET /reports/:id/export, POST /reports/schedule
- [ ] Validação: período máximo de 1 ano por consulta
- [ ] Rate limiting: máximo 10 relatórios por hora
- [ ] Multi-tenancy: relatórios filtrados por company_id

## Checklist Frontend
- [ ] ReportList: lista de relatórios disponíveis com descrição
- [ ] ReportBuilder: seletor de tipo + filtros (período, tags, stages, responsável) + seleção de colunas
- [ ] ReportPreview: preview dos dados antes de exportar (tabela com paginação)
- [ ] ReportExport: botões de exportação (CSV, Excel, PDF)
- [ ] ReportScheduler: formulário para agendar relatório (frequência, email destinatário)
- [ ] ReportHistory: histórico de relatórios gerados (download)
- [ ] Hook useReports: getTypes, generate, export, schedule, getHistory
- [ ] Loading state durante geração/exportação
- [ ] Download automático ao clicar em exportar

## Checklist Banco
- [ ] Tabela `reports`: id, name, type, filters (JSON), columns (JSON), company_id (FK), created_by (FK users), created_at
- [ ] Tabela `report_schedules`: id, report_id (FK), frequency, email, is_active, last_run_at, next_run_at
- [ ] Tabela `report_files`: id, report_id (FK), file_url, file_size, format, generated_at, expires_at
- [ ] Índices: reports.company_id, report_schedules.next_run_at
- [ ] Foreign keys com ON DELETE CASCADE

## Checklist Testes
- [ ] Testes unitários: queries de relatório (dados mockados)
- [ ] Testes unitários: exportação CSV/Excel/PDF
- [ ] Testes de integração: gerar relatório com filtros corretos
- [ ] Testes de integração: exportação gera arquivo válido
- [ ] Testes de integração: agendamento funciona
- [ ] Testes de integração: multi-tenancy isola dados
- [ ] Testes de performance: relatório complexo gera em < 5s
- [ ] Testes E2E: selecionar tipo → filtrar → preview → exportar

## Checklist Documentação
- [ ] Atualizar `docs/Reports.md` com tipos de relatório, endpoints e exemplos
- [ ] Documentar formatos de exportação
- [ ] Documentar agendamento de relatórios
- [ ] Documentar variáveis de ambiente (FileStorage config)

## Checklist Final
- [ ] Relatórios são gerados com dados corretos
- [ ] Exportação CSV/Excel/PDF funciona
- [ ] Agendamento de relatórios funciona
- [ ] Arquivos são salvos e podem ser baixados
- [ ] Multi-tenancy isola relatórios por empresa
- [ ] Performance aceitável para relatórios complexos
- [ ] Todos os testes passam
