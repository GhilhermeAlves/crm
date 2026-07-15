# Validation — Validação de Dados

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Schemas](#schemas)
- [Validação no Cliente](#validação-no-cliente)
- [Validação no Servidor](#validação-no-servidor)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o sistema de validação de dados, incluindo schemas, validação cliente/servidor e mensagens de erro.

## Descrição

Validação usa Zod no frontend e Jakarta Validation no backend. Schemas são definidos uma vez e compartilhados quando possível.

## Schemas

### Contact

```typescript
const contactSchema = z.object({
  firstName: z.string().min(1, 'Nome é obrigatório').max(100),
  lastName: z.string().max(100).optional(),
  email: z.string().email('Email inválido').optional(),
  phone: z.string().regex(/^\+[1-9]\d{1,14}$/, 'Formato E.164').optional(),
  company: z.string().max(200).optional(),
  notes: z.string().max(1000).optional(),
});
```

### Lead

```typescript
const leadSchema = contactSchema.extend({
  origin: z.enum(['WHATSAPP', 'FORM', 'API', 'IMPORT', 'MANUAL']),
  pipelineId: z.string().uuid().optional(),
});
```

## Validação no Cliente

- Validação em tempo real (onBlur)
- Mensagens de erro claras em português
- Campos obrigatórios com asterisco
- Feedback visual (borda vermelha, ícone)

## Validação no Servidor

- Validação final antes de persistir
- Erros retornados com field-specific messages
- Frontend exibe erros nos campos correspondentes

## Responsabilidades

- Validar dados antes de enviar ao backend
- Exibir mensagens de erro claras
- Prevenir envio de dados inválidos
- Sincronizar validação cliente/servidor

## Dependências

- [01-backend/Overview.md](../01-backend/Overview.md) — Validação server-side
- [Forms.md](./Forms.md) — Formulários

## Regras

- Todo input de usuário deve ser validado
- Mensagens de erro em português
- Máximo de 3 erros visíveis por vez
- Validação não deve bloquear o usuário
- Erros de servidor são exibidos nos campos

## Futuras Melhorias

- Schemas compartilhados (frontend/backend via JSON Schema)
- Validação cross-field (senha = confirmação)
- Validação de CPF/CNPJ customizada
- Validação de telefone por país
- Sanitização de HTML (XSS prevention)

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
