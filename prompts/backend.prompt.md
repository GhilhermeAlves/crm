# Backend Prompt - Clean Architecture Java

## Quando utilizar

- Criando ou modificando código backend Java
- Implementando novas entidades, serviços ou controllers
- Modificando lógica de negócio no backend

## Objetivo

Implementar código backend seguindo Clean Architecture com Java, Spring Boot e boas práticas do projeto.

## Entrada esperada

- Nome do módulo (ex: `user`, `product`, `order`)
- Descrição da funcionalidade
- Regras de negócio aplicáveis

## Resultado esperado

- Domain entities com validações
- Application services (use cases)
- Infrastructure implementations (repositories, mappers)
- REST controllers

## Arquivos normalmente envolvidos

```
backend/src/main/java/com/becommerce/crm/{module}/
  ├── domain/
  │   ├── model/
  │   │   └── {Entity}.java
  │   ├── port/
  │   │   └── {Entity}Repository.java
  │   └── exception/
  │       └── {Entity}NotFoundException.java
  ├── application/
  │   ├── service/
  │   │   └── {Entity}ServiceImpl.java
  │   ├── dto/
  │   │   ├── request/
  │   │   │   └── {Entity}Request.java
  │   │   └── response/
  │   │       └── {Entity}Response.java
  │   └── mapper/
  │       └── {Entity}Mapper.java
  ├── infrastructure/
  │   ├── persistence/
  │   │   ├── entity/
  │   │   │   └── {Entity}JpaEntity.java
  │   │   ├── repository/
  │   │   │   └── {Entity}JpaRepository.java
  │   │   └── mapper/
  │   │       └── {Entity}PersistenceMapper.java
  │   └── config/
  │       └── {Module}Config.java
  └── presentation/
      └── rest/
          ├── {Entity}Controller.java
          └── dto/
              ├── request/
              │   └── {Entity}RestRequest.java
              └── response/
                  └── {Entity}RestResponse.java
```

## Boas práticas

- **Dependency Rule**: Domain não depende de nada. Application depende só de Domain. Infrastructure depende de Application.
- **Records para DTOs**: Usar `public record` para DTOs de request/response.
- **MapStruct para mappers**: Usar `@Mapper(componentModel = "spring")`.
- **Lombok**: Usar `@Getter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` quando necessário.
- **Soft delete**: Nunca deletar registros fisicamente. Usar campo `deleted_at`.
- **UUID PKs**: Usar `UUID` como chave primária em todas as entidades.
- **Naming conventions**:
  - Entidades: `PascalCase` singular (ex: `User`, `Product`)
  - Tabelas: `snake_case` plural (ex: `users`, `products`)
  - Colunas: `snake_case` (ex: `created_at`, `first_name`)
  - Métodos: `camelCase` (ex: `findAll`, `findById`)
- **Validação**: Usar `@Valid` e annotations Jakarta (ex: `@NotNull`, `@Size`, `@Email`).
- **Tratamento de erros**: Usar `@ControllerAdvice` com exception handler específico.
- **Pageable**: Usar `Pageable` do Spring para endpoints de listagem.

## Exemplo de uso

```
Criar o módulo de Categories com:
- Entidade Category com id (UUID), name, description, active, createdAt, updatedAt, deletedAt
- CRUD completo
- Validação: name não pode ser vazio, max 100 chars
- Paginação na listagem
- Soft delete
```
