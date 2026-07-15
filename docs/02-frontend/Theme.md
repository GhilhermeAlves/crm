# Theme — Tema e Design Tokens

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Design Tokens](#design-tokens)
- [Cores](#cores)
- [Tipografia](#tipografia)
- [Espaçamento](#espaçamento)
- [Dark Mode](#dark-mode)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o design system, incluindo tokens, cores, tipografia e tema.

## Descrição

O tema é baseado em CSS custom properties do Shadcn UI, permitindo dark/light mode e customização por empresa. Design tokens garantem consistência visual em toda a aplicação.

## Design Tokens

### Cores

| Token | Light | Dark | Uso |
|---|---|---|---|
| `--background` | 0 0% 100% | 222.2 84% 4.9% | Fundo da página |
| `--foreground` | 222.2 84% 4.9% | 210 40% 98% | Texto principal |
| `--primary` | 222.2 47.4% 11.2% | 210 40% 98% | Botões primários |
| `--primary-foreground` | 210 40% 98% | 222.2 47.4% 11.2% | Texto em primary |
| `--secondary` | 210 40% 96.1% | 217.2 32.6% 17.5% | Botões secundários |
| `--muted` | 210 40% 96.1% | 217.2 32.6% 17.5% | Texto muted |
| `--accent` | 210 40% 96.1% | 217.2 32.6% 17.5% | Elementos de destaque |
| `--destructive` | 0 84.2% 60.2% | 0 62.8% 30.6% | Erros, delete |
| `--border` | 214.3 31.8% 91.4% | 217.2 32.6% 17.5% | Bordas |
| `--ring` | 222.2 84% 4.9% | 212.7 26.8% 83.9% | Focus ring |

### Status Colors

| Status | Cor | Uso |
|---|---|---|
| Success | Green 500 | Sucesso, lead ganho |
| Warning | Yellow 500 | Atenção, SLA próximo |
| Error | Red 500 | Erro, lead perdido |
| Info | Blue 500 | Informação |
| Neutral | Gray 500 | Neutro |

## Tipografia

| Elemento | Fonte | Tamanho | Peso |
|---|---|---|---|
| H1 | Inter | 30px | Bold |
| H2 | Inter | 24px | Semibold |
| H3 | Inter | 20px | Semibold |
| H4 | Inter | 16px | Medium |
| Body | Inter | 14px | Regular |
| Small | Inter | 12px | Regular |
| Code | JetBrains Mono | 14px | Regular |

## Espaçamento

| Token | Valor | Uso |
|---|---|---|
| `--space-1` | 4px | Espaçamento mínimo |
| `--space-2` | 8px | Entre elementos |
| `--space-3` | 12px | Padding interno |
| `--space-4` | 16px | Padding padrão |
| `--space-6` | 24px | Seções |
| `--space-8` | 32px | Entre seções |
| `--space-12` | 48px | Margens externas |

## Dark Mode

- Toggle via ThemeToggle no header
- Preferência salva no localStorage
- Respeita `prefers-color-scheme` do SO
- Transição suave entre temas

## Responsabilidades

- Manter consistência visual em toda a aplicação
- Facilitar customização por empresa (white-label)
- Suportar dark/light mode
- Garantir acessibilidade (contraste WCAG)

## Dependências

- [Components.md](./Components.md) — Usa os design tokens
- [Layout.md](./Layout.md) — Layout usa o tema
- [00-core/TechStack.md](../00-core/TechStack.md) — Shadcn + Tailwind

## Regras

- Nunca usar cores hardcoded (sempre via tokens)
- Tokens devem ser usados via CSS variables
- Contraste mínimo WCAG 2.1 AA (4.5:1 para texto)
- Tema deve ser customizável por empresa
- Dark mode é obrigatório

## Futuras Melhorias

- Tema customizável por empresa (white-label)
- Design tokens compartilhados com backend
- Temas de alta acessibilidade (alto contraste)
- Animações consistentes (motion design)

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
