# CRM Visual Redesign - Design Spec (Vibrante)

**Date:** 2026-09-26  
**Status:** Approved (Mockup Visual)  
**Approach:** Quick Polish + Design System Iterativo (Vibrante & Dinâmico)  
**Author:** Claude Haiku 4.5  
**Mockup:** https://claude.ai/artifact/VDhoC4vBEvczs3UvgfgXMa

---

## Executive Summary

Redesign visual completo do CRM SaaS com foco em **vibrante, dinâmico e moderno** (não minimalista clean). Estratégia: começar com polish rápido nos componentes críticos (Tier 1), documentar padrões conforme implementamos, deixar design system emergir naturalmente da prática.

**Filosofia:** Cores ricas + gradientes + animações suaves + feedback visual claro. Inspirado no Capim mas com identidade própria do CRM.

---

## 1. Estratégia Geral

### 1.1 Visão
Transformar o CRM de uma interface "funcional" para uma interface "polida e profissional", mantendo a estrutura existente mas elevando drasticamente a qualidade visual.

### 1.2 Abordagem: Quick Polish + Design System Iterativo

1. **Começar com componentes críticos (Tier 1)** → Dashboard, Navigation, Cards, Forms, Buttons
2. **Fazer polish visual inspirado no Capim** → Cores, tipografia, espaçamento, estados
3. **Documentar padrões conforme emergem** → Arquivo central `DESIGN_SYSTEM.md`
4. **Expandir para Tier 2 e 3** → Tabelas, modais, alerts, etc
5. **Consolidar o DS** → No final, ter um design system robusto e documentado

### 1.3 Por Que Funciona
- **Progresso visível rapidamente:** Usuários veem melhorias logo
- **DS real, não teórico:** Padrões baseados em implementação prática
- **Flexível:** Se descobrir algo melhor no meio do caminho, muda sem impactar spec upfront

### 1.4 Timeline
- Sem deadline definido → Iterativo, contínuo
- Prioridade: Qualidade > Velocidade

---

## 2. Componentes Críticos (Priorização)

### Tier 1 - Impacto Máximo (Fazer Primeiro)
Componentes que os usuários veem todo dia e definem a primeira impressão:

1. **Dashboard/Home** (`/app/(dashboard)/dashboard`)
   - Layout geral
   - Cards de métricas
   - Widgets
   
2. **Navigation/Sidebar** (`components/Layout/Sidebar`)
   - Menu principal
   - Ícones
   - Estados (active, hover)
   
3. **Cards e Data Display**
   - Card component
   - Spacing, shadows, borders
   - Hover states
   
4. **Forms e Inputs** (`components/Form/*`)
   - Input fields
   - Labels
   - Validation states
   - Help text
   
5. **Buttons e Actions** (`components/Button/*`)
   - Variants (primary, secondary, danger, ghost)
   - States (default, hover, active, disabled, loading)
   - Sizes

### Tier 2 - Alto Impacto (Próximo)
1. **Tabelas** (Contacts, Campaigns, Activities)
2. **Modais e Dialogs**
3. **Banners, Alerts, Toasts**
4. **Avatares e User Info**
5. **Badges e Tags**

### Tier 3 - Complementar (Depois)
1. Páginas específicas refinadas
2. Componentes auxiliares
3. Animações e micro-interactions

---

## 3. Design Reference (Vibrante & Dinâmico)

### 3.1 Paleta de Cores com Gradientes

**Cores Primárias + Gradientes:**
- `primary-main`: `#3B82F6` (Blue) - Cor principal, botões
- `primary-gradient`: `linear-gradient(135deg, #3B82F6 0%, #1E40AF 100%)`
- `primary-hover`: `#1E40AF` (Blue Dark) - Estado hover
- `primary-active`: `#1E3A8A` (Blue Darker) - Estado pressionado

**Cores Secundárias + Gradientes:**
- `secondary-purple`: `#A855F7` - Accent, elementos ativos
- `secondary-purple-gradient`: `linear-gradient(135deg, #A855F7 0%, #6D28D9 100%)`
- `secondary-indigo`: `#6366F1` - Complemento
- `secondary-indigo-gradient`: `linear-gradient(135deg, #6366F1 0%, #4338CA 100%)`

**Cores Terciárias (Accents):**
- `tertiary-cyan`: `#06B6D4` - Destaque, CTAs secundárias
- `tertiary-cyan-gradient`: `linear-gradient(135deg, #06B6D4 0%, #0369A1 100%)`
- `tertiary-pink`: `#EC4899` - Highlight, emphasis
- `tertiary-amber`: `#F59E0B` - Atenção suave

**Status Semânticos:**
- `success`: `#10B981` (Green) → `linear-gradient(135deg, #10B981 0%, #065F46 100%)`
- `warning`: `#F97316` (Orange) → `linear-gradient(135deg, #F97316 0%, #9A3412 100%)`
- `error`: `#EF4444` (Red) → `linear-gradient(135deg, #EF4444 0%, #991B1B 100%)`
- `info`: `#14B8A6` (Teal) → `linear-gradient(135deg, #14B8A6 0%, #0D9488 100%)`

**Superfícies:**
- `background`: `#FFFFFF` (Light), `#0F172A` (Dark)
- `surface-primary`: `#F8FAFC` (Light gray bg)
- `surface-secondary`: `#1E293B` (Dark sidebar)
- `surface-tertiary`: `#0F172A` (Dark accent)
- `border`: `#E2E8F0`
- `border-dark`: `#334155`

**Neutros:**
- `gray-50` a `gray-900`: Escala completa para textos e borders

### 3.2 Tipografia

**Font Family:**
- Primária: `Inter` (variável ou regular)
- Código: `JetBrains Mono` (monospace)

**Escala:**
- `xs`: 12px, line-height: 1.5
- `sm`: 14px, line-height: 1.5
- `base`: 16px, line-height: 1.5
- `lg`: 18px, line-height: 1.6
- `xl`: 20px, line-height: 1.6
- `2xl`: 24px, line-height: 1.6
- `3xl`: 30px, line-height: 1.4
- `4xl`: 36px, line-height: 1.2

**Weights:**
- `light`: 300
- `normal`: 400
- `medium`: 500
- `semibold`: 600
- `bold`: 700

**Semantic Usage:**
- **H1 (Page Title):** 3xl, bold (700)
- **H2 (Section Title):** 2xl, semibold (600)
- **H3 (Card Title):** xl, semibold (600)
- **Body Text:** base, normal (400)
- **Small/Help Text:** sm, normal (400) + `gray-500`
- **Labels (Forms):** sm, medium (500)

### 3.3 Espaçamento (Scale)

```
xs: 4px
sm: 8px
md: 16px
lg: 24px
xl: 32px
2xl: 48px
3xl: 64px
```

**Aplicação:**
- **Padding interna (buttons, inputs):** sm/md
- **Padding em cards:** md/lg
- **Margin entre componentes:** md/lg
- **Margin entre sections:** lg/xl

### 3.4 Sombras e Elevação

```
shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.05)
shadow-md: 0 4px 6px rgba(0, 0, 0, 0.1)
shadow-lg: 0 10px 15px rgba(0, 0, 0, 0.1)
shadow-xl: 0 20px 25px rgba(0, 0, 0, 0.1)
```

**Uso:**
- **Cards normais:** shadow-sm
- **Hover em cards:** shadow-md
- **Modais/Popovers:** shadow-lg
- **Dropdowns:** shadow-md

### 3.5 Border Radius

```
radius-sm: 4px
radius-md: 8px
radius-lg: 12px
radius-xl: 16px
radius-full: 999px (circles)
```

**Uso:**
- **Buttons:** radius-md
- **Inputs:** radius-md
- **Cards:** radius-lg
- **Avatares:** radius-full

### 3.5.5 Efeitos Visuais (Novo)

**Gradientes:**
- Usados em: buttons primários, cards ativos, headers, sidebars, status badges
- Direção padrão: `135deg` (top-left para bottom-right)
- Transição: sempre suave entre cores (nunca brusco)

**Animações:**
- **Transition padrão:** `all 0.3s ease` para hover states
- **Transform no hover:** 
  - Buttons: `translateY(-2px)`
  - Cards: `translateY(-4px)`
  - Menu items: `translateX(4px)`
- **Sem animação em:** disabled states, loading

**Sombras em Camadas:**
- **shadow-sm:** Suave, cards normais
- **shadow-md:** Hover moderado, pequenos elementos
- **shadow-lg:** Cards em hover, overlays
- **shadow-xl:** Modais, dialogs, dropdowns

**Feedback Visual:**
- Hover sempre tem feedback (cor, sombra, transform)
- Clique tem feedback imediato (ativo visual diferente)
- Loading tem spinner com animação

### 3.6 Estados de Componentes (Dinâmicos)

**Button States:**
- **Default:** gradient primária (`linear-gradient(135deg, #3B82F6, #1E40AF)`), text white, shadow-md
- **Hover:** gradient mais escura + `translateY(-2px)` + shadow maior (300ms transition)
- **Active/Pressed:** cor base mais escura (#1E3A8A)
- **Disabled:** background gray-200, text gray-400, cursor not-allowed, sem sombra
- **Loading:** spinner + disabled state com opacity reduzida

**Input States:**
- **Default:** border gray-300, background white, text gray-900
- **Focus:** border primary-600, shadow com cor primária (`0 0 0 3px rgba(59,130,246,0.1)`), outline none
- **Error:** border error (#EF4444), text error, shadow em vermelho suave
- **Disabled:** background gray-100, text gray-400, border gray-200

**Card States:**
- **Default:** background white, border-left 4px em cor temática, shadow-sm
- **Hover:** `translateY(-4px)` + shadow-lg + gradiente de fundo suave (300ms)
- **Selected:** border-left em cor primária, background com gradiente primário (5% opacity)
- **Active:** Igual a selected com animação de entrada

**Sidebat Item States:**
- **Default:** texto white, background transparent
- **Hover:** background `rgba(139, 92, 246, 0.2)` + `translateX(4px)` (300ms)
- **Active:** gradient `linear-gradient(90deg, #A855F7, #6366F1)` com shadow

---

## 4. Abordagem de Design e Implementação

### 4.1 Passo 1: Análise + Documentação
- [ ] Extrair padrões visuais do Capim (colors, spacing, typography)
- [ ] Revisar design system atual do projeto
- [ ] Documentar "estilo alvo" (seção 3 acima)

### 4.2 Passo 2: Criar Design Reference Doc
- [ ] Criar/atualizar `DESIGN_SYSTEM.md` no `docs/`
- [ ] Incluir todos os tokens (cores, typography, spacing, shadows, radius)
- [ ] Adicionar exemplos de uso para cada token

### 4.3 Passo 3: Polish Visual - Tier 1
Atualizar cada componente Tier 1:
- [ ] Cores alinhadas com paleta definida
- [ ] Espaçamento consistente (usar scale)
- [ ] Tipografia melhorada (sizes, weights)
- [ ] Estados visuais claros (hover, focus, disabled, loading)
- [ ] Ícones e imagery coerentes

**Componentes específicos:**
- [ ] Button (all variants)
- [ ] Input fields
- [ ] Card component
- [ ] Sidebar/Navigation
- [ ] Dashboard layout

### 4.4 Passo 4: Documentar DS Emergente
Enquanto implementa, manter um changelog de padrões:
- Quais componentes foram refatorados
- Padrões que emergiram
- Tokens reutilizáveis
- Decisões de design (e por quê)

**Arquivo:** `docs/DESIGN_SYSTEM.md` (será atualizado ao longo do processo)

### 4.5 Passo 5: Expandir para Tier 2 e 3
- [ ] Aplicar padrões a tabelas
- [ ] Aplicar a modais/dialogs
- [ ] Aplicar a alerts/banners
- [ ] Refinar conforme necessário

### 4.6 Consolidar DS
- [ ] Revisar `DESIGN_SYSTEM.md`
- [ ] Documentar componentes principais no `/design-system` page
- [ ] Garantir consistência cross-app

---

## 5. Ferramentas e Infraestrutura

### 5.1 Tecnologia Recomendada

**CSS-in-JS / Styling:**
- Se usar Tailwind: Atualizar `tailwind.config.js` com tokens
- Se usar CSS Modules: Criar arquivo de variáveis centralizadas
- Se usar CSS custom properties: Criar arquivo `:root` com tokens

**Documentation:**
- `docs/DESIGN_SYSTEM.md` → Design tokens e padrões
- Componentes já têm página em `/design-system`

**Version Control:**
- Commits por componente/seção
- Mensagens claras ("design: update button component", etc)

### 5.2 Processo de Implementação

1. **Branch:** `feature/crm-visual-redesign` ou `feature/design-system-refine`
2. **Commits:** Um por componente/seção (atomic commits)
3. **Documentation:** Atualizar `DESIGN_SYSTEM.md` no mesmo commit
4. **Review:** Self-review de design antes de merge

---

## 6. Métricas de Sucesso

- ✅ Paleta de cores consistente em toda a app
- ✅ Tipografia padronizada (apenas 2-3 font sizes principais)
- ✅ Espaçamento segue a scale 4/8/16/24 (90%+ dos casos)
- ✅ Estados visuais claros em todos os componentes interativos
- ✅ Design System documentado e reutilizável
- ✅ Tier 1 completamente renovado
- ✅ Feedback positivo de usuários (visual profissional)

---

## 7. Riscos e Mitigações

| Risco | Mitigação |
|-------|-----------|
| Padrões inconsistentes emergem | Review regular de DS emergente, manter doc atualizada |
| Muito tempo em Tier 1 | Timeboxar seções (ex: 1 semana por componente) |
| Mudar decisões de design no meio | Documentar decisões, revisar com usuário |
| Faltar componentes no Tier 1 | Listar exhaustivamente antes de começar |

---

## 8. Próximos Passos

1. **Revisar esta spec** com o usuário
2. **Aprovar tokens** (colors, typography, spacing)
3. **Invocar writing-plans skill** → Quebrar em tasks concretas
4. **Começar Tier 1** → Dashboard, Navigation, Cards, Forms, Buttons
5. **Iterar conforme aprova cada seção**

---

## Anexo A: Estrutura de Diretórios Esperada

```
frontend-refine/
├── src/
│   ├── app/
│   │   └── (dashboard)/
│   │       ├── dashboard/
│   │       ├── contacts/
│   │       └── ...
│   ├── components/
│   │   ├── Button/
│   │   ├── Card/
│   │   ├── Form/
│   │   ├── Layout/
│   │   └── ...
│   └── styles/
│       ├── globals.css
│       └── design-tokens.css (novo)
├── docs/
│   ├── DESIGN_SYSTEM.md (novo/atualizado)
│   └── superpowers/specs/
│       └── 2026-09-26-crm-visual-redesign-design.md (este arquivo)
└── tailwind.config.js (será atualizado)
```

---

**Spec Versioning:**
- v1.0 - Initial design spec (2026-09-26)
