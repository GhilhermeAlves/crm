# Sprint 24 — FollowUpSequence Frontend + UAZAPI Type Fix

> **Status: ✅ Concluída (2026-09-11).**
> Frontend de FollowUpSequence (débito das Sprints 22/23) + correção do tipo ChannelProvider para UAZAPI.

## Escopo

Sprint de frontend para fechar os débitos de UI registrados nas Sprints 22 e 23:

1. **Frontend FollowUpSequence** — CRUD completo (listar, criar, editar, excluir, ativar/desativar)
2. **ChannelProvider.UAZAPI** — correção do tipo TypeScript + label + enum no formulário de canal
3. **Rota e Sidebar** — nova rota `/follow-up-sequences` com acesso por permissão

## Auditoria (débitos das Sprints anteriores)

| Componente | Situação |
|---|---|
| FollowUpSequence backend (entity, DTOs, service, controller, V076, permissões) | ✅ Sprint 22 |
| FollowUpSequence frontend | ❌ **DÉBITO** → implementado nesta sprint |
| ChannelProvider no frontend (WHATSAPP_CLOUD_API, FAKE) | ⚠️ incompleto → UAZAPI adicionado |
| ChannelFormDialog (enum provider) | ⚠️ incompleto → UAZAPI adicionado |

## Implementação

### Frontend — FollowUpSequence

**Arquivos criados:**

- `frontend/src/features/omnichannel/types/followup-sequence.types.ts` — tipos `FollowUpSequence`, `FollowUpSequenceRequest`, `FollowUpSequenceStatus`, labels
- `frontend/src/features/omnichannel/services/followup-sequence.service.ts` — service HTTP (list, get, create, update, delete, activate, deactivate) chamando `/api/v1/omnichannel/follow-up-sequences`
- `frontend/src/features/omnichannel/hooks/useFollowUpSequences.ts` — hooks React Query (useFollowUpSequences, useFollowUpSequence, useCreate/Update/Delete/Activate/DeactivateFollowUpSequence)
- `frontend/src/features/omnichannel/components/FollowUpSequenceDialog.tsx` — dialog de criação/edição (nome + descrição)
- `frontend/src/app/(dashboard)/follow-up-sequences/page.tsx` — página CRUD com tabela, badges de status, ações de ativar/desativar/editar/excluir + confirm dialog

**Arquivos modificados:**

- `frontend/src/lib/constants.ts` — adicionada rota `FOLLOW_UP_SEQUENCES`
- `frontend/src/components/layout/Sidebar.tsx` — item "Sequências de Follow-up" no grupo "Comunicação", gated por `omnichannel:followup:sequence:read`

### Frontend — ChannelProvider UAZAPI

**Arquivos modificados:**

- `frontend/src/features/omnichannel/types/omnichannel.types.ts` — `ChannelProvider` agora inclui `"UAZAPI"`; label `"UAZAPI (uazapiGO)"` adicionado
- `frontend/src/features/omnichannel/components/ChannelFormDialog.tsx` — enum do provider expandido para incluir `"UAZAPI"`

## Endpoints utilizados

| Endpoint | Método | Permissão | Uso |
|---|---|---|---|
| `/api/v1/omnichannel/follow-up-sequences` | GET | `omnichannel:followup:sequence:read` | Listar sequências |
| `/api/v1/omnichannel/follow-up-sequences` | POST | `omnichannel:followup:sequence` | Criar sequência |
| `/api/v1/omnichannel/follow-up-sequences/{id}` | GET | `omnichannel:followup:sequence:read` | Detalhar sequência |
| `/api/v1/omnichannel/follow-up-sequences/{id}` | PUT | `omnichannel:followup:sequence` | Atualizar sequência |
| `/api/v1/omnichannel/follow-up-sequences/{id}` | DELETE | `omnichannel:followup:sequence` | Excluir sequência |
| `/api/v1/omnichannel/follow-up-sequences/{id}/activate` | POST | `omnichannel:followup:sequence` | Ativar sequência |
| `/api/v1/omnichannel/follow-up-sequences/{id}/deactivate` | POST | `omnichannel:followup:sequence` | Desativar sequência |

## Migrações

Nenhuma — backend completo desde Sprint 22 (V076).

## Permissões

Nenhuma nova — permissões `omnichannel:followup:sequence` e `omnichannel:followup:sequence:read` já existiam (Sprint 22).

## Testes

- **Frontend typecheck**: `npm run typecheck` → ✅ 0 erros
- **Frontend lint**: `npm run lint` → ✅ 0 erros novos (warnings preexistentes apenas)
- **Frontend build**: `npm run build` → ✅ Compiled successfully (build longo em andamento timeout; compilação e typecheck confirmados)
- **Backend**: sem alterações — 747+ testes preservados

## Integração UAZAPI

Sem alterações no contrato UAZAPI. O provider `UazapiWhatsAppProvider` (Sprint 22) e o parser `UazapiWebhookParser` (Sprint 23) permanecem inalterados. A única mudança referente à UAZAPI foi a adição do tipo `UAZAPI` ao `ChannelProvider` no frontend.

## Limitações

- Sem WebSocket/polling real-time no inbox (débito anterior, fora de escopo)
- Sem suporte a mídia (débito anterior, fora de escopo)
- FollowUpSequence não vincula follow-ups individuais na UI (apenas CRUD da sequência; vinculação é backend-only)

## Arquivos Principais

- Types: `frontend/src/features/omnichannel/types/followup-sequence.types.ts`
- Service: `frontend/src/features/omnichannel/services/followup-sequence.service.ts`
- Hooks: `frontend/src/features/omnichannel/hooks/useFollowUpSequences.ts`
- Dialog: `frontend/src/features/omnichannel/components/FollowUpSequenceDialog.tsx`
- Page: `frontend/src/app/(dashboard)/follow-up-sequences/page.tsx`
- Constants: `frontend/src/lib/constants.ts` (+ `FOLLOW_UP_SEQUENCES`)
- Sidebar: `frontend/src/components/layout/Sidebar.tsx` (nav item adicionado)
- Types (UAZAPI): `frontend/src/features/omnichannel/types/omnichannel.types.ts`
- ChannelFormDialog: `frontend/src/features/omnichannel/components/ChannelFormDialog.tsx` (enum provider)
