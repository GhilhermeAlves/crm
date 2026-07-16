# API Prompt - REST Endpoints

## Quando utilizar

- Projetando ou implementando endpoints REST
- Criando controllers e DTOs
- Modificando contrato da API

## Objetivo

Criar endpoints REST padronizados com controllers, DTOs e documentação.

## Entrada esperada

- Módulo
- Endpoints necessários (método, path, descrição)
- Requisitos de autenticação/autorização

## Resultado esperado

- Controller com endpoints
- Request DTOs com validação
- Response DTOs padronizados
- Configuração de rotas

## Arquivos normalmente envolvidos

```
backend/src/main/java/com/becommerce/crm/presentation/rest/
  ├── {Entity}Controller.java
  └── dto/
      ├── request/
      │   └── {Entity}RestRequest.java
      └── response/
          └── {Entity}RestResponse.java
```

## Boas práticas

- **Versionamento**: Usar `/api/v1/` como prefixo.
- **Recursos plurais**: Usar `/api/v1/users`, não `/api/v1/user`.
- **HTTP methods corretos**:
  - `GET` para leitura
  - `POST` para criação
  - `PUT` para atualização completa
  - `PATCH` para atualização parcial
  - `DELETE` para remoção
- **Validação**: Usar `@Valid` em todos os request bodies.
- **Response wrapping**: Padronizar respostas com body consistente.
- **Paginação**: Suportar `page`, `size`, `sort` em listagens.
- **HTTP Status corretos**:
  - 200 OK
  - 201 Created
  - 204 No Content
  - 400 Bad Request
  - 404 Not Found
  - 500 Internal Server Error

## Exemplo de uso

```
Criar endpoints para Categories:
- GET /api/v1/categories - Listar com paginação
- GET /api/v1/categories/{id} - Buscar por ID
- POST /api/v1/categories - Criar nova categoria
- PUT /api/v1/categories/{id} - Atualizar categoria
- PATCH /api/v1/categories/{id}/active - Ativar/desativar
- DELETE /api/v1/categories/{id} - Soft delete
```
