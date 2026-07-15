# CodingStandards — Padrões de Codificação

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Padrões Gerais](#padrões-gerais)
- [Java / Backend](#java--backend)
- [TypeScript / Frontend](#typescript--frontend)
- [SQL / Database](#sql--database)
- [Git](#git)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Estabelecer padrões de codificação obrigatórios para garantir consistência, legibilidade e manutenibilidade do código-fonte em toda a equipe.

## Descrição

Todos os desenvolvedores devem seguir estes padrões independentemente de preferências pessoais. O código é escrito uma vez, mas lido centenas de vezes. Consistência reduz o custo de manutenção.

## Padrões Gerais

### Princípios Fundamentais

1. **SOLID** — Cinco princípios de design orientado a objeto
2. **KISS** — Keep It Simple, Stupid
3. **DRY** — Don't Repeat Yourself
4. **YAGNI** — You Aren't Gonna Need It
5. **Boy Scout Rule** — Deixe o código mais limpo do que encontrou

### Formatação

| Regra | Backend (Java) | Frontend (TypeScript) |
|---|---|---|
| Indentação | 4 espaços | 2 espaços |
| Max line length | 120 caracteres | 100 caracteres |
| Imports | Ordem alfabética | Ordem alfabética |
| Blank lines | 1 entre métodos | 1 entre funções |
| Trailing commas | N/A | Permitidas |

### Comentários

- **NÃO** adicionar comentários óbvios (`// incrementa contador`)
- **SIM** adicionar comentários de intenção (`// Aguarda 3s para rate limit do WhatsApp`)
- **SIM** adicionar TODO com ticket number (`// TODO(CRM-123): Remover após migração`)
- **SIM** adicionar javadoc/JSDoc em interfaces públicas

### Funções e Métodos

- Máximo 30 linhas por função
- Máximo 4 parâmetros (usar objetos para mais)
- Uma função deve fazer apenas uma coisa (Single Responsibility)
- Nomes devem descrever o que fazem, não como fazem

### Variáveis

- Nomes devem ser autoexplicativos
- NÃO usar abreviações (`usr` → `user`, `msg` → `message`)
- Constantes em UPPER_SNAKE_CASE
- Booleanos com prefixo `is`, `has`, `can`, `should`

## Java / Backend

### Padrão de Classes

```
NomeDaClasse          → PascalCase
metodoOuVariavel      → camelCase
CONSTANTE             → UPPER_SNAKE_CASE
pacote                → lowercase (com.becommerce.crm.lead)
```

### Estrutura de uma Classe

1. Atributos estáticos/finais
2. Atributos de instância
3. Construtor
4. Métodos públicos
5. Métodos privados
6. Métodos protegidos

### Regras Específicas Java

- Usar `record` para DTOs e Value Objects quando possível
- Usar `sealed` para hierarquias controladas
- Usar `var` quando o tipo é óbvio no contexto
- Preferir `Optional` a retornar `null`
- Usar `Stream API` para operações em coleções
- Nunca usar `@SuppressWarnings` sem justificativa

### Naming - Spring Boot

| Elemento | Padrão | Exemplo |
|---|---|---|
| Entity | Singular, PascalCase | `Lead`, `Contact`, `Company` |
| Repository | Entity + Repository | `LeadRepository` |
| Service | Entity + Service | `LeadService` |
| Controller | Entity + Controller | `LeadController` |
| DTO (Input) | Create/Update + Entity + Command | `CreateLeadCommand` |
| DTO (Output) | Entity + Response | `LeadResponse` |
| Mapper | Entity + Mapper | `LeadMapper` |
| Exception | Entity + Exception | `LeadNotFoundException` |

## TypeScript / Frontend

### Padrão de Arquivos

| Tipo | Padrão | Exemplo |
|---|---|---|
| Componente | PascalCase | `LeadCard.tsx` |
| Hook | camelCase com prefixo `use` | `useLeads.ts` |
| Util | camelCase | `formatCurrency.ts` |
| Tipo/Interface | PascalCase | `LeadProps`, `LeadData` |
| Context | PascalCase + Context | `AuthContext.tsx` |
| Estilo | camelCase | `theme.ts` |

### Regras Específicas TypeScript

- Sempre usar `type` ao invés de `interface` para novos tipos
- Usar `zod` para validação de schemas
- Nunca usar `any` — usar `unknown` e fazer type narrowing
- Preferir `const` functions (arrow functions) para componentes
- Usar destructuring para props
- Exportar tipos junto com componentes

### Naming - React/Next.js

| Elemento | Padrão | Exemplo |
|---|---|---|
| Componente | PascalCase | `LeadCard.tsx` |
| Página | page.tsx (Next.js) | `app/leads/page.tsx` |
| Layout | layout.tsx | `app/layout.tsx` |
| Hook | use + PascalCase | `useLeads.ts` |
| Context | PascalCase + Provider | `AuthProvider.tsx` |
| API Route | route.ts | `app/api/leads/route.ts` |

## SQL / Database

### Padrões de Nomes

| Elemento | Padrão | Exemplo |
|---|---|---|
| Tabela | snake_case, plural | `lead_contacts`, `companies` |
| Coluna | snake_case | `created_at`, `first_name` |
| Índice | idx_{table}_{columns} | `idx_leads_company_id` |
| FK | fk_{table}_{ref} | `fk_contacts_company_id` |
| Unique | uk_{table}_{columns} | `uk_users_email` |

### Regras SQL

- Sempre usar `UUID` como primary key
- Sempre incluir `created_at` e `updated_at`
- Sempre usar `soft delete` (coluna `deleted_at`)
- Usar `NOT NULL` sempre que possível
- Nunca usar `SELECT *` em queries de produção

## Git

### Branch Naming

```
feature/CRM-123-add-lead-module
bugfix/CRM-456-fix-message-delivery
hotfix/CRM-789-security-patch
release/v1.2.0
```

### Commit Messages

```
feat(lead): add lead creation endpoint
fix(message): resolve duplicate message issue
docs(api): update OpenAPI specification
refactor(contact): extract contact validation logic
test(lead): add unit tests for lead qualification
```

### Pull Requests

- Título claro e descritivo
- Link para o ticket/tarefa
- Descrição do que foi feito e por quê
- Screenshots/videos para mudanças visuais
- Testes incluídos e passando
- Code review obrigatório (mínimo 1 aprovador)

## Responsabilidades

- Todo desenvolvedor é responsável por seguir estes padrões
- Tech leads são responsáveis por verificar na code review
- O Arquiteto Principal mantém e atualiza este documento

## Dependências

- [Constitution.md](./Constitution.md) — Princípios fundamentais
- [NamingConvention.md](./NamingConvention.md) — Convenções de nomenclatura detalhadas
- [DesignPatterns.md](./DesignPatterns.md) — Padrões de design aplicados
- [FolderStructure.md](./FolderStructure.md) — Estrutura de pastas

## Regras

- Pull requests que violem estes padrões devem ser rejeitados
- Ferramentas de linting devem ser configuradas para automatizar verificação
- Exceções devem ser documentadas no PR
- Estes padrões são revisados trimestralmente

## Futuras Melhorias

- Configurar SonarQube para verificação automática de qualidade
- Adicionar regras de performance (memory leaks, N+1 queries)
- Configurar pre-commit hooks para formatação automática
- Adicionar guia de acessibilidade (WCAG)

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial dos padrões de codificação |
