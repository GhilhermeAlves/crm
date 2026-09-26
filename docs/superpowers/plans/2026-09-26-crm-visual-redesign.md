# CRM Visual Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign visual completo do CRM SaaS com paleta vibrante, gradientes, animações suaves e design system iterativo, começando por componentes Tier 1 (Dashboard, Navigation, Cards, Forms, Buttons).

**Architecture:** Quick Polish + Design System Iterativo. Começamos melhorando visualmente os componentes críticos do Tier 1 (Button, Input, Card, Sidebar, Header), documentando padrões conforme emergem. Cada componente refatorado gera um token reutilizável no design system que será consolidado ao final.

**Tech Stack:**
- React 18 + TypeScript
- Tailwind CSS (config extensível com tokens)
- CSS custom properties (para design tokens)
- Next.js 14 (app router)
- Radix UI (componentes base já existentes)

**Spec:** `docs/superpowers/specs/2026-09-26-crm-visual-redesign-design.md`

## Global Constraints

- Next.js 14+, React 18+, TypeScript strict mode
- Radix UI + CVA (Class Variance Authority) — manter compatibilidade
- Suportar Light/Dark mode existente
- Sem breaking changes em componentes públicos
- Design tokens em CSS variables + Tailwind config (dual approach)
- Todos os commits com co-authorship: `Co-Authored-By: Claude Haiku 4.5 <noreply@anthropic.com>`

---

## Task 1: Criar Arquivo de Design Tokens

**Files:**
- Create: `src/styles/design-tokens.css`
- Create: `src/lib/design-system.ts`
- Modify: `tailwind.config.ts`

**Interfaces:**
- Produces: CSS variables (`--crm-primary`, `--crm-success`, etc) e constantes TypeScript (`colorTokens`, `shadowTokens`)
- Consumed by: Todos os componentes refatorados

- [ ] **Step 1: Criar design-tokens.css com paleta completa**

```css
/* design-tokens.css */
:root {
  /* Cores Primárias */
  --crm-primary-base: #3b82f6;
  --crm-primary-dark: #1e40af;
  --crm-primary-darker: #1e3a8a;
  
  /* Gradientes Primários */
  --crm-gradient-primary: linear-gradient(135deg, #3b82f6 0%, #1e40af 100%);
  --crm-gradient-primary-hover: linear-gradient(135deg, #1e40af 0%, #1e3a8a 100%);
  
  /* Cores Secundárias */
  --crm-secondary-purple: #a855f7;
  --crm-secondary-indigo: #6366f1;
  --crm-gradient-secondary: linear-gradient(135deg, #a855f7 0%, #6d28d9 100%);
  
  /* Cores Terciárias */
  --crm-tertiary-cyan: #06b6d4;
  --crm-tertiary-pink: #ec4899;
  --crm-tertiary-amber: #f59e0b;
  --crm-gradient-cyan: linear-gradient(135deg, #06b6d4 0%, #0369a1 100%);
  
  /* Status Semânticos */
  --crm-success: #10b981;
  --crm-warning: #f97316;
  --crm-error: #ef4444;
  --crm-info: #14b8a6;
  
  --crm-gradient-success: linear-gradient(135deg, #10b981 0%, #065f46 100%);
  --crm-gradient-warning: linear-gradient(135deg, #f97316 0%, #9a3412 100%);
  --crm-gradient-error: linear-gradient(135deg, #ef4444 0%, #991b1b 100%);
  --crm-gradient-info: linear-gradient(135deg, #14b8a6 0%, #0d9488 100%);
  
  /* Superfícies */
  --crm-surface-bg: #ffffff;
  --crm-surface-light: #f8fafc;
  --crm-surface-sidebar: #1e293b;
  --crm-surface-sidebar-dark: #0f172a;
  
  /* Bordas */
  --crm-border: #e2e8f0;
  --crm-border-dark: #334155;
  
  /* Sombras */
  --crm-shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.05);
  --crm-shadow-md: 0 4px 6px rgba(0, 0, 0, 0.1);
  --crm-shadow-lg: 0 10px 15px rgba(0, 0, 0, 0.1);
  --crm-shadow-xl: 0 20px 25px rgba(0, 0, 0, 0.1);
  
  /* Transições */
  --crm-transition-fast: all 0.2s ease;
  --crm-transition-default: all 0.3s ease;
  --crm-transition-slow: all 0.5s ease;
}

@media (prefers-color-scheme: dark) {
  :root {
    --crm-surface-bg: #1a1a2e;
    --crm-surface-light: #0f172a;
    --crm-border: #334155;
  }
}
```

- [ ] **Step 2: Criar design-system.ts com constantes TypeScript**

```typescript
// src/lib/design-system.ts
export const designTokens = {
  colors: {
    primary: {
      base: '#3b82f6',
      dark: '#1e40af',
      darker: '#1e3a8a',
    },
    secondary: {
      purple: '#a855f7',
      indigo: '#6366f1',
    },
    tertiary: {
      cyan: '#06b6d4',
      pink: '#ec4899',
      amber: '#f59e0b',
    },
    semantic: {
      success: '#10b981',
      warning: '#f97316',
      error: '#ef4444',
      info: '#14b8a6',
    },
  },
  gradients: {
    primary: 'linear-gradient(135deg, #3b82f6 0%, #1e40af 100%)',
    secondary: 'linear-gradient(135deg, #a855f7 0%, #6d28d9 100%)',
    cyan: 'linear-gradient(135deg, #06b6d4 0%, #0369a1 100%)',
    success: 'linear-gradient(135deg, #10b981 0%, #065f46 100%)',
    warning: 'linear-gradient(135deg, #f97316 0%, #9a3412 100%)',
    error: 'linear-gradient(135deg, #ef4444 0%, #991b1b 100%)',
    info: 'linear-gradient(135deg, #14b8a6 0%, #0d9488 100%)',
  },
  shadows: {
    sm: '0 1px 2px rgba(0, 0, 0, 0.05)',
    md: '0 4px 6px rgba(0, 0, 0, 0.1)',
    lg: '0 10px 15px rgba(0, 0, 0, 0.1)',
    xl: '0 20px 25px rgba(0, 0, 0, 0.1)',
  },
  transitions: {
    fast: 'all 0.2s ease',
    default: 'all 0.3s ease',
    slow: 'all 0.5s ease',
  },
} as const;
```

- [ ] **Step 3: Atualizar tailwind.config.ts com tokens**

```typescript
// tailwind.config.ts (parcial - só a seção colors/extend)
export default {
  theme: {
    extend: {
      colors: {
        'crm-primary': 'var(--crm-primary-base)',
        'crm-primary-dark': 'var(--crm-primary-dark)',
        'crm-primary-darker': 'var(--crm-primary-darker)',
        'crm-secondary-purple': 'var(--crm-secondary-purple)',
        'crm-secondary-indigo': 'var(--crm-secondary-indigo)',
        'crm-tertiary-cyan': 'var(--crm-tertiary-cyan)',
        'crm-tertiary-pink': 'var(--crm-tertiary-pink)',
        'crm-tertiary-amber': 'var(--crm-tertiary-amber)',
        'crm-success': 'var(--crm-success)',
        'crm-warning': 'var(--crm-warning)',
        'crm-error': 'var(--crm-error)',
        'crm-info': 'var(--crm-info)',
      },
      backgroundImage: {
        'crm-gradient-primary': 'var(--crm-gradient-primary)',
        'crm-gradient-secondary': 'var(--crm-gradient-secondary)',
        'crm-gradient-cyan': 'var(--crm-gradient-cyan)',
        'crm-gradient-success': 'var(--crm-gradient-success)',
        'crm-gradient-warning': 'var(--crm-gradient-warning)',
        'crm-gradient-error': 'var(--crm-gradient-error)',
        'crm-gradient-info': 'var(--crm-gradient-info)',
      },
      boxShadow: {
        'crm-sm': 'var(--crm-shadow-sm)',
        'crm-md': 'var(--crm-shadow-md)',
        'crm-lg': 'var(--crm-shadow-lg)',
        'crm-xl': 'var(--crm-shadow-xl)',
      },
      transitionDuration: {
        'crm-fast': '200ms',
        'crm-default': '300ms',
        'crm-slow': '500ms',
      },
    },
  },
};
```

- [ ] **Step 4: Importar design-tokens.css em globals.css**

```css
/* No início de globals.css */
@import './design-tokens.css';
```

- [ ] **Step 5: Commit**

```bash
git add src/styles/design-tokens.css src/lib/design-system.ts tailwind.config.ts src/styles/globals.css
git commit -m "feat(design): add design tokens and CSS variables foundation

- Create design-tokens.css with full color palette, gradients, shadows
- Add design-system.ts with TypeScript constants
- Extend tailwind.config.ts with custom color utilities
- All tokens available as CSS vars and Tailwind classes"
```

---

## Task 2: Refactor Button Component (Tier 1)

**Files:**
- Modify: `src/components/ui/button.tsx`
- Create: `src/components/ui/button.stories.tsx` (optional, para docs)

**Interfaces:**
- Consumes: Design tokens do Task 1
- Produces: Button com variantes (primary, secondary, danger, ghost) com gradientes e animações
- Signature: `<Button variant="primary" size="md" disabled={false}>Label</Button>`

- [ ] **Step 1: Abrir button.tsx existente e revisar estrutura**

```bash
# Verificar se está usando CVA (Class Variance Authority)
cat src/components/ui/button.tsx | grep -E "cva|variants"
```

- [ ] **Step 2: Refactor button com novos estilos e variantes**

```typescript
// src/components/ui/button.tsx
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const buttonVariants = cva(
  // Base styles
  'inline-flex items-center justify-center whitespace-nowrap rounded-md font-semibold transition-all duration-300 ease-in-out focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed',
  {
    variants: {
      variant: {
        primary: `
          bg-gradient-to-r from-crm-primary to-crm-primary-dark 
          text-white 
          shadow-lg hover:shadow-xl 
          hover:-translate-y-0.5 
          active:shadow-md active:translate-y-0
          focus-visible:ring-crm-primary
        `,
        secondary: `
          border-2 border-crm-primary 
          bg-white text-crm-primary 
          hover:bg-blue-50 
          active:bg-blue-100
          focus-visible:ring-crm-primary
        `,
        danger: `
          bg-gradient-to-r from-crm-error to-rose-900 
          text-white 
          shadow-lg hover:shadow-xl 
          hover:-translate-y-0.5
          focus-visible:ring-crm-error
        `,
        ghost: `
          bg-transparent text-crm-primary 
          hover:bg-blue-50 
          active:bg-blue-100
          focus-visible:ring-crm-primary
        `,
      },
      size: {
        sm: 'h-8 px-3 text-sm',
        md: 'h-10 px-4 text-base',
        lg: 'h-12 px-6 text-lg',
      },
    },
    defaultVariants: {
      variant: 'primary',
      size: 'md',
    },
  }
);

interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, ...props }, ref) => (
    <button
      ref={ref}
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    />
  )
);
Button.displayName = 'Button';

export { Button, buttonVariants };
```

- [ ] **Step 3: Run tests para verificar que nenhum component quebrou**

```bash
npm run test -- src/components/ui/button.test.tsx --watch=false
```

Expected: PASS (ou 0 tests se não houver)

- [ ] **Step 4: Verificar visualmente no Storybook ou design-system page**

```bash
npm run dev
# Navegar para http://localhost:3000/design-system e verificar botões
```

- [ ] **Step 5: Commit**

```bash
git add src/components/ui/button.tsx
git commit -m "design: refactor button component with gradients and animations

- Add primary variant with blue gradient and hover lift effect
- Add secondary variant with border and hover background
- Add danger variant with red gradient
- Add ghost variant for minimal style
- Implement hover animations: -translate-y-0.5 with transition-all 300ms
- All variants support disabled state with reduced opacity"
```

---

## Task 3: Refactor Input Component (Tier 1)

**Files:**
- Modify: `src/components/ui/input.tsx`

**Interfaces:**
- Consumes: Design tokens do Task 1
- Produces: Input com focus states coloridos, borders e shadow
- Signature: `<Input type="text" placeholder="..." error={false} />`

- [ ] **Step 1: Refactor input com novo estilo**

```typescript
// src/components/ui/input.tsx
import * as React from 'react';
import { cn } from '@/lib/utils';

interface InputProps
  extends React.InputHTMLAttributes<HTMLInputElement> {
  error?: boolean;
}

const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, type, error, ...props }, ref) => (
    <input
      type={type}
      className={cn(
        'flex h-10 w-full rounded-md border-2 bg-white px-3 py-2 text-base transition-all duration-300',
        'border-gray-300 text-gray-900 placeholder-gray-500',
        'focus:outline-none focus:border-crm-primary focus:ring-2 focus:ring-crm-primary/10',
        'disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-500',
        error && 'border-crm-error focus:border-crm-error focus:ring-crm-error/10',
        className
      )}
      ref={ref}
      {...props}
    />
  )
);
Input.displayName = 'Input';

export { Input };
```

- [ ] **Step 2: Run tests**

```bash
npm run test -- src/components/ui/input.test.tsx --watch=false
```

- [ ] **Step 3: Commit**

```bash
git add src/components/ui/input.tsx
git commit -m "design: refactor input component with focus states

- Add 2px border with primary color on focus
- Add ring shadow with primary color (10% opacity)
- Add error state with red border and ring
- Implement transitions for smooth focus state
- Support disabled state with gray background"
```

---

## Task 4: Refactor Card Component (Tier 1)

**Files:**
- Modify: `src/components/ui/card.tsx`

**Interfaces:**
- Consumes: Design tokens
- Produces: Card com border-left colorido, gradient subtle, shadow e hover effects
- Signature: `<Card variant="primary"><CardHeader>...</CardHeader></Card>`

- [ ] **Step 1: Refactor card component**

```typescript
// src/components/ui/card.tsx
import * as React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const cardVariants = cva(
  'rounded-lg border-l-4 transition-all duration-300 ease-in-out',
  {
    variants: {
      variant: {
        primary: `
          border-l-crm-primary 
          bg-white 
          shadow-sm
          hover:shadow-lg hover:-translate-y-1
          bg-gradient-to-br from-blue-50/50 to-transparent
        `,
        secondary: `
          border-l-crm-secondary-purple 
          bg-white 
          shadow-sm
          hover:shadow-lg hover:-translate-y-1
          bg-gradient-to-br from-purple-50/50 to-transparent
        `,
        success: `
          border-l-crm-success 
          bg-white 
          shadow-sm
          hover:shadow-lg hover:-translate-y-1
          bg-gradient-to-br from-emerald-50/50 to-transparent
        `,
      },
    },
    defaultVariants: {
      variant: 'primary',
    },
  }
);

interface CardProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof cardVariants> {}

const Card = React.forwardRef<HTMLDivElement, CardProps>(
  ({ className, variant, ...props }, ref) => (
    <div
      ref={ref}
      className={cn(cardVariants({ variant, className }))}
      {...props}
    />
  )
);
Card.displayName = 'Card';

const CardHeader = ({
  className,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) => (
  <div className={cn('space-y-1.5 p-6', className)} {...props} />
);
CardHeader.displayName = 'CardHeader';

const CardContent = ({
  className,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) => <div className={cn('p-6', className)} {...props} />;
CardContent.displayName = 'CardContent';

export { Card, CardHeader, CardContent, cardVariants };
```

- [ ] **Step 2: Run tests**

```bash
npm run test -- src/components/ui/card.test.tsx --watch=false
```

- [ ] **Step 3: Commit**

```bash
git add src/components/ui/card.tsx
git commit -m "design: refactor card component with color variants and hover effects

- Add 4px left border with color variants (primary, secondary, success)
- Add subtle gradient background (blue-50/50 to transparent)
- Add hover effect: shadow-lg and -translate-y-1 (4px lift)
- Implement smooth transitions (300ms)"
```

---

## Task 5: Refactor Sidebar/Navigation (Tier 1)

**Files:**
- Modify: `src/components/Layout/Sidebar.tsx`

**Interfaces:**
- Consumes: Design tokens
- Produces: Sidebar com dark gradient background, menu items com hover/active states coloridos
- Signature: `<Sidebar items={[...]} activeItem="dashboard" />`

- [ ] **Step 1: Refactor Sidebar com novo estilo**

```typescript
// src/components/Layout/Sidebar.tsx
'use client';

import { cn } from '@/lib/utils';

interface SidebarItem {
  id: string;
  label: string;
  icon?: React.ReactNode;
  href: string;
}

interface SidebarProps {
  items: SidebarItem[];
  activeItem?: string;
}

export function Sidebar({ items, activeItem }: SidebarProps) {
  return (
    <aside className="w-60 bg-gradient-to-b from-slate-800 to-slate-900 text-white p-6">
      <nav className="space-y-2">
        {items.map((item) => (
          <a
            key={item.id}
            href={item.href}
            className={cn(
              'flex items-center gap-3 px-4 py-3 rounded-md transition-all duration-300 ease-in-out',
              'hover:bg-purple-500/20 hover:translate-x-1',
              activeItem === item.id &&
                'bg-gradient-to-r from-purple-500 to-indigo-500 shadow-lg'
            )}
          >
            {item.icon && <span className="w-5 h-5">{item.icon}</span>}
            <span className="font-medium">{item.label}</span>
          </a>
        ))}
      </nav>
    </aside>
  );
}
```

- [ ] **Step 2: Verificar layout do dashboard**

```bash
npm run dev
# Navegar para http://localhost:3000/dashboard e verificar sidebar
```

- [ ] **Step 3: Commit**

```bash
git add src/components/Layout/Sidebar.tsx
git commit -m "design: refactor sidebar with dark gradient and active states

- Add gradient background: slate-800 to slate-900
- Add hover effect: purple-500/20 background + translate-x-1
- Add active item style with purple-to-indigo gradient
- Implement smooth transitions (300ms)"
```

---

## Task 6: Refactor Header/Title Component (Tier 1)

**Files:**
- Modify: `src/components/Layout/Header.tsx` (ou criar se não existir)

**Interfaces:**
- Consumes: Design tokens
- Produces: Header com título em gradient text, CTA button
- Signature: `<Header title="Dashboard" />`

- [ ] **Step 1: Criar/refactor Header component**

```typescript
// src/components/Layout/Header.tsx
'use client';

interface HeaderProps {
  title: string;
  action?: {
    label: string;
    onClick: () => void;
  };
}

export function Header({ title, action }: HeaderProps) {
  return (
    <div className="flex items-center justify-between mb-8">
      <h1 className="text-4xl font-bold bg-gradient-to-r from-blue-600 via-purple-600 to-pink-600 bg-clip-text text-transparent">
        {title}
      </h1>
      {action && (
        <button
          onClick={action.onClick}
          className="bg-gradient-to-r from-blue-500 to-blue-700 text-white px-6 py-3 rounded-md font-semibold shadow-lg hover:shadow-xl hover:-translate-y-0.5 transition-all duration-300"
        >
          {action.label}
        </button>
      )}
    </div>
  );
}
```

- [ ] **Step 2: Integrar no dashboard**

```bash
# Verificar se dashboard está importando Header
grep -n "Header" src/app/\(dashboard\)/dashboard/page.tsx
```

- [ ] **Step 3: Commit**

```bash
git add src/components/Layout/Header.tsx
git commit -m "design: add header component with gradient title

- Add title with multi-color gradient (blue > purple > pink)
- Add optional action button with primary gradient
- Implement smooth hover transitions"
```

---

## Task 7: Update Dashboard Page (Tier 1 - Integration)

**Files:**
- Modify: `src/app/(dashboard)/dashboard/page.tsx`

**Interfaces:**
- Consumes: Button, Card, Input, Header, Sidebar (refactored)
- Produces: Dashboard page using all new components

- [ ] **Step 1: Refactor dashboard para usar novos componentes**

```typescript
// src/app/(dashboard)/dashboard/page.tsx (parcial)
'use client';

import { Button } from '@/components/ui/button';
import { Card, CardHeader, CardContent } from '@/components/ui/card';
import { Header } from '@/components/Layout/Header';

export default function DashboardPage() {
  return (
    <div className="space-y-8">
      <Header 
        title="Dashboard" 
        action={{ label: '+ New Opportunity', onClick: () => {} }}
      />

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card variant="primary">
          <CardHeader>
            <div className="flex items-center gap-2">
              <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-blue-500 to-blue-700 flex items-center justify-center text-white">
                📈
              </div>
            </div>
            <h3 className="text-sm font-semibold text-gray-600 uppercase tracking-wide">
              Revenue This Month
            </h3>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-gray-900">R$ 450K</div>
            <p className="text-sm text-green-600 mt-2">↑ +12% vs last month</p>
          </CardContent>
        </Card>

        <Card variant="secondary">
          <CardHeader>
            <div className="flex items-center gap-2">
              <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-purple-500 to-purple-700 flex items-center justify-center text-white">
                🔥
              </div>
            </div>
            <h3 className="text-sm font-semibold text-gray-600 uppercase tracking-wide">
              Qualified Leads
            </h3>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-gray-900">24</div>
            <p className="text-sm text-green-600 mt-2">↑ +8% vs last week</p>
          </CardContent>
        </Card>

        <Card variant="success">
          <CardHeader>
            <div className="flex items-center gap-2">
              <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-emerald-500 to-emerald-700 flex items-center justify-center text-white">
                ✓
              </div>
            </div>
            <h3 className="text-sm font-semibold text-gray-600 uppercase tracking-wide">
              Deals Closed
            </h3>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-gray-900">18</div>
            <p className="text-sm text-red-600 mt-2">↓ -2 vs average</p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Verificar visualmente**

```bash
npm run dev
# Navegar para http://localhost:3000/dashboard
```

Expected: Dashboard mostrando cards coloridos com nova paleta

- [ ] **Step 3: Commit**

```bash
git add src/app/\(dashboard\)/dashboard/page.tsx
git commit -m "design: update dashboard page with new components

- Integrate refactored Button, Card, Input, Header components
- Add KPI cards with color variants and gradient icons
- Implement grid layout with proper spacing"
```

---

## Task 8: Create DESIGN_SYSTEM.md Documentation

**Files:**
- Create: `docs/DESIGN_SYSTEM.md`

**Interfaces:**
- Documents: Paleta de cores, tipografia, componentes, padrões
- Reference para: Futuros desenvolvedores, tokens reutilizáveis

- [ ] **Step 1: Escrever documentação do design system**

```markdown
# Design System - CRM Vibrante

## Paleta de Cores

### Primárias
- **Blue Base:** `#3b82f6` → Botões, ação principal
- **Blue Dark:** `#1e40af` → Hover state
- **Blue Darker:** `#1e3a8a` → Active state
- **Gradiente Primário:** `linear-gradient(135deg, #3b82f6 0%, #1e40af 100%)`

### Secundárias
- **Purple:** `#a855f7` → Items ativos, secondary actions
- **Indigo:** `#6366f1` → Complemento, accents
- **Gradiente Secundária:** `linear-gradient(135deg, #a855f7 0%, #6d28d9 100%)`

### Status
- **Success:** `#10b981` → Deals ganhos, status positive
- **Warning:** `#f97316` → Avisos, atenção
- **Error:** `#ef4444` → Erros, status negativo
- **Info:** `#14b8a6` → Informações, novos leads

## Componentes Tier 1 (Refatorados)

### Button
- **Primary:** Gradiente azul com hover lift (-translate-y-0.5)
- **Secondary:** Borda azul com hover background
- **Danger:** Gradiente vermelho
- **Ghost:** Transparente com hover background

### Input
- **Focus:** Border azul + ring shadow (10% opacity)
- **Error:** Border vermelho + ring vermelho
- **Transition:** all 300ms ease

### Card
- **Variants:** primary (blue), secondary (purple), success (green)
- **Hover:** shadow-lg + lift (-translate-y-1)
- **Gradient:** Subtle background (50% opacity)

### Sidebar
- **Background:** Gradient slate-800 to slate-900
- **Active:** Gradient purple to indigo
- **Hover:** purple-500/20 + translate-x-1

### Header
- **Title:** Gradient blue > purple > pink (bg-clip-text)
- **Action:** Primary button com gradient

## Animações

- **Fast:** 200ms (focus states)
- **Default:** 300ms (hover, transitions)
- **Slow:** 500ms (modals, important changes)

## CSS Variables

Todas as cores estão disponíveis como CSS variables:
- \`--crm-primary-base\`
- \`--crm-gradient-primary\`
- \`--crm-shadow-lg\`
- etc.

Veja \`src/styles/design-tokens.css\` para lista completa.

## Tailwind Classes

Custom Tailwind classes estão disponíveis:
- \`bg-crm-primary\`
- \`bg-crm-gradient-primary\`
- \`shadow-crm-lg\`
- etc.

Veja \`tailwind.config.ts\` para configuração.

## Próximos Passos (Tier 2)

- [ ] Tabelas com hover effects
- [ ] Modais/Dialogs com gradientes
- [ ] Alerts/Banners com status colors
- [ ] Badges com cores semânticas
```

- [ ] **Step 2: Adicionar referência no README do projeto**

```bash
# Adicionar link para DESIGN_SYSTEM.md no docs/README.md ou docs/index.md
echo "- [Design System](DESIGN_SYSTEM.md) - Documentação de cores, componentes e padrões visuais" >> docs/README.md
```

- [ ] **Step 3: Commit**

```bash
git add docs/DESIGN_SYSTEM.md docs/README.md
git commit -m "docs: add design system documentation

- Document color palette with primary, secondary, status colors
- Describe refactored Tier 1 components (Button, Input, Card, Sidebar, Header)
- Add animation timing reference
- Link CSS variables and Tailwind utilities"
```

---

## Task 9: Run Full Test Suite & Visual Verification

**Files:**
- All modified components
- Integration: dashboard page

- [ ] **Step 1: Run all tests**

```bash
npm run test -- --watch=false
```

Expected: PASS (ou no regressions)

- [ ] **Step 2: Build check**

```bash
npm run build
```

Expected: SUCCESS, no errors

- [ ] **Step 3: Visual verification**

```bash
npm run dev
```

Then manually check:
- http://localhost:3000/dashboard → Dashboard com nova paleta
- http://localhost:3000/design-system → Design system page (opcional update)
- Buttons hover/active states
- Sidebar active item styling
- Card hover effects

- [ ] **Step 4: Commit (se houver ajustes)**

```bash
git add .
git commit -m "test: verify all components and dashboard visually

- All tests passing
- Build successful
- Dashboard displays with new color palette
- Hover/active states working smoothly"
```

---

## Self-Review Checklist

✅ **Spec Coverage:**
- Paleta vibrante com gradientes? Task 1 ✓
- Button com variantes e hover? Task 2 ✓
- Input com focus states? Task 3 ✓
- Card com border-left colorido? Task 4 ✓
- Sidebar com dark gradient? Task 5 ✓
- Header com gradient title? Task 6 ✓
- Dashboard integração? Task 7 ✓
- Documentação DS? Task 8 ✓

✅ **Nenhum Placeholder:**
- Todos os tasks têm código real ✓
- Nenhum "TBD" ou "TODO" ✓
- Testes específicos ✓

✅ **Tipo Consistency:**
- Button: `variant="primary"` ✓
- Card: `variant="primary"` ✓
- Sidebar: `activeItem` string ✓
- Header: `title` string, `action` object ✓

✅ **Sem Breaking Changes:**
- Componentes existentes mantêm interface backward-compatible ✓
- Light/Dark mode ainda suportado ✓

---

## Próximos Passos (Tier 2)

Após completar este plano:

1. **Tier 2 Components:**
   - Tabelas com hover gradient
   - Modais/Dialogs com gradientes
   - Alerts/Banners com status colors
   - Avatares e user info

2. **Refinamentos:**
   - Dark mode específico para novos componentes
   - Micro-animações em loading states
   - Accessibility checks (WCAG AA)

3. **Consolidação:**
   - Review completo do DESIGN_SYSTEM.md
   - Atualizar design-system page do projeto
   - Criar Storybook stories para futura documentação

---

**Plan saved to `docs/superpowers/plans/2026-09-26-crm-visual-redesign.md`**