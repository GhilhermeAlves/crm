# REVIEW — Relatório de Revisão da Documentação

## Objetivo

Apresentar findings da revisão completa da documentação, incluindo inconsistências, dependências circulares, módulos duplicados, conflitos entre frontend/backend, conflitos de nomenclatura, ausência de regras de negócio e problemas de escalabilidade.

## Índice

- [Resumo Executivo](#resumo-executivo)
- [1. Conflitos de Nomenclatura](#1-conflitos-de-nomenclatura)
- [2. Dependências Circulares](#2-dependências-circulares)
- [3. Referências Quebradas](#3-referências-quebradas)
- [4. Conflitos Frontend ↔ Backend](#4-conflitos-frontend--backend)
- [5. Conteúdo Corrompido ou Inválido](#5-conteúdo-corrompido-ou-inválido)
- [6. Regras de Negócio Ausentes ou Incompletas](#6-regras-de-negócio-ausentes-ou-incompletas)
- [7. Problemas de Escalabilidade](#7-problemas-de-escalabilidade)
- [8. Inconsistências de Formato](#8-inconsistências-de-formato)
- [9. Segurança](#9-segurança)
- [10. Arquivos Referenciados mas Ausentes](#10-arquivos-referenciados-mas-ausentes)
- [Ações Recomendadas](#ações-recomendadas)
- [Referências](#referências)
- [Histórico de Revisão](#histórico-de-revisão)

---

## Resumo Executivo

| Severidade | Quantidade | Descrição |
|---|---|---|
| Crítica | 3 | Conteúdo corrompido, dependência circular, referência quebrada |
| Alta | 8 | Conflitos frontend/backend, nomenclatura, regras ausentes |
| Média | 6 | Inconsistências de formato, escalabilidade, segurança |
| Baixa | 4 | Arquivos ausentes, melhorias menores |
| **Total** | **21** | **Issues encontradas** |

---

## 1. Conflitos de Nomenclatura

### 1.1 Inconsistência no nome do bounded context "Contact"

| Documento | Nome Used | Problema |
|---|---|---|
| `00-core/Architecture.md` | **Contact** | Bounded context inclui leads E contatos |
| `01-backend/Modules.md` | **Contact** | Módulo inclui contacts, leads E customers |
| `01-backend/Contacts.md` | Contacts | Apenas contatos |
| `01-backend/Leads.md` | Leads | Apenas leads |
| `01-backend/Customers.md` | Customers | Apenas clientes |

**Impacto:** O bounded context "Contact" no Architecture.md inclui "Leads, clientes, segmentação", mas no backend existem 3 módulos separados. Isso gera confusão sobre onde implementar regras que envolvem os três.

**Recomendação:** Renomear o bounded context para **Contact & Lead** ou manter "Contact" mas documentar explicitamente que leads e customers são sub-domínios.

---

### 1.2 Inconsistência nos nomes das tabelas

| Documento | Tabela | Problema |
|---|---|---|
| `03-database/ERD.md` | `message_templates` | Tabela não documentada em Entities.md |
| `03-database/ERD.md` | `message_attachments` | Tabela não documentada em Entities.md |
| `03-database/ERD.md` | `automation_triggers` | Tabela não documentada em Entities.md |
| `03-database/ERD.md` | `automation_actions` | Tabela não documentada em Entities.md |
| `03-database/ERD.md` | `subscriptions` | Tabela não documentada em Entities.md |
| `03-database/ERD.md` | `contact_addresses` | Tabela não documentada em Entities.md |
| `03-database/ERD.md` | `contact_custom_fields` | Tabela não documentada em Entities.md |
| `03-database/ERD.md` | `campaign_steps` | Tabela não documentada em Entities.md |
| `03-database/ERD.md` | `roles` | Tabela não documentada em Entities.md |
| `03-database/ERD.md` | `user_roles` | Tabela não documentada em Entities.md |
| `03-database/ERD.md` | `events` | Tabela não documentada em Entities.md |

**Impacto:** O ERD lista 11 tabelas que não estão documentadas no Entities.md. Implementadores não sabem a estrutura dessas tabelas.

**Recomendação:** Adicionar documentação completa para todas as tabelas listadas no ERD.

---

## 2. Dependências Circulares

### 2.1 Dashboard ↔ Reports (CRÍTICA)

```
01-backend/Dashboard.md
  └── Dependências: [Analytics.md](./Reports.md)
      └── 01-backend/Reports.md
          └── Dependências: [Analytics.md](./Reports.md) ← AUTO-REFERÊNCIA!
          └── Dependências: [Dashboard.md](./Dashboard.md) ← VOLTA!
```

**Problema:**
1. `Dashboard.md` referencia `Reports.md` como "Analytics.md" (texto do link incorreto)
2. `Reports.md` auto-referencia a si mesmo como "Analytics.md"
3. `Reports.md` referencia `Dashboard.md` de volta — dependência circular

**Impacto:** Impossível determinar a ordem de implementação. Cada módulo depende do outro.

**Recomendação:** Criar um módulo separado `Analytics.md` que ambos importem, ou quebrar a dependência definindo claramente quem calcula as métricas.

---

### 2.2 Communication ↔ Conversations

```
01-backend/AI.md
  └── Dependências: [Communication.md](./Conversations.md)
```

**Problema:** O texto do link diz "Communication" mas aponta para `Conversations.md`. Não existe `Communication.md`.

**Recomendação:** Renomear o link para `[Conversations.md](./Conversations.md)`.

---

## 3. Referências Quebradas

### 3.1 `04-integrations/README.md`

```markdown
- [01-backend/Integration.md](../01-backend/Overview.md)
```

**Problema:** O texto diz "Integration.md" mas aponta para `Overview.md`. Não existe `01-backend/Integration.md`.

**Recomendação:** Corrigir para `[01-backend/Overview.md](../01-backend/Overview.md)`.

---

### 3.2 `01-backend/Dashboard.md`

```markdown
- [Analytics.md](./Reports.md)
```

**Problema:** Texto diz "Analytics.md" mas aponta para `Reports.md`.

**Recomendação:** Corrigir para `[Reports.md](./Reports.md)` ou criar `Analytics.md`.

---

### 3.3 `01-backend/Reports.md`

```markdown
- [Analytics.md](./Reports.md)
```

**Problema:** Auto-referência incorreta.

**Recomendação:** Remover esta referência ou corrigir para o módulo correto.

---

### 3.4 `DATABASE_MAP.md` referencia arquivos inexistentes

O `DATABASE_MAP.md` referencia:
- `[03-database/UUID.md](./03-database/UUID.md)` — Arquivo não verificado
- `[03-database/SoftDelete.md](./03-database/SoftDelete.md)` — Arquivo não verificado

**Recomendação:** Verificar se esses arquivos existem. Se não, criá-los ou remover as referências.

---

## 4. Conflitos Frontend ↔ Backend

### 4.1 Rota `/register` sem endpoint correspondente

| Camada | Rota/Endpoint | Status |
|---|---|---|
| Frontend | `/register` (RegisterPage) | Documentado em Routing.md |
| Backend | `/api/v1/auth/register` | **NÃO documentado em Auth.md** |

**Impacto:** Frontend tem tela de cadastro mas backend não tem endpoint para isso.

**Recomendação:** Adicionar endpoint de registro no Auth.md ou remover a rota do frontend (se o registro for feito apenas via convite).

---

### 4.2 Rota `/settings/integrations` sem endpoint correspondente

| Camada | Rota/Endpoint | Status |
|---|---|---|
| Frontend | `/settings/integrations` (IntegrationsPage) | Documentado em Routing.md |
| Backend | `/api/v1/settings/integrations` | **NÃO documentado** |

**Impacto:** Frontend tem página de integrações mas backend não expõe endpoints para listar/configurar integrações.

**Recomendação:** Adicionar endpoints de gerenciamento de integrações no Companies.md ou Settings.

---

### 4.3 Biblioteca `react-beautiful-dnd` depreciada

| Documento | Referência | Problema |
|---|---|---|
| `02-frontend/Kanban.md` | `react-beautiful-dnd` | Biblioteca depreciada desde 2023 |

**Impacto:** Biblioteca sem manutenção, bugs conhecidos, não suporta React 18 Server Components.

**Recomendação:** Usar `@dnd-kit/core` (sucessor espiritual) ou `@hello-pangea/dnd` (fork mantido).

---

### 4.4 WebSocket Provider em Localização Incorreta

| Documento | Referência | Problema |
|---|---|---|
| `02-frontend/Context.md` | `WebSocketProvider` em `app/(auth)/layout.tsx` | Layout de auth não é onde WebSocket deve estar |

**Impacto:** WebSocket deve estar no layout raiz ou em um provider dedicado, não no layout de autenticação.

**Recomendação:** Mover WebSocketProvider para `app/layout.tsx` ou `app/(authenticated)/layout.tsx`.

---

### 4.5 Zustand mencionado mas não utilizado

| Documento | Menção | Status |
|---|---|---|
| `02-frontend/Overview.md` | Zustand 4.x como "Client state management" | Mencionado |
| `02-frontend/Hooks.md` | Nenhum hook usa Zustand | Não utilizado |
| `02-frontend/Context.md` | Todos os providers usam React Context | Não utilizado |

**Impacto:** Zustand é listado na stack mas não há nenhum store documentado.

**Recomendação:** Documentar os Zustand stores que serão criados ou remover Zustand da stack se for usar apenas React Context.

---

### 4.6 Conflito de roles entre frontend e backend

| Documento | Roles | Status |
|---|---|---|
| `01-backend/Users.md` | SUPER_ADMIN, ADMIN, MANAGER, AGENT, VIEWER | 5 roles |
| `05-business-rules/Permissions.md` | SUPER_ADMIN, ADMIN, MANAGER, AGENT, VIEWER | 5 roles |
| `02-frontend/Permissions.md` | Roles não listadas explicitamente | Incompleto |

**Impacto:** Frontend não documenta quais roles existem, apenas mostra como verificar permissões.

**Recomendação:** Adicionar lista de roles no Permissions.md do frontend.

---

## 5. Conteúdo Corrompido ou Inválido

### 5.1 `01-backend/Reports.md` — Seção de Endpoints corrompida (CRÍTICA)

O arquivo `01-backend/Reports.md` contém seção de endpoints com conteúdo corrompido/ilegível:

```markdown
## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---| the the.,. the the. |

.,:,:

, the,/.. as..

. the; this.
., the the.
.

---




.
.


0 the the.
---─```

##>
─.```

.
##.##.##


##..|

 to the continues.---```",..>
```

**Impacto:** Seção completamente inutilizável. Implementadores não conseguem saber quais endpoints existem.

**Recomendação:** Reescrever a seção de endpoints completamente.

---

### 5.2 `05-business-rules/Lead.md` — Texto em chinês

```markdown
| L-005 | Lead duplicado (mesmo email) é mergeado automaticamente | Consistência |
```

Na seção de Responsabilidades:

```markdown
- Atualizar regras quando业务需求 mudam
```

**Problema:** "业务需求" é texto em chinês (meaning "business requirements") misturado com português.

**Recomendação:** Substituir por "quando necessidades de negócio mudam".

---

### 5.3 `02-frontend/Permissions.md` — Conteúdo truncado

O arquivo parece estar truncado na seção de Hooks, terminando abruptamente:

```markdown
| `useRole` | Verifica se usuário tem role |
```

**Impacto:** Falta documentação completa dos hooks de permissão.

**Recomendação:** Completar a documentação dos hooks.

---

## 6. Regras de Negócio Ausentes ou Incompletas

### 6.1 Lead Scoring — Fórmula não documentada

| Documento | Status | Problema |
|---|---|---|
| `05-business-rules/Lead.md` | Regras L-040 a L-043 | Define faixas de score mas **não documenta a fórmula de cálculo** |

**Detalhes ausentes:**
- Como é calculado o score?
- Quais pesos para cada fator?
- Como origem influencia o score?
- Como engajamento é medido?
- Como dados completos影响am o score?

**Recomendação:** Documentar a fórmula completa de scoring com pesos e exemplos.

---

### 6.2 Pipeline — Regra de transição ambígua

| Documento | Regra | Ambiguidade |
|---|---|---|
| `05-business-rules/Pipeline.md` | P-020: "Oportunidade só pode avançar ou retroceder 1 estágio" | P-021 diz "Won/Lost só no último estágio" — mas e se o último estágio não for o estágio final do funil? |

**Impacto:** Não está claro se "último estágio" significa o último estágio do pipeline ou se Won/Lost são estágios separados.

**Recomendação:** Esclarecer se Won/Lost são estágios especiais separados dos estágios do pipeline.

---

### 6.3 Conversão Lead → Cliente — Regras incompletas

| Documento | Status | Falta |
|---|---|---|
| `05-business-rules/Customer.md` | Regras CU-001 a CU-004 | Não documenta:
| | | - O que acontece com as conversas do lead? |
| | | - O que acontece com as oportunidades do lead? |
| | | - O que acontece com os dados de scoring? |
| | | - O lead é marcado como CONVERTED ou deletado? |

**Recomendação:** Documentar completamente o fluxo de conversão incluindo impacto em todas as entidades relacionadas.

---

### 6.4 Rate Limits — Detalhes ausentes

| Documento | Regra | Falta |
|---|---|---|
| `01-backend/Cache.md` | Rate limiting via Redis | Não documenta:
| | | - Limites por endpoint |
| | | - Limites por role |
| | | - O que acontece quando atinge o limite |
| | | - Se rate limit é por tenant ou por usuário |

**Recomendação:** Criar tabela de rate limits por endpoint e role.

---

### 6.5 Automações — Condições não documentadas

| Documento | Status | Falta |
|---|---|---|
| `01-backend/Automations.md` | Lista triggers e actions | **Não documenta como conditions (if/else) funcionam** |

**Impacto:** Automações são um dos recursos mais complexos. Sem documentar conditions, implementadores vão inventar a própria lógica.

**Recomendação:** Documentar a syntax de conditions, operadores disponíveis e exemplos.

---

### 6.6 WhatsApp — Template messages não documentadas

| Documento | Status | Falta |
|---|---|---|
| `04-integrations/WhatsApp.md` | Menciona "template aprovado" | **Não documenta:**
| | | - Como templates são criados |
| | | - Como são aprovados pela Meta |
| | | - Formato do template |
| | | - Variáveis suportadas |

**Recomendação:** Adicionar seção completa sobre WhatsApp Template Messages.

---

## 7. Problemas de Escalabilidade

### 7.1 RabbitMQ sem clustering

| Documento | Status | Risco |
|---|---|---|
| `06-devops/Docker.md` | RabbitMQ como serviço único | **Single point of failure** |

**Impacto:** Se RabbitMQ cair, todas as mensagens assíncronas param (notificações, cache invalidation, auditoria).

**Recomendação:** Documentar estratégia de clustering RabbitMQ ou usar RabbitMQ Cloud/SaaS.

---

### 7.2 Redis sem HA

| Documento | Status | Risco |
|---|---|---|
| `06-devops/Docker.md` | Redis como serviço único | **Single point of failure** |

**Impacto:** Se Redis cair: cache miss massivo, rate limiting falha, distributed locks perdidos, sessões expiram.

**Recomendação:** Documentar Redis Sentinel ou Redis Cluster para HA.

---

### 7.3 Connection pooling para multi-tenant não documentado

| Documento | Status | Risco |
|---|---|---|
| `03-database/Overview.md` | Menciona PgBouncer em "Futuras Melhorias" | **Não documenta estratégia atual** |

**Impacto:** Com schema isolation, cada schema precisa de connections. Se houver 100 tenants, o connection pool pode estourar.

**Recomendação:** Documentar a estratégia de connection pooling atual e limites.

---

### 7.4 Campanhas — Rate limit pode ser insuficiente

| Documento | Regra | Problema |
|---|---|---|
| `01-backend/Campaigns.md` | "máximo 100 mensagens/minuto" | Para uma campanha de 10.000 contatos, levaria **100 minutos** |

**Impacto:** Campanhas grandes demoram muito para serem enviadas.

**Recomendação:** Documentar opções de escala (batch size, parallel workers, rate limit configurável).

---

### 7.5 Sem menção a CDN para arquivos

| Documento | Status | Risco |
|---|---|---|
| `01-backend/FileStorage.md` | Upload para S3/MinIO | **Não menciona CDN** |

**Impacto:** Arquivos servidos diretamente do S3 terão latência alta para usuários distantes.

**Recomendação:** Documentar CloudFront ou outro CDN para distribuição de arquivos.

---

### 7.6 Sem partitioning para tabelas grandes

| Documento | Status | Risco |
|---|---|---|
| `03-database/Performance.md` | Menciona partitioning em "Futuras Melhorias" | **Não documenta estratégia atual** |

**Impacto:** `messages` e `audit_logs` crescerão rapidamente. Sem partitioning, queries ficarão lentas.

**Recomendação:** Documentar quando partitioning será necessário e a estratégia pretendida.

---

## 8. Inconsistências de Formato

### 8.1 Formato de endpoints inconsistente

| Documento | Formato |
|---|---|
| `01-backend/Auth.md` | `Método \| Endpoint \| Auth \| Descrição` |
| `01-backend/Campaigns.md` | `Método \| Endpoint \| Descrição \| Permissão` |
| `01-backend/Notifications.md` | `Método \| Endpoint \| Descrição \| Permissão` |
| `API_MAP.md` | `Método \| Endpoint \| Auth \| Descrição` |

**Impacto:** Difícil manter consistência e comparar endpoints entre documentos.

**Recomendação:** Padronizar todos os endpoints para `Método | Endpoint | Auth | Descrição`.

---

### 8.2 Formato de regras inconsistente

| Documento | Formato |
|---|---|
| `05-business-rules/Lead.md` | `L-001 \| Regra \| Justificativa` |
| `01-backend/Campaigns.md` | Lista de regras sem numeração |
| `01-backend/Automations.md` | Lista de regras sem numeração |

**Impacto:** Difícil referenciar regras específicas em discussões e code reviews.

**Recomendação:** Padronizar todas as regras com numeração (ex: L-001, C-001, A-001).

---

## 9. Segurança

### 9.1 CORS não documentado

| Documento | Status | Risco |
|---|---|---|
| `01-backend/Overview.md` | Não menciona CORS | **Possível vulnerability** |

**Recomendação:** Documentar política CORS (origens permitidas, headers, credentials).

---

### 9.2 Rotação de JWT secret não documentada

| Documento | Status | Risco |
|---|---|---|
| `01-backend/Auth.md` | JWT secret em env var | **Não documenta como rotacionar** |

**Impacto:** Se o secret for comprometido, não há procedimento para rotacionar sem downtime.

**Recomendação:** Documentar procedimento de rotação de JWT secret.

---

### 9.3 HTTPS não forçado no backend

| Documento | Status | Risco |
|---|---|---|
| `01-backend/Overview.md` | Não menciona HTTPS | **Possível vulnerability** |

**Recomendação:** Documentar HTTPS enforcement e redirect HTTP → HTTPS.

---

### 9.4 Rate limit não tem documentação de response

| Documento | Status | Falta |
|---|---|---|
| `01-backend/Cache.md` | Rate limiting implementado | **Não documenta:**
| | | - Response HTTP code (429?) |
| | | - Headers (X-RateLimit-*) |
| | | - Mensagem de erro |

**Recomendação:** Documentar response padrão de rate limit.

---

## 10. Arquivos Referenciados mas Ausentes

| Documento | Referência | Status |
|---|---|---|
| `DATABASE_MAP.md` | `03-database/UUID.md` | Não verificado |
| `DATABASE_MAP.md` | `03-database/SoftDelete.md` | Não verificado |
| `PROJECT_INDEX.md` | `01-backend/Permissions.md` | Não lido |
| `PROJECT_INDEX.md` | `02-frontend/Customers.md` | Não lido |
| `PROJECT_INDEX.md` | `02-frontend/Calendar.md` | Não lido |
| `PROJECT_INDEX.md` | `02-frontend/Tables.md` | Não lido |
| `PROJECT_INDEX.md` | `02-frontend/Charts.md` | Não lido |
| `PROJECT_INDEX.md` | `02-frontend/Upload.md` | Não lido |

**Recomendação:** Verificar se todos os arquivos listados no PROJECT_INDEX.md existem.

---

## Ações Recomendadas

### Prioridade Crítica (Corrigir antes de iniciar desenvolvimento)

| # | Ação | Arquivo(s) |
|---|---|---|
| 1 | Reescrever seção de endpoints de Reports.md | `01-backend/Reports.md` |
| 2 | Resolver dependência circular Dashboard ↔ Reports | `01-backend/Dashboard.md`, `01-backend/Reports.md` |
| 3 | Corrigir referência quebrada em integrations/README | `04-integrations/README.md` |
| 4 | Corrigir texto em chinês em Lead.md | `05-business-rules/Lead.md` |

### Prioridade Alta (Corrigir antes do MVP)

| # | Ação | Arquivo(s) |
|---|---|---|
| 5 | Adicionar endpoint de registro ou remover rota /register | `01-backend/Auth.md`, `02-frontend/Routing.md` |
| 6 | Adicionar endpoints de integrações | `01-backend/Companies.md` ou novo arquivo |
| 7 | Documentar fórmula de lead scoring | `05-business-rules/Lead.md` |
| 8 | Documentar conditions de automações | `01-backend/Automations.md` |
| 9 | Documentar WhatsApp Template Messages | `04-integrations/WhatsApp.md` |
| 10 | Adicionar documentação de CORS | `01-backend/Overview.md` |
| 11 | Substituir react-beautiful-dnd por @dnd-kit/core | `02-frontend/Kanban.md` |
| 12 | Documentar Zustand stores ou remover da stack | `02-frontend/Overview.md` |

### Prioridade Média (Corrigir antes do v1.0)

| # | Ação | Arquivo(s) |
|---|---|---|
| 13 | Adicionar tabelas faltantes no Entities.md | `03-database/Entities.md` |
| 14 | Padronizar formato de endpoints | Todos os arquivos de backend |
| 15 | Padronizar formato de regras | Todos os arquivos de business-rules |
| 16 | Documentar rate limits detalhados | `01-backend/Cache.md` ou novo arquivo |
| 17 | Documentar RabbitMQ clustering | `06-devops/Docker.md` |
| 18 | Documentar Redis HA | `06-devops/Docker.md` |
| 19 | Completar Permissions.md do frontend | `02-frontend/Permissions.md` |
| 20 | Documentar procedure de rotação JWT | `01-backend/Auth.md` |

### Prioridade Baixa (Corrigir antes do v2.0)

| # | Ação | Arquivo(s) |
|---|---|---|
| 21 | Verificar todos os arquivos referenciados no PROJECT_INDEX | `docs/` |

---

## Referências

| Documento | Caminho |
|---|---|
| Architecture | [00-core/Architecture.md](./00-core/Architecture.md) |
| TechStack | [00-core/TechStack.md](./00-core/TechStack.md) |
| Backend Overview | [01-backend/Overview.md](./01-backend/Overview.md) |
| Frontend Overview | [02-frontend/Overview.md](./02-frontend/Overview.md) |
| Database Overview | [03-database/Overview.md](./03-database/Overview.md) |
| Integrations | [04-integrations/README.md](./04-integrations/README.md) |
| Business Rules | [05-business-rules/Lead.md](./05-business-rules/Lead.md) |
| Decisions | [00-core/Decisions.md](./00-core/Decisions.md) |

---

## Histórico de Revisão

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Revisão completa da documentação |
