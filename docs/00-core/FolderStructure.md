# FolderStructure — Estrutura de Pastas do Projeto

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Estrutura Geral](#estrutura-geral)
- [Backend — Java/Spring Boot](#backend---java-spring-boot)
- [Frontend — Next.js](#frontend---nextjs)
- [Docs](#docs)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Definir a estrutura de pastas e arquivos do projeto para garantir organização, escalabilidade e facilidade de navegação.

## Descrição

A estrutura é baseada em Clean Architecture (backend) e feature-based organization (frontend). Cada pasta tem uma responsabilidade clara e os limites são bem definidos.

## Estrutura Geral

```
crm-omnichannel/
├── docs/                          # Toda a documentação do projeto
│   ├── 00-core/
│   ├── 01-backend/
│   ├── 02-frontend/
│   ├── 03-database/
│   ├── 04-integrations/
│   ├── 05-business-rules/
│   ├── 06-devops/
│   └── 07-roadmap/
│
├── backend/                       # API Backend (Java/Spring Boot)
│   ├── src/
│   ├── Dockerfile
│   ├── pom.xml
│   └── ...
│
├── frontend/                      # Frontend (Next.js)
│   ├── src/
│   ├── Dockerfile
│   ├── package.json
│   └── ...
│
├── docker/                        # Configurações Docker
│   ├── docker-compose.yml
│   ├── docker-compose.dev.yml
│   └── docker-compose.prod.yml
│
├── scripts/                       # Scripts auxiliares
│   ├── setup.sh
│   ├── seed.sh
│   └── deploy.sh
│
├── .github/                       # GitHub Actions
│   └── workflows/
│
├── .gitignore
├── README.md
└── LICENSE
```

## Backend — Java/Spring Boot

### Estrutura Clean Architecture

```
backend/src/main/java/com/becommerce/crm/
│
├── domain/                        # Camada de Domínio (CORE)
│   ├── shared/                    # Compartilhado entre todos os módulos
│   │   ├── valueobject/           # Objetos de valor (Email, Phone, Money)
│   │   ├── event/                 # Eventos de domínio base
│   │   └── exception/             # Exceções base
│   │
│   ├── identity/                  # Bounded Context: Identity
│   │   ├── model/                 # Entidades e Value Objects
│   │   ├── event/                 # Eventos de domínio
│   │   ├── repository/           # Interfaces de repositório (portas)
│   │   └── exception/             # Exceções do contexto
│   │
│   ├── company/                   # Bounded Context: Company
│   │   ├── model/
│   │   ├── event/
│   │   ├── repository/
│   │   └── exception/
│   │
│   ├── contact/                   # Bounded Context: Contact
│   │   ├── model/
│   │   ├── event/
│   │   ├── repository/
│   │   └── exception/
│   │
│   ├── pipeline/                  # Bounded Context: Pipeline
│   │   ├── model/
│   │   ├── event/
│   │   ├── repository/
│   │   └── exception/
│   │
│   ├── communication/             # Bounded Context: Communication
│   │   ├── model/
│   │   ├── event/
│   │   ├── repository/
│   │   └── exception/
│   │
│   ├── campaign/                  # Bounded Context: Campaign
│   │   ├── model/
│   │   ├── event/
│   │   ├── repository/
│   │   └── exception/
│   │
│   └── analytics/                 # Bounded Context: Analytics
│       ├── model/
│       ├── repository/
│       └── exception/
│
├── application/                   # Camada de Aplicação
│   ├── shared/                    # Utilitários compartilhados
│   │   ├── dto/                   # DTOs compartilhados
│   │   ├── mapper/                # Mappers compartilhados
│   │   └── service/               # Services compartilhados
│   │
│   ├── identity/
│   │   ├── command/               # Commands (CUD operations)
│   │   ├── query/                 # Queries (R operations)
│   │   ├── service/               # Application Services
│   │   ├── dto/                   # DTOs deste contexto
│   │   └── mapper/                # Mappers deste contexto
│   │
│   ├── company/
│   │   ├── command/
│   │   ├── query/
│   │   ├── service/
│   │   ├── dto/
│   │   └── mapper/
│   │
│   ├── contact/
│   │   ├── command/
│   │   ├── query/
│   │   ├── service/
│   │   ├── dto/
│   │   └── mapper/
│   │
│   ├── pipeline/
│   │   ├── command/
│   │   ├── query/
│   │   ├── service/
│   │   ├── dto/
│   │   └── mapper/
│   │
│   ├── communication/
│   │   ├── command/
│   │   ├── query/
│   │   ├── service/
│   │   ├── dto/
│   │   └── mapper/
│   │
│   ├── campaign/
│   │   ├── command/
│   │   ├── query/
│   │   ├── service/
│   │   ├── dto/
│   │   └── mapper/
│   │
│   └── analytics/
│       ├── query/
│       ├── service/
│       ├── dto/
│       └── mapper/
│
├── infrastructure/                # Camada de Infraestrutura
│   ├── persistence/               # Implementações de persistência
│   │   ├── repository/           # Implementações dos repositórios
│   │   ├── entity/                # JPA/Hibernate entities
│   │   ├── mapper/                # Mappers DB ↔ Domain
│   │   └── config/               # Configurações de datasource
│   │
│   ├── cache/                     # Implementações de cache (Redis)
│   │   ├── repository/
│   │   └── config/
│   │
│   ├── messaging/                 # Implementações de messaging (RabbitMQ)
│   │   ├── producer/
│   │   ├── consumer/
│   │   └── config/
│   │
│   ├── security/                  # Segurança (JWT, OAuth)
│   │   ├── filter/
│   │   ├── provider/
│   │   └── config/
│   │
│   ├── integration/               # Integrações externas
│   │   ├── whatsapp/
│   │   ├── openai/
│   │   ├── google/
│   │   └── email/
│   │
│   └── config/                    # Configurações gerais
│       ├── web/
│       ├── async/
│       └── scheduler/
│
├── presentation/                  # Camada de Apresentação
│   ├── rest/                      # REST Controllers
│   │   ├── controller/
│   │   ├── request/               # Request bodies
│   │   ├── response/              # Response bodies
│   │   └── assembler/             # Assemblers (HATEOAS)
│   │
│   └── graphql/                   # GraphQL (futuro)
│       ├── resolver/
│       └── dto/
│
├── shared/                        # Compartilhado entre todas as camadas
│   ├── util/                      # Utilitários gerais
│   ├── constant/                  # Constantes
│   └── exception/                 # Exceções globais
│
└── test/                          # Testes
    ├── unit/
    ├── integration/
    ├── e2e/
    └── fixtures/
```

## Frontend — Next.js

### Estrutura Feature-Based

```
frontend/src/
│
├── app/                           # Next.js App Router
│   ├── (auth)/                    # Grupo de rotas autenticadas
│   │   ├── layout.tsx
│   │   ├── dashboard/
│   │   │   └── page.tsx
│   │   ├── leads/
│   │   │   ├── page.tsx
│   │   │   └── [id]/
│   │   │       └── page.tsx
│   │   ├── contacts/
│   │   ├── pipeline/
│   │   ├── chat/
│   │   ├── campaigns/
│   │   ├── reports/
│   │   └── settings/
│   │
│   ├── (public)/                  # Grupo de rotas públicas
│   │   ├── login/
│   │   ├── register/
│   │   └── forgot-password/
│   │
│   ├── api/                       # API Routes (BFF)
│   │   └── ...
│   │
│   ├── layout.tsx                 # Root layout
│   ├── page.tsx                   # Home/Landing
│   └── not-found.tsx
│
├── components/                    # Componentes React
│   ├── ui/                        # Componentes base (Shadcn)
│   │   ├── button.tsx
│   │   ├── input.tsx
│   │   ├── dialog.tsx
│   │   └── ...
│   │
│   ├── layout/                    # Componentes de layout
│   │   ├── Sidebar.tsx
│   │   ├── Header.tsx
│   │   ├── Footer.tsx
│   │   └── ThemeToggle.tsx
│   │
│   ├── leads/                     # Componentes do módulo Lead
│   │   ├── LeadCard.tsx
│   │   ├── LeadForm.tsx
│   │   ├── LeadList.tsx
│   │   └── LeadDetail.tsx
│   │
│   ├── contacts/                  # Componentes do módulo Contact
│   ├── pipeline/                  # Componentes do módulo Pipeline
│   ├── chat/                      # Componentes do módulo Chat
│   ├── campaigns/                 # Componentes do módulo Campaign
│   ├── reports/                   # Componentes do módulo Reports
│   └── shared/                    # Componentes compartilhados
│       ├── DataTable.tsx
│       ├── SearchInput.tsx
│       ├── StatusBadge.tsx
│       └── LoadingSpinner.tsx
│
├── hooks/                         # Custom React Hooks
│   ├── useAuth.ts
│   ├── useLeads.ts
│   ├── useContacts.ts
│   ├── useDebounce.ts
│   └── usePagination.ts
│
├── lib/                           # Utilitários e configurações
│   ├── api.ts                     # Cliente HTTP configurado
│   ├── auth.ts                    # Utilitários de autenticação
│   ├── utils.ts                   # Funções auxiliares
│   ├── validations.ts             # Schemas de validação (zod)
│   └── constants.ts               # Constantes
│
├── providers/                     # Context Providers
│   ├── AuthProvider.tsx
│   ├── ThemeProvider.tsx
│   └── QueryProvider.tsx
│
├── types/                         # Tipos TypeScript compartilhados
│   ├── api.ts
│   ├── models.ts
│   └── index.ts
│
└── styles/                        # Estilos globais
    └── globals.css
```

## Docs

```
docs/
├── 00-core/                       # Documentação fundamental
├── 01-backend/                    # Documentação backend
├── 02-frontend/                   # Documentação frontend
├── 03-database/                   # Documentação de banco de dados
├── 04-integrations/               # Documentação de integrações
├── 05-business-rules/             # Regras de negócio
├── 06-devops/                     # DevOps e infraestrutura
├── 07-roadmap/                    # Roadmap do produto
└── 08-history/                    # Histórico de mudanças
```

## Responsabilidades

- Manter a estrutura organizada e consistente
- Não criar pastas ou arquivos fora do padrão definido
- Revisar periodicamente a necessidade de novas pastas
- Documentar mudanças na estrutura em Decisions.md

## Dependências

- [NamingConvention.md](./NamingConvention.md) — Nomes de pastas e arquivos
- [Architecture.md](./Architecture.md) — Estrutura baseada em Clean Architecture
- [DesignPatterns.md](./DesignPatterns.md) — Organização baseada em padrões

## Regras

- Nenhum arquivo pode ser criado fora da estrutura definida
- Novas pastas requerem aprovação do Arquiteto Principal
- Arquivos temporários devem ir para `/tmp` ou pasta `.cache`
- Cada pasta deve ter seu README.md
- Arquivos de configuração vão na raiz da pasta correspondente

## Futuras Melhorias

- Adicionar pasta `mobile/` quando houver app nativo
- Adicionar pasta `shared/` no raiz para código compartilhado entre frontend e backend
- Considerar monorepo com Nx ou Turborepo quando houver múltiplos pacotes
- Adicionar pasta `infra/` para Terraform/Pulumi

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial da estrutura |
