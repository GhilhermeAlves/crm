# TechStack — Stack Tecnológico

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Frontend](#frontend)
- [Backend](#backend)
- [Banco de Dados](#banco-de-dados)
- [Infraestrutura](#infraestrutura)
- [Integrações](#integrações)
- [Ferramentas de Desenvolvimento](#ferramentas-de-desenvolvimento)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar todas as tecnologias, frameworks, bibliotecas e ferramentas utilizadas no projeto, incluindo versões e justificativas.

## Descrição

O TechStack define a base tecnológica do CRM SaaS Omnichannel. Cada tecnologia foi selecionada considerando escalabilidade, comunidade ativa, longevidade e aderência aos princípios da [Constitution.md](./Constitution.md).

## Frontend

| Tecnologia | Versão | Finalidade | Justificativa |
|---|---|---|---|
| React | 18.x | Biblioteca UI | Ecossistema maduro, performance com fibers |
| Next.js | 14.x | Framework SSR/SSG | Server components, routing, otimizações built-in |
| TypeScript | 5.x | Tipagem estática | Segurança de tipos, melhor DX, refactoring seguro |
| Tailwind CSS | 3.x | CSS utility-first | Produtividade, consistência visual, bundle mínimo |
| Shadcn UI | latest | Component library | Componentes acessíveis, customizáveis, sem vendor lock-in |

### Justificativa do Frontend

- **React + Next.js**: Combinação mais madura e performática do mercado para SPA/SSR
- **TypeScript**: Elimina classes inteiros de bugs em tempo de compilação
- **Tailwind + Shadcn**: Permite criar UI customizada sem depender de uma lib de componentes pesada

## Backend

| Tecnologia | Versão | Finalidade | Justificativa |
|---|---|---|---|
| Java | 21 LTS | Linguagem principal | LTS, records, sealed classes, virtual threads |
| Spring Boot | 3.x | Framework principal | Ecossistema maduro, convention over configuration |
| PostgreSQL | 16.x | Database relacional | Robusto, extensível, open source, battle-tested |
| Redis | 7.x | Cache e sessões | Performance, estruturas de dados ricas, pub/sub |
| RabbitMQ | 3.x | Message broker | Confiabilidade, padrão AMQP, clustering |
| Flyway | 10.x | Database migrations | Versionamento de schema, rollback, CI-friendly |
| JWT | - | Autenticação | Stateless, scalável, padrão de mercado |
| Docker | 24.x | Containerização | Padronização de ambiente, CI/CD |
| OpenAPI | 3.1 | API documentation | Contrato de API, geração de clientes |

### Justificativa do Backend

- **Java 21 + Spring Boot 3**: LTS com recursos modernos (virtual threads para I/O intensivo)
- **PostgreSQL**: Suporta JSON, partitioning, full-text search — reduz dependências externas
- **Redis**: Cache distribuído, rate limiting, pub/sub para eventos em tempo real
- **RabbitMQ**: Desacoplamento de serviços, retry logic, dead letter queues

## Banco de Dados

| Componente | Tecnologia | Finalidade |
|---|---|---|
| Primary DB | PostgreSQL 16 | Dados transacionais |
| Cache Layer | Redis 7 | Cache, sessões, filas leves |
| Message Broker | RabbitMQ 3 | Eventos assíncronos |
| File Storage | MinIO / S3 | Arquivos, mídia, documentos |

## Infraestrutura

| Componente | Tecnologia | Finalidade |
|---|---|---|
| Containerization | Docker 24 | Empacotamento de aplicações |
| Orchestration | Kubernetes (futuro) | Escalabilidade e alta disponibilidade |
| Reverse Proxy | Nginx / Traefik | Load balancing, SSL termination |
| CI/CD | GitHub Actions | Integração e deploy contínuos |
| Monitoring | Prometheus + Grafana | Métricas e dashboards |
| Logging | ELK Stack / Loki | Logs centralizados |

## Integrações

| Sistema | Tecnologia | Finalidade |
|---|---|---|
| WhatsApp | Evolution API | Gateway oficial via provedor |
| OpenAI | API REST | IA para automação e chatbot |
| Google | OAuth 2.0 / APIs | Calendar, Contacts, Sheets |
| Email | SMTP / SendGrid | Envio e recebimento de emails |
| SMS | Twilio / Vonage | Envio de SMS |

## Ferramentas de Desenvolvimento

| Ferramenta | Finalidade |
|---|---|
| IntelliJ IDEA | IDE principal para backend |
| VS Code | IDE principal para frontend |
| DBeaver | Database management |
| Postman / Insomnia | API testing |
| Git | Versionamento de código |
| SonarQube | Qualidade de código estática |

## Responsabilidades

- Definir versões mínimas e recomendadas para cada tecnologia
- Manter atualizações de segurança documentadas
- Justificar mudanças de tecnologia em [Decisions.md](./Decisions.md)

## Dependências

- [Architecture.md](./Architecture.md) — Define como as tecnologias se conectam
- [CodingStandards.md](./CodingStandards.md) — Define como usar cada tecnologia

## Regras

- Atualizações de versão menor (patch) podem ser feitas sem aprovação
- Atualizações de versão maior (minor/major) requerem análise de impacto
- Novas tecnologias só são adicionadas após proof of concept
- Tecnologias em end-of-life devem ser substituídas em até 6 meses

## Futuras Melhorias

- Avaliar migração para Java 25 quando disponível
- Considerar GraalVM para native image em produção
- Avaliar Edge Runtime para Next.js em produção
- Considerar CockroachDB para distribuição geográfica de dados

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial do TechStack |
