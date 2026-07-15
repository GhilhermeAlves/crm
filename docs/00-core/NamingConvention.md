# NamingConvention — Convenções de Nomenclatura

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Convenções Gerais](#convenções-gerais)
- [Java / Backend](#java--backend)
- [TypeScript / Frontend](#typescript--frontend)
- [SQL / Database](#sql--database)
- [Docker / DevOps](#docker--devops)
- [API / HTTP](#api--http)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Definir convenções de nomenclatura unificadas para todos os elementos do projeto, garantindo consistência e legibilidade.

## Descrição

Convenções de nomenclatura são a base da comunicação entre desenvolvedores. Um nome bem escolhado elimina a necessidade de comentários e reduz significativamente o tempo de entendimento de código.

## Convenções Gerais

### Cases

| Case | Uso | Exemplo |
|---|---|---|
| `PascalCase` | Classes, tipos, interfaces | `LeadService`, `CreateLeadCommand` |
| `camelCase` | Variáveis, funções, métodos | `leadName`, `getLeadById()` |
| `snake_case` | Tabelas, colunas, constantes DB | `lead_contacts`, `created_at` |
| `UPPER_SNAKE_CASE` | Constantes globais | `MAX_RETRY_COUNT`, `API_VERSION` |
| `kebab-case` | URLs, arquivos, componentes CSS | `/api/v1/leads`, `lead-card.tsx` |

### Regras Universalmente

- **Sempre usar inglês** para nomes de código e documentação técnica
- **Sempre usar português** para textos visíveis ao usuário (UI labels)
- **Nunca usar abreviações** exceto as universalmente aceitas (`id`, `url`, `http`, `api`)
- **Nomes devem ser autoexplicativos** — se precisa de comentário, o nome está ruim

## Java / Backend

### Pacotes

```
com.becommerce.crm.{bounded_context}.{layer}.{sub_layer}

Exemplo:
com.becommerce.crm.lead.model.Lead
com.becommerce.crm.lead.repository.LeadRepository
com.becommerce.crm.lead.service.LeadService
com.becommerce.crm.lead.controller.LeadController
```

### Classes

| Tipo | Padrão | Exemplo |
|---|---|---|
| Entity | Singular, PascalCase | `Lead`, `Contact`, `Company` |
| Value Object | Singular, PascalCase | `Email`, `Phone`, `Money` |
| Aggregate Root | Singular, PascalCase | `Lead`, `Campaign` |
| Repository Interface | Entity + Repository | `LeadRepository` |
| Repository Impl | Entity + Jpa/RedisRepository | `LeadJpaRepository` |
| Service | Entity + Service | `LeadService` |
| Controller | Entity + Controller | `LeadController` |
| DTO (Input) | Action + Entity + Command/Request | `CreateLeadCommand`, `UpdateLeadRequest` |
| DTO (Output) | Entity + Response/DTO | `LeadResponse`, `LeadSummaryDto` |
| Mapper | Entity + Mapper | `LeadMapper` |
| Exception | Entity + Exception | `LeadNotFoundException` |
| Event | Entity + Past Tense Verb + Event | `LeadCreatedEvent` |
| Config | Feature + Config | `SecurityConfig`, `RedisConfig` |
| Filter | Feature + Filter | `JwtAuthenticationFilter` |
| Listener | Event + Listener | `LeadCreatedListener` |

### Métodos

| Ação | Padrão | Exemplo |
|---|---|---|
| Buscar um | `findBy{What}` | `findByEmail()`, `findById()` |
| Buscar todos | `findAllBy{What}` | `findAllByCompanyId()` |
| Criar | `create{Entity}` | `createLead()` |
| Atualizar | `update{Entity}` | `updateLead()` |
| Deletar | `delete{Entity}` / `softDelete{Entity}` | `deleteLead()` |
| Validar | `validate{What}` | `validateEmail()` |
| Converter | `to{Target}` / `from{Source}` | `toResponse()`, `fromEntity()` |
| Verificar existência | `exists{What}` | `existsByEmail()` |
| Contar | `count{What}` | `countByCompanyId()` |

### Variáveis

| Contexto | Padrão | Exemplo |
|---|---|---|
| Boolean | `is/has/can/should` prefix | `isActive`, `hasPermission` |
| Collection | Plural | `leads`, `contacts` |
| Single | Singular | `lead`, `contact` |
| Constante | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE` |
| Logger | `log` (static final) | `private static final Logger log` |

## TypeScript / Frontend

### Arquivos

| Tipo | Padrão | Exemplo |
|---|---|---|
| Componente | PascalCase.tsx | `LeadCard.tsx` |
| Página | page.tsx (Next.js) | `page.tsx` |
| Layout | layout.tsx (Next.js) | `layout.tsx` |
| Hook | use + PascalCase.ts | `useLeads.ts` |
| Context | PascalCase + Context.tsx | `AuthContext.tsx` |
| Util | camelCase.ts | `formatCurrency.ts` |
| Tipo/Interface | PascalCase.ts | `LeadData.ts` |
| Estilo | camelCase.css/ts | `theme.ts` |
| Teste | NomeArquivo.test.ts | `LeadCard.test.ts` |
| Estória | NomeArquivo.stories.tsx | `LeadCard.stories.tsx` |

### Componentes

```tsx
// Componente: LeadCard.tsx
// Padrão: Function Component com arrow function

interface LeadCardProps {
  lead: LeadData;
  onSelect: (id: string) => void;
  className?: string;
}

export function LeadCard({ lead, onSelect, className }: LeadCardProps) {
  // Hooks primeiro
  // Lógica depois
  // Return com JSX
}
```

### Hooks

```typescript
// Hook: useLeads.ts
// Padrão: retorna objeto com data, loading, error e methods

export function useLeads() {
  const [leads, setLeads] = useState<Lead[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  return { leads, loading, error, refetch };
}
```

### Funções utilitárias

```
formatXxx()     → Formata dados (formatDate, formatCurrency)
validateXxx()   → Valida dados (validateEmail, validateCpf)
getXxx()        → Obtém dados (getUserFromToken)
buildXxx()      → Constrói objetos (buildQueryString)
```

## SQL / Database

### Tabelas

```
nome_no_plural, snake_case, sem artigos

✅ leads
✅ contact_addresses
✅ campaign_templates
❌ lead
❌ the_contacts
❌ tbl_leads
```

### Colunas

```
padrão snake_case com significado claro

✅ created_at
✅ first_name
✅ phone_number
✅ is_active
❌ created
❌ fname
❌ phone
❌ active
```

### Índices

```
idx_{tabela}_{coluna(s)}

✅ idx_leads_company_id
✅ idx_contacts_email
✅ uk_users_email
❌ leads_index
❌ index_on_leads
```

### Foreign Keys

```
fk_{tabela_origem}_{tabela_referenciada}

✅ fk_contacts_company_id
✅ fk_leads_owner_id
❌ fk1
❌ contacts_company_fk
```

## Docker / DevOps

### Containers

```
{projeto}-{serviço}

✅ crm-backend
✅ crm-frontend
✅ crm-postgres
✅ crm-redis
❌ backend
❌ app
❌ postgres
```

### Imagens Docker

```
{registry}/{projeto}-{serviço}:{versão}

✅ ghcr.io/becommerce/crm-backend:1.2.0
✅ ghcr.io/becommerce/crm-backend:latest
❌ backend:latest
❌ becommerce/backend:v1
```

### Variáveis de Ambiente

```
{SERVICE}_{PROPERTY}

✅ CRM_DATABASE_URL
✅ CRM_REDIS_HOST
✅ CRM_JWT_SECRET
❌ databaseUrl
❌ DATABASE_URL (sem prefixo do serviço)
```

## API / HTTP

### Endpoints REST

```
Recursos no plural, ações via HTTP method

GET    /api/v1/leads           → Listar leads
POST   /api/v1/leads           → Criar lead
GET    /api/v1/leads/{id}      → Buscar lead por ID
PUT    /api/v1/leads/{id}      → Atualizar lead
DELETE /api/v1/leads/{id}      → Deletar lead

GET    /api/v1/leads/{id}/contacts  → Contatos do lead
```

### Query Parameters

```
camelCase para parâmetros de busca

✅ ?firstName=João
✅ ?pageSize=20
✅ ?sortDirection=DESC
❌ ?first_name=João
❌ ?page_size=20
```

### Headers

```
X-{Custom-Header} para headers customizados
Authorization para autenticação

✅ X-Request-Id
✅ X-Tenant-Id
✅ Authorization: Bearer {token}
✅ Content-Type: application/json
❌ custom-header
❌ authorization
```

## Responsabilidades

- Todo desenvolvedor deve seguir estas convenções
- Tech leads verificam na code review
- O Arquiteto Principal mantém e atualiza este documento

## Dependências

- [CodingStandards.md](./CodingStandards.md) — Padrões gerais de codificação
- [FolderStructure.md](./FolderStructure.md) — Estrutura de pastas
- [Architecture.md](./Architecture.md) — Arquitetura que define as camadas

## Regras

- Nomes violados em PRs devem ser corrigidos antes do merge
- Ferramentas de linting devem automatizar a verificação
- Exceções devem ser documentadas
- Revisão semestral das convenções

## Futuras Melhorias

- Configurar ESLint rules customizadas para naming conventions
- Criar snippet templates para IDE com nomes pré-definidos
- Adicionar convenções para GraphQL (quando implementado)
- Adicionar convenções para Event Sourcing (quando implementado)

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial das convenções |
