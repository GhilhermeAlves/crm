# Checklist de Revisão

## Arquitetura
- [ ] Código segue Clean Architecture (Domain → Application → Infrastructure → Presentation)
- [ ] Dependências seguem direção correta (nunca do domain para infrastructure)
- [ ] Ports (interfaces) estão nos pacotes corretos
- [ ] Implementações estão nos pacotes corretos
- [ ] Não há vazamento de responsabilidades entre camadas

## Código
- [ ] Naming conventions seguidas (camelCase, PascalCase, CONSTANT_CASE)
- [ ] Sem duplicação de código
- [ ] Métodos coesos (max 30 linhas)
- [ ] Classes coesas (responsabilidade única)
- [ ] Tratamento de erros adequado
- [ ] Logs apropriados
- [ ] Sem System.out ou print statements
- [ ] Comentários apenas quando necessário (código autoexplicativo)

## Backend (Java)
- [ ] Entities com UUID v4 como PK
- [ ] Entities tem created_at e updated_at
- [ ] Soft delete implementado (deleted_at)
- [ ] Value objects validam dados na criação
- [ ] Repositories implementam interfaces do application layer
- [ ] Mappers implementados com MapStruct
- [ ] Controllers validam entrada (@Valid)
- [ ] Rotas REST seguem padrão RESTful
- [ ] Códigos HTTP corretos (200, 201, 204, 400, 401, 403, 404, 500)
- [ ] JWT claims incluem userId, companyId, roles
- [ ] RBAC implementado com @PreAuthorize
- [ ] Flyway migrations nomeadas corretamente (VXXX__descricao.sql)
- [ ] Migrations são idempotentes (IF NOT EXISTS)
- [ ] Índices em todas as FKs
- [ ] Constraints nomeadas

## Frontend
- [ ] Components Server Component por padrão
- [ ] Client Component apenas quando necessário
- [ ] Components com max 200 linhas
- [ ] Tailwind CSS classes usadas
- [ ] Shadcn UI usado para componentes base
- [ ] React Query para estado servidor
- [ ] Zod para validação de formulários
- [ ] Types definidos para todas as entidades
- [ ] Hooks customizados para lógica reutilizável
- [ ] Proteção de rotas implementada

## Banco de Dados
- [ ] Migrations criadas, não alteradas
- [ ] Naming snake_case plural
- [ ] UUID v4 como PK
- [ ] FKs indexadas
- [ ] Soft delete implementado (deleted_at)
- [ ] created_at/updated_at presentes
- [ ] Constraint de unicidade aplicada
- [ ] Tipos corretos (TIMESTAMP, VARCHAR, BOOLEAN)

## Testes
- [ ] Testes unitários para domain entities
- [ ] Testes unitários para application services
- [ ] Testes de integração para repositories
- [ ] Testes de integração para controllers
- [ ] Cobertura ≥80% (unit), ≥60% (integration)
- [ ] Testes de borda incluídos
- [ ] Mocks usados para dependências externas
- [ ] Testes são independentes

## Performance
- [ ] N+1 queries evitadas
- [ ] Índices apropriados criados
- [ ] Paginação implementada (se aplicável)
- [ ] Cache considerado (se aplicável)
- [ ] Conexões com banco gerenciadas

## Segurança
- [ ] Sem secrets no código
- [ ] Senhas hashadas (BCrypt 12 rounds)
- [ ] JWT com expiry apropriado (15min access + 7d refresh)
- [ ] Refresh token rotation implementada
- [ ] Rate limiting no login
- [ ] Input validation em todos os endpoints
- [ ] SQL injection prevenido (JPA parameters)
- [ ] CORS configurado
- [ ] Headers de segurança presentes

## Documentação
- [ ] `docs/CHANGELOG.md` atualizado
- [ ] `IMPLEMENTATION_REPORT.md` atualizado
- [ ] Contexto atualizado (se necessário)
- [ ] Playbook atualizado (se necessário)
- [ ] Endpoints documentados (Swagger/OpenAPI)

## Context & Playbook
- [ ] Contexto do módulo reflete implementação
- [ ] Playbook reflete processo real
- [ ] Dependências documentadas
- [ ] APIs documentadas com exemplos

## Changelog
- [ ] Mudanças registradas com clareza
- [ ] Versão incrementada corretamente
- [ ] Breaking changes destacados
- [ ] Novos arquivos listados

## .ai (Memória do Projeto)
- [ ] `.ai/LAST_SESSION.md` atualizado
- [ ] `.ai/WORKLOG.md` atualizado
- [ ] `.ai/CURRENT_TASK.md` atualizado
- [ ] `.ai/CURRENT_SPRINT.md` atualizado
- [ ] `.ai/PROJECT_STATUS.md` atualizado
- [ ] `.ai/IMPLEMENTATION_QUEUE.md` atualizado
- [ ] `.ai/CURRENT_DECISIONS.md` atualizado (se novas decisões)
- [ ] `.ai/BLOCKERS.md` atualizado (se necessário)
- [ ] `.ai/KNOWN_ISSUES.md` atualizado (se necessário)

## Resultado da Revisão

- [ ] ✅ Aprovado
- [ ] ⚠️ Aprovado com ressalvas (descrever abaixo)
- [ ] ❌ Reprovado (justificar abaixo)

### Observações
