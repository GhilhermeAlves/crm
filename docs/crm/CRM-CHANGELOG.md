# CRM CHANGELOG

> Histórico oficial de evolução do CRM.
>
> Sempre registrar uma entrada quando uma evolução importante do CRM for concluída.
>
> **Formato:**
>
> ```markdown
> ## YYYY-MM-DD — [Nome da alteração]
>
> ### Frontend
> -
>
> ### Backend
> -
>
> ### Banco
> -
>
> ### API
> -
>
> ### Permissões
> -
>
> ### Componentes reutilizados
> -
>
> ### Novos componentes
> -
>
> ### Impacto funcional
> -
>
> ### Observações
> -
> ```

---

## 2026-09-03 — Implementação da Página inicial do CRM

### Frontend
- Criação da ``Página inicial do CRM`` (`/crm`) como ponto central de acesso aos módulos comerciais.
- Criada a rota `src/app/(dashboard)/crm/page.tsx`.
- Criado o componente de navegação específico `src/features/crm/components/CrmModuleCard.tsx`.

### Backend
- Sem alterações de código.

### Banco
- Nenhuma alteração/migration.

### API
- Nenhum endpoint novo (página essencialmente de navegação).

### Permissões
- Nenhuma permissão nova. Reutilizadas: `contact:page:view`, `pipeline:page:view`, `lead:page:view`, `activity:page:view`, `analytics:read`.

### Componentes reutilizados
- `PageTitle` (`src/components/common/PageTitle.tsx`).
- `Card`, `CardContent` (`src/components/ui/card.tsx`).
- `useAuthorization` (`src/features/auth/hooks/useAuthorization.ts`).
- Ícones de `lucide-react` (mesmo padrão visual do projeto).
- Layout do dashboard (`(dashboard)` + `DashboardLayout`) — sem alterações.
- Padrões visuais: `space-y-6`, grid responsivo, loading/empty state (consistente com páginas existentes).

### Novos componentes
- `CrmModuleCard` (card de navegação específico da tela, seguindo o padrão visual existente).

### Impacto funcional
- Nova página `/crm` como entrada dos módulos CRM (Contatos, Negociações, Leads, Contas, Projetos de clientes, Atividades, Painel de vendas).
- Módulos existentes são linkáveis; módulos ainda inexistentes (Contas, Projetos de clientes) ficam como cards "Em breve" preparados para rotas futuras, sem inventar dados/endpoints.

### Observações
- `Contas` (`/accounts`) e `Projetos de clientes` (`/projects`) ainda não existem; cards preparados para integração futura.
- "Painel de vendas" aponta para `/reports` (painel analítico existente com indicadores de desempenho comercial).
- Sidebar não foi alterada; incluir um link de menu para `/crm` é um próximo passo opcional.

---

## 2026-09-03 — Criação da documentação oficial do CRM

### Frontend
- Criação da documentação oficial de arquitetura, changelog e decisões do CRM.
- Sem alterações de código.

### Backend
- Sem alterações de código.

### Banco
- Nenhuma alteração/migration.

### API
- Nenhuma alteração de endpoint.

### Permissões
- Nenhuma alteração.

### Componentes reutilizados
- Nenhuma alteração.

### Novos componentes
- Nenhum.

### Impacto funcional
- Registro do estado atual conhecido do CRM como referência para evolução futura.
- Estabelecimento do `CRM-ARCHITECTURE.md`, `CRM-CHANGELOG.md` e `CRM-DECISIONS.md` como fonte oficial.

### Observações
- Validação de componentes (Tabs, Pagination, Combobox/Command):
  - **Tabs:** Radix (shadcn) instalado, mas sem wrapper reutilizável próprio confirmado → `BIBLIOTECA EXISTENTE / COMPONENTE DO PROJETO AUSENTE`.
  - **Pagination:** sem componente/padrão genérico reutilizável → `AUSENTE`.
  - **Combobox/Command:** cmdk instalado, mas sem componente reutilizável próprio confirmado → `BIBLIOTECA EXISTENTE / COMPONENTE DO PROJETO AUSENTE`.
- Confirmação de que `companies` representa o tenant do SaaS e deve ser tratado de forma distinta de uma futura entidade `Account` (conta de cliente do CRM).
