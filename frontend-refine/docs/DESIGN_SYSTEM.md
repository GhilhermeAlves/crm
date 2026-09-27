# Design System - CRM Vibrante

## Paleta de Cores

### Primárias
- **Blue Base:** `--crm-primary` (globals.css, HSL; ~`#3b82f6` no light, mais claro no dark) — Botões, ação principal
- **Blue Dark:** `#1e40af` — Hover state, gradientes
- **Blue Darker:** `#1e3a8a` — Active state

### Gradientes Primários
- **Gradient Primary:** `linear-gradient(135deg, #3b82f6 0%, #1e40af 100%)` — Botões e ações principais
- **Gradient Primary Hover:** `linear-gradient(135deg, #1e40af 0%, #1e3a8a 100%)` — Estados de hover

### Secundárias
- **Purple:** `#a855f7` — Items ativos, secondary actions
- **Indigo:** `#6366f1` — Complemento, accents
- **Gradient Secondary:** `linear-gradient(135deg, #a855f7 0%, #6d28d9 100%)`

### Terciárias
- **Cyan:** `#06b6d4` — Complemento, accents
- **Pink:** `#ec4899` — Complemento visual
- **Amber:** `#f59e0b` — Complemento visual
- **Gradient Cyan:** `linear-gradient(135deg, #06b6d4 0%, #0369a1 100%)`

### Status Semânticos
- **Success:** `--crm-success` (globals.css, HSL `142 71% 45%`) → Deals ganhos, status positive. Gradiente: `#10b981 → #065f46`
- **Warning:** `#ea580c` → Avisos, atenção (escurecido de `#f97316` para melhor contraste com ícone branco)
- **Error:** `#ef4444` → Erros, status negativo
- **Info:** `#14b8a6` → Informações, novos leads

Cada cor semântica também possui um gradiente correspondente em `--crm-gradient-{semantic}`.

### Superfícies
- **Surface BG (Light):** `#ffffff` — Fundo padrão
- **Surface Light:** `#f8fafc` — Fundo secundário
- **Surface Sidebar:** `#1e293b` — Sidebar base (dark mode)
- **Surface Sidebar Dark:** `#0f172a` — Sidebar mais escuro (dark mode)

### Bordas
- **Border:** `--crm-border` (globals.css, HSL; troca automaticamente em `.dark`) — Bordas padrão
- **Border Dark:** `#334155` — Bordas em dark mode

## Componentes Tier 1 (Refatorados)

### Button

Componente: `src/components/ui/button.tsx`

**Variantes disponíveis:**
- **default:** Gradiente azul (`from-crm-primary to-crm-primary-dark`), texto sempre branco (light e dark), hover lift (-translate-y-0.5) e sombra aumentada
- **crm:** Azul sólido com hover background
- **destructive:** Gradiente vermelho padrão
- **danger:** Gradiente vermelho (crm-error) com hover lift
- **outline:** Borda 1px neutra (`border-input`) em repouso; fica azul (`hover:border-crm-primary`) apenas no hover, com hover background
- **secondary:** Background secundário com hover lift
- **ghost:** Transparente com hover background
- **link:** Texto com underline

**Tamanhos:**
- **default:** h-10 px-4 py-2
- **sm:** h-9 px-3
- **lg:** h-11 px-8
- **icon:** h-10 w-10

**Props:**
- `variant`: um dos valores acima (default: "default")
- `size`: um dos tamanhos acima (default: "default")
- `asChild`: permite usar como Slot via @radix-ui/react-slot

**Estados:**
- Focus: ring-2 ring-ring com offset
- Disabled: opacity-50, sem sombra, sem translate
- Hover: lift (-translate-y-0.5), sombra aumentada
- Active: volta ao nível de base (translate-y-0), sombra reduzida

**Transição:** duration-crm-default (300ms) ease-in-out

---

### Input

Componente: `src/components/ui/input.tsx`

**Props:**
- `error?: boolean` — Aplica estilo de erro
- Suporta todas as props padrão de `<input>`

**Estados:**
- **Base:** border (1px) border-input, bg-background
- **Focus:** border-crm-primary + ring-2 ring-crm-primary/20 (a ênfase vem do ring, a largura da borda não muda, sem layout shift)
- **Error (via `error` prop ou `aria-invalid`):** border-crm-error, ring-crm-error/20

**Transição:** all 300ms ease (duration-crm-default)

**Outros:**
- Suporta `file:` pseudo-element para inputs type=file
- Disabled: opacity-50, cursor-not-allowed

---

### Card

Componente: `src/components/ui/card.tsx`

**Props:**
- `accent?: "none" | "primary" | "purple" | "success" | "warning"` — Variante visual (default: "none")

**Variantes (accent):**
- **none:** Sem decoração especial, apenas sombra base
- **primary:** Border-left azul (4px), gradiente subtle azul (5% opacity)
- **purple:** Border-left roxo (4px), gradiente subtle roxo (5% opacity)
- **success:** Border-left verde (4px), gradiente subtle verde (5% opacity)
- **warning:** Border-left laranja (4px), gradiente subtle laranja (5% opacity)

O Card não tem hover lift embutido. Cards puramente de exibição (ex.: KPIs do dashboard) podem adicionar `hover:-translate-y-1 hover:shadow-crm-lg` via `className`; cards com filhos interativos (botões, links) não devem usar lift.

**Sub-componentes:**
- `Card` — Container principal
- `CardHeader` — Seção de cabeçalho (p-6)
- `CardTitle` — Título (h3, 2xl bold)
- `CardDescription` — Subtítulo (p, sm, muted-foreground)
- `CardContent` — Conteúdo principal (p-6 pt-0)
- `CardFooter` — Rodapé (flex items-center p-6 pt-0)

**Transição:** all 300ms ease (duration-crm-default)

---

### PageTitle

Componente: `src/components/common/PageTitle.tsx`

**Props:**
- `children: ReactNode` — Conteúdo do título
- `className?: string` — Classes adicionais
- `as?: "h1" | "h2"` — Tag HTML (default: "h1")

**Estilo:**
- Gradiente multi-cor: from-crm-secondary-purple via-crm-secondary-indigo to-crm-tertiary-cyan
- `bg-clip-text` com `text-transparent` para renderizar o gradiente
- Tamanho: text-2xl (lg: text-3xl)
- Font: bold, tracking-tight
- Renderiza como h1 ou h2 conforme `as` prop

---

### Sidebar

Componentes: `src/components/layout/Sidebar.tsx`, `SidebarGroup.tsx`, `SidebarItem.tsx`

**Background:**
- Gradiente: from-slate-800 to-slate-900
- Desktop: w-64 normal, w-16 collapsed, transição duration-crm-default
- Mobile: Sheet com w-64, sem border

**Elementos:**

**Logo (SidebarContent):**
- Gradiente badge: from-crm-secondary-purple to-crm-secondary-indigo
- Exibido como "C" quando collapsed, "CRM" quando expandido

**Grupos (SidebarGroup):**
- Título (se não collapsed): texto uppercase, xs, tracking-wider, slate-400
- Separador visual quando collapsed
- Pode ser expandido/colapsado via chevron

**Items (SidebarItem):**
- **Base:** text-slate-200, rounded-md, py-2 px-3
- **Hover:** hover:bg-purple-500/20, hover:text-white, hover:translate-x-1
- **Active:** bg-gradient-to-r from-crm-secondary-purple to-crm-secondary-indigo, text-white, shadow-crm-lg
- **Collapsed:** Ícone centrado em w-16, com tooltip ao hover
- Transição: all duration-crm-default ease-in-out

**Footer:**
- Botão "Sair" com LogOut icon
- Mesmo styling que items (ghost variant)

---

### Header

Componente: `src/components/layout/Header.tsx`

**Estrutura:**
- h-14, flex items-center, bg-card, border-b, shadow-sm
- Gradiente decorativo no topo: from-crm-secondary-purple via-crm-secondary-indigo to-crm-tertiary-cyan (h-0.5)

**Elementos:**
1. **Mobile Menu Button:** Button ghost icon, h-9 w-9, lg:hidden
2. **Desktop Sidebar Toggle:** Button ghost icon, h-9 w-9, hidden lg:inline-flex
3. **Breadcrumb:** Hidden em mobile, lg:block
4. **Spacer:** flex-1
5. **Notification Bell:** NotificationBell component
6. **Theme Toggle:** ThemeToggle component
7. **User Menu:** UserMenu component

**Espaçamento:** gap-4, px-4 (mobile), px-6 (lg)

---

## Sombras

Todos os componentes usam sombras do design token:
- **shadow-crm-sm:** 0 1px 2px rgba(0,0,0,0.05)
- **shadow-crm-md:** 0 4px 6px rgba(0,0,0,0.1)
- **shadow-crm-lg:** 0 10px 15px rgba(0,0,0,0.1)
- **shadow-crm-xl:** 0 20px 25px rgba(0,0,0,0.1)

Padrão para cards e componentes elevados: `shadow-sm`, hover em alguns: `shadow-crm-lg`

---

## Animações & Transições

Definidas em `design-tokens.css`:
- **Fast:** 200ms (focus states, quick interactions)
- **Default:** 300ms (hover effects, component transitions)
- **Slow:** 500ms (modals, major changes)

Aplicadas via `transition-all duration-crm-{timing} ease-in-out`

---

## Acessibilidade de Cores

Todos os componentes implementam estados visuais além de cores:
- **Button:** Hover lift (-translate-y-0.5) em gradientes
- **Card:** Hover transform (-translate-y-1) + sombra apenas em cards de exibição (KPIs)
- **SidebarItem:** Hover translate-x-1, active gradient bold + shadow
- **Input:** Focus ring + border color change, error border change + ring

---

## CSS Variables

Há dois conjuntos de tokens, sem nomes em comum:

1. **`src/styles/globals.css`** (sistema HSL original, shadcn): `--crm-primary`, `--crm-primary-hover`, `--crm-primary-active`, `--crm-primary-foreground`, `--crm-secondary`, `--crm-text`, `--crm-text-secondary`, `--crm-border`, `--crm-background`, `--crm-surface`, `--crm-danger`, `--crm-success`. Valores light em `:root`, dark em `.dark`.
2. **`src/styles/design-tokens.css`** (redesign), importado no topo de globals.css. Cores como triplets HSL (sem `hsl()`), para suportar modificadores de opacidade do Tailwind:

```css
--crm-primary-dark: 226 71% 40%       /* #1e40af */
--crm-primary-darker: 224 64% 33%     /* #1e3a8a */
--crm-secondary-purple: 271 91% 65%   /* #a855f7 */
--crm-secondary-indigo: 239 84% 67%   /* #6366f1 */
--crm-tertiary-cyan: 189 94% 43%      /* #06b6d4 */
--crm-tertiary-pink: 330 81% 60%      /* #ec4899 */
--crm-tertiary-amber: 38 92% 50%      /* #f59e0b */
--crm-warning: 21 90% 48%             /* #ea580c */
--crm-error: 0 84% 60%                /* #ef4444 */
--crm-info: 173 80% 40%               /* #14b8a6 */
--crm-surface-bg / --crm-surface-light / --crm-surface-sidebar / --crm-surface-sidebar-dark
--crm-border-dark: 215 25% 27%        /* #334155 */
--crm-gradient-{primary,primary-hover,secondary,cyan,success,warning,error,info}: linear-gradient(...)
--crm-shadow-sm/md/lg/xl, --crm-transition-fast/default/slow
```

Primário e sucesso NÃO são redefinidos em design-tokens.css: usam `--crm-primary` / `--crm-success` de globals.css.

**Dark mode:** a app usa `darkMode: ["class"]` (next-themes). Os overrides de design-tokens.css ficam em `.dark` (hoje: `--crm-surface-bg`, `--crm-surface-light`); não há `prefers-color-scheme`. `--crm-primary` e `--crm-border` mudam no dark via globals.css. Os gradientes são fixos nos dois temas.

---

## Tailwind Classes

Custom utilities disponíveis via `tailwind.config.ts`:

**Cores:**
- `bg-crm-primary` (aninhado `crm.primary`, HSL de globals.css), `bg-crm-primary-dark`, `bg-crm-primary-darker`
- `bg-crm-secondary-purple`, `bg-crm-secondary-indigo`
- `bg-crm-tertiary-{cyan,pink,amber}`
- `bg-crm-success`, `bg-crm-warning`, `bg-crm-error`, `bg-crm-info`
- `text-crm-{colors}` e variantes
- Todas aceitam opacidade (`/5`, `/10`, `/20`): os tokens de globals.css via `hsl(var(--x))` e os de design-tokens.css via `hsl(var(--x) / <alpha-value>)`

**Gradientes:**
- `bg-crm-gradient-primary`, `bg-crm-gradient-secondary`
- `bg-crm-gradient-cyan`, `bg-crm-gradient-{semantic}`

**Sombras:**
- `shadow-crm-sm`, `shadow-crm-md`, `shadow-crm-lg`, `shadow-crm-xl`

**Transições:**
- `duration-crm-fast` (200ms), `duration-crm-default` (300ms), `duration-crm-slow` (500ms)

---

## TypeScript

Exports e tipos em `src/lib/design-system.ts`:
```typescript
export const designTokens = {
  colors: { primary, secondary, tertiary, semantic },
  gradients: { primary, secondary, cyan, success, warning, error, info },
  shadows: { sm, md, lg, xl },
  transitions: { fast, default, slow },
}
```

Útil para acesso programático ao design tokens em componentes e utilitários.

---

## Próximos Passos (Tier 2)

- [ ] Tabelas com hover effects e status colors
- [ ] Modais/Dialogs com gradientes de header
- [ ] Alerts/Banners com semantic colors
- [ ] Badges com cores semânticas
- [ ] Form controls avançados (Select, Checkbox, Radio)
- [ ] Toast/Notifications com animações

---

## Referências

- **Token Definition:** `src/styles/design-tokens.css`
- **TypeScript Constants:** `src/lib/design-system.ts`
- **Tailwind Config:** `tailwind.config.ts`
- **Component Examples:**
  - Button: `src/components/ui/button.tsx`
  - Input: `src/components/ui/input.tsx`
  - Card: `src/components/ui/card.tsx`
  - Sidebar: `src/components/layout/Sidebar*.tsx`
  - Header: `src/components/layout/Header.tsx`
  - PageTitle: `src/components/common/PageTitle.tsx`
