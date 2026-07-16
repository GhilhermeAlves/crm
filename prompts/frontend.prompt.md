# Frontend Prompt - React/Next.js

## Quando utilizar

- Criando ou modificando componentes React/Next.js
- Implementando novas páginas ou hooks
- Modificando a UI do sistema

## Objetivo

Implementar frontend seguindo boas práticas com Next.js 15, React, TypeScript e Tailwind CSS.

## Entrada esperada

- Nome do módulo (ex: `user`, `product`, `order`)
- Requisitos da UI (componentes necessários, layout, interações)

## Resultado esperado

- Componentes React reutilizáveis
- Páginas Next.js com Server Components
- Hooks customizados
- Tipos TypeScript

## Arquivos normalmente envolvidos

```
frontend/src/
  ├── app/
  │   └── {module}/
  │       ├── page.tsx
  │       ├── loading.tsx
  │       ├── error.tsx
  │       └── {sub-route}/
  │           └── page.tsx
  ├── components/
  │   └── {module}/
  │       ├── {Component}.tsx
  │       ├── {Component}Form.tsx
  │       └── index.ts
  ├── hooks/
  │   └── use{Module}.ts
  ├── types/
  │   └── {module}.ts
  └── lib/
      ├── api.ts
      └── utils.ts
```

## Boas práticas

- **Server Components por padrão**: Usar Server Components para dados e Server Actions para mutações.
- **Client Components só quando necessário**: Só usar `"use client"` para interatividade (eventos, state local, effects).
- **React Query para server state**: Usar `@tanstack/react-query` para cache e sincronização de dados.
- **Zod para validação**: Validar inputs com schemas Zod tanto no client quanto no server.
- **Componentes <200 linhas**: Se um componente passar de 200 linhas, decompor em sub-componentes.
- **Tailwind CSS**: Usar apenas classes Tailwind, sem CSS customizado.
- **Server Actions**: Usar para mutações de dados (POST, PUT, DELETE).
- **Error boundaries**: Sempre implementar `error.tsx` em rotas.
- **Loading states**: Sempre implementar `loading.tsx` em rotas.
- **Tipagem forte**: Nunca usar `any`. Sempre tipar props, state e responses.

## Exemplo de uso

```
Criar a página de listagem de Products com:
- Tabela com colunas: Name, Category, Price, Status, Actions
- Filtros por category e status
- Paginação
- Botão de criar novo produto
- Modal de confirmação para deletar
- Server Component para carregar dados
```
