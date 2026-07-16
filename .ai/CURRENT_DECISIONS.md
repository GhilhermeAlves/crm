# Decisões de Arquitetura

## D-001: Arquitetura Clean Architecture + Hexagonal + DDD
- **Data:** 2026-07-15
- **Descrição:** Adopt Clean Architecture with Hexagonal ports/adapters and DDD tactical patterns
- **Motivo:** Separation of concerns, testability, framework independence
- **Impacto:** All backend code follows layer separation
- **Documentos:** docs/00-core/Architecture.md, docs/00-core/Backend.md

## D-002: Schema-per-Tenant Multi-tenancy
- **Data:** 2026-07-15
- **Descrição:** Each tenant gets a separate PostgreSQL schema, isolated via Row-Level Security
- **Motivo:** Strong data isolation, easy backup/restore per tenant
- **Impacto:** All queries must include tenant context
- **Documentos:** docs/03-database/Overview.md

## D-003: Modular Monolith First
- **Data:** 2026-07-15
- **Descrição:** Start as modular monolith, prepare for microservices decomposition
- **Motivo:** Simpler to start, easier to refactor later
- **Impacto:** Bounded contexts are well-defined, ready for extraction
- **Documentos:** docs/00-core/Architecture.md

## D-004: JWT with Refresh Token Rotation
- **Data:** 2026-07-15
- **Descrição:** Access Token (15min) + Refresh Token (7 days) with rotation on each use
- **Motivo:** Security best practice, prevents token reuse
- **Impacto:** Auth implementation must handle rotation
- **Documentos:** docs/01-backend/Auth.md

## D-005: RBAC with 5 Roles
- **Data:** 2026-07-15
- **Descrição:** SUPER_ADMIN, ADMIN, MANAGER, AGENT, VIEWER
- **Motivo:** Granular access control per tenant
- **Impacto:** All endpoints must check permissions
- **Documentos:** docs/05-business-rules/Permissions.md

## D-006: Flyway for Database Migrations
- **Data:** 2026-07-15
- **Descrição:** Version-controlled schema migrations with Flyway
- **Motivo:** Reproducible schema changes, rollback capability
- **Impacto:** All schema changes must be migration files
- **Documentos:** docs/03-database/Overview.md

## D-007: MapStruct for Object Mapping
- **Data:** 2026-07-15
- **Descrição:** Use MapStruct for entity↔DTO mapping
- **Motivo:** Compile-time safety, better performance than reflection
- **Impacto:** All mappers must use MapStruct interfaces
- **Documentos:** docs/01-backend/Overview.md

## D-008: React Query for Frontend State
- **Data:** 2026-07-15
- **Descrição:** Use React Query for server state management
- **Motivo:** Caching, deduplication, optimistic updates
- **Impacto:** All API calls must use React Query hooks
- **Documentos:** docs/02-frontend/Overview.md

---

*Última atualização: 2026-07-15*
