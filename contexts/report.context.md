# Report Context

## Resumo do Módulo
7 tipos de relatórios: Sales, Team, Conversations, Campaigns, Pipeline, Contacts, Automations. Geração assíncrona. Export PDF/CSV/Excel. 7 dias retenção de exports.

## Objetivo
Gerar relatórios analíticos detalhados com exportação em múltiplos formatos.

## Responsabilidades
- 7 tipos de relatórios configuráveis
- Geração assíncrona (jobs em background)
- Export: PDF, CSV, Excel
- Retenção de exports: 7 dias
- Período máximo: 12 meses

## Tipos de Relatório
| Tipo | Escopo |
|------|--------|
| Sales | Vendas, receita, forecast |
| Team | Performance por agente/equipe |
| Conversations | Volume, tempo resposta, satisfação |
| Campaigns | Entrega, abertura, conversão |
| Pipeline | Funil, conversão por estágio |
| Contacts | Crescimento, segmentação |
| Automations | Execuções, sucesso/falha |

## APIs Relacionadas
- `GET /reports` - Listar relatórios disponíveis
- `POST /reports/:type` - Gerar relatório (async)
- `GET /reports/:id` - Status do relatório
- `GET /reports/:id/download` - Download do arquivo
- `GET /reports/exports` - Listar exports disponíveis
- `DELETE /reports/:id` - Remover relatório

## Banco Relacionado
- `reports` - Metadados do relatório gerado
- `report_schedules` - Agendamento de relatórios

## Componentes Frontend
- ReportsList, ReportBuilder
- ReportViewer (preview)
- ExportSelector (PDF/CSV/Excel)
- ScheduleConfig

## Componentes Backend
- `report` module (Controllers, Services, Domain)
- `generator` module (jobs de geração assíncrona)
- `export` module (PDF, CSV, Excel renderers)
- `file-storage` module (armazenamento temporário)
- `cleanup` job (remove exports > 7 dias)

## Eventos
- `ReportRequested` - Relatório solicitado
- `ReportGenerated` - Relatório pronto
- `ReportExported` - Export criado
- `ReportExpired` - Export expirado (7 dias)

## Permissões
- `report:create` - ADMIN, MANAGER
- `report:read` - Todos (próprios)
- `report:download` - ADMIN, MANAGER
- `report:schedule` - ADMIN, MANAGER
- `report:delete` - ADMIN

## Dependências
- **Cache** - Dados para cálculos
- **FileStorage** - Armazenamento de exports
- **Scheduler** - Agendamento de relatórios
- **Events** - Dados para métricas

## Fluxo Resumido
1. Usuário solicita relatório → job criado → processamento assíncrono
2. Relatório pronto → notificação → download disponível (7 dias)
3. Cleanup job remove exports expirados diariamente

## Checklist de Implementação
- [ ] 7 tipos de relatório
- [ ] Geração assíncrona
- [ ] Export PDF/CSV/Excel
- [ ] Retenção 7 dias
- [ ] Período máximo 12 meses
- [ ] Agendamento de relatórios
- [ ] Preview antes de exportar
- [ ] Cleanup de exports expirados

## Checklist de Testes
- [ ] Relatório gerado corretamente
- [ ] Export em 3 formatos funciona
- [ ] Retenção 7 dias remove arquivos
- [ ] Período máximo 12 meses validado
- [ ] Agendamento executa no horário

## Documentação Oficial Relacionada
- `docs/report/REPORT-TYPES.md`
- `docs/report/EXPORT-GUIDE.md`
- `docs/report/SCHEDULING.md`

## Histórico de Revisão
| Data | Autor | Descrição |
|------|-------|-----------|
| 2026-07-15 | System | Criação do contexto |
