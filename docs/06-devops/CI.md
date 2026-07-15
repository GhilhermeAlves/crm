# CI — Integração Contínua

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Pipeline](#pipeline)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o pipeline de Integração Contínua (CI).

## Descrição

CI garante que todo commit seja validado automaticamente com build, testes, linting e análise de segurança.

## Pipeline

### Trigger

```
Pull Request → CI Pipeline
Push to main → CI Pipeline + Build Image
```

### Etapas

```
1. Checkout
        │
2. Setup (Java 21, Node 20, PostgreSQL)
        │
3. Cache Dependencies
        │
4. Lint & Format
   ├── Backend: Checkstyle, SpotBugs
   └── Frontend: ESLint, Prettier
        │
5. Unit Tests
   ├── Backend: JUnit 5
   └── Frontend: Vitest
        │
6. Integration Tests
   └── Backend: Testcontainers
        │
7. Build
   ├── Backend: Maven Package
   └── Frontend: Next.js Build
        │
8. Security Scan
   ├── Dependency Check (OWASP)
   └── Container Scan (Trivy)
        │
9. Build Docker Images
        │
10. Push to Registry (ghcr.io)
```

### GitHub Actions

```yaml
name: CI Pipeline
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  backend:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: crm_test
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
      - run: ./mvnw verify
      - run: ./mvnw checkstyle:check

  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: npm ci
      - run: npm run lint
      - run: npm run typecheck
      - run: npm run test
      - run: npm run build
```

## Responsabilidades

- Validar código em todo PR
- Rodar testes automaticamente
- Detectar vulnerabilidades
- Build de imagens Docker

## Dependências

- [CD.md](./CD.md) — Deploy contínuo
- [00-core/CodingStandards.md](../00-core/CodingStandards.md) — Padrões

## Regras

- Todo PR deve passar no CI antes do merge
- Code coverage mínimo: 80% unit, 60% integration
- Nenhum vulnerability crítica/high pode ser merged
- Build deve completar em < 10 minutos
- Cache deve ser usado agressivamente

## Futuras Melhorias

- Mutation testing
- Visual regression testing
- Performance testing no CI
- AI para review de código
- Auto-merge para dependabot

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
