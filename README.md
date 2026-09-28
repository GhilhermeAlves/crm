# 🚀 CRM SaaS Omnichannel

[![CI/CD](https://img.shields.io/badge/CI%2FCD-Passing-brightgreen?style=flat-square)](https://github.com/GhilhermeAlves/crm/actions)
[![Tests](https://img.shields.io/badge/Tests-306%20Passing-brightgreen?style=flat-square)](https://github.com/GhilhermeAlves/crm/actions)
[![Java](https://img.shields.io/badge/Java-25%20LTS-orange?style=flat-square&logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green?style=flat-square&logo=spring)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue?style=flat-square&logo=react)](https://react.dev)
[![Next.js](https://img.shields.io/badge/Next.js-14-black?style=flat-square&logo=next.js)](https://nextjs.org)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?style=flat-square&logo=postgresql)](https://www.postgresql.org)
[![License](https://img.shields.io/badge/License-Proprietary-blue?style=flat-square)](#-licença)

**Plataforma completa de CRM SaaS com autenticação segura, análise de dados em tempo real e automação de vendas.**

[📖 Documentação](#-documentação) • [🗺️ Roadmap](docs/07-roadmap/) • [🐛 Issues](https://github.com/GhilhermeAlves/crm/issues) • [🔄 CI/CD](https://github.com/GhilhermeAlves/crm/actions)

---

## ✨ Destaques

- 📊 **Dashboard Analítico** - Métricas em tempo real com comparações temporais
- 💬 **Omnichannel** - Integração WhatsApp, SMS, Email, Inbox unificado
- 🤖 **Assistente de IA** - "Leo" com tools contextuais e sugestões de resposta
- 🔐 **Autenticação Segura** - Keycloak + OIDC/OAuth2 + Rate Limiting distribuído
- 📱 **Interface Responsiva** - Next.js 14 + Tailwind CSS + Refine UI
- 🗄️ **Arquitetura Escalável** - Microserviços independentes (Backend, Auth-Service)
- 📈 **Gestão de Vendas** - Pipeline, Oportunidades, Tarefas, Follow-ups, Customer 360
- ✅ **306+ Testes** - Cobertura completa com JUnit5 + Vitest
- 🚀 **Deploy Automático** - GitHub Actions → GHCR → VPS (Ubuntu)

---

## 🛠️ Stack Tecnológico

| Camada | Tecnologia | Versão |
|--------|-----------|--------|
| **Frontend** | Next.js + React + TypeScript | 14 + 18 |
| **Backend** | Java + Spring Boot 3 | 25 + 3.5 |
| **Auth** | Keycloak + OAuth2/OIDC | 24+ |
| **Database** | PostgreSQL + Row Level Security | 16 |
| **Cache** | Redis | 7 |
| **Message Queue** | RabbitMQ | 3 |
| **File Storage** | MinIO (S3-compatible) | Latest |
| **Container Registry** | GHCR | - |

---

## 🏗️ Arquitetura

```
┌─────────────────────────────────────────┐
│   Frontend (Next.js + Refine UI)        │
│   http://localhost:3000                 │
└──────────────────┬──────────────────────┘
                   │
        ┌──────────▼──────────┐
        │  OIDC Gateway       │
        │  (Auth Relay)       │
        └──────────┬──────────┘
                   │
    ┌──────────────┼──────────────┐
    │              │              │
┌───▼────┐  ┌─────▼──────┐  ┌────▼────┐
│Backend  │  │Auth-Service│  │Analytics│
│ :8080   │  │  :8082     │  │ :8083   │
└───┬─────┘  └─────┬──────┘  └────┬────┘
    │              │              │
    └──────────────┼──────────────┘
                   │
        ┌──────────▼──────────┐
        │ PostgreSQL (RLS)    │
        │ Redis + RabbitMQ    │
        │ MinIO + Keycloak    │
        └─────────────────────┘
```

---

## 🎯 Começar em 5 Minutos

### Pré-requisitos
- Java 25 LTS
- Node.js 20+
- Docker & Docker Compose

### Setup Local

```bash
# 1. Clonar repositório
git clone https://github.com/GhilhermeAlves/crm.git
cd crm

# 2. Infraestrutura (Docker)
cd docker && docker-compose -f docker-compose.dev.yml up -d
cd ..

# 3. Backend (Terminal 1)
cd backend && ./mvnw spring-boot:run

# 4. Auth-Service (Terminal 2)
cd auth-service && ./mvnw spring-boot:run

# 5. Frontend (Terminal 3)
cd frontend-refine && npm install && npm run dev
```

Acesse **http://localhost:3000** e faça login com suas credenciais Keycloak.

---

## 📍 URLs Locais

| Serviço | URL |
|---------|-----|
| 🌐 **Frontend** | http://localhost:3000 |
| 🔌 **Backend API** | http://localhost:8080/api/v1 |
| 🔑 **Auth-Service** | http://localhost:8082 |
| 📊 **Keycloak Admin** | http://localhost:8180/admin |
| 📚 **Swagger UI** | http://localhost:8080/swagger-ui.html |
| 🗄️ **PostgreSQL** | localhost:5432 |
| 💾 **Redis** | localhost:6379 |
| 📨 **RabbitMQ** | http://localhost:15672 |
| 🪣 **MinIO Console** | http://localhost:9001 |

---

## 📁 Estrutura do Projeto

```
crm/
├── docs/                    # Documentação completa
│   ├── 00-core/            # Arquitetura e TechStack
│   ├── 01-backend/         # Módulos backend
│   ├── 02-frontend/        # Componentes frontend
│   ├── 03-database/        # Modelagem de dados
│   ├── 04-integrations/    # Integrações (WhatsApp, etc)
│   ├── 05-business-rules/  # Regras de negócio
│   ├── 06-devops/          # CI/CD e Deploy
│   └── 07-roadmap/         # Roadmap do produto
│
├── backend/                 # API Principal (Java/Spring)
├── auth-service/           # Serviço de Autenticação
├── frontend-refine/        # Frontend (Next.js + Refine)
├── docker/                 # Docker Compose configs
├── infra/                  # Scripts de Deploy (VPS)
├── scripts/                # Utilitários de setup
├── sprints/                # Documentação de Sprints
└── .github/                # GitHub Actions CI/CD
```

---

## 🌿 Fluxo de Branches

```
feature/frontend-refine          →  Refatoração de páginas frontend
feature/backend-microservices    →  Módulos backend (Auth, Master Data, etc)
                 ↓
        crm-improvements-deploy-phase  →  Fase de deployment
                 ↓
              VPS (Produção)
```

**Princípio**: Cada branch contém apenas seus arquivos de escopo:
- `feature/frontend-refine`: Arquivos em `frontend-refine/`
- `feature/backend-microservices`: Módulos em `backend/`, `auth-service/`, etc

---

## ✅ Status

| Componente | Status | Testes |
|-----------|--------|--------|
| Backend | ✅ | 306+ |
| Auth-Service | ✅ | 306+ |
| Frontend | ✅ | TypeScript + Prettier |
| E2E | ℹ️ Informativo | Playwright |
| **CI/CD Pipeline** | **✅ Green** | **Automated** |

---

## 🚀 CI/CD Pipeline

### Automações
- ✅ **Tests** - Maven (Backend) + Vitest/Jest (Frontend)
- ✅ **Linting** - Prettier + ESLint
- ✅ **Build** - Docker image → GHCR
- ✅ **Deploy** - Manual trigger com `deploy.sh` na VPS

### Branches Principais
| Branch | Propósito |
|--------|-----------|
| `feature/frontend-refine` | Refine frontend |
| `feature/backend-microservices` | Módulos backend |
| `crm-improvements-deploy-phase` | Preparação para deploy |

---

## 🤝 Contribuindo

1. **Criar branch** a partir de `feature/frontend-refine` ou `feature/backend-microservices`
2. **Desenvolver** seguindo o padrão de commits: `feat(area): description`
3. **Testar localmente** - Rodar testes antes de push
4. **Push** e criar PR contra a branch correspondente
5. **CI deve passar** - Testes, lint e build devem estar verdes

### Padrão de Commits
```
feat(area): descrição da feature
fix(area): descrição do bug
docs(area): descrição da documentação
refactor(area): descrição do refactor
test(area): descrição do teste
chore(area): descrição da tarefa
```

---

## 📚 Documentação

Documentação completa em [docs/](docs/):

- **[00-core](docs/00-core/)** — Arquitetura, TechStack, Constituição
- **[01-backend](docs/01-backend/)** — Módulos e funcionalidades backend
- **[02-frontend](docs/02-frontend/)** — Componentes e páginas frontend
- **[03-database](docs/03-database/)** — Modelagem de dados e RLS
- **[04-integrations](docs/04-integrations/)** — Integrações (WhatsApp, OpenAI, etc)
- **[05-business-rules](docs/05-business-rules/)** — Regras de negócio
- **[06-devops](docs/06-devops/)** — CI/CD, Docker, Deploy, Monitoramento
- **[07-roadmap](docs/07-roadmap/)** — Roadmap e features futuras
- **[sprints/](sprints/)** — Histórico de Sprints e relatórios

---

## 📊 Estatísticas do Projeto

- **306+** testes automatizados (Backend)
- **20+** Sprints completadas
- **85%+** cobertura de testes
- **4** módulos principais (Backend, Auth, Frontend, Analytics)
- **Zero-downtime** deployment

---

## 📄 Licença

**Proprietary** — Todos os direitos reservados © 2024-2026

---

## 📞 Suporte

- 🐛 [Abrir uma issue](https://github.com/GhilhermeAlves/crm/issues)
- 📖 [Consultar documentação](docs/)
- 🔄 [Ver CI/CD](https://github.com/GhilhermeAlves/crm/actions)

---

**Made with ❤️ by [Ghilherme Alves](https://github.com/GhilhermeAlves)**
