# DesignPatterns — Padrões de Design

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Padrões de Apresentação](#padrões-de-apresentação)
- [Padrões de Aplicação](#padrões-de-aplicação)
- [Padrões de Domínio](#padrões-de-domínio)
- [Padrões de Infraestrutura](#padrões-de-infraestrutura)
- [Padrões de Concorrência](#padrões-de-concorrência)
- [Anti-Patterns a Evitar](#anti-patterns-a-evitar)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar todos os padrões de design utilizados no projeto, incluindo quando e como cada padrão deve ser aplicado.

## Descrição

Padrões de design são soluções reutilizáveis para problemas recorrentes de design. O uso correto de padrões facilita a manutenção, comunicação entre desenvolvedores e escalabilidade do sistema.

## Padrões de Apresentação

### DTO Pattern (Data Transfer Object)

**Quando usar**: Transferir dados entre camadas, expor dados via API.

**Benefícios**: Desacoplamento entre camadas, controle do que é exposto, validação independente.

```
Responsabilidade:
- Representar dados de entrada/saída da API
- Conter validações de entrada (@Valid)
- NUNCA conter lógica de negócio
```

### Mapper Pattern

**Quando usar**: Converter entre entidades de domínio e DTOs, entre diferentes representações.

**Benefícios**: Separação de responsabilidades, facilita testes, evita poluição de entidades.

```
Responsabilidade:
- Converter Domain Entity → Response DTO
- Converter Request DTO → Domain Entity
- Converter Domain Entity → Database Entity
- Converter Database Entity → Domain Entity
```

### Assembler Pattern (HATEOAS)

**Quando usar**: Adicionar links HATEOAS em responses REST.

**Benefícios**: API discoverable, contratos claros entre frontend e backend.

### BFF Pattern (Backend for Frontend)

**Quando usar**: Rotas de API no Next.js que compilam dados de múltiplos endpoints backend.

**Benefícios**: Reduz round-trips, melhora performance do frontend, abstrai complexidade do backend.

## Padrões de Aplicação

### Command Pattern

**Quando usar**: Encapsular operações de criação/atualização como objetos.

**Benefícios**: Validação centralizada, audit trail, undo/redo facilitado.

```
Responsabilidade:
- Encapsular dados de entrada para operações CUD
- Conter regras de validação básicas
- Ser imutável após criação
- Prefixo: Create, Update, Delete + Entity + Command
```

### CQRS (Command Query Responsibility Segregation)

**Quando usar**: Separar operações de leitura e escrita em contextos com alta demanda de leitura.

**Benefícios**: Otimização independente de reads e writes, escalabilidade assimétrica.

```
Responsabilidade:
- Commands: Operações de criação, atualização, exclusão
- Queries: Operações de leitura otimizadas
- Read Models: Projetções otimizadas para consultas
```

### Mediator Pattern

**Quando usar**: Desacoplar handlers de commands/queries do controller.

**Benefícios**: Controllers finos, handlers independentes, testabilidade isolada.

### Facade Pattern

**Quando usar**: Simplificar acesso a subsistemas complexos.

**Benefícios**: Interface simples para operações complexas, encapsulamento de complexidade.

### Chain of Responsibility

**Quando usar**: Pipelines de validação e processamento.

**Benefícios**: Flexibilidade, reutilização, separação de concerns.

## Padrões de Domínio

### Repository Pattern

**Quando usar**: Abstrair acesso a dados do domínio.

**Benefícios**: Troca de implementação sem afetar o domínio, testabilidade com mocks.

```
Responsabilidade:
- Definir interface para persistência de aggregates
- Retornar entidades de domínio (não database entities)
- Ser implementado na camada de infraestrutura
- Um aggregate root = um repository
```

### Unit of Work Pattern

**Quando usar**: Gerenciar transações e mudanças pendentes.

**Benefícios**: Consistência transacional, batch de operações, flush controlado.

### Domain Event Pattern

**Quando usar**: Comunicar mudanças de estado dentro e entre bounded contexts.

**Benefícios**: Desacoplamento, audit trail, extensibilidade.

```
Responsabilidade:
- Representar algo que aconteceu (passado)
- Ser imutável
- Conter dados mínimos necessários
- Ser publicado via message broker
- Ser idempotente
```

### Value Object Pattern

**Quando usar**: Representar conceitos do domínio sem identidade.

**Benefícios**: Imutabilidade, auto-validação, eliminação de primitivos obscessivos.

```
Exemplos:
- Email (valida formato)
- Phone (valida formato)
- Money (valida valor + moeda)
- Address (valida campos obrigatórios)
```

### Aggregate Pattern

**Quando usar**: Definir limites de consistência transacional.

**Benefícios**: Consistency boundary, transação garantida dentro do aggregate.

### Factory Pattern

**Quando usar**: Criar entidades com regras complexas de construção.

**Benefícios**: Construção consistente, validação na criação, código centralizado.

### Specification Pattern

**Quando usar**: Compor regras de negócio complexas e reutilizáveis.

**Benefícios**: Regras combináveis, testáveis isoladamente, legíveis.

## Padrões de Infraestrutura

### Circuit Breaker Pattern

**Quando usar**: Proteger contra falhas em serviços externos.

**Benefícios**: Resiliência, graceful degradation, prevenção de cascata de falhas.

### Retry Pattern

**Quando usar**: Tentar novamente operações que falharam temporariamente.

**Benefícios**: Resiliência, tolerância a falhas transitórias.

### Bulkhead Pattern

**Quando usar**: Isolar recursos para prevenir falhas em cascata.

**Benefícios**: Isolamento de falhas, disponibilidade parcial.

### Cache-Aside Pattern

**Quando usar**: Cache de leitura com invalidação por TTL ou evento.

**Benefícios**: Performance, redução de carga no database.

### Event Sourcing Pattern

**Quando usar**: Persistir mudanças de estado como sequência de eventos.

**Benefícios**: Audit trail completo, ability to rebuild state, temporal queries.

## Padrões de Concorrência

### Optimistic Locking

**Quando usar**: Concorrência em operações de leitura-escrita.

**Benefícios**: Performance sem locks pesados, detecção de conflitos.

### Pessimistic Locking

**Quando usar**: Operações que exigem consistência absoluta.

**Benefícios**: Garantia de serialização, prevenção de dirty reads.

### Outbox Pattern

**Quando usar**: Garantir entrega de eventos junto com mudança de dados.

**Benefícios**: Consistência entre database e message broker.

## Anti-Patterns a Evitar

| Anti-Pattern | Por quê é ruim | Alternativa |
|---|---|---|
| God Class | Viola SRP, difícil de manter | Dividir em classes menores |
| Spaghetti Code | Impossível de manter | Clean Architecture |
| N+1 Queries | Performance terrível | Eager loading, batch queries |
| Anemic Domain Model | Lógica espalhada em services | Rich Domain Model |
| Primitive Obsession | Perde semântica do domínio | Value Objects |
| Copy-Paste Programming | Code duplication | Extração de métodos/classes |
| Golden Hammer | Solução genérica para problemas específicos | Padrões apropriados |
| Tight Coupling | Mudanças afetam múltiplas partes | DI, interfaces, events |

## Responsabilidades

- Documentar quando cada padrão deve ser usado
- Revisar código para garantir uso correto dos padrões
- Ensurer que padrões não são over-engineered

## Dependências

- [Architecture.md](./Architecture.md) — Arquitetura que define as camadas
- [CodingStandards.md](./CodingStandards.md) — Padrões de codificação
- [Constitution.md](./Constitution.md) — Princípios fundamentais

## Regras

- Padrões devem ser usados apenas quando resolvem um problema real
- Documentar em Decisions.md quando um padrão é escolhido ou rejeitado
- Não implementar padrões por implementar (YAGNI)
- Revisar trimestralmente a aplicação dos padrões

## Futuras Melhorias

- Adicionar exemplos de código para cada padrão
- Criar Decision Records para padrões discutidos
- Adicionar padrões específicos para Event Sourcing
- Documentar padrões de testes (Builder, Object Mother, Fake, Stub)

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial dos padrões de design |
