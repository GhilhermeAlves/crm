# Playbook: Implementação do Módulo Company

## Objetivo
Implementar o gerenciamento de empresas (tenants), configurações da empresa, e o sistema de multi-tenancy que baseia todo o isolamento de dados do CRM.

## Pré-requisitos
- Módulo Auth implementado (usuários autenticados)
- Estrutura de banco de dados configurada
- Configurações de schema multi-tenancy definidas

## Documentos que DEVEM ser lidos
- `docs/Companies.md`
- `docs/03-database/Overview.md`
- `contexts/company-context.md`
- `contexts/multi-tenancy-context.md`

## Arquivos que poderão ser alterados
### Backend
- `packages/backend/src/domain/company/` — Entidades: Company, CompanySettings, Subscription
- `packages/backend/src/application/company/` — Casos de uso: CreateCompanyUseCase, UpdateCompanyUseCase, GetCompanySettingsUseCase, UpdateCompanySettingsUseCase
- `packages/backend/src/infrastructure/persistence/` — CompanyRepository, CompanySettingsRepository
- `packages/backend/src/presentation/rest/controller/CompanyController.ts`
- `packages/backend/src/presentation/rest/controller/CompanySettingsController.ts`
- `packages/backend/src/infrastructure/multi-tenancy/` — TenantContext, TenantMiddleware

## Arquivos proibidos
- `packages/backend/src/infrastructure/database/migrations/` — NÃO alterar migrations executadas
- `packages/backend/src/config/database.ts` — Config de conexão não pertence a este módulo
- `packages/backend/src/presentation/rest/controller/AuthController.ts` — Auth não deve ser alterado

## Ordem de implementação
1. Definir entidades de domínio: Company, CompanySettings, Subscription
2. Implementar repositórios de persistência
3. Implementar TenantMiddleware (extrai company_id do token/logged user)
4. Implementar TenantContext (propaga company_id para todas as queries)
5. Implementar casos de uso: Create, Update, GetSettings, UpdateSettings
6. Implementar CompanyController e CompanySettingsController
7. Implementar criação automática de schema ao registrar empresa
8. Configurar isolamento de dados por tenant em todas as queries
9. Implementar configurações padrão da empresa ao criar
10. Testar multi-tenancy: dados de empresa A não aparecem para empresa B

## Checklist Backend
- [ ] Entidade Company: id, name, slug, cnpj, email, phone, logo, isActive, createdAt, updatedAt
- [ ] Entidade CompanySettings: id, companyId, timezone, locale, currency, businessHours, notificationPreferences
- [ ] Entidade Subscription: id, companyId, plan, startDate, endDate, status, maxUsers, maxContacts
- [ ] TenantMiddleware extrai company_id do JWT e injeta no contexto
- [ ] TenantContext propaga company_id para todas as queries do repositório
- [ ] CreateCompanyUseCase cria empresa + configurações padrão + schema (se schema-per-tenant)
- [ ] UpdateCompanyUseCase com validação de dados
- [ ] GetCompanySettingsUseCase retorna configurações da empresa logada
- [ ] UpdateCompanySettingsUseCase atualiza configurações
- [ ] Todas as queries de dados filtram por company_id automaticamente
- [ ] Validação: slug único
- [ ] Validação: cnpj único (se preenchido)
- [ ] Rate limiting por empresa (se aplicável)

## Checklist Frontend
- [ ] Página de configurações da empresa (nome, logo, dados fiscais)
- [ ] Formulário de edição de dados da empresa
- [ ] Página de configurações: timezone, moeda, idioma
- [ ] Upload de logo da empresa
- [ ] Exibição de informações da assinatura/plano
- [ ] Hook useCompany com: get, update, getSettings, updateSettings

## Checklist Banco
- [ ] Tabela `companies`: id, name, slug (unique), cnpj, email, phone, logo_url, is_active, created_at, updated_at
- [ ] Tabela `company_settings`: id, company_id (FK UNIQUE), timezone, locale, currency, business_hours (JSON), notification_preferences (JSON)
- [ ] Tabela `subscriptions`: id, company_id (FK), plan, start_date, end_date, status, max_users, max_contacts
- [ ] Se schema-per-tenant: função de criação de schema
- [ ] Índices em: companies.slug, companies.cnpj
- [ ] Foreign keys com ON DELETE CASCADE para settings e subscription

## Checklist Testes
- [ ] Testes unitários: CreateCompanyUseCase (validações)
- [ ] Testes unitários: CompanySettings (validações)
- [ ] Testes de integração: CRUD completo de empresas
- [ ] Testes de integração: Multi-tenancy — isolamento de dados entre empresas
- [ ] Testes de integração: TenantMiddleware extrai company_id corretamente
- [ ] Testes de integração: Configurações padrão são criadas automaticamente
- [ ] Testes de integração: Validação de slug único
- [ ] Testes E2E: Criar empresa → configurar → verificar isolamento

## Checklist Documentação
- [ ] Atualizar `docs/Companies.md` com endpoints e exemplos
- [ ] Atualizar `docs/03-database/Overview.md` com schema de empresas
- [ ] Documentar estratégia de multi-tenancy (schema-per-tenant vs row-level)
- [ ] Documentar configurações padrão de nova empresa

## Checklist Final
- [ ] CRUD de empresas funciona
- [ ] Multi-tenancy isola dados corretamente
- [ ] Configurações da empresa são persistidas e recuperadas
- [ ] Criação de empresa gera configurações padrão
- [ ] TenantMiddleware propaga company_id em todas as operações
- [ ] Nenhum dado vaza entre empresas
- [ ] Todos os testes passam
