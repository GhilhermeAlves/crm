# Tables — Componentes de Tabela

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [DataTable](#datatable)
- [Funcionalidades](#funcionalidades)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o componente DataTable genérico utilizado em toda a aplicação.

## Descrição

DataTable é um componente reutilizável baseado em TanStack Table. Suporta sorting, filtering, pagination, selection e export.

## DataTable

### Props

```typescript
interface DataTableProps<T> {
  data: T[];
  columns: ColumnDef<T>[];
  isLoading?: boolean;
  pagination?: PaginationState;
  onPaginationChange?: (page: number, pageSize: number) => void;
  onSort?: (column: string, direction: 'asc' | 'desc') => void;
  onFilter?: (filters: Record<string, any>) => void;
  onRowClick?: (row: T) => void;
  selectable?: boolean;
  onSelectionChange?: (selected: T[]) => void;
  searchable?: boolean;
  exportable?: boolean;
  emptyMessage?: string;
}
```

### Funcionalidades

| Funcionalidade | Descrição |
|---|---|
| Sorting | Click no header para ordenar |
| Filtering | Filtros por coluna ou busca global |
| Pagination | Server-side pagination |
| Selection | Checkbox para selecionar múltiplas rows |
| Export | Export para CSV/Excel |
| Responsive | Colunas responsivas em mobile |
| Loading | Skeleton loading state |
| Empty state | Estado vazio customizável |

## Responsabilidades

- Exibir dados em formato tabular
- Suportar operações de dados (sort, filter, page)
- Ser reutilizável em todos os módulos
- Performar bem com muitas linhas
- Ser acessível (WCAG)

## Dependências

- [Components.md](./Components.md) — Componentes base
- [00-core/CodingStandards.md](../00-core/CodingStandards.md) — Padrões

## Regras

- Tabelas devem ser server-side (não carregar tudo no cliente)
- Máximo de 20 items por página (default)
- Colunas responsivas em mobile (hidden/sm)
- Empty state sempre exibido quando sem dados
- Loading state com skeleton
- Acessibilidade: headers th, roles ARIA

## Futuras Melhorias

- Column resize
- Column reorder
- Virtual scrolling para listas grandes
- Inline editing
- Column presets salvos pelo usuário
- Drag-and-drop de colunas

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
