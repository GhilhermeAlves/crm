# Contacts — Gestão de Contatos

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Fluxo](#fluxo)
- [Endpoints](#endpoints)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o módulo de gestão de contatos, incluindo criação, atualização, segmentação e campos customizados.

## Descrição

Contatos são as pessoas com quem a empresa se comunia — podem ser leads, clientes ou parceiros. O módulo suporta segmentação, tags, campos customizados e importação em massa.

## Responsabilidades

- Criar, atualizar e gerenciar contatos
- Gerenciar endereços e informações de contato
- Aplicar tags e segmentação
- Suportar campos customizados
- Importar e exportar contatos em massa
- Deduplicar contatos

## Fluxo

### Criação de Contato

```
1. Usuário preenche dados do contato
        │
2. Backend valida dados (email/phone únicos na empresa)
        │
3. Backend cria Contact + ContactAddress
        │
4. Evento ContactCreated é publicado
        │
5. Contato disponível para pipeline e comunicação
```

### Importação

```
1. Usuário faz upload de CSV/Excel
        │
2. Backend valida arquivo e formatação
        │
3. Backend identifica duplicatas
        │
4. Usuário confirma importação (com/sem duplicatas)
        │
5. Backend importa em background (batch)
        │
6. Usuário recebe notificação quando completa
```

## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| GET | `/api/v1/contacts` | Listar contatos | `contact:read` |
| GET | `/api/v1/contacts/{id}` | Buscar contato | `contact:read` |
| POST | `/api/v1/contacts` | Criar contato | `contact:write` |
| PUT | `/api/v1/contacts/{id}` | Atualizar contato | `contact:write` |
| DELETE | `/api/v1/contacts/{id}` | Deletar contato | `contact:delete` |
| POST | `/api/v1/contacts/import` | Importar contatos | `contact:import` |
| GET | `/api/v1/contacts/export` | Exportar contatos | `contact:export` |
| POST | `/api/v1/contacts/{id}/tags` | Adicionar tag | `contact:write` |
| DELETE | `/api/v1/contacts/{id}/tags/{tagId}` | Remover tag | `contact:write` |
| GET | `/api/v1/contacts/segments` | Listar segmentos | `contact:read` |
| POST | `/api/v1/contacts/segments` | Criar segmento | `contact:write` |

### DTOs

**CreateContactCommand**:
```json
{
  "firstName": "string (required)",
  "lastName": "string (optional)",
  "email": "string (optional, unique per company)",
  "phone": "string (optional, E.164 format)",
  "company": "string (optional)",
  "notes": "string (optional)",
  "tags": ["string"],
  "customFields": {
    "fieldName": "value"
  }
}
```

## Dependências

- [Companies.md](./Companies.md) — Tenant isolation
- [Pipeline.md](./Pipeline.md) — Contatos em pipelines
- [Conversations.md](./Conversations.md) — Comunicação com contatos
- [03-database/ERD.md](../03-database/ERD.md) — Modelo de dados

## Regras

- Email é único por empresa (não global)
- Phone deve estar em formato E.164
- Contato deletado é soft deleted (preserva histórico)
- Campos customizados são definidos por empresa
- Máximo de 5 tags por contato
- Máximo de 50 campos customizados por empresa
- Importação máxima de 10.000 contatos por vez
- Deduplicação por email + phone na importação

## Futuras Melhorias

- Score de engajamento por contato
- Histórico completo de interações
- Importação de contatos via API
- Integração com Google Contacts
- IA para enriquecimento de dados
- Múltiplos canais de contato por pessoa
- Merge de contatos duplicados

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
