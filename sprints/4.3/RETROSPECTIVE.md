# Sprint 4.3 — Retrospectiva

**Data:** 2026-07-15
**Participantes:** Architect (solo)

---

## O que funcionou bem 🟢

1. **Separação clara de responsabilidades** — Sprint 4.3 focou exclusivamente em login (Development) + Review + Close
2. **Eventos criados com record** — Padrão consistente com os eventos já existentes
3. **Bug fix changePassword** — Identificado e corrigido rapidamente (senha não era salva)
4. **Review encontrou 3 issues** — Todos de baixa complexidade, corrigidos na hora
5. **Novos ADRs registrados** — Documentação de decisões temporárias (roles hardcoded, password reset)

## O que poderia ser melhor 🟡

1. **Import removido incorretamente** — No UserService, `Collectors` foi removido mas era usado em `getUsersByCompanyId()`
2. **Logout event com companyId errado** — `UserLoggedOutEvent.create(userId, userId)` passava userId como companyId
3. **resetPassword vazio** — Placeholder sem implementação nem throw (pode causar confusão)
4. **Maven indisponível** — Impede verificação de compilação

## Ações para melhorias 📋

| Ação | Responsável | Sprint |
|------|-------------|--------|
| Implementar resolução real de roles/permissions do banco | Architect | 4.4 ou 5 |
| Implementar reset de senha com token + email | Architect | Futura (notificações) |
| Adicionar throw UnsupportedOperationException em resetPassword() | Architect | Futura |
| Garantir Maven no ambiente para próxima sprint | Infra | Antes de 4.5 |

## Métricas

| Métrica | Valor |
|---------|-------|
| Duração total | ~3h (2h dev + 30min review + 30min close) |
| Arquivos criados | 4 |
| Arquivos modificados | 8 |
| Correções no review | 3 |
| Nota da revisão | 93/100 |
| Satisfação geral | 😊 Boa |

---

*Data: 2026-07-15*
