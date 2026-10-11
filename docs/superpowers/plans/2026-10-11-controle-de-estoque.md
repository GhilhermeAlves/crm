# Controle de estoque Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Estender o Catálogo com estoque (unidade, mínimo, quantidade) e criar a tela `/estoque` para registrar movimentações (entrada/saída/ajuste) e ver o histórico.

**Architecture:** Hexagonal espelhando `masterdata/catalog`. Os campos de estoque (`unit`, `min_stock`, `quantity`) são colunas novas em `catalog_items`; a fonte da verdade é a nova tabela `inventory_movements` (RLS FORCE por `company_id`), e a quantidade atual é uma coluna cache atualizada na mesma transação. Permissões `inventory:read`/`inventory:manage` semeadas na migration e no `RoleSeedService`.

**Tech Stack:** Spring Boot (backend/), Next.js App Router + TanStack Query + shadcn/ui (frontend-refine/), PostgreSQL + Flyway, JUnit 5 + Mockito + Testcontainers, Vitest + Playwright.

**Spec:** `docs/superpowers/specs/2026-10-11-controle-de-estoque-design.md`

## Global Constraints

- Trabalhar na branch `feat/inventory` criada a partir de **`feat/finance`** (não da `crm-improvements-deploy-phase`): o `RoleSeedService` com `finance:*` e as migrations V092–V094 só existem lá. Se o PR do financeiro já tiver sido mergeado, pode partir da `crm-improvements-deploy-phase`. O estoque **nunca** vai para produção antes do financeiro (o Flyway rejeitaria V092–V094 fora de ordem depois da V095).
- **Nunca editar migrations antigas** (checksum do Flyway). A próxima migration é `V095`.
- Padrão RLS (cópia de V078/V092): `ENABLE/FORCE ROW LEVEL SECURITY`, `tenant_isolation_policy` com `app.current_tenant_id()`, e `GRANT SELECT, INSERT, UPDATE, DELETE ... TO crm_app` — sem GRANT, o `crm_app` (NOBYPASSRLS) retorna 500 na escrita.
- Permissões são linhas em `permissions` (`resource:action`, ex. `inventory:read`) + bloco `DO $$` por empresa em `role_permissions`. Novo tenant recebe na migração E no `RoleSeedService`.
- Backend segue o padrão `masterdata/catalog` (domain/application/{dto,port/output,service}/infrastructure/persistence/web), `@CurrentCompanyId(OWN_COMPANY)` + `@PreAuthorize("hasAuthority(...)")`, `TenantContext.setCompanyId`/`clear` em `finally`, auditoria via `TenantAuditRecorder.record(companyId, action, module, entity, id, description, diff, metadata)`.
- Textos visíveis em pt-BR. Valores monetários em centavos (não se aplica ao estoque; quantidades são `int`).
- Visual: componentes de `frontend-refine/src/components/ui/` + tokens do tema. Não usar cores hex fixas.
- Checkstyle: linhas até 200 caracteres. `prettier` do repo: `printWidth: 100`, `singleQuote: false`, `trailingComma: "all"`.
- Verificação local backend: `cd backend && mvn -q test` (unitários, surefire). O `mvn -q verify` roda os ITs de Testcontainers — exige Docker → **só no CI** (esta máquina não tem Docker).
- Verificação frontend: `cd frontend-refine && npm run lint && npx tsc --noEmit && npx prettier --check "src/**/*.{ts,tsx,css}" && npx vitest run --pool=threads`. Vitest instável com o pool default → **sempre** `--pool=threads`. Sem `user-event`/jest-dom → `fireEvent`; fallback inline de `ResizeObserver` nos testes de componente que renderizam gráfico/tabela.
- `npm run format:check` do repo reporta ~460 arquivos pré-existentes — verificar (prettier) apenas os arquivos novos/editados.
- Um commit por task (Conventional Commits, ex. `feat(inventory): ...`). Não fazer push/merge.
- Baixo estoque = `min_stock > 0 AND quantity <= min_stock` (mesma regra no badge e no filtro `lowStock`).

---

## Task 1: Migration `V095__inventory.sql`

Colunas de estoque em `catalog_items`, categoria fixa só para produtos, tabela `inventory_movements`, RLS, grants e permissões.

**Files:**
- Create: `backend/src/main/resources/db/migration/V095__inventory.sql`

**Interfaces:**
- Produces: tabela `inventory_movements` (`movement_type` em `('ENTRADA','SAIDA','AJUSTE')`, `delta INTEGER`, `company_id`, `catalog_item_id`), colunas `unit`/`min_stock`/`quantity` em `catalog_items`, permissões `inventory:read`/`inventory:manage`.

- [ ] **Step 1: Escrever a migration**

`backend/src/main/resources/db/migration/V095__inventory.sql`:

```sql
-- V095__inventory.sql
-- Controle de estoque dos itens de PRODUTO do catálogo (V078):
--   - catalog_items ganha unit, min_stock e quantity (cache; fonte da verdade
--     é inventory_movements, atualizada na mesma transação).
--   - category de produtos vira lista fixa (serviços seguem com texto livre).
--   - inventory_movements registra ENTRADA/SAIDA/AJUSTE por item.
-- Permissões: inventory:read (todos) e inventory:manage (ADMIN/MANAGER).

-- ===========================================================================
-- catalog_items (alterações)
-- ===========================================================================
ALTER TABLE catalog_items
    ADD COLUMN unit        VARCHAR(20),
    ADD COLUMN min_stock   INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN quantity    INTEGER NOT NULL DEFAULT 0;

-- Categorias antigas de produtos que não estão na lista fixa deixam de ser
-- classificadas (NULL) para o usuário reclassificar. Serviços não são tocados.
-- Precisa vir ANTES do CHECK (que valida as linhas existentes) e rodar por
-- empresa: catalog_items tem RLS FORCE, então sem app.current_company_id o
-- UPDATE não enxerga nenhuma linha.
DO $$
DECLARE c RECORD;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::text, false);
        UPDATE catalog_items
           SET category = NULL
         WHERE item_type = 'PRODUCT' AND category IS NOT NULL
           AND category NOT IN ('EPI','INSTRUMENTO','MATERIAL_DE_CONSUMO','MEDICAMENTO','OUTROS');
    END LOOP;
    PERFORM set_config('app.current_company_id', NULL::text, false);
END $$;

ALTER TABLE catalog_items
    ADD CONSTRAINT chk_catalog_items_unit CHECK (unit IS NULL OR unit IN
        ('CAIXA','CARTELA','MILILITROS','PACOTE','ROLO','UNIDADE')),
    ADD CONSTRAINT chk_catalog_items_min_stock CHECK (min_stock >= 0),
    ADD CONSTRAINT chk_catalog_items_quantity  CHECK (quantity  >= 0),
    ADD CONSTRAINT chk_catalog_items_category  CHECK (
        item_type <> 'PRODUCT' OR category IS NULL OR category IN
        ('EPI','INSTRUMENTO','MATERIAL_DE_CONSUMO','MEDICAMENTO','OUTROS')),
    -- Alvo da FK composta de inventory_movements (impede movimento de um
    -- tenant apontar para item de outro: a checagem de FK ignora RLS).
    ADD CONSTRAINT uq_catalog_items_id_company UNIQUE (id, company_id);

CREATE INDEX idx_catalog_items_company_type ON catalog_items (company_id, item_type);

-- ===========================================================================
-- inventory_movements
-- ===========================================================================
CREATE TABLE inventory_movements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    catalog_item_id UUID NOT NULL,
    movement_type   VARCHAR(20) NOT NULL,
    delta           INTEGER NOT NULL,
    note            TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- FK composta: o item precisa ser da MESMA empresa do movimento.
    CONSTRAINT fk_inventory_movements_item FOREIGN KEY (catalog_item_id, company_id)
        REFERENCES catalog_items (id, company_id) ON DELETE CASCADE,
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

-- ===========================================================================
-- Permissões do módulo.
-- ===========================================================================
INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('inventory:read',   'Read product stock',   'inventory', 'inventory', 'read'),
    ('inventory:manage', 'Manage product stock', 'inventory', 'inventory', 'manage')
ON CONFLICT (name) DO NOTHING;

DO $$
DECLARE c RECORD; inserted INTEGER;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::text, false);

        INSERT INTO role_permissions (role_id, permission_id)
        SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
        WHERE (p.name = 'inventory:read' AND r.name IN ('ADMIN', 'MANAGER', 'AGENT', 'VIEWER'))
           OR (p.name = 'inventory:manage' AND r.name IN ('ADMIN', 'MANAGER'))
        ON CONFLICT (role_id, permission_id) DO NOTHING;

        GET DIAGNOSTICS inserted = ROW_COUNT;
        IF inserted > 0 THEN
            RAISE NOTICE 'V095: granted % inventory permissions for company %', inserted, c.id;
        END IF;
    END LOOP;
    PERFORM set_config('app.current_company_id', NULL::text, false);
END $$;
```

- [ ] **Step 2: Garantir que o resource compila**

Run: `cd backend && mvn -q -DskipTests compile`
Expected: `BUILD SUCCESS`. (A migration em si só é executada em runtime; a Task 5 a valida de verdade.)

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/db/migration/V095__inventory.sql
git commit -m "feat(inventory): catalog fields and inventory movements schema (V095)"
```

---

## Task 2: Backend — campos de estoque no Catálogo

Estende o domínio/banco do `CatalogItem` com `unit`, `minStock`, `quantity` (cache). `quantity` não é editável pelo CRUD do catálogo — muda só via movimentação.

**Files:**
- Modify: `backend/src/main/java/com/becommerce/crm/masterdata/catalog/domain/CatalogItem.java`
- Modify: `backend/src/main/java/com/becommerce/crm/masterdata/catalog/application/dto/CatalogItemRequest.java`
- Modify: `backend/src/main/java/com/becommerce/crm/masterdata/catalog/application/dto/CatalogItemResponse.java`
- Modify: `backend/src/main/java/com/becommerce/crm/masterdata/catalog/infrastructure/persistence/CatalogItemJpaEntity.java`
- Modify: `backend/src/main/java/com/becommerce/crm/masterdata/catalog/infrastructure/persistence/CatalogItemRepositoryImpl.java` (linhas 55-76: `toDomain`/`toEntity`)
- Modify: `backend/src/main/java/com/becommerce/crm/masterdata/catalog/application/service/CatalogService.java`
- Modify (testes): `backend/src/test/java/com/becommerce/crm/masterdata/catalog/domain/CatalogItemTest.java`, `backend/src/test/java/com/becommerce/crm/masterdata/catalog/application/service/CatalogServiceTest.java`

**Interfaces:**
- Produces: `CatalogItem.create(...)` e `update(...)` **ganham** `String unit, Integer minStock`; `reconstitute(...)` ganha `String unit, Integer minStock, int quantity`; novo método `CatalogItem.updateQuantity(int newQuantity)`; getters `getUnit()`, `getMinStock()`, `getQuantity()`. `CatalogItemRequest` ganha `String unit, Integer minStock`; `CatalogItemResponse` ganha `String unit, Integer minStock, int quantity`. Entities mapeiam colunas `unit`/`min_stock`/`quantity`.
- Consumed by: Task 3 (InventoryService usa `getQuantity()/getUnit()/updateQuantity()`).

- [ ] **Step 1: Escrever os testes que falham**

Em `backend/src/test/java/com/becommerce/crm/masterdata/catalog/domain/CatalogItemTest.java`, adicione:

```java
@Test
void createProductNormalizesStockFields() {
    CatalogItem item = CatalogItem.create(COMPANY, CatalogItemType.PRODUCT, "Luva", null,
            "EPI", new BigDecimal("10.00"), "BRL", null, "CAIXA", 5);
    assertEquals("CAIXA", item.getUnit());
    assertEquals(5, item.getMinStock());
    assertEquals(0, item.getQuantity());
}

@Test
void updateQuantityRejectsNegative() {
    CatalogItem item = CatalogItem.create(COMPANY, CatalogItemType.PRODUCT, "A", null, null, null,
            null, null, null, 0);
    assertThrows(IllegalArgumentException.class, () -> item.updateQuantity(-1));
}
```

Em `backend/src/test/java/com/becommerce/crm/masterdata/catalog/application/service/CatalogServiceTest.java`, atualize o helper e adicione:

```java
private static CatalogItemRequest request(String sku) {
    return new CatalogItemRequest(CatalogItemType.PRODUCT, "Plano Pro", null, null,
            new BigDecimal("10.00"), "BRL", sku, null, null);
}
```
E em todos os `CatalogItem.create(...)` do arquivo acrescente `null, 0` após o `sku` (novos args `unit, minStock`), ex.: linha 64 vira `CatalogItem.create(COMPANY, CatalogItemType.PRODUCT, "A", null, null, null, null, "SKU-1", null, 0)`.

Adicione também o teste de normalização de serviço (unit/minStock viram null/0):

```java
@Test
void serviceItemNeverHasStockFields() {
    service.create(COMPANY, new CatalogItemRequest(CatalogItemType.SERVICE, "Consulta", null,
            "Consultas", null, "BRL", null, "CAIXA", 7));
    verify(repository).save(argThat(i ->
            i.getType() == CatalogItemType.SERVICE && i.getUnit() == null && i.getMinStock() == 0));
}
```
(importe `static org.mockito.ArgumentMatchers.argThat`.)

- [ ] **Step 2: Rodar para ver falhar**

Run: `cd backend && mvn -q test -Dtest=CatalogItemTest,CatalogServiceTest`
Expected: falha de compilação (assinaturas de `create`/`reconstitute` sem os novos parâmetros).

- [ ] **Step 3: Implementar o domínio e DTOs**

`CatalogItem.java`:
- Adicione campos `private String unit; private int minStock; private int quantity;`.
- `create(..., String sku, String unit, Integer minStock)`: `return new CatalogItem(UUID.randomUUID(), companyId, type, name, description, category, price, currency, sku, unit, minStock != null ? minStock : 0, 0, true, now, now);`
- `reconstitute(..., String sku, boolean active, String unit, Integer minStock, int quantity, LocalDateTime createdAt, LocalDateTime updatedAt)`.
- `update(..., String sku, String unit, Integer minStock)` — chama `apply(...)` estendido.
- Em `apply(...)`, `this.unit = blankToNull(unit); this.minStock = Math.max((minStock != null ? minStock : 0), 0); ` e preserve `quantity` (não é alterada por `update`).
- Novo método com invariante:

```java
public void updateQuantity(int newQuantity) {
    if (newQuantity < 0) {
        throw new IllegalArgumentException("Quantidade em estoque não pode ser negativa: " + newQuantity);
    }
    this.quantity = newQuantity;
    touch();
}
```

`CatalogItemRequest.java` — adicione à listagem do record:

```java
        @Size(max = 20) String unit,
        @Min(0) Integer minStock
```
(imports: `jakarta.validation.constraints.Min`.)

`CatalogItemResponse.java` — adicione campos `String unit, Integer minStock, int quantity` e preencha no `from(...)`.

- [ ] **Step 4: Entity + mapeamento**

`CatalogItemJpaEntity.java`: campos `@Column(name = "unit") private String unit;`, `@Column(name = "min_stock") private int minStock;`, `@Column(name = "quantity") private int quantity;` + getters/setters.

`CatalogItemRepositoryImpl.java`:
- `toDomain(...)`: `..., e.isActive(), e.getUnit(), e.getMinStock(), e.getQuantity(), e.getCreatedAt(), e.getUpdatedAt()`.
- `toEntity(...)`: `e.setUnit(i.getUnit()); e.setMinStock(i.getMinStock()); e.setQuantity(i.getQuantity());`.

- [ ] **Step 5: Validar/normalizar no service**

`CatalogService.java` — na `create` e `update`, normalize antes de chamar o domínio. Adicione um helper privado e troque as chamadas:

```java
private String normalizeCategory(CatalogItemType type, String category) {
    String normalized = blankToNull(category);
    if (type == CatalogItemType.PRODUCT && normalized != null
            && !Set.of("EPI", "INSTRUMENTO", "MATERIAL_DE_CONSUMO", "MEDICAMENTO", "OUTROS")
                .contains(normalized)) {
        throw new InvalidCatalogItemException("Categoria inválida para produto: " + normalized);
    }
    return normalized;
}

private String normalizeUnit(CatalogItemType type, String unit) {
    String normalized = blankToNull(unit);
    if (type == CatalogItemType.PRODUCT && normalized != null
            && !Set.of("CAIXA", "CARTELA", "MILILITROS", "PACOTE", "ROLO", "UNIDADE")
                .contains(normalized)) {
        throw new InvalidCatalogItemException("Unidade inválida para produto: " + normalized);
    }
    return type == CatalogItemType.PRODUCT ? normalized : null;
}
```
(import `java.util.Set`.)

O `GlobalExceptionHandler` **não** mapeia `IllegalArgumentException` (viraria 500). Crie
`backend/src/main/java/com/becommerce/crm/masterdata/catalog/domain/exception/InvalidCatalogItemException.java`:

```java
package com.becommerce.crm.masterdata.catalog.domain.exception;

public class InvalidCatalogItemException extends RuntimeException {
    public InvalidCatalogItemException(String message) {
        super(message);
    }
}
```

E no `GlobalExceptionHandler.java` (import + handler 400, ao lado de `handleCatalogSkuConflict`):

```java
    @ExceptionHandler(InvalidCatalogItemException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCatalogItem(InvalidCatalogItemException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }
```

Adicione em `CatalogServiceTest` um teste de categoria fora da lista para produto →
`assertThrows(InvalidCatalogItemException.class, ...)`. Em `create`, passe `normalizeCategory(request.type(), request.category())`, `request.type(), request.unit()` → `normalizeUnit(...)`, e `request.type(), request.minStock()` → `request.type() == CatalogItemType.PRODUCT ? (request.minStock() != null ? request.minStock() : 0) : 0`. Aplique o mesmo em `update`.

- [ ] **Step 6: Rodar os testes**

Run: `cd backend && mvn -q test`
Expected: todos os testes passam (verifique `BUILD SUCCESS` e zero falhas).

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/becommerce/crm/masterdata/catalog backend/src/test/java/com/becommerce/crm/masterdata/catalog \
  backend/src/main/java/com/becommerce/crm/shared/web/handler/GlobalExceptionHandler.java
git commit -m "feat(catalog): stock fields on catalog items (unit, min stock, quantity)"
```

---

## Task 3: Backend — módulo `masterdata/inventory` (domínio + service + persistência + controller + wiring)

Todo o módulo de estoque num só task (espelha a criação do módulo `catalog`): porta de produtos do catálogo (listagem de produtos + lock), domínio/porta de movimentos, service com as regras, adapter JPA, controller e o cross-cutting (permissões no seed, auditoria, handler).

**Files:**
- Create: `backend/src/main/java/com/becommerce/crm/masterdata/inventory/domain/StockMovementType.java`
- Create: `.../inventory/domain/StockMovement.java`
- Create: `.../inventory/domain/exception/InventoryItemNotFoundException.java`
- Create: `.../inventory/domain/exception/InsufficientStockException.java`
- Create: `.../inventory/application/dto/RegisterMovementRequest.java`
- Create: `.../inventory/application/dto/StockMovementResponse.java`
- Create: `.../inventory/application/dto/InventoryItemResponse.java`
- Create: `.../inventory/application/port/output/StockMovementRepository.java`
- Create: `.../inventory/application/service/InventoryService.java`
- Create: `.../inventory/infrastructure/persistence/InventoryMovementJpaEntity.java`
- Create: `.../inventory/infrastructure/persistence/InventoryMovementJpaRepository.java`
- Create: `.../inventory/infrastructure/persistence/InventoryMovementRepositoryImpl.java`
- Create: `.../inventory/web/InventoryController.java`
- Create (testes): `backend/src/test/java/com/becommerce/crm/masterdata/inventory/application/service/InventoryServiceTest.java`, `backend/src/test/java/com/becommerce/crm/masterdata/inventory/web/InventoryControllerTest.java`
- Modify: `backend/src/main/java/com/becommerce/crm/masterdata/catalog/application/port/output/CatalogItemRepository.java` (novos métodos)
- Modify: `backend/src/main/java/com/becommerce/crm/masterdata/catalog/infrastructure/persistence/CatalogItemJpaRepository.java` (`searchProducts`, `findByIdForUpdate`)
- Modify: `backend/src/main/java/com/becommerce/crm/masterdata/catalog/infrastructure/persistence/CatalogItemRepositoryImpl.java` (implementar os novos métodos)
- Modify: `backend/src/main/java/com/becommerce/crm/analytics/audit/domain/AuditModule.java` (+ `INVENTORY`)
- Modify: `backend/src/main/java/com/becommerce/crm/identity/infrastructure/persistence/RoleSeedService.java` (+ permissões nos 4 papéis)
- Modify: `backend/src/main/java/com/becommerce/crm/shared/web/handler/GlobalExceptionHandler.java` (imports + handlers)

**Interfaces:**
- Consumes: Task 2 (`CatalogItem.getUnit/getMinStock/getQuantity/updateQuantity`, `CatalogItemType`), `PageResponse.of(content, page, pageSize, total)` (`shared/application/dto/PageResponse.java`), `TenantAuditRecorder`, `CurrentCompanyId`.
- Produces (consumidos pela Task 4/Isolation IT e por todo o frontend): `InventoryService.listProducts(UUID companyId, String query, String category, Boolean lowStock, Boolean active, int page, int pageSize) → PageResponse<InventoryItemResponse>`; `InventoryService.listMovements(UUID companyId, UUID itemId, int page, int pageSize) → PageResponse<StockMovementResponse>`; `InventoryService.registerMovement(UUID companyId, UUID itemId, RegisterMovementRequest) → StockMovementResponse` (com `balanceAfter`); `CatalogItemRepository.searchProducts(UUID, String, String, Boolean, Boolean, int, int) → PageResult`; `CatalogItemRepository.findByIdForUpdate(UUID) → Optional<CatalogItem>`.

- [ ] **Step 1: Portas do catálogo (listagem de produtos + lock)**

`CatalogItemRepository.java` (interface) — adicione:

```java
    /** Produtos (itemType = PRODUCT) com estoque. {@code lowStock} filtra min_stock > 0 e quantity <= min_stock. */
    PageResult searchProducts(UUID companyId, String query, String category, Boolean lowStock,
                              Boolean active, int page, int pageSize);

    /** Lock pessimista de linha para registrar movimentação (PESSIMISTIC_WRITE). */
    Optional<CatalogItem> findByIdForUpdate(UUID id);
```

`CatalogItemJpaRepository.java` — adicione:

```java
    @Query("SELECT i FROM CatalogItemJpaEntity i WHERE i.companyId = :companyId "
            + "AND i.itemType = 'PRODUCT' "
            + "AND (:active IS NULL OR i.active = :active) "
            + "AND (:lowStock IS NULL OR NOT (:lowStock = TRUE) "
            + "     OR (i.minStock > 0 AND i.quantity <= i.minStock)) "
            + "AND (:category IS NULL OR i.category = :category) "
            + "AND (:pattern IS NULL OR LOWER(i.name) LIKE :pattern "
            + "     OR LOWER(COALESCE(i.category, '')) LIKE :pattern "
            + "     OR LOWER(COALESCE(i.sku, '')) LIKE :pattern)")
    Page<CatalogItemJpaEntity> searchProducts(@Param("companyId") UUID companyId,
                                              @Param("category") String category,
                                              @Param("lowStock") Boolean lowStock,
                                              @Param("active") Boolean active,
                                              @Param("pattern") String pattern,
                                              Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM CatalogItemJpaEntity i WHERE i.id = :id")
    Optional<CatalogItemJpaEntity> findByIdForUpdate(@Param("id") UUID id);
```
(imports: `jakarta.persistence.LockModeType`, `org.springframework.data.jpa.repository.Lock`.)

`CatalogItemRepositoryImpl.java` — implemente:

```java
    @Override
    public PageResult searchProducts(UUID companyId, String query, String category, Boolean lowStock,
                                     Boolean active, int page, int pageSize) {
        String pattern = query == null ? null : "%" + escapeLike(query.toLowerCase(Locale.ROOT)) + "%";
        Page<CatalogItemJpaEntity> result = jpaRepository.searchProducts(companyId,
                category == null ? null : category, lowStock, active, pattern,
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.ASC, "name")));
        return new PageResult(result.getContent().stream().map(CatalogItemRepositoryImpl::toDomain).toList(),
                result.getTotalElements());
    }

    @Override
    public Optional<CatalogItem> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(CatalogItemRepositoryImpl::toDomain);
    }
```

- [ ] **Step 2: Escrever o teste unitário do service que falha**

`backend/src/test/java/com/becommerce/crm/masterdata/inventory/application/service/InventoryServiceTest.java`:

```java
package com.becommerce.crm.masterdata.inventory.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.masterdata.catalog.application.port.output.CatalogItemRepository;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItem;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItemType;
import com.becommerce.crm.masterdata.inventory.application.dto.RegisterMovementRequest;
import com.becommerce.crm.masterdata.inventory.application.port.output.StockMovementRepository;
import com.becommerce.crm.masterdata.inventory.domain.StockMovementType;
import com.becommerce.crm.masterdata.inventory.domain.exception.InsufficientStockException;
import com.becommerce.crm.masterdata.inventory.domain.exception.InventoryItemNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryServiceTest {

    private static final UUID COMPANY = UUID.randomUUID();

    private CatalogItemRepository catalogRepository;
    private StockMovementRepository movementRepository;
    private InventoryService service;

    @BeforeEach
    void setUp() {
        catalogRepository = mock(CatalogItemRepository.class);
        movementRepository = mock(StockMovementRepository.class);
        service = new InventoryService(catalogRepository, movementRepository, mock(TenantAuditRecorder.class));
        when(catalogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(movementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private CatalogItem product(int quantity) {
        return CatalogItem.reconstitute(UUID.randomUUID(), COMPANY, CatalogItemType.PRODUCT, "Luva",
                null, "EPI", null, "BRL", "SKU", true, "CAIXA", 5, quantity,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
    }

    @Test
    void entradaIncreasesQuantityAndStoresPositiveDelta() {
        CatalogItem item = product(2);
        when(catalogRepository.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));

        var response = service.registerMovement(COMPANY, item.getId(),
                new RegisterMovementRequest(StockMovementType.ENTRADA, 10, "Compra"));

        assertEquals(12, response.balanceAfter());
        verify(movementRepository).save(any());
    }

    @Test
    void saidaDecreasesQuantity() {
        CatalogItem item = product(10);
        when(catalogRepository.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));

        var response = service.registerMovement(COMPANY, item.getId(),
                new RegisterMovementRequest(StockMovementType.SAIDA, 3, "Uso"));

        assertEquals(7, response.balanceAfter());
    }

    @Test
    void ajusteComputesDeltaFromRealCount() {
        CatalogItem item = product(10);
        when(catalogRepository.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));

        var response = service.registerMovement(COMPANY, item.getId(),
                new RegisterMovementRequest(StockMovementType.AJUSTE, 6, "Contagem"));

        assertEquals(6, response.balanceAfter());
    }

    @Test
    void saidaBelowZeroIsRejectedAndNothingPersists() {
        CatalogItem item = product(2);
        when(catalogRepository.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));

        assertThrows(InsufficientStockException.class, () -> service.registerMovement(COMPANY,
                item.getId(), new RegisterMovementRequest(StockMovementType.SAIDA, 5, null)));
        verify(movementRepository, never()).save(any());
    }

    @Test
    void serviceItemIsNotAStockItem() {
        CatalogItem serviceItem = CatalogItem.reconstitute(UUID.randomUUID(), COMPANY,
                CatalogItemType.SERVICE, "Consulta", null, null, null, "BRL", null, true, null, 0, 0,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        when(catalogRepository.findByIdForUpdate(serviceItem.getId())).thenReturn(Optional.of(serviceItem));

        assertThrows(InventoryItemNotFoundException.class, () -> service.registerMovement(COMPANY,
                serviceItem.getId(), new RegisterMovementRequest(StockMovementType.ENTRADA, 1, null)));
    }

    @Test
    void itemOfAnotherCompanyIsNotFound() {
        CatalogItem other = product(1);
        when(catalogRepository.findByIdForUpdate(other.getId())).thenReturn(Optional.of(other));

        assertThrows(InventoryItemNotFoundException.class, () -> service.registerMovement(
                UUID.randomUUID(), other.getId(), new RegisterMovementRequest(
                        StockMovementType.ENTRADA, 1, null)));
    }
}
```

- [ ] **Step 3: Rodar para ver falhar**

Run: `cd backend && mvn -q test -Dtest=InventoryServiceTest`
Expected: fail (classes não existem).

- [ ] **Step 4: Implementar domínio, DTOs e a porta de movimentos**

`StockMovementType.java`:
```java
package com.becommerce.crm.masterdata.inventory.domain;

public enum StockMovementType {
    ENTRADA,
    SAIDA,
    AJUSTE
}
```

`StockMovement.java` (padrão de `CatalogItem`: POJO imutável em `id`/`companyId`/`catalogItemId`/`createdAt`, factory `create`/`reconstitute`, getters):

```java
package com.becommerce.crm.masterdata.inventory.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class StockMovement {

    private final UUID id;
    private final UUID companyId;
    private final UUID catalogItemId;
    private StockMovementType type;
    private int delta;
    private String note;
    private final LocalDateTime createdAt;

    private StockMovement(UUID id, UUID companyId, UUID catalogItemId, StockMovementType type,
                          int delta, String note, LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.catalogItemId = Objects.requireNonNull(catalogItemId, "catalogItemId");
        this.createdAt = createdAt;
        this.type = Objects.requireNonNull(type, "type");
        this.delta = delta;
        this.note = note == null || note.isBlank() ? null : note.trim();
    }

    public static StockMovement create(UUID companyId, UUID catalogItemId, StockMovementType type,
                                       int delta, String note) {
        return new StockMovement(UUID.randomUUID(), companyId, catalogItemId, type, delta, note,
                LocalDateTime.now());
    }

    public static StockMovement reconstitute(UUID id, UUID companyId, UUID catalogItemId,
                                             StockMovementType type, int delta, String note,
                                             LocalDateTime createdAt) {
        return new StockMovement(id, companyId, catalogItemId, type, delta, note, createdAt);
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getCatalogItemId() { return catalogItemId; }
    public StockMovementType getType() { return type; }
    public int getDelta() { return delta; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

`domain/exception/InventoryItemNotFoundException.java`:
```java
package com.becommerce.crm.masterdata.inventory.domain.exception;

import java.util.UUID;

public class InventoryItemNotFoundException extends RuntimeException {
    public InventoryItemNotFoundException(UUID id) {
        super("Item de estoque não encontrado ou não é de estoque: " + id);
    }
}
```

`domain/exception/InsufficientStockException.java`:
```java
package com.becommerce.crm.masterdata.inventory.domain.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String itemName, int current, int requested) {
        super("Estoque insuficiente para \"" + itemName + "\" (atual: " + current
                + ", solicitado: " + requested + ").");
    }
}
```

`application/dto/RegisterMovementRequest.java`:
```java
package com.becommerce.crm.masterdata.inventory.application.dto;

import com.becommerce.crm.masterdata.inventory.domain.StockMovementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterMovementRequest(
        @NotNull StockMovementType type,
        @NotNull @Min(1) Integer quantity,
        @Size(max = 500) String note
) {}
```
(Em `AJUSTE`, `quantity` é a **contagem real**; o delta é calculado no service.)

`application/dto/StockMovementResponse.java`:
```java
package com.becommerce.crm.masterdata.inventory.application.dto;

import com.becommerce.crm.masterdata.inventory.domain.StockMovement;
import com.becommerce.crm.masterdata.inventory.domain.StockMovementType;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockMovementResponse(
        UUID id,
        UUID catalogItemId,
        StockMovementType type,
        int delta,
        String note,
        Integer balanceAfter,
        LocalDateTime createdAt
) {
    /** {@code balanceAfter} é {@code null} na listagem do histórico (só o registro conhece o saldo). */
    public static StockMovementResponse from(StockMovement m, Integer balanceAfter) {
        return new StockMovementResponse(m.getId(), m.getCatalogItemId(), m.getType(), m.getDelta(),
                m.getNote(), balanceAfter, m.getCreatedAt());
    }
}
```

`application/dto/InventoryItemResponse.java`:
```java
package com.becommerce.crm.masterdata.inventory.application.dto;

import com.becommerce.crm.masterdata.catalog.domain.CatalogItem;

import java.util.UUID;

public record InventoryItemResponse(
        UUID id,
        String name,
        String category,
        String unit,
        int minStock,
        int quantity,
        String sku,
        boolean active
) {
    public static InventoryItemResponse from(CatalogItem i) {
        return new InventoryItemResponse(i.getId(), i.getName(), i.getCategory(), i.getUnit(),
                i.getMinStock(), i.getQuantity(), i.getSku(), i.isActive());
    }
}
```

`application/port/output/StockMovementRepository.java`:
```java
package com.becommerce.crm.masterdata.inventory.application.port.output;

import com.becommerce.crm.masterdata.inventory.domain.StockMovement;

import java.util.List;
import java.util.UUID;

/** Porta de escrita/leitura de movimentos ({@code inventory_movements}, V095). RLS FORCE isola o tenant. */
public interface StockMovementRepository {

    StockMovement save(StockMovement movement);

    PageResult findByCatalogItem(UUID companyId, UUID catalogItemId, int page, int pageSize);

    record PageResult(List<StockMovement> content, long totalElements) {}
}
```

- [ ] **Step 5: Implementar o `InventoryService`**

```java
package com.becommerce.crm.masterdata.inventory.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.masterdata.catalog.application.port.output.CatalogItemRepository;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItem;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItemType;
import com.becommerce.crm.masterdata.inventory.application.dto.InventoryItemResponse;
import com.becommerce.crm.masterdata.inventory.application.dto.RegisterMovementRequest;
import com.becommerce.crm.masterdata.inventory.application.dto.StockMovementResponse;
import com.becommerce.crm.masterdata.inventory.application.port.output.StockMovementRepository;
import com.becommerce.crm.masterdata.inventory.domain.StockMovement;
import com.becommerce.crm.masterdata.inventory.domain.StockMovementType;
import com.becommerce.crm.masterdata.inventory.domain.exception.InsufficientStockException;
import com.becommerce.crm.masterdata.inventory.domain.exception.InventoryItemNotFoundException;
import com.becommerce.crm.shared.application.dto.PageResponse;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class InventoryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CatalogItemRepository catalogRepository;
    private final StockMovementRepository movementRepository;
    private final TenantAuditRecorder auditor;

    public InventoryService(CatalogItemRepository catalogRepository,
                            StockMovementRepository movementRepository, TenantAuditRecorder auditor) {
        this.catalogRepository = catalogRepository;
        this.movementRepository = movementRepository;
        this.auditor = auditor;
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryItemResponse> listProducts(UUID companyId, String query, String category,
                                                            Boolean lowStock, Boolean active,
                                                            int page, int pageSize) {
        int size = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        try {
            TenantContext.setCompanyId(companyId);
            var result = catalogRepository.searchProducts(companyId, blankToNull(query), blankToNull(category),
                    lowStock, active, safePage, size);
            return PageResponse.of(result.content().stream().map(InventoryItemResponse::from).toList(),
                    safePage, size, result.totalElements());
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> listMovements(UUID companyId, UUID itemId, int page, int pageSize) {
        int size = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        try {
            TenantContext.setCompanyId(companyId);
            var result = movementRepository.findByCatalogItem(companyId, itemId, safePage, size);
            return PageResponse.of(result.content().stream().map(m -> StockMovementResponse.from(m, null)).toList(),
                    safePage, size, result.totalElements());
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public StockMovementResponse registerMovement(UUID companyId, UUID itemId, RegisterMovementRequest request) {
        try {
            TenantContext.setCompanyId(companyId);
            CatalogItem item = requireProduct(companyId, itemId);
            int delta = switch (request.type()) {
                case ENTRADA -> request.quantity();
                case SAIDA -> -request.quantity();
                case AJUSTE -> request.quantity() - item.getQuantity();
            };
            int newQuantity = item.getQuantity() + delta;
            if (newQuantity < 0) {
                throw new InsufficientStockException(item.getName(), item.getQuantity(), request.quantity());
            }
            item.updateQuantity(newQuantity);
            CatalogItem saved = catalogRepository.save(item);
            StockMovement movement = movementRepository.save(StockMovement.create(
                    companyId, item.getId(), request.type(), delta, request.note()));
            auditor.record(companyId, AuditAction.CREATE, AuditModule.INVENTORY, "InventoryMovement",
                    movement.getId().toString(),
                    "Movimento de estoque: " + request.type() + " (" + delta + ") em " + saved.getName(),
                    null, Map.of("type", request.type().name(), "delta", String.valueOf(delta)));
            return StockMovementResponse.from(movement, newQuantity);
        } finally {
            TenantContext.clear();
        }
    }

    private CatalogItem requireProduct(UUID companyId, UUID itemId) {
        CatalogItem item = catalogRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new InventoryItemNotFoundException(itemId));
        if (!item.getCompanyId().equals(companyId) || item.getType() != CatalogItemType.PRODUCT) {
            throw new InventoryItemNotFoundException(itemId);
        }
        return item;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
```

> **Nota sobre `balanceAfter` no histórico:** ao listar movimentos, `balanceAfter` vem `null` (calcular o saldo histórico seria custoso; a UI mostra `delta` e o saldo **atual** do item na tabela `/estoque`). Só a resposta do registro traz o saldo real após o movimento. Por isso o campo é `Integer` (anulável), não `int`.

- [ ] **Step 6: Rodar o teste do service**

Run: `cd backend && mvn -q test -Dtest=InventoryServiceTest`
Expected: PASS.

- [ ] **Step 7: Persistência dos movimentos**

`InventoryMovementJpaEntity.java`:
```java
package com.becommerce.crm.masterdata.inventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovementJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "catalog_item_id")
    private UUID catalogItemId;

    @Column(name = "movement_type")
    private String movementType;

    @Column(name = "delta")
    private int delta;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // getters e setters simples para todos os campos (padrão CatalogItemJpaEntity)
}
```
(crie os getters/setters de todos os 7 campos.)

`InventoryMovementJpaRepository.java`:
```java
package com.becommerce.crm.masterdata.inventory.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface InventoryMovementJpaRepository extends JpaRepository<InventoryMovementJpaEntity, UUID> {

    @Query("SELECT m FROM InventoryMovementJpaEntity m WHERE m.companyId = :companyId "
            + "AND m.catalogItemId = :catalogItemId")
    Page<InventoryMovementJpaEntity> findByCatalogItem(@Param("companyId") UUID companyId,
                                                       @Param("catalogItemId") UUID catalogItemId,
                                                       Pageable pageable);
}
```

`InventoryMovementRepositoryImpl.java` (espelha `CatalogItemRepositoryImpl`, com `toDomain`/`toEntity`):
```java
package com.becommerce.crm.masterdata.inventory.infrastructure.persistence;

import com.becommerce.crm.masterdata.inventory.application.port.output.StockMovementRepository;
import com.becommerce.crm.masterdata.inventory.domain.StockMovement;
import com.becommerce.crm.masterdata.inventory.domain.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class InventoryMovementRepositoryImpl implements StockMovementRepository {

    private final InventoryMovementJpaRepository jpaRepository;

    public InventoryMovementRepositoryImpl(InventoryMovementJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public StockMovement save(StockMovement movement) {
        return toDomain(jpaRepository.save(toEntity(movement)));
    }

    @Override
    public StockMovementRepository.PageResult findByCatalogItem(UUID companyId, UUID catalogItemId,
                                                                int page, int pageSize) {
        Page<InventoryMovementJpaEntity> result = jpaRepository.findByCatalogItem(companyId, catalogItemId,
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new StockMovementRepository.PageResult(
                result.getContent().stream().map(InventoryMovementRepositoryImpl::toDomain).toList(),
                result.getTotalElements());
    }

    static StockMovement toDomain(InventoryMovementJpaEntity e) {
        return StockMovement.reconstitute(e.getId(), e.getCompanyId(), e.getCatalogItemId(),
                StockMovementType.valueOf(e.getMovementType()), e.getDelta(), e.getNote(), e.getCreatedAt());
    }

    static InventoryMovementJpaEntity toEntity(StockMovement m) {
        InventoryMovementJpaEntity e = new InventoryMovementJpaEntity();
        e.setId(m.getId());
        e.setCompanyId(m.getCompanyId());
        e.setCatalogItemId(m.getCatalogItemId());
        e.setMovementType(m.getType().name());
        e.setDelta(m.getDelta());
        e.setNote(m.getNote());
        e.setCreatedAt(m.getCreatedAt());
        return e;
    }
}
```

- [ ] **Step 8: Controller + teste do controller**

`InventoryController.java`:
```java
package com.becommerce.crm.masterdata.inventory.web;

import com.becommerce.crm.masterdata.inventory.application.dto.InventoryItemResponse;
import com.becommerce.crm.masterdata.inventory.application.dto.RegisterMovementRequest;
import com.becommerce.crm.masterdata.inventory.application.dto.StockMovementResponse;
import com.becommerce.crm.masterdata.inventory.application.service.InventoryService;
import com.becommerce.crm.shared.application.dto.PageResponse;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Controle de estoque dos produtos do catálogo (V095). Isolamento por RLS FORCE + {@code @CurrentCompanyId}. */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/inventory")
public class InventoryController {

    private static final String OWN_COMPANY = "Você só pode acessar o estoque da sua própria empresa.";

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    public ResponseEntity<PageResponse<InventoryItemResponse>> list(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(inventoryService.listProducts(companyId, q, category, lowStock, active,
                page, pageSize));
    }

    @GetMapping("/{itemId}/movements")
    @PreAuthorize("hasAuthority('inventory:read')")
    public ResponseEntity<PageResponse<StockMovementResponse>> movements(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @PathVariable UUID itemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(inventoryService.listMovements(companyId, itemId, page, pageSize));
    }

    @PostMapping("/{itemId}/movements")
    @PreAuthorize("hasAuthority('inventory:manage')")
    public ResponseEntity<StockMovementResponse> register(
            @CurrentCompanyId(OWN_COMPANY) UUID companyId,
            @PathVariable UUID itemId,
            @Valid @RequestBody RegisterMovementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryService.registerMovement(companyId, itemId, request));
    }
}
```

`InventoryControllerTest.java` (padrão `ContactControllerTest`: MockMvc standalone + `CurrentCompanyIdArgumentResolver` + `AuthenticationPrincipalArgumentResolver` + `GlobalExceptionHandler`). Copie a estrutura de `backend/src/test/java/com/becommerce/crm/masterdata/contact/web/ContactControllerTest.java` (setUp, `login(...)`, tearDown) e escreva:

```java
@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    @Mock private InventoryService inventoryService;
    @InjectMocks private InventoryController inventoryController;

    private MockMvc mockMvc;
    private UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(inventoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new CurrentCompanyIdArgumentResolver(),
                        new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void login(UUID activeCompanyId) {
        CurrentUser principal = new CurrentUser(UUID.randomUUID(), "admin@empresa.com",
                activeCompanyId, activeCompanyId, List.of("ADMIN"),
                List.of("inventory:read", "inventory:manage"), "k", null, "keycloak", "Admin", null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null,
                principal.permissions().stream().map(SimpleGrantedAuthority::new).toList()));
    }

    @Test
    void registersMovementInOwnCompany() throws Exception {
        login(companyId);
        when(inventoryService.registerMovement(any(), any(), any(RegisterMovementRequest.class)))
                .thenReturn(new StockMovementResponse(UUID.randomUUID(), UUID.randomUUID(),
                        StockMovementType.ENTRADA, 10, "Compra", 10, java.time.LocalDateTime.now()));

        mockMvc.perform(post("/api/v1/companies/" + companyId + "/inventory/" + UUID.randomUUID() + "/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ENTRADA\",\"quantity\":10,\"note\":\"Compra\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.delta").value(10))
                .andExpect(jsonPath("$.balanceAfter").value(10));
    }

    @Test
    void listsInventoryOfOwnCompany() throws Exception {
        login(companyId);
        when(inventoryService.listProducts(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/companies/" + companyId + "/inventory"))
                .andExpect(status().isOk());
    }

    @Test
    void returns403ForOtherCompany() throws Exception {
        UUID other = UUID.randomUUID();
        login(companyId);

        mockMvc.perform(post("/api/v1/companies/" + other + "/inventory/" + UUID.randomUUID() + "/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ENTRADA\",\"quantity\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CRM_ACCESS_DENIED"));
    }
}
```
(imports: os mesmos do `ContactControllerTest` + `get`, `anyInt`. Verifique o construtor de `PageResponse` — em `shared/application/dto/PageResponse.java` ele aceita `(content, page, pageSize, totalElements, totalPages)` ou há um `of(...)` estático; use o construtor/`of` que já existir no arquivo.)

- [ ] **Step 9: Rodar testes do controller**

Run: `cd backend && mvn -q test -Dtest=InventoryControllerTest`
Expected: PASS.

- [ ] **Step 10: Wiring (auditoria, papéis e handler)**

`AuditModule.java`: adicione `INVENTORY` após `ANAMNESIS`.

`RoleSeedService.java`:
- ADMIN (após linha 74 `"finance:read", "finance:manage",`): adicionar `"inventory:read", "inventory:manage",`.
- MANAGER (após linha 107): idem.
- AGENT (após linha 130): adicionar `"inventory:read",`.
- VIEWER (após linha 152): adicionar `"inventory:read",`.

`GlobalExceptionHandler.java`:
- imports: `com.becommerce.crm.masterdata.inventory.domain.exception.InsufficientStockException` e `InventoryItemNotFoundException`.
- Adicione `InventoryItemNotFoundException.class` no agrupamento 404 (linha 351-353, junto de `CatalogItemNotFoundException`).
- Novo handler 409 (cópia do `handleCatalogSkuConflict`):

```java
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientStock(InsufficientStockException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of(
                "status", 409,
                "error", "Conflict",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
    }
```

- [ ] **Step 11: Rodar a suíte unitária do backend**

Run: `cd backend && mvn -q test`
Expected: `BUILD SUCCESS`, todos os testes passam.

- [ ] **Step 12: Commit**

```bash
git add backend/src/main/java/com/becommerce/crm/masterdata/inventory \
  backend/src/main/java/com/becommerce/crm/masterdata/catalog \
  backend/src/main/java/com/becommerce/crm/analytics/audit/domain/AuditModule.java \
  backend/src/main/java/com/becommerce/crm/identity/infrastructure/persistence/RoleSeedService.java \
  backend/src/main/java/com/becommerce/crm/shared/web/handler/GlobalExceptionHandler.java \
  backend/src/test/java/com/becommerce/crm/masterdata/inventory
git commit -m "feat(inventory): inventory module with stock movements (entrada/saida/ajuste)"
```

---

## Task 4: `InventoryIsolationIT` (Testcontainers)

Garante a migration real `V095` + isolamento RLS entre tenants, FK cascade, grants e permissões.

**Files:**
- Create: `backend/src/test/java/com/becommerce/crm/masterdata/inventory/infrastructure/persistence/InventoryIsolationIT.java`

**Interfaces:**
- Consumes: migrations `V078` e `V095` (em `src/main/resources/db/migration/`), roles/permissions sintéticas do bootstrap, `TenantAwareDataSource`.

- [ ] **Step 1: Copiar a base do `CatalogIsolationIT` e adaptar**

Copie `backend/src/test/java/com/becommerce/crm/masterdata/catalog/infrastructure/persistence/CatalogIsolationIT.java`. Ajustes obrigatórios:
- Classe `InventoryIsolationIT`, container `postgres:17-alpine`, nome do database `crm_it_inventory`, usuário app `crm_inventory_app_user`.
- `@BeforeAll` deve executar **duas** migrations no mesmo `Statement.execute`: `db/migration/V078__catalog_items.sql` e `db/migration/V095__inventory.sql`.
- `insertItem` continua igual. Após inserir, o saldo default é `quantity = 0`.

- [ ] **Step 2: Testes específicos de estoque**

Adicione (mantendo os de isolamento/SKU/permissões do arquivo copiado):

```java
@Test
void rlsIsolatesMovementsBetweenTenants() throws Exception {
    TenantContext.setCompanyId(TENANT_A);
    UUID itemA;
    UUID itemB;
    try (Connection conn = tenantAwareDataSource.getConnection()) {
        itemA = insertItemReturningId(conn, TENANT_A, "Luva A");
    }
    TenantContext.setCompanyId(TENANT_B);
    try (Connection conn = tenantAwareDataSource.getConnection()) {
        itemB = insertItemReturningId(conn, TENANT_B, "Luva B");
        // B registra movimento no item de B
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO inventory_movements (company_id, catalog_item_id, movement_type, delta) "
                + "VALUES (?, ?, 'ENTRADA', 5)")) {
            ps.setObject(1, TENANT_B);
            ps.setObject(2, itemB);
            ps.executeUpdate();
        }
        // B NÃO consegue inserir movimento apontando para o item de A
        assertThrows(SQLException.class, () -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO inventory_movements (company_id, catalog_item_id, movement_type, delta) "
                    + "VALUES (?, ?, 'ENTRADA', 1)")) {
                ps.setObject(1, TENANT_B);
                ps.setObject(2, itemA);
                ps.executeUpdate();
            }
        });
        // B só enxerga o próprio movimento
        assertEquals("5", countDelta(conn));
    }
}

@Test
void appUserCanUpdateQuantityCacheOnCatalogItem() throws Exception {
    TenantContext.setCompanyId(TENANT_A);
    UUID itemA;
    try (Connection conn = tenantAwareDataSource.getConnection()) {
        itemA = insertItemReturningId(conn, TENANT_A, "Luva A");
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE catalog_items SET quantity = 3 WHERE id = ?")) {
            ps.setObject(1, itemA);
            ps.executeUpdate();
        }
        assertEquals(3, quantityOf(conn, itemA));
    }
}

@Test
void creatingProductWithOutOfListCategoryIsRejectedByDb() {
    TenantContext.setCompanyId(TENANT_A);
    assertThrows(SQLException.class, () -> {
        try (Connection conn = tenantAwareDataSource.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO catalog_items (company_id, item_type, name, category) "
                    + "VALUES (?, 'PRODUCT', 'Item', 'Planos')")) {
                ps.setObject(1, TENANT_A);
                ps.executeUpdate();
            }
        }
    });
}
```

Helpers a acrescentar no arquivo (métodos que hoje são `void insertItem` — transforme-os em uma variante com retorno `UUID`):

```java
private static UUID insertItemReturningId(Connection conn, UUID tenant, String name) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO catalog_items (company_id, item_type, name) VALUES (?, 'PRODUCT', ?) "
            + "RETURNING id")) {
        ps.setObject(1, tenant);
        ps.setString(2, name);
        try (ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getObject(1, UUID.class);
        }
    }
}

private static String countDelta(Connection conn) throws SQLException {
    try (Statement st = conn.createStatement();
         ResultSet rs = st.executeQuery("SELECT COALESCE(SUM(delta), 0) FROM inventory_movements")) {
        rs.next();
        return rs.getString(1);
    }
}

private static int quantityOf(Connection conn, UUID itemId) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement("SELECT quantity FROM catalog_items WHERE id = ?")) {
        ps.setObject(1, itemId);
        try (ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
```
(Se o arquivo copiado já tiver um `insertItem` sem retorno, mantenha-o e adicione `insertItemReturningId`; o teste `skuIsUniquePerCompany` continua usando `insertItem`.)

- [ ] **Step 3: Rodar o IT (localmente exige Docker; senão roda no CI)**

Run: `cd backend && mvn -q verify -Dtest=InventoryIsolationIT -Dit.test=InventoryIsolationIT`
Expected: PASS quando houver Docker (Testcontainers). **Nesta máquina sem Docker, não rodar** — o ISR trava em pull da imagem. A verificação real acontece no CI (Playwright/Backend jobs).

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/com/becommerce/crm/masterdata/inventory/infrastructure/persistence/InventoryIsolationIT.java
git commit -m "test(inventory): tenant isolation and grants for stock movements (IT)"
```

---

## Task 5: Frontend — fundação do inventory (types + service + hooks + testes)

**Files:**
- Create: `frontend-refine/src/features/masterdata/inventory/types/inventory.types.ts`
- Create: `frontend-refine/src/features/masterdata/inventory/services/inventory.service.ts`
- Create: `frontend-refine/src/features/masterdata/inventory/hooks/useInventory.ts`
- Create: `frontend-refine/src/__tests__/features/masterdata/inventory/types/inventory.types.test.ts`
- Create: `frontend-refine/src/__tests__/features/masterdata/inventory/services/inventory.service.test.ts`
- Create: `frontend-refine/src/__tests__/features/masterdata/inventory/hooks/useInventory.test.tsx`

**Interfaces:**
- Consumes: catálogo do backend (`CatalogItem` ainda sem os novos campos — a Task 8 os adiciona), `api` de `@/lib/api`, `useMutationDefaults` de `@/lib/query/mutation-utils`.
- Produces: `InventoryItem`, `InventoryFilter`, `InventoryPage`, `StockUnit`/`UNIT_LABELS`/`STOCK_UNITS`, `StockMovementType`/`STOCK_MOVEMENT_TYPE_LABELS`/`STOCK_MOVEMENT_TYPES`, `StockMovement`, `StockMovementPage`, `RegisterMovementRequest`, `isLowStock(item)`, `InventoryService.list/listMovements/registerMovement`, hooks `useInventory`, `useItemMovements`, `useRegisterMovement`.

- [ ] **Step 1: Escrever os testes que falham**

`src/__tests__/features/masterdata/inventory/types/inventory.types.test.ts`:

```ts
import { describe, expect, it } from "vitest";
import { STOCK_MOVEMENT_TYPES, isLowStock, STOCK_UNITS } from "@/features/masterdata/inventory/types/inventory.types";

describe("inventory.types helpers", () => {
  it("isLowStock considera minStock > 0", () => {
    expect(isLowStock({ quantity: 0, minStock: 0 })).toBe(false);
    expect(isLowStock({ quantity: 5, minStock: 5 })).toBe(true);
    expect(isLowStock({ quantity: 6, minStock: 5 })).toBe(false);
  });

  it("expõe as constants dos enums", () => {
    expect(STOCK_MOVEMENT_TYPES).toEqual(["ENTRADA", "SAIDA", "AJUSTE"]);
    expect(STOCK_UNITS).toContain("CAIXA");
  });
});
```

`src/__tests__/features/masterdata/inventory/services/inventory.service.test.ts` (padrão `anamnesis.service.test.ts`: mock de toda a `@/lib/api`):

```ts
import { beforeEach, describe, expect, it, vi } from "vitest";

const api = {
  get: vi.fn(),
  post: vi.fn(),
};

vi.mock("@/lib/api", () => ({ default: api }));

import { InventoryService } from "@/features/masterdata/inventory/services/inventory.service";

describe("InventoryService", () => {
  const companyId = "comp-1";

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("list passa filtros e paginação como query params", async () => {
    api.get.mockResolvedValue({ data: { content: [], page: 0, pageSize: 20, totalElements: 0, totalPages: 0 } });
    await InventoryService.list(companyId, { q: "luva", category: "EPI", lowStock: true, page: 2 });
    expect(api.get).toHaveBeenCalledWith(`/companies/${companyId}/inventory`, {
      params: { q: "luva", category: "EPI", lowStock: true, active: undefined, page: 2, pageSize: 20 },
    });
  });

  it("listMovements busca na rota do item", async () => {
    api.get.mockResolvedValue({ data: { content: [], page: 0, pageSize: 20, totalElements: 0, totalPages: 0 } });
    await InventoryService.listMovements(companyId, "item-1", 0, 20);
    expect(api.get).toHaveBeenCalledWith(`/companies/${companyId}/inventory/item-1/movements`, {
      params: { page: 0, pageSize: 20 },
    });
  });

  it("registerMovement faz POST na rota do item", async () => {
    api.post.mockResolvedValue({ data: { id: "m1" } });
    await InventoryService.registerMovement(companyId, "item-1", { type: "ENTRADA", quantity: 10, note: "Compra" });
    expect(api.post).toHaveBeenCalledWith(`/companies/${companyId}/inventory/item-1/movements`, {
      type: "ENTRADA",
      quantity: 10,
      note: "Compra",
    });
  });
});
```

`src/__tests__/features/masterdata/inventory/hooks/useInventory.test.tsx` (padrão `useContacts.test.ts`: `renderHook` + `QueryClientProvider` + mock do service):

```ts
import { describe, expect, it, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { PropsWithChildren } from "react";
import { useRegisterMovement } from "@/features/masterdata/inventory/hooks/useInventory";
import { InventoryService } from "@/features/masterdata/inventory/services/inventory.service";

vi.mock("@/features/masterdata/inventory/services/inventory.service", () => ({
  InventoryService: {
    list: vi.fn(),
    listMovements: vi.fn(),
    registerMovement: vi.fn(),
  },
}));

vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }));

const wrapper = ({ children }: PropsWithChildren) => (
  <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
    {children}
  </QueryClientProvider>
);

describe("useRegisterMovement", () => {
  beforeEach(() => vi.clearAllMocks());

  it("envia o movimento e invalida as queries de inventory", async () => {
    (InventoryService.registerMovement as ReturnType<typeof vi.fn>).mockResolvedValue({ id: "m1" });
    const { result } = renderHook(() => useRegisterMovement("comp-1"), { wrapper });

    result.current.mutate({ itemId: "item-1", request: { type: "ENTRADA", quantity: 5 } });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(InventoryService.registerMovement).toHaveBeenCalledWith("comp-1", "item-1", {
      type: "ENTRADA",
      quantity: 5,
    });
  });
});
```

- [ ] **Step 2: Rodar para ver falhar**

Run: `cd frontend-refine && npx vitest run --pool=threads src/__tests__/features/masterdata/inventory`
Expected: fail (arquivo service/types não existem).

- [ ] **Step 3: Implementar types**

`src/features/masterdata/inventory/types/inventory.types.ts`:

```ts
import type { CatalogItem } from "@/features/masterdata/catalog/types/catalog.types";

export type StockUnit = "CAIXA" | "CARTELA" | "MILILITROS" | "PACOTE" | "ROLO" | "UNIDADE";

export const UNIT_LABELS: Record<StockUnit, string> = {
  CAIXA: "Caixa",
  CARTELA: "Cartela",
  MILILITROS: "Mililitros",
  PACOTE: "Pacote",
  ROLO: "Rolo",
  UNIDADE: "Unidade",
};

export const STOCK_UNITS = Object.keys(UNIT_LABELS) as StockUnit[];

export type StockMovementType = "ENTRADA" | "SAIDA" | "AJUSTE";

export const STOCK_MOVEMENT_TYPE_LABELS: Record<StockMovementType, string> = {
  ENTRADA: "Entrada",
  SAIDA: "Saída",
  AJUSTE: "Ajuste",
};

export const STOCK_MOVEMENT_TYPES = Object.keys(STOCK_MOVEMENT_TYPE_LABELS) as StockMovementType[];

export type InventoryItem = Pick<
  CatalogItem,
  "id" | "name" | "category" | "sku" | "active" | "unit" | "minStock" | "quantity"
>;

export type InventoryFilters = {
  q?: string;
  category?: string;
  lowStock?: boolean;
  active?: boolean;
  page?: number;
  pageSize?: number;
};

export type InventoryPage = {
  content: InventoryItem[];
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
};

export type StockMovement = {
  id: string;
  catalogItemId: string;
  type: StockMovementType;
  delta: number;
  note: string | null;
  /** Só vem preenchido na resposta do registro; no histórico é null. */
  balanceAfter: number | null;
  createdAt: string;
};

export type StockMovementPage = {
  content: StockMovement[];
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
};

export type RegisterMovementRequest = {
  type: StockMovementType;
  quantity: number;
  note?: string;
};

export function isLowStock(item: Pick<InventoryItem, "quantity" | "minStock">): boolean {
  return item.minStock > 0 && item.quantity <= item.minStock;
}
```

> **Antes deste Step:** aplique o **Step 1 da Task 8** (tipos do catálogo: `CatalogCategory`, `unit`/`minStock`/`quantity` em `CatalogItem`/`CatalogItemRequest`) e inclua `catalog.types.ts` no commit desta task. Sem isso o `Pick` acima não compila.

- [ ] **Step 4: Implementar service**

`src/features/masterdata/inventory/services/inventory.service.ts`:

```ts
import api from "@/lib/api";
import type {
  InventoryPage,
  InventoryFilters,
  RegisterMovementRequest,
  StockMovement,
  StockMovementPage,
} from "../types/inventory.types";

const base = (companyId: string) => `/companies/${companyId}/inventory`;

export const InventoryService = {
  async list(companyId: string, filters: InventoryFilters = {}): Promise<InventoryPage> {
    const response = await api.get<InventoryPage>(base(companyId), {
      params: {
        q: filters.q || undefined,
        category: filters.category || undefined,
        lowStock: filters.lowStock,
        active: filters.active,
        page: filters.page ?? 0,
        pageSize: filters.pageSize ?? 20,
      },
    });
    return response.data;
  },

  async listMovements(companyId: string, itemId: string, page = 0, pageSize = 20): Promise<StockMovementPage> {
    const response = await api.get<StockMovementPage>(`${base(companyId)}/${itemId}/movements`, {
      params: { page, pageSize },
    });
    return response.data;
  },

  async registerMovement(
    companyId: string,
    itemId: string,
    data: RegisterMovementRequest,
  ): Promise<StockMovement> {
    const response = await api.post<StockMovement>(`${base(companyId)}/${itemId}/movements`, data);
    return response.data;
  },
};
```

- [ ] **Step 5: Implementar hooks**

`src/features/masterdata/inventory/hooks/useInventory.ts`:

```ts
import { keepPreviousData, useMutation, useQuery } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { InventoryService } from "../services/inventory.service";
import type { InventoryFilters, RegisterMovementRequest } from "../types/inventory.types";

export function useInventory(companyId: string | null, filters: InventoryFilters) {
  return useQuery({
    queryKey: ["inventory", companyId, filters],
    queryFn: () => InventoryService.list(companyId as string, filters),
    enabled: !!companyId,
    placeholderData: keepPreviousData,
  });
}

export function useItemMovements(companyId: string | null, itemId: string | null) {
  return useQuery({
    queryKey: ["inventory", companyId, itemId, "movements"],
    queryFn: () => InventoryService.listMovements(companyId as string, itemId as string),
    enabled: !!companyId && !!itemId,
    placeholderData: keepPreviousData,
  });
}

export function useRegisterMovement(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults<unknown, { itemId: string; request: RegisterMovementRequest }>();
  return useMutation({
    mutationFn: ({ itemId, request }: { itemId: string; request: RegisterMovementRequest }) =>
      InventoryService.registerMovement(companyId as string, itemId, request),
    onSuccess: onSuccess({
      successMessage: "Movimento registrado",
      invalidateKeys: [["inventory", companyId]],
    }),
    onError: onError({ errorMessage: "Erro ao registrar movimento" }),
  });
}
```

- [ ] **Step 6: Rodar os testes + tipo**

Run: `cd frontend-refine && npx vitest run --pool=threads src/__tests__/features/masterdata/inventory && npx tsc --noEmit`
Expected: PASS + tipo limpo (após aplicar a parte de `types` da Task 8).

- [ ] **Step 7: Commit**

```bash
git add frontend-refine/src/features/masterdata/inventory frontend-refine/src/__tests__/features/masterdata/inventory \
  frontend-refine/src/features/masterdata/catalog/types/catalog.types.ts
git commit -m "feat(inventory): frontend types, service and hooks"
```

---

## Task 6: Frontend — componentes (dialog, sheet e badge)

**Files:**
- Create: `frontend-refine/src/features/masterdata/inventory/components/LowStockBadge.tsx`
- Create: `frontend-refine/src/features/masterdata/inventory/components/StockMovementDialog.tsx`
- Create: `frontend-refine/src/features/masterdata/inventory/components/MovementHistorySheet.tsx`
- Create: `frontend-refine/src/__tests__/features/masterdata/inventory/components/StockMovementDialog.test.tsx`

**Interfaces:**
- Consumes: Task 5 (`InventoryItem`, `StockMovementType`, `STOCK_MOVEMENT_TYPE_LABELS`, `RegisterMovementRequest`, `useItemMovements`, `useRegisterMovement`).
- Produces: `<StockMovementDialog open onOpenChange item isSubmitting onSubmit>`; `<MovementHistorySheet open onOpenChange item>`; `<LowStockBadge item>`.

- [ ] **Step 1: Escrever o teste do dialog que falha**

`src/__tests__/features/masterdata/inventory/components/StockMovementDialog.test.tsx`:

```tsx
import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { StockMovementDialog } from "@/features/masterdata/inventory/components/StockMovementDialog";
import type { InventoryItem } from "@/features/masterdata/inventory/types/inventory.types";

const item: InventoryItem = {
  id: "item-1",
  name: "Luva",
  category: "EPI",
  sku: null,
  active: true,
  unit: "CAIXA",
  minStock: 5,
  quantity: 2,
};

describe("StockMovementDialog", () => {
  it("chama onSubmit com entrada após preencher e salvar", () => {
    const onSubmit = vi.fn();
    render(<StockMovementDialog open onOpenChange={() => {}} item={item} isSubmitting={false} onSubmit={onSubmit} />);

    fireEvent.click(screen.getByLabelText("Tipo"));
    fireEvent.click(screen.getByRole("option", { name: "Entrada" }));
    fireEvent.change(screen.getByLabelText("Quantidade"), { target: { value: "10" } });
    fireEvent.change(screen.getByLabelText("Observação"), { target: { value: "Compra" } });
    fireEvent.click(screen.getByRole("button", { name: "Registrar" }));

    expect(onSubmit).toHaveBeenCalledWith({ type: "ENTRADA", quantity: 10, note: "Compra" });
  });
});
```

- [ ] **Step 2: Rodar para ver falhar**

Run: `cd frontend-refine && npx vitest run --pool=threads src/__tests__/features/masterdata/inventory/components/StockMovementDialog.test.tsx`
Expected: fail (componente não existe).

- [ ] **Step 3: Implementar os componentes**

`components/LowStockBadge.tsx`:

```tsx
import { StatusBadge } from "@/components/ui/status-badge";
import { isLowStock, type InventoryItem } from "../types/inventory.types";

export function LowStockBadge({ item }: { item: Pick<InventoryItem, "quantity" | "minStock"> }) {
  if (!isLowStock(item)) return null;
  return <StatusBadge intent="warning">Baixo estoque</StatusBadge>;
}
```

`components/StockMovementDialog.tsx` (padrão de `CatalogItemDialog`, com `useState`):

```tsx
"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  STOCK_MOVEMENT_TYPE_LABELS,
  STOCK_MOVEMENT_TYPES,
  type InventoryItem,
  type RegisterMovementRequest,
  type StockMovementType,
} from "../types/inventory.types";

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  item: InventoryItem;
  isSubmitting: boolean;
  onSubmit: (request: RegisterMovementRequest) => void;
};

type FormState = {
  type: StockMovementType;
  quantity: string;
  note: string;
};

const initialState: FormState = { type: "ENTRADA", quantity: "", note: "" };

export function StockMovementDialog({ open, onOpenChange, item, isSubmitting, onSubmit }: Props) {
  const [form, setForm] = useState<FormState>(initialState);

  useEffect(() => {
    if (open) setForm(initialState);
  }, [open]);

  const set = <K extends keyof FormState>(key: K, value: FormState[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const quantity = Number(form.quantity);
    if (!form.quantity.trim() || !Number.isInteger(quantity) || quantity < 1) {
      toast.error("Informe uma quantidade inteira maior que zero.");
      return;
    }
    onSubmit({
      type: form.type,
      quantity,
      note: form.note.trim() || undefined,
    });
  };

  const quantityLabel = form.type === "AJUSTE" ? "Contagem real" : "Quantidade";

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Movimentar estoque — {item.name}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="movement-type">Tipo</Label>
            <Select value={form.type} onValueChange={(v) => set("type", v as StockMovementType)}>
              <SelectTrigger id="movement-type">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {STOCK_MOVEMENT_TYPES.map((type) => (
                  <SelectItem key={type} value={type}>
                    {STOCK_MOVEMENT_TYPE_LABELS[type]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="movement-quantity">{quantityLabel}</Label>
            <Input
              id="movement-quantity"
              type="number"
              inputMode="numeric"
              min={1}
              step={1}
              value={form.quantity}
              onChange={(e) => set("quantity", e.target.value)}
            />
            {form.type === "AJUSTE" ? (
              <p className="text-xs text-muted-foreground">
                Quantidade real contada. O sistema calcula a diferença (atual: {item.quantity}).
              </p>
            ) : null}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="movement-note">Observação</Label>
            <Textarea
              id="movement-note"
              value={form.note}
              onChange={(e) => set("note", e.target.value)}
              rows={2}
              maxLength={500}
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancelar
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Salvando…" : "Registrar"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
```

`components/MovementHistorySheet.tsx` (usa `useItemMovements`, `Sheet`, tabela simples, badge de `delta` com sinal):

```tsx
"use client";

import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { StatusBadge } from "@/components/ui/status-badge";
import { useItemMovements } from "../hooks/useInventory";
import { STOCK_MOVEMENT_TYPE_LABELS, type InventoryItem } from "../types/inventory.types";

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  companyId: string;
  item: InventoryItem;
};

function deltaBadge(delta: number) {
  if (delta > 0) return <StatusBadge intent="success">+{delta}</StatusBadge>;
  if (delta < 0) return <StatusBadge intent="danger">{delta}</StatusBadge>;
  return <StatusBadge intent="neutral">0</StatusBadge>;
}

export function MovementHistorySheet({ open, onOpenChange, companyId, item }: Props) {
  const { data, isLoading, error, refetch } = useItemMovements(companyId, open ? item.id : null);

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="w-full sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>Histórico — {item.name}</SheetTitle>
        </SheetHeader>
        <div className="mt-4">
          {isLoading ? (
            <p className="text-sm text-muted-foreground">Carregando movimentos…</p>
          ) : error ? (
            <div className="space-y-2">
              <p className="text-sm text-destructive">Não foi possível carregar o histórico.</p>
              <button onClick={() => refetch()} className="text-sm underline">
                Tentar novamente
              </button>
            </div>
          ) : data && data.content.length > 0 ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Data</TableHead>
                  <TableHead>Tipo</TableHead>
                  <TableHead className="text-right">Delta</TableHead>
                  <TableHead>Observação</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((movement) => (
                  <TableRow key={movement.id}>
                    <TableCell>
                      {new Date(movement.createdAt).toLocaleString("pt-BR")}
                    </TableCell>
                    <TableCell>{STOCK_MOVEMENT_TYPE_LABELS[movement.type]}</TableCell>
                    <TableCell className="text-right">{deltaBadge(movement.delta)}</TableCell>
                    <TableCell>{movement.note ?? "—"}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          ) : (
            <p className="text-sm text-muted-foreground">Nenhuma movimentação registrada.</p>
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}
```

- [ ] **Step 4: Rodar os testes**

Run: `cd frontend-refine && npx vitest run --pool=threads src/__tests__/features/masterdata/inventory && npx tsc --noEmit`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend-refine/src/features/masterdata/inventory/components frontend-refine/src/__tests__/features/masterdata/inventory/components
git commit -m "feat(inventory): stock movement dialog, history sheet and low-stock badge"
```

---

## Task 7: Frontend — página `/estoque` + navegação

**Files:**
- Create: `frontend-refine/src/app/(dashboard)/estoque/page.tsx`
- Modify: `frontend-refine/src/lib/constants.ts` (ROUTES)
- Modify: `frontend-refine/src/components/layout/navigation.ts`
- Modify: `frontend-refine/src/components/navigation/Breadcrumb.tsx`

**Interfaces:**
- Consumes: Tasks 5/6 (`useInventory`, `useRegisterMovement`, `StockMovementDialog`, `MovementHistorySheet`, `LowStockBadge`, `InventoryFilters`), `useAuth`, `useAuthorization`, `useDebouncedValue`, `SearchInput`, `SkeletonTable`, `ErrorCard`, `EmptyState`.
- Produces: `ROUTES.INVENTORY = "/estoque"`, item de menu "Estoque" (grupo CRM, após Catálogo), breadcrumb.

- [ ] **Step 1: Rota e navegação**

`src/lib/constants.ts` — após `CATALOG` (linha 40):

```ts
  INVENTORY: "/estoque",
```

`src/components/layout/navigation.ts`:
- Importe `Boxes` de `lucide-react` (acerte já que `Package` e `HardDrive` já estão importados na ordem alfabética dos blocos).
- Após o item "Catálogo" (linhas 80-85):

```ts
      {
        label: "Estoque",
        href: ROUTES.INVENTORY,
        icon: Boxes,
        permission: "inventory:read",
      },
```

`src/components/navigation/Breadcrumb.tsx` — no map `routeLabels` (após `catalog`? não existe; adicione perto de `campaigns`):

```ts
  estoque: "Estoque",
```

- [ ] **Step 2: Escrever a página**

`src/app/(dashboard)/estoque/page.tsx` (espelha a estrutura de `catalog/page.tsx`):

```tsx
"use client";

import { useState } from "react";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useAuthorization } from "@/features/identity/auth/hooks/useAuthorization";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { SearchInput } from "@/components/common/SearchInput";
import { SkeletonTable } from "@/components/common/SkeletonTable";
import { ErrorCard } from "@/components/common/ErrorCard";
import { EmptyState } from "@/components/common/EmptyState";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useInventory, useRegisterMovement } from "@/features/masterdata/inventory/hooks/useInventory";
import { StockMovementDialog } from "@/features/masterdata/inventory/components/StockMovementDialog";
import { MovementHistorySheet } from "@/features/masterdata/inventory/components/MovementHistorySheet";
import { LowStockBadge } from "@/features/masterdata/inventory/components/LowStockBadge";
import {
  UNIT_LABELS,
  type InventoryItem,
} from "@/features/masterdata/inventory/types/inventory.types";
import {
  CATALOG_CATEGORIES,
  CATALOG_CATEGORY_LABELS,
} from "@/features/masterdata/catalog/types/catalog.types";

export default function EstoquePage() {
  const { user } = useAuth();
  const { can } = useAuthorization();
  const companyId = user?.companyId ?? null;
  const canManage = can("inventory:manage");

  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search, 300);
  const [category, setCategory] = useState<string>("");
  const [lowStock, setLowStock] = useState<boolean | undefined>(undefined);
  const [page, setPage] = useState(0);

  const { data, isLoading, error, refetch } = useInventory(companyId, {
    q: debouncedSearch,
    category: category || undefined,
    lowStock,
    page,
    pageSize: 20,
  });

  const registerMovement = useRegisterMovement(companyId);

  const [movementItem, setMovementItem] = useState<InventoryItem | null>(null);
  const [historyItem, setHistoryItem] = useState<InventoryItem | null>(null);

  const resetPage = (setter: (v: string) => void) => (v: string) => {
    setPage(0);
    setter(v);
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold tracking-tight">Estoque</h1>

      <div className="flex flex-wrap items-center gap-2">
        <SearchInput
          aria-label="Buscar no estoque"
          placeholder="Buscar no estoque"
          value={search}
          onChange={(e) => resetPage(setSearch)(e.target.value)}
        />
        {/* Radix Select não aceita SelectItem com value="" → sentinela "ALL". */}
        <Select
          value={category || "ALL"}
          onValueChange={(v) => {
            setPage(0);
            setCategory(v === "ALL" ? "" : v);
          }}
        >
          <SelectTrigger className="w-[200px]" aria-label="Filtrar por categoria">
            <SelectValue placeholder="Todas as categorias" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">Todas as categorias</SelectItem>
            {CATALOG_CATEGORIES.map((c) => (
              <SelectItem key={c} value={c}>
                {CATALOG_CATEGORY_LABELS[c]}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Button
          variant={lowStock ? "default" : "outline"}
          onClick={() => { setPage(0); setLowStock(lowStock ? undefined : true); }}
        >
          {lowStock ? "Mostrando só baixo estoque" : "Só baixo estoque"}
        </Button>
      </div>

      <Card>
        <CardContent className="pt-6">
          {isLoading ? (
            <SkeletonTable rows={5} columns={6} />
          ) : error ? (
            <ErrorCard message={error.message} onRetry={() => refetch()} />
          ) : data && data.content.length > 0 ? (
            <div className="space-y-4">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Nome</TableHead>
                    <TableHead>Categoria</TableHead>
                    <TableHead>Unidade</TableHead>
                    <TableHead className="text-right">Quantidade</TableHead>
                    <TableHead className="text-right">Mínimo</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="text-right">Ações</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.content.map((item) => (
                    <TableRow key={item.id}>
                      <TableCell className="font-medium">{item.name}</TableCell>
                      <TableCell>
                        {item.category ? CATALOG_CATEGORY_LABELS[item.category] : "—"}
                      </TableCell>
                      <TableCell>{item.unit ? UNIT_LABELS[item.unit] : "—"}</TableCell>
                      <TableCell className="text-right" data-testid="inventory-quantity">
                        {item.quantity}
                      </TableCell>
                      <TableCell className="text-right">{item.minStock}</TableCell>
                      <TableCell>
                        {item.active ? <Badge>Ativo</Badge> : <Badge variant="secondary">Inativo</Badge>}
                        <LowStockBadge item={item} />
                      </TableCell>
                      <TableCell className="text-right">
                        {canManage ? (
                          <Button variant="outline" size="sm" onClick={() => setMovementItem(item)}>
                            Movimentar
                          </Button>
                        ) : null}
                        <Button variant="ghost" size="sm" onClick={() => setHistoryItem(item)}>
                          Histórico
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <div className="flex items-center justify-between">
                <span className="text-sm text-muted-foreground">
                  Página {data.page + 1} de {Math.max(data.totalPages, 1)}
                </span>
                <div className="flex gap-2">
                  <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>
                    Anterior
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page >= data.totalPages - 1}
                    onClick={() => setPage(page + 1)}
                  >
                    Próxima
                  </Button>
                </div>
              </div>
            </div>
          ) : (
            <EmptyState title="Nenhum produto em estoque" description="Cadastre produtos no Catálogo." />
          )}
        </CardContent>
      </Card>

      {movementItem ? (
        <StockMovementDialog
          open={!!movementItem}
          onOpenChange={(open) => !open && setMovementItem(null)}
          item={movementItem}
          isSubmitting={registerMovement.isPending}
          onSubmit={(request) =>
            registerMovement.mutate(
              { itemId: movementItem.id, request },
              { onSuccess: () => setMovementItem(null) },
            )
          }
        />
      ) : null}

      {historyItem && companyId ? (
        <MovementHistorySheet
          open={!!historyItem}
          onOpenChange={(open) => !open && setHistoryItem(null)}
          companyId={companyId}
          item={historyItem}
        />
      ) : null}
    </div>
  );
}
```

> Se `EmptyState` exigir props diferentes (ex.: `message` em vez de `description`), use o que `catalog/page.tsx` usa. Se `SearchInput` não aceitar `aria-label`, envolva em `<label>` com o texto "Buscar no estoque".

- [ ] **Step 3: Verificar lint e tipo**

Run: `cd frontend-refine && npm run lint && npx tsc --noEmit`
Expected: sem erros (ajuste detalhes de props conforme os componentes reais).

- [ ] **Step 4: Commit**

```bash
git add frontend-refine/src/app/'(dashboard)'/estoque/page.tsx frontend-refine/src/lib/constants.ts frontend-refine/src/components/layout/navigation.ts frontend-refine/src/components/navigation/Breadcrumb.tsx
git commit -m "feat(inventory): /estoque page and navigation"
```

---

## Task 8: Frontend — Catálogo (tipos + dialog) e teste de regressão do e2e catalog

**Files:**
- Modify: `frontend-refine/src/features/masterdata/catalog/types/catalog.types.ts`
- Modify: `frontend-refine/src/features/masterdata/catalog/components/CatalogItemDialog.tsx`
- Modify: `frontend-refine/e2e/catalog.spec.ts` (linha 20: categoria virou Select)

**Interfaces:**
- Consumes: Task 5 (`StockUnit`, `UNIT_LABELS`, `STOCK_UNITS` — importados de `inventory/types`).
- Produces: `CatalogItem` com `unit/minStock/quantity`, `CatalogItemRequest` com `unit/minStock`, `CatalogCategory`/`CATALOG_CATEGORY_LABELS`/`CATALOG_CATEGORIES` em `catalog.types.ts`.

- [ ] **Step 1: Tipos do catálogo** — **já aplicado na Task 5** (pré-requisito do `Pick`). Aqui só confira; não duplique.

Em `src/features/masterdata/catalog/types/catalog.types.ts`:

```ts
import type { StockUnit } from "@/features/masterdata/inventory/types/inventory.types";

export type CatalogCategory = "EPI" | "INSTRUMENTO" | "MATERIAL_DE_CONSUMO" | "MEDICAMENTO" | "OUTROS";

export const CATALOG_CATEGORY_LABELS: Record<CatalogCategory, string> = {
  EPI: "EPI",
  INSTRUMENTO: "Instrumento",
  MATERIAL_DE_CONSUMO: "Material de consumo",
  MEDICAMENTO: "Medicamento",
  OUTROS: "Outros",
};

export const CATALOG_CATEGORIES = Object.keys(CATALOG_CATEGORY_LABELS) as CatalogCategory[];
```
E nos tipos:
```ts
export type CatalogItem = {
  // ...campos atuais...
  category: CatalogCategory | null;
  unit: StockUnit | null;
  minStock: number;
  quantity: number;
  // ...createdAt, updatedAt
};

export type CatalogItemRequest = {
  // ...campos atuais...
  category?: CatalogCategory;
  unit?: StockUnit | null;
  minStock?: number;
};
```

- [ ] **Step 2: Escrever o teste que falha (amostra de categorias)**

Em `src/__tests__/features/masterdata/catalog/types/catalog.types.test.ts` (crie se não existir):

```ts
import { describe, expect, it } from "vitest";
import { CATALOG_CATEGORIES } from "@/features/masterdata/catalog/types/catalog.types";

describe("catalog.types", () => {
  it("lista as categorias fixas de produtos", () => {
    expect(CATALOG_CATEGORIES).toEqual([
      "EPI",
      "INSTRUMENTO",
      "MATERIAL_DE_CONSUMO",
      "MEDICAMENTO",
      "OUTROS",
    ]);
  });
});
```

- [ ] **Step 3: Atualizar o `CatalogItemDialog`**

Em `src/features/masterdata/catalog/components/CatalogItemDialog.tsx`:
- Importe `CATALOG_CATEGORIES`/`CATALOG_CATEGORY_LABELS` de `../types/catalog.types` e `UNIT_LABELS`/`STOCK_UNITS`/`type StockUnit` de `@/features/masterdata/inventory/types/inventory.types`.
- `FormState` ganha `unit: StockUnit | ""` e `minStock: string`, além do `category` atual.
- `initialState(item)`:

```ts
function initialState(item: CatalogItem | null): FormState {
  return {
    type: item?.type ?? "PRODUCT",
    name: item?.name ?? "",
    description: item?.description ?? "",
    category: item?.category ?? "",
    price: item?.price != null ? String(item.price).replace(".", ",") : "",
    sku: item?.sku ?? "",
    unit: item?.unit ?? "",
    minStock: item ? String(item.minStock) : "0",
  };
}
```
- `category` (linhas 116-125) vira condicional:
  - `form.type === "PRODUCT"` → `Select` (id `catalog-category`) com placeholder "Selecione…" e itens `CATALOG_CATEGORIES.map((c) => <SelectItem key={c} value={c}>{CATALOG_CATEGORY_LABELS[c]}</SelectItem>)`, `onValueChange={(v) => set("category", v as CatalogCategory)}`.
  - `form.type === "SERVICE"` → `Input` de texto livre (como hoje).
- Novos campos (visíveis só quando `form.type === "PRODUCT"`), numa grade abaixo de nome/descrição:

```tsx
{form.type === "PRODUCT" ? (
  <div className="grid gap-4 sm:grid-cols-2">
    <div className="space-y-1.5">
      <Label htmlFor="catalog-unit">Unidade</Label>
      <Select value={form.unit} onValueChange={(v) => set("unit", v as StockUnit)}>
        <SelectTrigger id="catalog-unit">
          <SelectValue placeholder="Sem unidade" />
        </SelectTrigger>
        <SelectContent>
          {STOCK_UNITS.map((unit) => (
            <SelectItem key={unit} value={unit}>
              {UNIT_LABELS[unit]}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
    <div className="space-y-1.5">
      <Label htmlFor="catalog-min-stock">Estoque mínimo</Label>
      <Input
        id="catalog-min-stock"
        type="number"
        inputMode="numeric"
        min={0}
        step={1}
        value={form.minStock}
        onChange={(e) => set("minStock", e.target.value)}
      />
    </div>
  </div>
) : null}
```
- No `handleSubmit`, monte o request:

```ts
    const minStock = form.minStock.trim() ? Math.max(0, Math.floor(Number(form.minStock) || 0)) : 0;
    onSubmit({
      type: form.type,
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      category: form.type === "PRODUCT" ? (form.category || undefined) : (form.category.trim() || undefined),
      price,
      currency: "BRL",
      sku: form.sku.trim() || undefined,
      unit: form.type === "PRODUCT" && form.unit ? form.unit : null,
      minStock: form.type === "PRODUCT" ? minStock : 0,
    });
```

- [ ] **Step 4: Atualizar o e2e do catálogo**

Em `frontend-refine/e2e/catalog.spec.ts`, linha 20 — substitua o `.fill("Planos")` por seleção:

```ts
    await page.getByLabel("Categoria").click();
    await page.getByRole("option", { name: "Outros" }).click();
```
(botão "Novo item" cria `PRODUCT`; o Select de categoria aparece.)

- [ ] **Step 5: Rodar lint, tipo, prettier e vitest**

Run: `cd frontend-refine && npm run lint && npx tsc --noEmit && npx prettier --check "src/features/masterdata/catalog/**" "src/features/masterdata/inventory/**" "src/__tests__/features/masterdata/**" && npx vitest run --pool=threads`
Expected: tudo verde.

- [ ] **Step 6: Commit**

```bash
git add frontend-refine/src/features/masterdata/catalog frontend-refine/src/features/masterdata/inventory/types frontend-refine/src/__tests__/features/masterdata/catalog frontend-refine/e2e/catalog.spec.ts
git commit -m "feat(catalog): fixed product category, unit and min stock in catalog form"
```

---

## Task 9: E2E — `estoque.spec.ts`

**Files:**
- Create: `frontend-refine/e2e/estoque.spec.ts`

**Interfaces:**
- Consumes: `login` fixture (`e2e/fixtures/auth.ts`), catálogo com `Novo item`, tela `/estoque`.
- Runs no CI (Playwright job com a stack do `e2e/compose.ci.yml`), nunca local sem Docker/liveserver.

- [ ] **Step 1: Escrever o spec**

`frontend-refine/e2e/estoque.spec.ts`:

```ts
import { test, expect } from "@playwright/test";
import { login } from "./fixtures/auth";

/**
 * Controle de estoque (V095): cadastra um produto com estoque mínimo no Catálogo,
 * abre /estoque, vê o badge de baixo estoque, registra uma entrada e confere o saldo.
 */
test.describe("Estoque", () => {
  test.describe.configure({ timeout: 120_000 });

  test("registra entrada e atualiza o saldo", async ({ page }) => {
    const name = `E2E Estoque ${Date.now().toString(36)}`;
    await login(page);

    await page.goto("/catalog");
    await expect(page.getByRole("heading", { name: "Catálogo", level: 1 })).toBeVisible();
    await page.getByRole("button", { name: "Novo item" }).click();
    await page.getByLabel("Nome").fill(name);
    await page.getByLabel("Categoria").click();
    await page.getByRole("option", { name: "Material de consumo" }).click();
    await page.getByLabel("Unidade").click();
    await page.getByRole("option", { name: "Caixa" }).click();
    await page.getByLabel("Estoque mínimo").fill("5");
    await page.getByRole("button", { name: "Salvar" }).click();
    await expect(page.getByText("Item criado")).toBeVisible();

    await page.goto("/estoque");
    await expect(page.getByRole("heading", { name: "Estoque", level: 1 })).toBeVisible();

    const row = page.locator("tbody tr").filter({ hasText: name });
    await expect(row).toHaveCount(1, { timeout: 15_000 });
    await expect(row).toContainText("Baixo estoque");

    await row.getByRole("button", { name: "Movimentar" }).click();
    await page.getByLabel("Tipo").click();
    await page.getByRole("option", { name: "Entrada" }).click();
    await page.getByLabel("Quantidade").fill("10");
    await page.getByRole("button", { name: "Registrar" }).click();
    await expect(page.getByText("Movimento registrado")).toBeVisible();

    await expect(row.getByTestId("inventory-quantity")).toHaveText("10", { timeout: 15_000 });
    await expect(row).not.toContainText("Baixo estoque", { timeout: 15_000 });
  });
});
```

> Se o `toBeVisible()` do badge falhar por causa de row recém-buscada, faça a asserção com `await expect(row.locator("text=Baixo estoque")).toBeVisible();` dentro de um `toHaveText`/`toContainText`.

- [ ] **Step 2: Rodar localmente (só com Docker/CI)**

Run: `cd frontend-refine && npx playwright test e2e/estoque.spec.ts`
Expected: PASS no CI (com a stack `e2e/compose.ci.yml`). **Localmente nesta máquina não roda** (sem Docker e com dev server obsoleto na porta 3000) — verificar no job E2E Playwright do CI.

- [ ] **Step 3: Commit**

```bash
git add frontend-refine/e2e/estoque.spec.ts
git commit -m "test(e2e): estoque movement flow"
```

---

## Self-Review do plano

- **Cobertura da spec:** migration/RLS/permissions → Task 1; campos do catálogo + categoria fixa só para produto → Task 2 + Task 8; módulo e endpoints → Task 3; isolamento/grants → Task 4; feature/tela/badge/navegação → Tasks 5-7; lowStock regra `min_stock > 0` → Task 1 (SQL) + isLowStock (Task 5) consistentes; e2e → Task 9.
- **Placeholder scan:** nenhum TBD/TODO; todos os arquivos novos têm conteúdo real; `PageResponse` e `EmptyState`/`SearchInput` têm nota para conferir o formato real em runtime.
- **Consistência de tipos:** `StockMovementResponse.balanceAfter` usado em Task 3 (service) e Task 5 (tipos); `updateQuantity` definido na Task 2 e consumido na Task 3; `searchProducts`/`findByIdForUpdate` definidos na Task 3 e consumidos lá mesmo; `unit/minStock/quantity` em `CatalogItem` (Task 2) refletidos em `catalog.types` (Task 8) e `inventory.types` (Task 5).