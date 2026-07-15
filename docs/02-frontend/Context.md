# Context — React Context Providers

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Providers](#providers)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar os React Context Providers utilizados para compartilhamento de estado global.

## Descrição

Providers encapsulam estado e lógica que precisa ser acessível em toda a aplicação. São usados no root layout do Next.js.

## Providers

### AuthProvider

**Responsabilidade**: Gerenciar autenticação, sessão e dados do usuário.

```typescript
interface AuthContextType {
  user: User | null;
  company: Company | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  refreshSession: () => Promise<void>;
}
```

**Uso**: `app/layout.tsx`

### ThemeProvider

**Responsabilidade**: Gerenciar tema (dark/light) e aplicar CSS variables.

```typescript
interface ThemeContextType {
  theme: 'light' | 'dark' | 'system';
  setTheme: (theme: 'light' | 'dark' | 'system') => void;
  resolvedTheme: 'light' | 'dark';
}
```

**Uso**: `app/layout.tsx`

### QueryProvider

**Responsabilidade**: Configurar React Query (TanStack Query) para server state.

```typescript
// Configuração padrão
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      retry: 2,
      refetchOnWindowFocus: false,
    },
  },
});
```

**Uso**: `app/layout.tsx`

### WebSocketProvider

**Responsabilidade**: Gerenciar conexões WebSocket para dados em tempo real.

```typescript
interface WebSocketContextType {
  isConnected: boolean;
  subscribe: (event: string, callback: Function) => void;
  unsubscribe: (event: string, callback: Function) => void;
}
```

**Uso**: `providers/WebSocketProvider.tsx` (envolto por AuthProvider)

### NotificationProvider

**Responsabilidade**: Gerenciar notificações in-app e push.

```typescript
interface NotificationContextType {
  notifications: Notification[];
  unreadCount: number;
  markAsRead: (id: string) => void;
  markAllAsRead: () => void;
}
```

**Uso**: `providers/NotificationProvider.tsx` (envolto por WebSocketProvider)

## Responsabilidades

- Fornecer estado global para a aplicação
- Gerenciar configurações compartilhadas
- Facilitar acesso a dados de autenticação
- Suportar atualizações em tempo real

## Dependências

- [01-backend/Auth.md](../01-backend/Auth.md) — Autenticação
- [01-backend/Notifications.md](../01-backend/Notifications.md) — Notificações

## Regras

- Providers devem ser aninhados no menor escopo possível
- Evitar providers desnecessários (causam re-renders)
- Usar `memo` e `useCallback` para otimizar
- Não colocar dados de servidor em Context (usar React Query)
- Providers de layout são server components (exceto os que usam hooks)

## Futuras Melhorias

- Contexts lazy-loaded por módulo
- DevTools para debug de contextos
- Context partitioning para performance

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
