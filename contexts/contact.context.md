# Contact Context

## Resumo do Módulo
Gestão centralizada de contatos com endereços, campos customizados, tags e segmentos. Email único por empresa, phone no padrão E.164.

## Objetivo
Ser a fonte única de verdade para dados de contatos de cada empresa.

## Responsabilidades
- CRUD de contatos com dados pessoais e de empresa
- Endereços múltiplos por contato
- Campos customizados (max 50 por empresa)
- Tags (max 5 por contato) e segmentos dinâmicos
- Importação em massa (max 10k contatos/batch)

## Entidades Relacionadas
- Contact, ContactAddress, ContactCustomField, Tag, ContactTag, Segment

## APIs Relacionadas
- `GET /contacts` - Listar contatos (paginado, filtros)
- `POST /contacts` - Criar contato
- `GET /contacts/:id` - Detalhes do contato
- `PUT /contacts/:id` - Atualizar contato
- `DELETE /contacts/:id` - Remover contato
- `POST /contacts/import` - Importar CSV (10k max)
- `GET /contacts/export` - Exportar contatos
- `GET /contacts/tags` - Listar tags
- `POST /contacts/segments` - Criar segmento

## Banco Relacionado
- `contacts` - Dados principais (email único/empresa, phone E.164)
- `contact_addresses` - Endereços múltiplos
- `contact_custom_fields` - Campos customizados (50/empresa)
- `tags` - Catálogo de tags
- `contact_tags` - Relação contato-tag (5/contato)
- `segments` - Segmentos dinâmicos

## Componentes Frontend
- ContactsList, ContactForm, ContactDetail
- TagManager, SegmentBuilder
- ImportModal, ExportButton

## Componentes Backend
- `contact` module (Controllers, Services, Domain, Repository)
- `import` module (CSV parser, batch processor)
- `segment` module (dynamic query builder)

## Eventos
- `ContactCreated` - Novo contato
- `ContactUpdated` - Dados atualizados
- `ContactDeleted` - Contato removido
- `ContactImported` - Importação concluída
- `ContactTagAdded/Removed` - Tags alteradas
- `ContactSegmentMatched` - Entra em segmento

## Permissões
- `contact:create` - ADMIN, MANAGER, AGENT
- `contact:read` - Todos
- `contact:update` - ADMIN, MANAGER, AGENT
- `contact:delete` - ADMIN, MANAGER
- `contact:import` - ADMIN, MANAGER
- `contact:export` - ADMIN, MANAGER

## Dependências
- **Companies** (isolamento por empresa)
- **Pipeline** (contato associado a oportunidades)
- **Conversations** (contato em conversas)

## Fluxo Resumido
1. Usuário cria importa CSV → batch processa 10k contatos → valida duplicatas/email
2. Contatos criados → campos customizados + tags atribuídas → segmentos atualizados
3. Contatos ficam disponíveis para Pipeline, Conversations e Campanhas

## Checklist de Implementação
- [ ] CRUD com validações (email único/empresa, E.164 phone)
- [ ] Endereços múltiplos por contato
- [ ] Campos customizados (50/empresa)
- [ ] Tags (5/contato) com validação
- [ ] Importação CSV (10k batch) com dedup
- [ ] Segmentos dinâmicos
- [ ] Soft delete 90 dias
- [ ] Filtros e paginação

## Checklist de Testes
- [ ] Email único por empresa (violação retorna erro)
- [ ] Phone E.164 validado
- [ ] Importação deduplica corretamente
- [ ] Tags limitadas a 5/contato
- [ ] Segmentos filtram dinamicamente

## Documentação Oficial Relacionada
- `docs/contact/CONTACT-MANAGEMENT.md`
- `docs/contact/IMPORT-GUIDE.md`
- `docs/contact/SEGMENTS.md`

## Histórico de Revisão
| Data | Autor | Descrição |
|------|-------|-----------|
| 2026-07-15 | System | Criação do contexto |
