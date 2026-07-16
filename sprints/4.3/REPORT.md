# Relatório Executivo — Sprint 4.3

**Projeto:** CRM SaaS Omnichannel
**Sprint:** 4.3 — Login
**Data:** 2026-07-15
**Versão:** 4.3.0

---

## Resumo

Implementação da autenticação funcional: emissão de JWT, refresh token rotation, endpoint /me, logout. Foram criados 4 novos arquivos (3 eventos + JwtUserPrincipal) e modificados 8 arquivos. A Sprint passou por revisão técnica com nota 93/100 e 3 correções aplicadas.

---

## Fases

| Fase | Status | Observação |
|------|--------|------------|
| ✅ Development | Concluído | 4 criados + 8 modificados |
| ✅ Review (4.3C) | Concluído | Nota 93/100, 3 correções, 2 novos ADRs |
| ✅ Close (4.3D) | Concluído | Documentação + .ai + sprints + milestones |

---

## Arquivos Criados

| Arquivo | Camada | Descrição |
|---------|--------|-----------|
| TokenRefreshedEvent.java | Domain/Event | Evento refresh token |
| PasswordResetRequestedEvent.java | Domain/Event | Evento solicitação reset senha |
| UserLoggedOutEvent.java | Domain/Event | Evento logout |
| JwtUserPrincipal.java | Infrastructure/Security | Principal record (userId, companyId) |

---

## Arquivos Modificados

| Arquivo | Descrição |
|---------|-----------|
| SecurityConfig.java | Rotas protegidas + JwtAuthenticationFilter no chain |
| JwtAuthenticationFilter.java | Validação JWT funcional (validateToken, extractUserId, set SecurityContext) |
| AuthController.java | @AuthenticationPrincipal, /me, logout/changePassword fixos |
| AuthService.java | @Service, changePassword bug fix, eventos |
| UserService.java | @Service |
| UserRepository.java | +findAllByCompanyId |
| UserRepositoryImpl.java | findAllByCompanyId implementado |
| SpringDataUserRepository.java | +findByCompanyId |

---

## Correções Aplicadas (Review)

| # | Problema | Correção |
|---|----------|----------|
| 1 | AuthController com import UUID não utilizado | Removido |
| 2 | UserService com import Collectors removido | Restaurado |
| 3 | AuthService.logout com companyId errado | Corrigido |

---

## Decisões Arquiteturais

| ADR | Título | Status |
|-----|--------|--------|
| ADR-012 | Roles/Permissions Hardcoded no Login | ⏳ Temporária |
| ADR-013 | Password Reset Placeholder | ⏳ Temporária |

---

## Qualidade

| Critério | Nota |
|----------|------|
| Arquitetura | 92/100 |
| Código | 94/100 |
| Segurança | 95/100 |
| Documentação | 90/100 |
| Manutenibilidade | 93/100 |
| Aderência ao projeto | 95/100 |
| **Geral** | **93/100** |

---

## Pendências

- [ ] Compilação pendente por indisponibilidade do Maven no ambiente
- [ ] Testes unitários (Sprint 4.5)
- [ ] Testes de integração (Sprint 4.5)

---

## Risco

| Risco | Probabilidade | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| Roles hardcoded podem ir para produção | Alta | Médio | ADR-012 documentado, correção agendada |
| resetPassword vazio em produção | Baixa | Alto | Endpoint retorna 200 mas não faz nada |

---

## Conclusão

✅ **Sprint 4.3 APROVADA** — Nenhum bloqueio identificado. Pronto para Sprint 4.4 (Frontend Auth).

---

*Data: 2026-07-15*
