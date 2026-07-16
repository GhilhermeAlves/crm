# Playbook: Implementação do Módulo Lead

## Objetivo
Implementar o gerenciamento de leads: captação, scoring, qualificação, conversão para oportunidade, e integração com o pipeline de vendas.

## Pré-requisitos
- Módulo Contact implementado (leads são extensões de contatos)
- Módulo Pipeline implementado (leads convertem para oportunidades no pipeline)
- Módulo Auth implementado

## Documentos que DEVEM ser lidos
- `docs/Leads.md`
- `docs/05-business-rules/Lead.md`
- `docs/Contacts.md`
- `docs/Pipeline.md`
- `contexts/contact-context.md`

## Arquivos que poderão ser alterados
### Backend
- `packages/backend/src/domain/contact/` — Entidade: Lead (entidade de contato com campos de lead)
- `packages/backend/src/application/contact/` — Casos de uso: CreateLeadUseCase, UpdateLeadUseCase, ListLeadsUseCase, ScoreLeadUseCase, ConvertLeadUseCase, QualifyLeadUseCase
- `packages/backend/src/infrastructure/persistence/` — LeadRepository
- `packages/backend/src/presentation/rest/controller/LeadController.ts`
- `packages/backend/src/presentation/rest/dto/` — LeadDTOs

### Frontend
- `packages/frontend/src/components/leads/` — LeadList, LeadForm, LeadCard, LeadDetail, LeadScore, LeadConversionModal
- `packages/frontend/src/hooks/useLeads.ts`
- `packages/frontend/src/app/(auth)/leads/` — Páginas: list, [id], create, edit

## Arquivos proibidos
- `packages/backend/src/domain/pipeline/` — Pipeline entities não devem ser alteradas aqui
- `packages/backend/src/presentation/rest/controller/ContactController.ts` — Contact controller não deve ser alterado
- `packages/frontend/src/components/contacts/` — Componentes de contact não devem ser alterados

## Ordem de implementação
1. Definir entidade Lead no domínio (estende Contact ou herda campos)
2. Definir fórmula de scoring (0-100 pontos)
3. Implementar repositório de leads
4. Implementar CreateLeadUseCase (cria contato + lead)
5. Implementar UpdateLeadUseCase
6. Implementar ListLeadsUseCase com filtros (status, score, origem)
7. Implementar ScoreLeadUseCase com fórmula de scoring
8. Implementar ConvertLeadUseCase (cria oportunidade no pipeline)
9. Implementar LeadController
10. Criar componentes frontend: lista Kanban/lista, formulário, detalhe
11. Criar modal de conversão (lead → oportunidade)
12. Integrar com hook useLeads

## Checklist Backend
- [ ] Entidade Lead: id, contactId, status (new/contacted/qualified/unqualified/converted), score, source, campaignId, assignedTo, companyId, createdAt, updatedAt
- [ ] Status do lead: new → contacted → qualified → converted (ou unqualified)
- [ ] Fórmula de scoring implementada: pontos por ações (abriu email +10, visitou site +15, respondeu +20, etc.)
- [ ] ScoreLeadUseCase calcula score baseado em histórico de interações
- [ ] CreateLeadUseCase cria contato associado + registro de lead
- [ ] ConvertLeadUseCase: cria oportunidade no pipeline + atualiza status para converted
- [ ] ConvertLeadUseCase: valida se lead não já convertido
- [ ] ListLeadsUseCase com filtros: status, score range, source, assignedTo
- [ ] ListLeadsUseCase com ordenação por score (decrescente)
- [ ] LeadController com endpoints: GET /leads, GET /leads/:id, POST /leads, PUT /leads/:id, POST /leads/:id/convert, POST /leads/:id/score
- [ ] Validação: lead não pode ser convertido duas vezes
- [ ] Multi-tenancy: leads filtrados por company_id

## Checklist Frontend
- [ ] LeadList com visualização Kanban (colunas por status) OU lista com filtros
- [ ] LeadCard: nome, empresa, score (indicador visual), status, fonte
- [ ] LeadForm: campos do contato + campos de lead (status, source, assignedTo)
- [ ] LeadScore: indicador visual de score (barra de progresso ou badge colorido)
- [ ] LeadDetail: informações completas + timeline de interações + botão converter
- [ ] LeadConversionModal: selecionar pipeline + stage de destino + dados da oportunidade
- [ ] Filtros: por status, score mínimo, fonte, responsável
- [ ] Hook useLeads: list, get, create, update, convert, getScore
- [ ] Dashboard widget: leads por status, score médio

## Checklist Banco
- [ ] Tabela `leads`: id, contact_id (FK UNIQUE), status, score (integer 0-100), source, campaign_id, assigned_to (FK users), company_id (FK), created_at, updated_at
- [ ] Índices em: leads.company_id, leads.status, leads.score, leads.assigned_to
- [ ] Foreign key: contact_id → contacts.id ON DELETE CASCADE
- [ ] Foreign key: assigned_to → users.id ON DELETE SET NULL
- [ ] Check constraint: score BETWEEN 0 AND 100
- [ ] Check constraint: status IN ('new', 'contacted', 'qualified', 'unqualified', 'converted')

## Checklist Testes
- [ ] Testes unitários: fórmula de scoring (diversos cenários)
- [ ] Testes unitários: CreateLeadUseCase (validações)
- [ ] Testes de integração: CRUD de leads
- [ ] Testes de integração: Conversão de lead para oportunidade
- [ ] Testes de integração: Validação — lead não converte duas vezes
- [ ] Testes de integração: Scoring reflete interações corretamente
- [ ] Testes de integração: Listagem com filtros
- [ ] Testes E2E: Criar lead → qualificar → converter → verificar oportunidade criada

## Checklist Documentação
- [ ] Atualizar `docs/Leads.md` com endpoints e exemplos
- [ ] Atualizar `docs/05-business-rules/Lead.md` com regras de scoring
- [ ] Documentar fórmula de scoring completa
- [ ] Documentar fluxo de conversão lead → oportunidade

## Checklist Final
- [ ] CRUD de leads funciona
- [ ] Scoring calcula corretamente baseado em interações
- [ ] Conversão lead → oportunidade cria registro no pipeline
- [ ] Lead não pode ser convertido duas vezes
- [ ] Filtros e listagem funcionam
- [ ] Multi-tenancy isola leads por empresa
- [ ] Todos os testes passam
