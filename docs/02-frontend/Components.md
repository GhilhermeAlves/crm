# Components — Biblioteca de Componentes

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Componentes Base (Shadcn)](#componentes-base-shadcn)
- [Componentes de Layout](#componentes-de-layout)
- [Componentes Compartilhados](#componentes-compartilhados)
- [Componentes por Módulo](#componentes-por-módulo)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar todos os componentes React utilizados no frontend, incluindo base, layout, compartilhados e de módulo.

## Descrição

Componentes são organizados em três camadas: base (Shadcn), layout (estrutura visual) e feature (específicos de módulo).

## Componentes Base (Shadcn)

Componentes do Shadcn UI, instalados localmente e customizáveis.

| Componente | Caminho | Descrição |
|---|---|---|
| Button | `ui/button.tsx` | Botão (variantes: default, destructive, outline, ghost) |
| Input | `ui/input.tsx` | Campo de texto |
| Textarea | `ui/textarea.tsx` | Área de texto |
| Select | `ui/select.tsx` | Dropdown de seleção |
| Dialog | `ui/dialog.tsx` | Modal/Dialog |
| Sheet | `ui/sheet.tsx` | Drawer lateral |
| Dropdown | `ui/dropdown-menu.tsx` | Menu dropdown |
| Tabs | `ui/tabs.tsx` | Abas |
| Card | `ui/card.tsx` | Card container |
| Badge | `ui/badge.tsx` | Badge/Tag |
| Avatar | `ui/avatar.tsx` | Avatar do usuário |
| Tooltip | `ui/tooltip.tsx` | Tooltip |
| Popover | `ui/popover.tsx` | Popover |
| Command | `ui/command.tsx` | Command palette |
| Form | `ui/form.tsx` | Formulário com validação |
| Table | `ui/table.tsx` | Tabela |
| Skeleton | `ui/skeleton.tsx` | Loading skeleton |
| Separator | `ui/separator.tsx` | Separador |
| Switch | `ui/switch.tsx` | Toggle switch |
| Checkbox | `ui/checkbox.tsx` | Checkbox |
| RadioGroup | `ui/radio-group.tsx` | Radio group |
| DatePicker | `ui/date-picker.tsx` | Seletor de data |
| Pagination | `ui/pagination.tsx` | Paginação |
| ScrollArea | `ui/scroll-area.tsx` | Área com scroll |
| Toast | `ui/toast.tsx` | Notificação toast |
| Alert | `ui/alert.tsx` | Alerta |
| Progress | `ui/progress.tsx` | Barra de progresso |
| Accordion | `ui/accordion.tsx` | Accordion |

## Componentes de Layout

| Componente | Caminho | Descrição |
|---|---|---|
| AppLayout | `layout/AppLayout.tsx` | Layout principal |
| Sidebar | `layout/Sidebar.tsx` | Menu lateral |
| Header | `layout/Header.tsx` | Cabeçalho |
| Footer | `layout/Footer.tsx` | Rodapé |
| PageHeader | `layout/PageHeader.tsx` | Título da página |
| Breadcrumbs | `layout/Breadcrumbs.tsx` | Navegação hierárquica |
| ThemeToggle | `layout/ThemeToggle.tsx` | Toggle dark/light |

## Componentes Compartilhados

| Componente | Caminho | Descrição |
|---|---|---|
| DataTable | `shared/DataTable.tsx` | Tabela genérica com sorting/filtering |
| SearchInput | `shared/SearchInput.tsx` | Campo de busca |
| StatusBadge | `shared/StatusBadge.tsx` | Badge de status colorido |
| LoadingSpinner | `shared/LoadingSpinner.tsx` | Indicador de carregamento |
| EmptyState | `shared/EmptyState.tsx` | Estado vazio |
| ErrorBoundary | `shared/ErrorBoundary.tsx` | Captura de erros |
| ConfirmDialog | `shared/ConfirmDialog.tsx` | Confirmação de ação |
| FileUpload | `shared/FileUpload.tsx` | Upload de arquivos |
| RichTextEditor | `shared/RichTextEditor.tsx` | Editor de texto rico |
| InfiniteScroll | `shared/InfiniteScroll.tsx` | Scroll infinito |

## Componentes por Módulo

### Leads
- LeadCard, LeadForm, LeadList, LeadDetail, LeadFilter

### Chat
- ChatSidebar, ChatWindow, MessageBubble, MessageInput, ChatHeader

### Pipeline
- PipelineBoard, OpportunityCard, StageColumn, OpportunityDetail

### Campaigns
- CampaignCard, CampaignForm, CampaignMetrics, CampaignPreview

## Responsabilidades

- Reutilizabilidade entre módulos
- Consistência visual
- Acessibilidade (WCAG 2.1 AA)
- Performance (lazy loading quando possível)
- Documentação com exemplos de uso

## Dependências

- [Theme.md](./Theme.md) — Design tokens
- [00-core/CodingStandards.md](../00-core/CodingStandards.md) — Padrões de codificação

## Regras

- Componentes devem ser < 200 lines
- Props devem ser tipadas com TypeScript
- Componentes devem ser tested com React Testing Library
- Não usar `any` em props
- Preferir composição sobre configuração
- Cada componente em seu próprio arquivo

## Futuras Melhorias

- Storybook para documentação interativa
- Visual regression tests
- A/B testing de componentes
- Design tokens compartilhados com backend
- Micro-frontends para módulos independentes

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
