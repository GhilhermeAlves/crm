# CRM SaaS Omnichannel

Sistema CRM Omnichannel para gestão de leads, contatos, pipeline de vendas e comunicação multicanal.

## 🏗️ Arquitetura

- **Backend**: Java 25 + Spring Boot 3 (Clean Architecture + DDD)
  - `backend/` - API principal
  - `auth-service/` - Serviço de autenticação independente com Keycloak
- **Frontend**: Next.js 14 + React 18 + TypeScript (`frontend-refine/`)
- **Database**: PostgreSQL 16
- **Cache**: Redis 7
- **Message Broker**: RabbitMQ 3
- **Auth**: Keycloak (OIDC/OAuth2)
- **File Storage**: MinIO (S3-compatible)

## 📁 Estrutura

```
crm/
├── docs/              # Documentação do projeto
├── backend/           # API Backend (Java/Spring Boot)
├── auth-service/      # Serviço de Autenticação (microserviço)
├── frontend-refine/   # Frontend (Next.js + Refine)
├── docker/            # Configurações Docker
├── infra/             # Infraestrutura (VPS deployment)
├── scripts/           # Scripts auxiliares
├── sprints/           # Documentação de Sprints
└── .github/           # GitHub Actions CI/CD
```

## 🌿 Fluxo de Branches

```
feature/frontend-refine           # Refatoração de páginas frontend
    ↓ (merge quando pronto)
crm-improvements-deploy-phase     # Fase de deployment
    ↓ (deploy)
VPS (Produção)

feature/backend-microservices     # Desenvolvimento de módulos backend
    ↓ (merge quando pronto)
crm-improvements-deploy-phase     # Fase de deployment
    ↓ (deploy)
VPS (Produção)
```

**Princípio**: Cada branch contém arquivos do seu escopo:
- `feature/frontend-refine`: Apenas arquivos frontend (`frontend-refine/`)
- `feature/backend-microservices`: Módulos backend (`backend/`, `auth-service/`, etc)

## 🚀 Início Rápido

### Pré-requisitos

- Java 25 LTS
- Node.js 20+
- Docker & Docker Compose
- Git

### Setup Local

```bash
# 1. Clonar repositório
git clone https://github.com/GhilhermeAlves/crm.git
cd crm

# 2. Setup da infraestrutura (Docker)
cd docker && docker-compose -f docker-compose.dev.yml up -d
cd ..

# 3. Backend
cd backend && ./mvnw spring-boot:run

# 4. Auth-Service (em outro terminal)
cd auth-service && ./mvnw spring-boot:run

# 5. Frontend (em outro terminal)
cd frontend-refine && npm install && npm run dev
```

### URLs Locais

| Serviço | URL |
|---------|-----|
| **Frontend** | http://localhost:3000 |
| **Backend API** | http://localhost:8080/api/v1 |
| **Auth-Service** | http://localhost:8082 |
| **Keycloak Admin** | http://localhost:8180/admin |
| **Swagger UI (Backend)** | http://localhost:8080/api/v1/docs/swagger |
| **PostgreSQL** | localhost:5432 |
| **Redis** | localhost:6379 |
| **RabbitMQ Admin** | http://localhost:15672 |
| **MinIO Console** | http://localhost:9001 |

## ✅ Status dos Testes

- **Backend**: 306 testes passando ✅
- **Auth-Service**: 306 testes passando ✅
- **Frontend**: Type checking + Prettier ✅
- **E2E**: Informativo (não bloqueia CI)

## 📚 Documentação

Toda a documentação está em `docs/`:

- [00-core](docs/00-core/) — Arquitetura, TechStack, Constituição
- [01-backend](docs/01-backend/) — Módulos e funcionalidades backend
- [02-frontend](docs/02-frontend/) — Componentes e páginas frontend
- [03-database](docs/03-database/) — Modelagem de dados
- [04-integrations](docs/04-integrations/) — Integrações externas
- [05-business-rules](docs/05-business-rules/) — Regras de negócio
- [06-devops](docs/06-devops/) — CI/CD, Docker, Monitoramento
- [07-roadmap](docs/07-roadmap/) — Roadmap do produto
- [sprints/](sprints/) — Histórico e status de Sprints

## 🔒 CI/CD Pipeline

### Branches Principais

- `feature/frontend-refine` → Refatoração frontend
- `feature/backend-microservices` → Módulos backend
- `crm-improvements-deploy-phase` → Preparação para deploy
- `main` (futura) → Releases em produção

### Automações

- ✅ Testes Backend (Maven + JUnit5) - 306 testes
- ✅ Testes Frontend (Vitest/Jest) - TypeScript
- ✅ Linting (Prettier, ESLint)
- ✅ Build Docker (GHCR)
- ✅ Deploy VPS (manual com `deploy.sh`)

## 🤝 Contribuindo

1. Criar branch a partir de `feature/frontend-refine` ou `feature/backend-microservices`
2. Seguir o padrão de commits: `feat(area): description`
3. Rodar testes localmente antes de push
4. CI deve passar (testes + lint) antes de merge

## 📋 Licença

Proprietary — Todos os direitos reservados.
