# Permissions — Controle de Permissões UI

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Componentes](#componentes)
- [Hooks](#hooks)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o sistema de controle de permissões no frontend.

## Descrição

Permissões controlam o que cada usuário pode ver e fazer na interface. Itens são exibidos/ocultados baseado nas permissões do usuário logado.

## Componentes

| Componente | Descrição |
|---|---|
| Can | Wrapper que renderiza children apenas se tiver permissão |
| RoleGuard | Guard baseado em role |

### Exemplo de Uso

```tsx
<Can permission="lead:write">
  <Button onClick={createLead}>Criar Lead</Button>
</Can>

<Can permission="user:manage">
  <Link href="/settings/users">Gerenciar Usuários</Link>
</Can>

<RoleGuard roles={['ADMIN', 'MANAGER']}>
  <Button>Deletar</Button>
</RoleGuard>
```

## Hooks

| Hook | Descrição |
|---|---|
| `usePermission` | Verifica se usuário tem permissão |
| `useRole` | Verifica se usuário tem role |

### Exemplo

```typescript
const { hasPermission } = usePermission();
const { hasRole } = useRole();

if (hasPermission('lead:write')) {
  // Mostrar botão de criar lead
}

if (hasRole('ADMIN')) {
  // Mostrar configurações de admin
}
```

## Responsabilidades

- Ocultar elementos que o usuário não pode acessar
- Desabilitar botões de ações não permitidas
- Redirecionar para 403 quando necessário
- Logging de tentativas de acesso não autorizado

## Dependências

- [01-backend/Auth.md](../01-backend/Auth.md) — Permissões no JWT
- [Context.md](./Context.md) — AuthProvider

## Regras

- Permissões vêm do JWT (claims)
- UI é a primeira camada de verificação
- Backend é a segunda camada (nunca confiar só no frontend)
- Elementos não devem ser renderizados (não apenas desabilitados)
- Redirecionar para 403, não para login

## Futuras Melhorias

- Permissões dinâmicas (carregadas do backend)
- Feature flags baseadas em permissão
- Audit de acessos no frontend
- Permissões por módulo

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
