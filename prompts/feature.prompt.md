# Feature Prompt - End-to-End Implementation

## Quando utilizar

- Implementando uma nova feature completa
- Criando novo módulo do sistema
- Implementando funcionalidade que envolve backend, frontend e banco

## Objetivo

Implementar feature completa seguindo o fluxo do projeto.

## Entrada esperada

- Requisitos da feature
- User stories (se disponíveis)
- Restrições técnicas

## Resultado esperado

- Backend completo (domain, application, infrastructure, presentation)
- Frontend completo (pages, components, hooks)
- Database migrations
- Testes unitários e de integração
- Documentação atualizada

## Arquivos normalmente envolvidos

Todos os arquivos do módulo sendo implementado:
```
backend/src/main/java/com/becommerce/crm/{module}/
backend/src/main/resources/db/migration/
backend/src/test/java/com/becommerce/crm/{module}/
frontend/src/app/{module}/
frontend/src/components/{module}/
frontend/src/hooks/use{Module}.ts
frontend/src/types/{module}.ts
docs/{module}/
docs-ai/{module}.md
contexts/{module}.context.md
```

## Boas práticas

- **Seguir AI_ROUTER**: Consultar AI_ROUTER.md para entender o fluxo correto.
- **Ler context**: Ler context files antes de implementar.
- **Ler playbook**: Ler playbooks relevantes antes de começar.
- **Backend primeiro**: Implementar backend antes do frontend.
- **Depois frontend**: Implementar frontend após backend estável.
- **Depois testes**: Criar testes após implementação.
- **Depois docs**: Atualizar documentação por último.

## Fluxo de implementação

1. **Planejamento**
   - Ler requisitos
   - Ler context files
   - Ler playbooks relevantes
   - Definir escopo

2. **Database**
   - Criar migrations
   - Criar entidades JPA

3. **Backend**
   - Domain (entities, ports, exceptions)
   - Application (services, DTOs, mappers)
   - Infrastructure (repositories, configs)
   - Presentation (controllers, REST DTOs)

4. **Frontend**
   - Types
   - API client
   - Hooks
   - Components
   - Pages

5. **Testes**
   - Unitários
   - Integração
   - E2E (se aplicável)

6. **Documentação**
   - CHANGELOG
   - Context files
   - Module docs

## Exemplo de uso

```
Implementar módulo de Products:

Requisitos:
- CRUD completo
- Categorias associadas (N:N)
- Imagens de produto
- Busca e filtros
- Paginação
- Soft delete

Fluxo:
1. Migration: tabela products, product_images, product_categories
2. Backend: domain, application, infrastructure, presentation
3. Frontend: listagem, formulário, detalhe
4. Testes: unitários e integração
5. Docs: CHANGELOG, context, module docs
```
