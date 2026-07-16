# Playbook: Implementação do Módulo Contact

## Objetivo
Implementar o gerenciamento completo de contatos: CRUD, endereços, campos customizados, tags, e busca full-text. Contatos são a entidade central do CRM.

## Pré-requisitos
- Módulo Company implementado (multi-tenancy ativo)
- Módulo Auth implementado (usuários autenticados)
- Estrutura de domínio e persistência configuradas

## Documentos que DEVEM ser lidos
- `docs/Contacts.md`
- `docs/Entities.md`
- `docs/03-database/Overview.md`
- `contexts/contact-context.md`

## Arquivos que poderão ser alterados
### Backend
- `packages/backend/src/domain/contact/` — Entidades: Contact, ContactAddress, ContactCustomField, Tag, ContactTag
- `packages/backend/src/application/contact/` — Casos de uso: CreateContactUseCase, UpdateContactUseCase, ListContactsUseCase, DeleteContactUseCase, SearchContactsUseCase, ManageTagsUseCase, ManageCustomFieldsUseCase
- `packages/backend/src/infrastructure/persistence/` — ContactRepository, TagRepository, ContactCustomFieldRepository
- `packages/backend/src/presentation/rest/controller/ContactController.ts`
- `packages/backend/src/presentation/rest/controller/TagController.ts`
- `packages/backend/src/presentation/rest/dto/` — ContactDTOs

### Frontend
- `packages/frontend/src/components/contacts/` — ContactList, ContactForm, ContactCard, ContactDetail, TagManager
- `packages/frontend/src/hooks/useContacts.ts`
- `packages/frontend/src/app/(auth)/contacts/` — Páginas: list, [id], create, edit

## Arquivos proibidos
- `packages/backend/src/domain/identity/` — Não alterar entidades de identidade
- `packages/backend/src/infrastructure/security/` — Não alterar serviços de auth
- `packages/frontend/src/contexts/AuthContext.tsx` — AuthContext é do módulo Auth

## Ordem de implementação
1. Definir entidades de domínio: Contact, ContactAddress, Tag, ContactTag, ContactCustomField
2. Implementar repositórios de persistência (com suporte a multi-tenancy)
3. Implementar casos de uso CRUD de contatos
4. Implementar sistema de tags (criar, renomear, deletar, associar a contatos)
5. Implementar campos customizados (definir schema, CRUD)
6. Implementar busca full-text por contatos (nome, email, telefone, empresa)
7. Implementar ContactController com endpoints REST
8. Implementar TagController
9. Criar componentes frontend: lista com filtros e paginação
10. Criar formulário de criação/edição com campos dinâmicos
11. Criar página de detalhe do contato com histórico
12. Integrar com hook useContacts

## Checklist Backend
- [ ] Entidade Contact: id, name, email, phone, mobile, company_name, position, birthdate, notes, leadSource, companyId, createdAt, updatedAt
- [ ] Entidade ContactAddress: id, contactId, type (billing/shipping), street, number, complement, neighborhood, city, state, zipCode, country
- [ ] Entidade Tag: id, name, color, companyId
- [ ] Entidade ContactTag: contactId, tagId
- [ ] Entidade ContactCustomField: id, contactId, fieldKey, fieldValue, fieldType (text/number/date/boolean)
- [ ] CreateContactUseCase com validação de dados
- [ ] UpdateContactUseCase com merge de dados
- [ ] ListContactsUseCase com paginação, ordenação, filtros (tag, empresa, leadSource)
- [ ] SearchContactsUseCase com busca full-text (ILIKE em name, email, phone, company_name)
- [ ] DeleteContactUseCase (hard delete ou soft delete conforme regra)
- [ ] ManageTagsUseCase: criar, renomear, deletar tags
- [ ] ManageTagsUseCase: associar/desassociar tags de contatos
- [ ] Todas as queries filtram por company_id (multi-tenancy)
- [ ] Validação: email único por empresa (se aplicável)
- [ ] ContactController com endpoints: GET /contacts, GET /contacts/:id, POST /contacts, PUT /contacts/:id, DELETE /contacts/:id, POST /contacts/:id/tags, DELETE /contacts/:id/tags/:tagId
- [ ] TagController com endpoints: GET /tags, POST /tags, PUT /tags/:id, DELETE /tags/:id

## Checklist Frontend
- [ ] ContactList com DataTable: nome, email, telefone, empresa, tags, ações
- [ ] Filtros avançados: por tag, por leadSource, por empresa, busca textual
- [ ] Paginação (server-side ou client-side)
- [ ] ContactForm: campos básicos + endereço + tags + campos customizados
- [ ] ContactDetail: visualização completa +=endereços + tags + notas
- [ ] TagManager: criar, editar, excluir tags com cores
- [ ] Seleção múltipla de contatos para ação em lote (etiquetar, deletar)
- [ ] Hook useContacts: list, get, create, update, delete, search, addTag, removeTag
- [ ] Importação de contatos (CSV) — se aplicável
- [ ] Exportação de contatos (CSV) — se aplicável

## Checklist Banco
- [ ] Tabela `contacts`: id, name, email, phone, mobile, company_name, position, birthdate, notes, lead_source, company_id (FK), created_at, updated_at
- [ ] Tabela `contact_addresses`: id, contact_id (FK), type, street, number, complement, neighborhood, city, state, zip_code, country
- [ ] Tabela `tags`: id, name, color, company_id (FK), created_at
- [ ] Tabela `contact_tags`: contact_id (FK), tag_id (FK), created_at
- [ ] Tabela `contact_custom_fields`: id, contact_id (FK), field_key, field_value, field_type, created_at
- [ ] Índices em: contacts.email, contacts.company_id, contacts.name (GIN para busca), tags.company_id
- [ ] Foreign keys com ON DELETE CASCADE para addresses, contact_tags, custom_fields
- [ ] Unique constraint: tags.name + company_id

## Checklist Testes
- [ ] Testes unitários: CreateContactUseCase (validações)
- [ ] Testes unitários: SearchContactsUseCase (busca)
- [ ] Testes de integração: CRUD completo de contatos
- [ ] Testes de integração: Tags — criar, associar, desassociar, deletar
- [ ] Testes de integração: Campos customizados
- [ ] Testes de integração: Busca full-text retorna resultados corretos
- [ ] Testes de integração: Multi-tenancy — contatos de empresa A não aparecem para empresa B
- [ ] Testes de integração: Paginação e filtros
- [ ] Testes E2E: Criar contato → adicionar tags → buscar → editar → deletar

## Checklist Documentação
- [ ] Atualizar `docs/Contacts.md` com endpoints, exemplos e DTOs
- [ ] Atualizar `docs/Entities.md` com entidades de contato
- [ ] Documentar sistema de tags
- [ ] Documentar campos customizados

## Checklist Final
- [ ] CRUD de contatos funciona completamente
- [ ] Tags funcionam (criar, associar, filtrar)
- [ ] Campos customizados funcionam
- [ ] Busca full-text retorna resultados relevantes
- [ ] Multi-tenancy isola contatos por empresa
- [ ] Paginação e filtros funcionam
- [ ] Todos os testes passam
