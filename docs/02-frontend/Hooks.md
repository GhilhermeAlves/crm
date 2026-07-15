# Hooks — Custom React Hooks

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Hooks de Dados](#hooks-de-dados)
- [Hooks de UI](#hooks-de-ui)
- [Hooks de Autenticação](#hooks-de-autenticação)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar todos os custom hooks React utilizados no frontend.

## Descrição

Hooks encapsulam lógica reutilizável. São organizados por responsabilidade: dados, UI e autenticação.

## Hooks de Dados

| Hook | Arquivo | Descrição |
|---|---|---|
| `useLeads` | `hooks/useLeads.ts` | CRUD de leads com React Query |
| `useContacts` | `hooks/useContacts.ts` | CRUD de contatos |
| `useConversations` | `hooks/useConversations.ts` | Lista de conversas |
| `useMessages` | `hooks/useMessages.ts` | Mensagens de uma conversa |
| `usePipeline` | `hooks/usePipeline.ts` | Dados do pipeline |
| `useDashboard` | `hooks/useDashboard.ts` | KPIs do dashboard |
| `useCampaigns` | `hooks/useCampaigns.ts` | CRUD de campanhas |
| `useReports` | `hooks/useReports.ts` | Geração de relatórios |

### Padrão

```typescript
export function useLeads(filters?: LeadFilters) {
  const queryClient = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ['leads', filters],
    queryFn: () => api.get('/leads', { params: filters }),
  });

  const createLead = useMutation({
    mutationFn: api.post('/leads'),
    onSuccess: () => queryClient.invalidateQueries(['leads']),
  });

  return { leads: data, isLoading, error, createLead };
}
```

## Hooks de UI

| Hook | Arquivo | Descrição |
|---|---|---|
| `useDebounce` | `hooks/useDebounce.ts` | Debounce de valores |
| `usePagination` | `hooks/usePagination.ts` | Controle de paginação |
| `useLocalStorage` | `hooks/useLocalStorage.ts` | Persistência local |
| `useMediaQuery` | `hooks/useMediaQuery.ts` | Queries de mídia |
| `useClickOutside` | `hooks/useClickOutside.ts` | Click fora do elemento |
| `useKeyPress` | `hooks/useKeyPress.ts` | Atalhos de teclado |
| `useWebSocket` | `hooks/useWebSocket.ts` | Conexão WebSocket |
| `useInfiniteScroll` | `hooks/useInfiniteScroll.ts` | Scroll infinito |

## Hooks de Autenticação

| Hook | Arquivo | Descrição |
|---|---|---|
| `useAuth` | `hooks/useAuth.ts` | Login, logout, sessão |
| `usePermission` | `hooks/usePermission.ts` | Verificação de permissão |
| `useRole` | `hooks/useRole.ts` | Verificação de role |

## Responsabilidades

- Encapsular lógica complexa reutilizável
- Separar lógica de apresentação
- Facilitar testes unitários
- Promover reutilização entre componentes

## Dependências

- [Context.md](./Context.md) — Providers de contexto
- [01-backend/Overview.md](../01-backend/Overview.md) — API endpoints
- [00-core/CodingStandards.md](../00-core/CodingStandards.md) — Padrões de codificação

## Regras

- Um hook por arquivo
- Nome always com prefixo `use`
- Hooks devem retornar objeto com props nomeadas
- Não aninhar hooks condicionalmente
- Usar React Query para server state
- Usar React Context + useState/useReducer para client state

## Futuras Melhorias

- Hooks de WebSocket reutilizáveis
- Hooks de cache otimísticos
- Hooks de form com validação integrada
- Hooks de debounce automáticos

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
