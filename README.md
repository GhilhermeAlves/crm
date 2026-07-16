# CRM SaaS Omnichannel

Sistema CRM Omnichannel para gestão de leads, contatos, pipeline de vendas e comunicação multicanal.

## Arquitetura

- **Backend**: Java 21 + Spring Boot 3 (Clean Architecture + DDD)
- **Frontend**: Next.js 14 + React 18 + TypeScript
- **Database**: PostgreSQL 16
- **Cache**: Redis 7
- **Message Broker**: RabbitMQ 3
- **File Storage**: MinIO (S3-compatible)

## Estrutura

```
crm/
├── docs/           # Documentação do projeto
├── backend/        # API Backend (Java/Spring Boot)
├── frontend/       # Frontend (Next.js)
├── docker/         # Configurações Docker
├── infra/          # Infraestrutura (futuro)
├── scripts/        # Scripts auxiliares
└── .github/        # GitHub Actions
```

## Início Rápido

### Pré-requisitos

- Java 21
- Node.js 20+
- Docker & Docker Compose

### Setup

```bash
# Setup completo
chmod +x scripts/setup.sh
./scripts/setup.sh

# Ou manualmente:
cd docker && docker-compose -f docker-compose.dev.yml up -d
cd backend && ./mvnw spring-boot:run
cd frontend && npm run dev
```

### URLs

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api/v1
- **Swagger UI**: http://localhost:8080/api/v1/docs/swagger
- **PostgreSQL**: localhost:5432
- **Redis**: localhost:6379
- **RabbitMQ**: http://localhost:15672
- **MinIO**: http://localhost:9001

## Documentação

Toda a documentação está em `docs/`:

- [00-core](docs/00-core/) — Arquitetura, TechStack, Constituição
- [01-backend](docs/01-backend/) — Módulos e funcionalidades backend
- [02-frontend](docs/02-frontend/) — Componentes e páginas frontend
- [03-database](docs/03-database/) — Modelagem de dados
- [04-integrations](docs/04-integrations/) — Integrações externas
- [05-business-rules](docs/05-business-rules/) — Regras de negócio
- [06-devops](docs/06-devops/) — CI/CD, Docker, Monitoramento
- [07-roadmap](docs/07-roadmap/) — Roadmap do produto

## Licença

Proprietary — Todos os direitos reservados.
