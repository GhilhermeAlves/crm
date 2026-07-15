# Decisions — Registro de Decisões Arquiteturais (ADR)

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Formato do Cada ADR](#formato-do-cada-adr)
- [Decisões Registradas](#decisões-registradas)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Manter um registro cronológico de todas as decisões arquiteturais significativas tomadas ao longo do projeto, incluindo contexto, opções avaliadas e justificativa da escolha.

## Descrição

Architecture Decision Records (ADRs) são documentos que capturam decisões importantes do projeto. Cada decisão, uma vez tomada e registrada, serve como referência permanente para a equipe e novos membros.

## Formato do Cada ADR

### Estrutura

```
# ADR-{NNN}: {Título da Decisão}

## Status

{Proposta | Aceita | Rejeitada | Descontinuada | Suplantada por ADR-XXX}

## Contexto

{Qual problema ou situação motivou esta decisão?}

## Decisão

{O que foi decidido?}

## Opções Avaliadas

### Opção A: {Nome}

- **Prós**: {lista}
- **Contras**: {lista}

### Opção B: {Nome}

- **Prós**: {lista}
- **Contras**: {lista}

### Opção C: {Nome}

- **Prós**: {lista}
- **Contras**: {lista}

## Justificativa

{Por que esta opção foi escolhida?}

## Consequências

### Positivas

- {consequência 1}
- {consequência 2}

### Negativas

- {consequência 1}
- {consequência 2}

## Impacto

- **Alcance**: {Quais áreas do projeto são afetadas?}
- **Risco**: {Baixo | Médio | Alto}
- **Custo**: {Estimativa de esforço para implementação}

## Referências

- {Links para docs, artigos, issues}

## Histórico

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0 | {data} | {autor} | Decisão inicial |
```

## Decisões Registradas

### ADR-001: Arquitetura Hexagonal com Clean Architecture

**Status**: Aceita

**Contexto**: O projeto precisa de uma arquitetura que suporte longevidade (10+ anos), escalabilidade e manutenção facilitada.

**Decisão**: Adotar Hexagonal Architecture como estrutura base, combinada com Clean Architecture para organização de camadas.

**Opções Avaliadas**:

| Opção | Prós | Contras |
|---|---|---|
| MVC Clássico | Simples, bem conhecido | Acoplamento alto, difícil de escalar |
| Hexagonal + Clean | Flexível, testável, desacoplado | Mais camadas, curva de aprendizado |
| Onion Architecture | Similar ao Hexagonal | Menos comunidade, menos ferramentas |

**Justificativa**: Hexagonal permite trocar facilmente a UI, database e APIs externas sem afetar o domínio. Clean adiciona organização clara de camadas.

**Consequências**:
- Positiva: Testes de domínio sem dependências externas
- Positiva: Facilidade para adicionar novos adaptadores
- Negativa: Mais boilerplate no início
- Negativa: Curva de aprendizado para devs acostumados com MVC

**Impacto**: Todo o backend. Risco: Médio. Custo: Médio.

---

### ADR-002: Multi-Tenancy via Separate Schema

**Status**: Aceita

**Contexto**: O sistema precisa suportar múltiplos clientes (empresas) com isolamento de dados.

**Decisão**: Usar PostgreSQL schemas separados por tenant, com row-level security como camada adicional.

**Opções Avaliadas**:

| Opção | Prós | Contras |
|---|---|---|
| Shared Schema (RLS) | Custo baixo, simples | Risco de vazamento, performance |
| Separate Schema | Bom isolamento, RLS compatível | Complexidade de migrations |
| Separate Database | Isolamento total | Custo alto, hard de manter |

**Justificativa**: Separate Schema oferece o melhor equilíbrio entre isolamento, custo e escalabilidade.

**Consequências**:
- Positiva: Isolamento efetivo entre tenants
- Positiva: Compatível com RLS do PostgreSQL
- Negativa: Migrations devem ser aplicadas em todos os schemas
- Negativa: Número de schemas pode impactar performance

**Impacto**: Database, backend. Risco: Médio. Custo: Médio.

---

### ADR-003: Event-Driven via RabbitMQ

**Status**: Aceita

**Contexto**: O sistema precisa de desacoplamento entre bounded contexts e processamento assíncrono.

**Decisão**: Usar RabbitMQ como message broker para eventos de integração e commands assíncronos.

**Opções Avaliadas**:

| Opção | Prós | Contras |
|---|---|---|
| Kafka | Throughput alto, event sourcing nativo | Complexo para uso simples |
| RabbitMQ | Simples, AMQP padrão, retry nativo | Throughput menor que Kafka |
| AWS SQS/SNS | Managed, sem infra | Vendor lock-in |

**Justificativa**: RabbitMQ é mais simples de operar, suporta AMQP padrão, e tem recursos de retry, dead letter queues e routing flexível.

**Consequências**:
- Positiva: Desacoplamento entre contextos
- Positiva: Retry e dead letter queues nativas
- Negativa: Throughput limitado vs Kafka
- Negativa: Não suporta event streaming nativo

**Impacto**: Infrastructure, todos os bounded contexts. Risco: Baixo. Custo: Baixo.

---

### ADR-004: Database Migrations com Flyway

**Status**: Aceita

**Contexto**: O schema do banco precisa ser versionado e reproducível em todos os ambientes.

**Decisão**: Usar Flyway para gerenciar migrations do PostgreSQL.

**Opções Avaliadas**:

| Opção | Prós | Contras |
|---|---|---|
| Flyway | Simples, Java nativo, rollback | Menos features que Liquibase |
| Liquibase | Mais features, XML/YAML | Mais complexo, mais verbose |
| Manual | Controle total | Error-prone, não auditável |

**Justificativa**: Flyway é mais simples, nativo para Java/Spring Boot, e suficiente para as necessidades do projeto.

**Consequências**:
- Positiva: Migrations versionadas e auditáveis
- Positiva: Integração nativa com Spring Boot
- Negativa: Rollback manual ( Flyway não suporta down migrations nativamente)

**Impacto**: Database, CI/CD. Risco: Baixo. Custo: Baixo.

---

### ADR-005: Frontend com Next.js App Router

**Status**: Aceita

**Contexto**: O frontend precisa ser performático, SEO-friendly e ter uma boa experiência de desenvolvimento.

**Decisão**: Usar Next.js 14 com App Router, React Server Components e Shadcn UI.

**Opções Avaliadas**:

| Opção | Prós | Contras |
|---|---|---|
| Next.js Pages Router | Maduro, bem documentado | Menos features modernas |
| Next.js App Router | RSC, layouts aninhados, streaming | Mais novo, menos resources |
| Vite + React SPA | Simples, rápido | Sem SSR, sem SEO |
| Remix | Convenções fortes | Menor ecossistema |

**Justificativa**: App Router é o futuro do Next.js, suporta Server Components para performance, e Shadcn UI oferece componentes acessíveis e customizáveis.

**Consequências**:
- Positiva: Performance com Server Components
- Positiva: Layouts aninhados nativos
- Negativa: Curva de aprendizado para devs acostumados com Pages Router
- Negativa: Alguns packages ainda não suportam App Router

**Impacto**: Todo o frontend. Risco: Médio. Custo: Médio.

---

### ADR-006: UUID como Primary Key

**Status**: Aceita

**Contexto**: O sistema precisa de identificadores únicos globais que funcionem em ambientes distribuídos.

**Decisão**: Usar UUID v4 como primary key em todas as tabelas.

**Opções Avaliadas**:

| Opção | Prós | Contras |
|---|---|---|
| Auto-increment | Simples, index-friendly | Não distribuível, expõe dados |
| UUID v4 | Globalmente único, distribuível | Maior tamanho, menor indexação |
| ULID | Ordenável, mais compacto | Menos suporte nativo |
| NanoID | Curto, legível | Menos padrão |

**Justificativa**: UUID v4 é o padrão da indústria para sistemas distribuídos, suportado nativamente por PostgreSQL e Java.

**Consequências**:
- Positiva: Geração distribuída de IDs
- Positiva: Segurança (não expõe sequência)
- Negativa: Indexação levemente menos eficiente
- Negativa: Ocupa mais espaço em disco

**Impacto**: Database, backend. Risco: Baixo. Custo: Baixo.

---

### ADR-007: Autenticação via JWT com Refresh Token

**Status**: Aceita

**Contexto**: O sistema precisa de autenticação stateless e segura.

**Decisão**: Usar JWT Access Token (curta duração) + Refresh Token (longa duração) com rotação.

**Opções Avaliadas**:

| Opção | Prós | Contras |
|---|---|---|
| JWT sem refresh | Simples, stateless | Re-login frequente |
| JWT com refresh | Stateless + longa duração | Mais complexo |
| Session-based | Simples, revogável | Stateful, não escala bem |
| OAuth 2.0 + OIDC | Padrão, federation | Muito complexo para início |

**Justificativa**: JWT com refresh token é o padrão de mercado, oferece boa segurança e experiência de usuário.

**Consequências**:
- Positiva: Autenticação stateless
- Positiva: Boa experiência de usuário (não precisa re-logar)
- Negativa: Complexidade de rotação de tokens
- Negativa: Difícil revogar tokens antes do expiry

**Impacto**: Security, auth flow. Risco: Médio. Custo: Médio.

---

## Responsabilidades

- Todo membro da equipe pode propor um ADR
- ADRs são discutidos em reuniões de arquitetura
- O Arquiteto Principal tem autoridade final para aceitar/rejeitar
- ADRs aceitos são implementados pela equipe de desenvolvimento

## Dependências

- [Architecture.md](./Architecture.md) — Arquitetura que sustenta as decisões
- [Constitution.md](./Constitution.md) — Princípios que limitam as opções
- [TechStack.md](./TechStack.md) — Tecnologias que são resultado das decisões

## Regras

- Um ADR por decisão significativa
- ADRs nunca são deletados, apenas suplantados
- Status deve ser atualizado quando a decisão muda
- Revisar ADRs antigos quando o contexto muda significativamente
- ADRs devem ser escritos em linguagem clara e acessível

## Futuras Melhorias

- Automatizar criação de ADRs via templates
- Integrar ADRs com o processo de code review
- Criar dashboard com status de todos os ADRs
- Adicionar ADRs para decisões de produto (não apenas técnicas)

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial com 7 ADRs |
