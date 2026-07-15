# Audit — Sistema de Auditoria

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

Documentar o sistema de auditoria, incluindo registro de ações, rastreabilidade e compliance.

## Descrição

O sistema de auditoria registra todas as ações significativas realizadas no sistema, garantindo rastreabilidade total e compliance com LGPD. Cada ação é registrada com timestamp, usuário, IP e dados antes/depois.

## Responsabilidades

- Registrar todas as ações de CREATE, UPDATE e DELETE
- Rastrear quem fez o quê, quando e de onde
- Manter log de alterações (before/after)
- Suportar consulta e exportação de logs de auditoria
- Garantir imutabilidade dos registros

## O que é Auditado

| Ação | Exemplo |
|---|---|
| Login/Logout | Usuário fez login |
| CRUD de entidades | Criou, atualizou ou deletou lead |
| Mudança de permissão | Admin mudou role de usuário |
| Envio de mensagem | Mensagem enviada via WhatsApp |
| Acesso a dados sensíveis | Consulta de dados pessoais |
| Exportação | Exportou lista de contatos |
| Configuração | Mudou configurações da empresa |

## Fluxo

```
1. Usuário realiza ação
        │
2. AOP interceptor captura a ação
        │
3. Dados antes/depois são capturados
        │
4. Registro de auditoria é criado
        │
5. Registro é persistido (imutável)
        │
6. Evento AuditLogged é publicado
```

## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| GET | `/api/v1/audit` | Listar registros | `audit:read` |
| GET | `/api/v1/audit/{id}` | Detalhes do registro | `audit:read` |
| GET | `/api/v1/audit/user/{userId}` | Ações por usuário | `audit:read` |
| GET | `/api/v1/audit/entity/{type}/{id}` | Histórico de entidade | `audit:read` |
| GET | `/api/v1/audit/export` | Exportar auditoria | `audit:export` |

## Dependências

- [Users.md](./Users.md) — Usuário que realizou a ação
- [Companies.md](./Companies.md) — Tenant da ação
- [03-database/Audit.md](../03-database/Audit.md) — Schema de auditoria

## Regras

- Registros de auditoria são imutáveis
- Não podem ser deletados ou editados
- Retenção mínima: 5 anos
- Logs são armazenados em tabela separada
- Dados sensíveis são mascarados (CPF, email)
- Exportação de auditoria requer aprovação de admin

## Futuras Melhorias

- Dashboard de auditoria
- Alertas para ações suspeitas
- Análise de comportamento (UEBA)
- Compliance automático (LGPD, SOC2)
- Integração com SIEM

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
