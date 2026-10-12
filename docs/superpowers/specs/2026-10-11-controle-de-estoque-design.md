# Controle de estoque (masterdata/inventory) — design

Data: 2026-10-11 · Branch sugerida: `feat/inventory` (a partir de `crm-improvements-deploy-phase`)

## Objetivo

Controlar o estoque dos itens **de produto** do Catálogo da clínica:

1. O produto é cadastrado no **Catálogo** (já existe), agora com **unidade**, **estoque mínimo**
   e **categoria** controlada.
2. Na tela **/estoque**, a equipe vê a quantidade atual de cada produto, registra
   **movimentações** (entrada, saída, ajuste) e consulta o **histórico** de cada item.
3. Produtos com quantidade ≤ estoque mínimo são sinalizados com um badge de **baixo estoque**.

## Decisões (fechadas)

| Tema | Decisão |
|---|---|
| Entidade do item | **Estende `catalog_items`** (sem nova entidade de item) |
| Escopo | Estoque só para itens `PRODUCT`; serviços não têm estoque |
| Quantidade atual | **Coluna cache** em `catalog_items`, atualizada na transação do movimento; a **fonte da verdade é o histórico** de movimentos |
| Movimentações | Tipos `ENTRADA`, `SAIDA` e `AJUSTE` (ajuste = contagem real; grava a diferença) |
| Categoria | O campo `category` existente vira **lista fixa**, mas **somente para produtos**; serviços mantêm texto livre |
| Interface | Rota **/estoque** nova (movimentar/ver quantidade); cadastro do produto continua no **/catalog** |
| Formato da tela | **Lista + dialog** (registrar movimento) **+ sheet** (histórico) — uma única rota `/estoque` |

Categorias fixas (produtos): `EPI`, `INSTRUMENTO`, `MATERIAL_DE_CONSUMO`, `MEDICAMENTO`, `OUTROS`.
Unidades: `CAIXA`, `CARTELA`, `MILILITROS`, `PACOTE`, `ROLO`, `UNIDADE`.

## Escopo

**Entra**
- Migration `V095__inventory.sql`: colunas novas em `catalog_items`, tabela `inventory_movements`,
  RLS, permissões.
- Módulo backend `masterdata/inventory` (listar produtos, histórico e registrar movimento).
- Ajustes no módulo `masterdata/catalog` (novos campos, categoria/unidade).
- Frontend: feature `masterdata/inventory`, tela `/estoque`, ajustes no Catálogo e navegação.

**Fica de fora**
- Fornecedores, ordens de compra, notas fiscais, laboratório.
- Alertas/notificações automáticas de baixo estoque (apenas o badge na tela).
- Integração com o módulo financeiro (compra = lançamento).
- Alterar a resposta da tool de IA `searchCatalog` (permanece só com dados públicos atuais).
- Solicitação de prótese (kanban) e Campanhas — outros planos.

## Modelo de dados (migration `V095__inventory.sql`)

### `catalog_items` (alterações)
```sql
ALTER TABLE catalog_items
    ADD COLUMN unit        VARCHAR(20),
    ADD COLUMN min_stock   INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN quantity    INTEGER NOT NULL DEFAULT 0;

ALTER TABLE catalog_items
    ADD CONSTRAINT chk_catalog_items_unit CHECK (unit IS NULL OR unit IN
        ('CAIXA','CARTELA','MILILITROS','PACOTE','ROLO','UNIDADE')),
    ADD CONSTRAINT chk_catalog_items_min_stock CHECK (min_stock >= 0),
    ADD CONSTRAINT chk_catalog_items_quantity  CHECK (quantity  >= 0),
    ADD CONSTRAINT chk_catalog_items_category  CHECK (
        item_type <> 'PRODUCT' OR category IS NULL OR category IN
        ('EPI','INSTRUMENTO','MATERIAL_DE_CONSUMO','MEDICAMENTO','OUTROS'));

-- Reclassifica valores antigos de produtos que não estão na lista fixa.
UPDATE catalog_items SET category = NULL
 WHERE item_type = 'PRODUCT' AND category IS NOT NULL
   AND category NOT IN ('EPI','INSTRUMENTO','MATERIAL_DE_CONSUMO','MEDICAMENTO','OUTROS');

CREATE INDEX idx_catalog_items_company_type ON catalog_items (company_id, item_type);
```

### `inventory_movements` (nova)
```sql
CREATE TABLE inventory_movements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    catalog_item_id UUID NOT NULL REFERENCES catalog_items(id) ON DELETE CASCADE,
    movement_type   VARCHAR(20) NOT NULL,
    delta           INTEGER NOT NULL,          -- variação aplicada (com sinal)
    note            TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_inventory_movements_type CHECK
        (movement_type IN ('ENTRADA','SAIDA','AJUSTE'))
);

CREATE INDEX idx_inventory_movements_item
    ON inventory_movements (company_id, catalog_item_id, created_at DESC);

ALTER TABLE inventory_movements ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_movements FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON inventory_movements
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

GRANT SELECT, INSERT, UPDATE, DELETE ON inventory_movements TO crm_app;
```

### Permissões (padrão V078/V092)
```sql
INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('inventory:read',   'Read product stock',   'inventory', 'inventory', 'read'),
    ('inventory:manage', 'Manage product stock', 'inventory', 'inventory', 'manage')
ON CONFLICT (name) DO NOTHING;
-- Bloco DO $$ por empresa: inventory:read -> ADMIN/MANAGER/AGENT/VIEWER;
--                          inventory:manage -> ADMIN/MANAGER.
```

## Backend — módulo `com.becommerce.crm.masterdata.inventory`

Estrutura espelhando `masterdata/catalog` (hexagonal, mesmo padrão de portas):

- `domain/StockMovement.java` (+ enum `StockMovementType`), `domain/exception/InventoryItemNotFoundException.java`,
  `domain/exception/InsufficientStockException.java`.
- `application/dto/RegisterMovementRequest.java` (`@NotNull StockMovementType type`,
  `@NotNull @Positive Integer quantity`, `@Size(max=500) String note`),
  `application/dto/StockMovementResponse.java`, `application/dto/InventoryItemResponse.java`.
- `application/port/output/InventoryRepository.java` (porta de leitura de produtos + escrita de movimento/listagem).
- `application/service/InventoryService.java`.
- `infrastructure/persistence/*` (JPA entity/repository/adapter para `inventory_movements`).
- `web/InventoryController.java`.

### Regras de negócio
- Só itens `PRODUCT` aparecem/aceitam movimento (serviço → `InventoryItemNotFoundException`/422).
- `ENTRADA` → `delta = +quantity`; `SAIDA` → `delta = -quantity`; `AJUSTE` →
  `delta = quantity - quantidadeAtual` (quantity = contagem real informada).
- A quantidade nunca pode ficar **negativa**: se o movimento resultar < 0, lançar
  `InsufficientStockException` (409) e não persistir.
- Tudo numa **transação**: lock da linha do item (`SELECT ... FOR UPDATE`), calcula o novo
  saldo, atualiza `catalog_items.quantity`, insere `inventory_movements` e grava auditoria
  (`AuditModule.INVENTORY`, `AuditAction.CREATE`).
- `TenantContext.setCompanyId(...)`/`clear()` em `finally` como no `CatalogService`.

### Endpoints (base `/api/v1/companies/{companyId}/inventory`)
| Método | Rota | Permissão | Descrição |
|---|---|---|---|
| GET | `/` | `inventory:read` | Lista produtos com `quantity`/`minStock`/`unit`. Filtros: `q`, `category`, `lowStock`, `active`, `page`, `pageSize`. Retorna `PageResponse<InventoryItemResponse>` |
| GET | `/{itemId}/movements` | `inventory:read` | Histórico paginado de movimentos do item |
| POST | `/{itemId}/movements` | `inventory:manage` | Registra movimento; retorna 201 com o movimento e o saldo atualizado |

`lowStock=true` filtra `min_stock > 0 AND quantity <= min_stock` (mesma regra do badge de baixo estoque).

### Ajustes no módulo `catalog`
- `CatalogItem` (domain): novos campos `unit`, `minStock`, `quantity`; `create`/`reconstitute`/`update`
  atualizados; `quantity` não é editável por `update` (só via movimento).
- `CatalogItemJpaEntity`, `CatalogItemRepositoryImpl`, `CatalogItemRequest`/`Response` atualizados
  (`unit`, `minStock`; `quantity` só na resposta).
- **Cross-cutting:** `AuditModule` += `INVENTORY`; `RoleSeedService` += `inventory:read`/`inventory:manage`
  nas listas ADMIN/MANAGER/AGENT/VIEWER; registrar novas exceptions no `GlobalExceptionHandler`.

## Frontend

### Feature nova `frontend-refine/src/features/masterdata/inventory`
- `types/inventory.types.ts`: `Unit`, `UNIT_LABELS`, `StockMovementType`, `STOCK_MOVEMENT_TYPE_LABELS`,
  `InventoryItem`, `InventoryFilters`, `InventoryPage`, `StockMovement`, `StockMovementPage`,
  `RegisterMovementRequest`, helper `isLowStock(item)`.
- `services/inventory.service.ts`: `base = /companies/${companyId}/inventory`; `list`, `listMovements`,
  `registerMovement`.
- `hooks/useInventory.ts`: `useInventory`, `useItemMovements`, `useRegisterMovement`
  (com `useMutationDefaults`, `invalidateKeys: [["inventory", companyId]]`).
- `components/`: `StockMovementDialog.tsx` (entrada/saída/ajuste + observação),
  `MovementHistorySheet.tsx` (lista paginada dos movimentos), `LowStockBadge.tsx`.

### Página `/estoque` (`src/app/(dashboard)/estoque/page.tsx`)
- `useAuth` → `companyId`; `can("inventory:manage")` libera a ação **Movimentar**.
- Tabela: nome, categoria, unidade, **quantidade**, **mínimo**, badge **Baixo estoque** quando
  `quantity <= minStock`. Filtros: busca (`useDebouncedValue`), categoria, "só baixo estoque", status.
- Ação por linha: **Movimentar** (abre `StockMovementDialog`) e **Histórico** (abre `MovementHistorySheet`).
- Estados: `SkeletonTable`, `ErrorCard` (retry), `EmptyState`; paginação "Página X de Y".

### Ajustes no Catálogo
- `catalog.types.ts`: `unit`, `minStock`, `quantity` no `CatalogItem`; `unit`/`minStock` no request;
  `CATALOG_CATEGORY_LABELS` e `UNIT_LABELS`.
- `CatalogItemDialog.tsx`: `category` vira `Select` fixo **quando `type = PRODUCT`** (texto livre para
  `SERVICE`); exibe `unidade` (`Select`) e `estoque mínimo` (input) só para `PRODUCT`.

### Navegação
- `ROUTES.INVENTORY = "/estoque"` em `src/lib/constants.ts`.
- Item no grupo **CRM**, após **Catálogo** (`icon: Boxes`, `permission: "inventory:read"`).
- `Breadcrumb.tsx`: `estoque: "Estoque"`.

## Testes

**Backend**
- `InventoryServiceTest` (Mockito): entrada/saída/ajuste, saldo negativo rejeitado, item de serviço rejeitado.
- `InventoryControllerTest` (MockMvc standalone, padrão `ContactControllerTest`): 201/403/409.
- `InventoryIsolationIT` (Testcontainers, padrão `CatalogIsolationIT`): isolamento RLS, FK cascade do item.
- Ajustar `CatalogServiceTest`/`CatalogIsolationIT` para os novos campos.

**Frontend**
- `src/__tests__/features/masterdata/inventory/services/inventory.service.test.ts` (mock `@/lib/api`).
- `.../hooks/useInventory.test.ts` (`renderHook` + `QueryClientProvider`).
- `.../components/StockMovementDialog.test.tsx` e teste do ajuste de `CatalogItemDialog`.
- `frontend-refine/e2e/estoque.spec.ts` (modelado em `catalog.spec.ts`: cadastra produto, movimenta, confere saldo).

**Verificação**
- Backend: `cd backend && mvn -q verify`.
- Frontend: `npm run lint && npx tsc --noEmit && npx prettier --check "src/**/*.{ts,tsx,css}" && npx vitest run --pool=threads`.

## Riscos / pontos de atenção

- A `CHECK` de categoria vale só para `PRODUCT`; garantir que o `Select` do Catálogo só aparece para produto
  e que o backend não aceite categoria fora da lista em produto (422).
- `quantity` fica como cache: os testes de isolamento/consistência devem cobrir a atualização transacional
  (nenhum caminho de escrita de `quantity` fora do `InventoryService`).
- Migração de dados: categorias antigas de produtos viram `NULL` (o usuário reclassifica). Serviços não são tocados.

## Verificação de escopo (checklist)

- [ ] Migration `V095` com colunas, tabela, RLS, GRANT e permissões + seed de papéis.
- [ ] Módulo `masterdata/inventory` (domain/application/infrastructure/web) e `AuditModule.INVENTORY`.
- [ ] `catalog` estendido (domain/DTOs/entity/repo) sem quebrar a tool de IA.
- [ ] Feature `masterdata/inventory` + página `/estoque` + ajustes no Catálogo + navegação.
- [ ] Testes backend e frontend verdes; `prettier`/`lint`/`tsc` ok nos arquivos novos.
