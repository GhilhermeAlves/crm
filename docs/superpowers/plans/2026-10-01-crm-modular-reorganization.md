# CRM Modular Reorganization — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reorganize the CRM backend from layer-first (domain/application/infrastructure/presentation) to feature-module-first architecture with 8 modules, each following hexagonal architecture internally.

**Architecture:** Each module lives under `com.becommerce.crm.<module>` with internal layers: `domain/`, `application/`, `infrastructure/`, `web/`. The `shared` module holds cross-cutting infrastructure (security, tenant, config, messaging). The `automation` module contains three subdomains: workflow, campaign, and ai. Modules depend only downward through the layer hierarchy: shared → {identity, masterdata} → {sales, communication} → {analytics} → automation. Cross-module communication uses ports (interfaces) or domain events.

**Tech Stack:** Java 25, Spring Boot 3.5.16, Maven, MapStruct, JPA/Hibernate

**Spec:** `docs/superpowers/specs/2026-10-01-crm-modular-architecture-design.md` (ADR-001 published as artifact)

## Global Constraints

- This is a **package reorganization only** — no logic changes, no API changes, no behavior changes
- Each task must end with a **successful `mvn compile`** — the project must never be broken between tasks
- Tests are moved alongside their source files using the same package mapping
- `CrmApplication.java` stays at `com.becommerce.crm` (root)
- The `contact` module is **already** at `com.becommerce.crm.contact` — it gets moved to `com.becommerce.crm.masterdata.contact` in Task 3
- `PageResponse` currently in `application.identity.dto` must move to `shared` since it's used cross-module
- `WorkflowSchedulingConfig` currently in `infrastructure.config.scheduler` goes to `automation`
- `IdentityInternalController` currently in `presentation.rest.internal` goes to `identity`
- After each task: move files, update all `package` declarations, update all `import` statements, compile

## Strategy: Safe Incremental Migration

Each task follows the same mechanical pattern:
1. Create the target package directories
2. Move source files to new locations
3. Update `package` declarations in every moved file
4. Update `import` statements in ALL files that reference the moved classes
5. Move corresponding test files, update their packages/imports
6. Run `mvn compile -q` to verify
7. Run `mvn test -q` to verify tests still pass
8. Commit

**Critical:** Steps 4 is the hardest — every file in the codebase that imports a moved class must be updated. Use IDE refactoring or `grep` + `sed` to find all references.

---

### Task 1: Create `shared` module — extract cross-cutting infrastructure

**Files:**
- Move 30 files from `infrastructure/{security,tenant,config,messaging,rabbit,websocket}` + `presentation/rest/handler` → `shared/`
- Move `PageResponse` from `application/identity/dto/` → `shared/application/dto/`
- Test files: 12 tests under `infrastructure/{security,tenant,rabbit}` + `presentation/rest/handler`

**Interfaces:**
- Produces: `com.becommerce.crm.shared.security.*`, `com.becommerce.crm.shared.tenant.*`, `com.becommerce.crm.shared.config.*`, `com.becommerce.crm.shared.messaging.*`, `com.becommerce.crm.shared.rabbit.*`, `com.becommerce.crm.shared.websocket.*`, `com.becommerce.crm.shared.web.handler.*`, `com.becommerce.crm.shared.application.dto.PageResponse`

- [ ] **Step 1: Create target directory structure**

```bash
# Under backend/src/main/java/com/becommerce/crm/shared/
mkdir -p shared/security/{authorization,config,filter}
mkdir -p shared/tenant/{config,context,datasource,filter}
mkdir -p shared/config/{web,scheduler}
mkdir -p shared/messaging
mkdir -p shared/rabbit
mkdir -p shared/websocket/{config,security}
mkdir -p shared/web/handler
mkdir -p shared/application/dto
```

- [ ] **Step 2: Move source files to `shared/`**

Move each file, updating its `package` declaration:

| From | To |
|------|-----|
| `infrastructure/security/**/*.java` (16 files) | `shared/security/**/*.java` |
| `infrastructure/tenant/**/*.java` (4 files) | `shared/tenant/**/*.java` |
| `infrastructure/config/web/OpenApiConfig.java` | `shared/config/web/OpenApiConfig.java` |
| `infrastructure/messaging/SpringEventPublisher.java` | `shared/messaging/SpringEventPublisher.java` |
| `infrastructure/rabbit/*.java` (4 files) | `shared/rabbit/*.java` |
| `infrastructure/websocket/**/*.java` (2 files) | `shared/websocket/**/*.java` |
| `presentation/rest/handler/GlobalExceptionHandler.java` | `shared/web/handler/GlobalExceptionHandler.java` |
| `application/identity/dto/PageResponse.java` | `shared/application/dto/PageResponse.java` |

**Do NOT move** `infrastructure/config/scheduler/WorkflowSchedulingConfig.java` — it goes to `automation` in Task 7.

- [ ] **Step 3: Update `package` declarations in all 31 moved files**

Example for `CurrentUser.java`:
```java
// Before:
package com.becommerce.crm.infrastructure.security.filter;
// After:
package com.becommerce.crm.shared.security.filter;
```

- [ ] **Step 4: Update `import` statements across the ENTIRE codebase**

Run these replacements across all `.java` files in `backend/src/`:

```bash
# Security
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.infrastructure\.security\./com.becommerce.crm.shared.security./g' {} +

# Tenant
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.infrastructure\.tenant\./com.becommerce.crm.shared.tenant./g' {} +

# Config
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.infrastructure\.config\.web\./com.becommerce.crm.shared.config.web./g' {} +

# Messaging
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.infrastructure\.messaging\./com.becommerce.crm.shared.messaging./g' {} +

# Rabbit
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.infrastructure\.rabbit\./com.becommerce.crm.shared.rabbit./g' {} +

# Websocket
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.infrastructure\.websocket\./com.becommerce.crm.shared.websocket./g' {} +

# Handler
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.presentation\.rest\.handler\./com.becommerce.crm.shared.web.handler./g' {} +

# PageResponse
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.application\.identity\.dto\.PageResponse/com.becommerce.crm.shared.application.dto.PageResponse/g' {} +
```

- [ ] **Step 5: Move corresponding test files**

| From | To |
|------|-----|
| `test/.../infrastructure/security/**` (4 tests) | `test/.../shared/security/**` |
| `test/.../infrastructure/tenant/**` (4 tests) | `test/.../shared/tenant/**` |
| `test/.../infrastructure/rabbit/**` (2 tests) | `test/.../shared/rabbit/**` |
| `test/.../presentation/rest/handler/**` (1 test) | `test/.../shared/web/handler/**` |

Update their `package` declarations and imports identically.

- [ ] **Step 6: Compile and verify**

```bash
cd backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 7: Run tests**

```bash
cd backend && mvn test -q
```

Expected: All tests pass

- [ ] **Step 8: Commit**

```bash
git add -A backend/src/
git commit -m "refactor(shared): extract cross-cutting infrastructure into shared module

Move security, tenant, config, messaging, rabbit, websocket, and
handler to com.becommerce.crm.shared. Move PageResponse to shared
as it is used cross-module."
```

---

### Task 2: Create `identity` module — extract auth & user management

**Files:**
- Move ~148 files from `{domain,application,infrastructure,presentation}/identity` + `{domain,application,infrastructure,presentation}/{invitation,membership}` + `{application,presentation}/{me,onboarding}` + `infrastructure/otp` + `presentation/rest/internal/IdentityInternalController.java` → `identity/`
- Test files: ~25 tests

**Interfaces:**
- Consumes: `com.becommerce.crm.shared.security.*`, `com.becommerce.crm.shared.tenant.*`, `com.becommerce.crm.shared.application.dto.PageResponse`
- Produces: `com.becommerce.crm.identity.domain.*`, `com.becommerce.crm.identity.application.*`, `com.becommerce.crm.identity.infrastructure.*`, `com.becommerce.crm.identity.web.*`

- [ ] **Step 1: Create target directory structure**

```bash
# Under backend/src/main/java/com/becommerce/crm/identity/
mkdir -p identity/domain/{event,exception,valueobject,repository}
mkdir -p identity/application/dto
mkdir -p identity/application/port/{in,out}
mkdir -p identity/application/service
mkdir -p identity/infrastructure/persistence
mkdir -p identity/infrastructure/client/dto
mkdir -p identity/infrastructure/config
mkdir -p identity/infrastructure/email
mkdir -p identity/infrastructure/sms
mkdir -p identity/infrastructure/rate
mkdir -p identity/web/dto
# Subdomains within identity
mkdir -p identity/invitation/domain/exception
mkdir -p identity/invitation/application/{dto,port/{in,out},service}
mkdir -p identity/invitation/infrastructure/persistence
mkdir -p identity/invitation/infrastructure/rate
mkdir -p identity/invitation/web
mkdir -p identity/membership/domain/exception
mkdir -p identity/membership/application/{dto,port/{in,out},service}
mkdir -p identity/membership/infrastructure/persistence
mkdir -p identity/membership/web
mkdir -p identity/me/application/{dto,port/{in,out},service}
mkdir -p identity/me/web
mkdir -p identity/onboarding/application/{port/in,service}
mkdir -p identity/onboarding/web
```

- [ ] **Step 2: Move all identity source files**

Map the old packages to new:

| Old package prefix | New package prefix |
|---|---|
| `domain.identity` | `identity.domain` |
| `application.identity` | `identity.application` |
| `infrastructure.identity` | `identity.infrastructure` |
| `presentation.rest.identity` | `identity.web` |
| `domain.invitation` | `identity.invitation.domain` |
| `application.invitation` | `identity.invitation.application` |
| `infrastructure.invitation` | `identity.invitation.infrastructure` |
| `presentation.rest.invitation` | `identity.invitation.web` |
| `domain.membership` | `identity.membership.domain` |
| `application.membership` | `identity.membership.application` |
| `infrastructure.membership` | `identity.membership.infrastructure` |
| `presentation.rest.membership` | `identity.membership.web` |
| `application.me` | `identity.me.application` |
| `presentation.rest.me` | `identity.me.web` |
| `application.onboarding` | `identity.onboarding.application` |
| `presentation.rest.onboarding` | `identity.onboarding.web` |
| `infrastructure.otp` | `identity.infrastructure.rate` (merge OtpRateLimiter here) |
| `presentation.rest.internal` | `identity.web.internal` |

- [ ] **Step 3: Update `package` declarations in all ~148 moved files**

- [ ] **Step 4: Update `import` statements across the ENTIRE codebase**

```bash
# Run sed replacements for each old → new package mapping.
# Order matters — do the most specific prefixes first to avoid partial matches.

# domain.identity
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.domain\.identity\./com.becommerce.crm.identity.domain./g' {} +

# application.identity
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.application\.identity\./com.becommerce.crm.identity.application./g' {} +

# infrastructure.identity
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.infrastructure\.identity\./com.becommerce.crm.identity.infrastructure./g' {} +

# presentation.rest.identity
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.presentation\.rest\.identity\./com.becommerce.crm.identity.web./g' {} +

# domain.invitation
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.domain\.invitation\./com.becommerce.crm.identity.invitation.domain./g' {} +

# application.invitation
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.application\.invitation\./com.becommerce.crm.identity.invitation.application./g' {} +

# infrastructure.invitation
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.infrastructure\.invitation\./com.becommerce.crm.identity.invitation.infrastructure./g' {} +

# presentation.rest.invitation
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.presentation\.rest\.invitation\./com.becommerce.crm.identity.invitation.web./g' {} +

# domain.membership
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.domain\.membership\./com.becommerce.crm.identity.membership.domain./g' {} +

# application.membership
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.application\.membership\./com.becommerce.crm.identity.membership.application./g' {} +

# infrastructure.membership
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.infrastructure\.membership\./com.becommerce.crm.identity.membership.infrastructure./g' {} +

# presentation.rest.membership
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.presentation\.rest\.membership\./com.becommerce.crm.identity.membership.web./g' {} +

# application.me
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.application\.me\./com.becommerce.crm.identity.me.application./g' {} +

# presentation.rest.me
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.presentation\.rest\.me\./com.becommerce.crm.identity.me.web./g' {} +

# application.onboarding
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.application\.onboarding\./com.becommerce.crm.identity.onboarding.application./g' {} +

# presentation.rest.onboarding
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.presentation\.rest\.onboarding\./com.becommerce.crm.identity.onboarding.web./g' {} +

# infrastructure.otp
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.infrastructure\.otp\./com.becommerce.crm.identity.infrastructure.rate./g' {} +

# presentation.rest.internal
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.presentation\.rest\.internal\./com.becommerce.crm.identity.web.internal./g' {} +
```

- [ ] **Step 5: Move corresponding test files (~25 tests)**

Same package mapping as source files, under `src/test/`.

- [ ] **Step 6: Compile** — `cd backend && mvn compile -q` → BUILD SUCCESS

- [ ] **Step 7: Run tests** — `cd backend && mvn test -q` → All pass

- [ ] **Step 8: Commit**

```bash
git add -A backend/src/
git commit -m "refactor(identity): extract auth and user management into identity module

Move identity, invitation, membership, me, onboarding, and otp
into com.becommerce.crm.identity with subdomain organization."
```

---

### Task 3: Create `masterdata` module — extract company, catalog, storage, contact, quota

**Files:**
- Move ~55 files from `{domain,application,infrastructure,presentation}/{company,catalog,storage}` + `domain/quota` → `masterdata/`
- Move `contact/` → `masterdata/contact/` (already feature-organized, just re-parent)
- Test files: ~6 tests

**Interfaces:**
- Consumes: `com.becommerce.crm.shared.*`, `com.becommerce.crm.identity.*` (for user references)
- Produces: `com.becommerce.crm.masterdata.company.*`, `com.becommerce.crm.masterdata.catalog.*`, `com.becommerce.crm.masterdata.storage.*`, `com.becommerce.crm.masterdata.contact.*`, `com.becommerce.crm.masterdata.quota.*`

- [ ] **Step 1: Create target directory structure**

```bash
mkdir -p masterdata/company/domain/{event,exception}
mkdir -p masterdata/company/application/{dto,port/{in,out},service}
mkdir -p masterdata/company/infrastructure/persistence
mkdir -p masterdata/company/web
mkdir -p masterdata/catalog/domain/exception
mkdir -p masterdata/catalog/application/{dto,port/out,service}
mkdir -p masterdata/catalog/infrastructure/persistence
mkdir -p masterdata/catalog/web
mkdir -p masterdata/storage/domain/exception
mkdir -p masterdata/storage/application/{dto,port/{in,out},service}
mkdir -p masterdata/storage/infrastructure/persistence
mkdir -p masterdata/storage/web
mkdir -p masterdata/quota/domain/exception
```

- [ ] **Step 2: Move source files**

| Old package prefix | New package prefix |
|---|---|
| `domain.company` | `masterdata.company.domain` |
| `application.company` | `masterdata.company.application` |
| `infrastructure.company` | `masterdata.company.infrastructure` |
| `presentation.rest.company` | `masterdata.company.web` |
| `domain.catalog` | `masterdata.catalog.domain` |
| `application.catalog` | `masterdata.catalog.application` |
| `infrastructure.catalog` | `masterdata.catalog.infrastructure` |
| `presentation.rest.catalog` | `masterdata.catalog.web` |
| `domain.storage` | `masterdata.storage.domain` |
| `application.storage` | `masterdata.storage.application` |
| `infrastructure.storage` | `masterdata.storage.infrastructure` |
| `presentation.rest.storage` | `masterdata.storage.web` |
| `domain.quota` | `masterdata.quota.domain` |
| `contact` | `masterdata.contact` (re-parent the existing module) |

- [ ] **Step 3: Update packages and imports**

```bash
# Company
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.domain\.company\./com.becommerce.crm.masterdata.company.domain./g' {} +
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.application\.company\./com.becommerce.crm.masterdata.company.application./g' {} +
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.infrastructure\.company\./com.becommerce.crm.masterdata.company.infrastructure./g' {} +
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.presentation\.rest\.company\./com.becommerce.crm.masterdata.company.web./g' {} +

# Catalog
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.domain\.catalog\./com.becommerce.crm.masterdata.catalog.domain./g' {} +
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.application\.catalog\./com.becommerce.crm.masterdata.catalog.application./g' {} +
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.infrastructure\.catalog\./com.becommerce.crm.masterdata.catalog.infrastructure./g' {} +
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.presentation\.rest\.catalog\./com.becommerce.crm.masterdata.catalog.web./g' {} +

# Storage
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.domain\.storage\./com.becommerce.crm.masterdata.storage.domain./g' {} +
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.application\.storage\./com.becommerce.crm.masterdata.storage.application./g' {} +
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.infrastructure\.storage\./com.becommerce.crm.masterdata.storage.infrastructure./g' {} +
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.presentation\.rest\.storage\./com.becommerce.crm.masterdata.storage.web./g' {} +

# Quota
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.domain\.quota\./com.becommerce.crm.masterdata.quota.domain./g' {} +

# Contact (re-parent)
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.contact\./com.becommerce.crm.masterdata.contact./g' {} +
```

- [ ] **Step 4: Move test files (~6 tests)** — update packages/imports

- [ ] **Step 5: Compile** — `cd backend && mvn compile -q` → BUILD SUCCESS

- [ ] **Step 6: Run tests** — `cd backend && mvn test -q` → All pass

- [ ] **Step 7: Commit**

```bash
git add -A backend/src/
git commit -m "refactor(masterdata): extract company, catalog, storage, contact, quota into masterdata module

Absorb the existing contact module under masterdata.contact."
```

---

### Task 4: Create `sales` module — extract pipeline, lead, followup, activity, task

**Files:**
- Move ~124 files from `{domain,application,infrastructure,presentation}/{pipeline,lead,followup,activity,task}` → `sales/`
- Test files: ~22 tests

**Interfaces:**
- Consumes: `com.becommerce.crm.shared.*`, `com.becommerce.crm.identity.*`, `com.becommerce.crm.masterdata.contact.*`
- Produces: `com.becommerce.crm.sales.pipeline.*`, `com.becommerce.crm.sales.lead.*`, `com.becommerce.crm.sales.followup.*`, `com.becommerce.crm.sales.activity.*`, `com.becommerce.crm.sales.task.*`

- [ ] **Step 1: Create target directories**

```bash
mkdir -p sales/{pipeline,lead,followup,activity,task}
# Each subdomain gets: domain/exception, application/{dto,port/{in,out},service}, infrastructure/persistence, web
for sub in pipeline lead followup activity task; do
  mkdir -p sales/$sub/domain/exception
  mkdir -p sales/$sub/application/{dto,port/{in,out},service}
  mkdir -p sales/$sub/infrastructure/persistence
  mkdir -p sales/$sub/web
done
# Extra dirs for specific subdomains
mkdir -p sales/pipeline/domain/exception
mkdir -p sales/followup/application/event
mkdir -p sales/followup/infrastructure/{messaging,scheduler}
```

- [ ] **Step 2: Move source files using same package mapping pattern**

| Old prefix | New prefix |
|---|---|
| `domain.pipeline` | `sales.pipeline.domain` |
| `application.pipeline` | `sales.pipeline.application` |
| `infrastructure.pipeline` | `sales.pipeline.infrastructure` |
| `presentation.rest.pipeline` | `sales.pipeline.web` |
| `domain.lead` | `sales.lead.domain` |
| `application.lead` | `sales.lead.application` |
| `infrastructure.lead` | `sales.lead.infrastructure` |
| `presentation.rest.lead` | `sales.lead.web` |
| `domain.followup` | `sales.followup.domain` |
| `application.followup` | `sales.followup.application` |
| `infrastructure.followup` | `sales.followup.infrastructure` |
| `presentation.rest.followup` | `sales.followup.web` |
| `domain.activity` | `sales.activity.domain` |
| `application.activity` | `sales.activity.application` |
| `infrastructure.activity` | `sales.activity.infrastructure` |
| `presentation.rest.activity` | `sales.activity.web` |
| `domain.task` | `sales.task.domain` |
| `application.task` | `sales.task.application` |
| `infrastructure.task` | `sales.task.infrastructure` |
| `presentation.rest.task` | `sales.task.web` |

- [ ] **Step 3: Update packages and imports** (sed for each mapping, same pattern as previous tasks)

- [ ] **Step 4: Move test files (~22 tests)** — update packages/imports

- [ ] **Step 5: Compile** — `cd backend && mvn compile -q` → BUILD SUCCESS

- [ ] **Step 6: Run tests** — `cd backend && mvn test -q` → All pass

- [ ] **Step 7: Commit**

```bash
git add -A backend/src/
git commit -m "refactor(sales): extract pipeline, lead, followup, activity, task into sales module"
```

---

### Task 5: Create `communication` module — extract notification, omnichannel, template

**Files:**
- Move ~93 files from `{domain,application,infrastructure,presentation}/{notification,omnichannel,template}` → `communication/`
- Test files: ~16 tests

**Interfaces:**
- Consumes: `com.becommerce.crm.shared.*`, `com.becommerce.crm.masterdata.*`
- Produces: `com.becommerce.crm.communication.notification.*`, `com.becommerce.crm.communication.omnichannel.*`, `com.becommerce.crm.communication.template.*`

- [ ] **Step 1: Create target directories**

```bash
for sub in notification omnichannel template; do
  mkdir -p communication/$sub/domain/exception
  mkdir -p communication/$sub/application/{dto,port/{in,out},service}
  mkdir -p communication/$sub/infrastructure/persistence
  mkdir -p communication/$sub/web
done
mkdir -p communication/omnichannel/application/event
mkdir -p communication/omnichannel/infrastructure/{messaging,whatsapp}
mkdir -p communication/notification/infrastructure/websocket
```

- [ ] **Step 2: Move source files**

| Old prefix | New prefix |
|---|---|
| `domain.notification` | `communication.notification.domain` |
| `application.notification` | `communication.notification.application` |
| `infrastructure.notification` | `communication.notification.infrastructure` |
| `presentation.rest.notification` | `communication.notification.web` |
| `domain.omnichannel` | `communication.omnichannel.domain` |
| `application.omnichannel` | `communication.omnichannel.application` |
| `infrastructure.omnichannel` | `communication.omnichannel.infrastructure` |
| `presentation.rest.omnichannel` | `communication.omnichannel.web` |
| `domain.template` | `communication.template.domain` |
| `application.template` | `communication.template.application` |
| `infrastructure.template` | `communication.template.infrastructure` |
| `presentation.rest.template` | `communication.template.web` |

- [ ] **Step 3: Update packages and imports** (sed for each mapping)

- [ ] **Step 4: Move test files (~16 tests)** — update packages/imports

- [ ] **Step 5: Compile** — `cd backend && mvn compile -q` → BUILD SUCCESS

- [ ] **Step 6: Run tests** — `cd backend && mvn test -q` → All pass

- [ ] **Step 7: Commit**

```bash
git add -A backend/src/
git commit -m "refactor(communication): extract notification, omnichannel, template into communication module"
```

---

### Task 6: Create `analytics` module — extract audit, analytics, dashboard, customer360

**Files:**
- Move ~40 files from `{domain,application,infrastructure,presentation}/{audit,analytics,dashboard,customer360}` → `analytics/`
- Test files: ~8 tests

**Interfaces:**
- Consumes: `com.becommerce.crm.shared.*`, `com.becommerce.crm.sales.pipeline.*`, `com.becommerce.crm.sales.activity.*`, `com.becommerce.crm.sales.task.*`, `com.becommerce.crm.masterdata.contact.*`
- Produces: `com.becommerce.crm.analytics.audit.*`, `com.becommerce.crm.analytics.reporting.*`, `com.becommerce.crm.analytics.dashboard.*`, `com.becommerce.crm.analytics.customer360.*`

- [ ] **Step 1: Create target directories**

```bash
mkdir -p analytics/audit/domain
mkdir -p analytics/audit/application/{dto,port/{in,out},service}
mkdir -p analytics/audit/infrastructure/{annotation,aspect,config,context,interceptor,listener,persistence}
mkdir -p analytics/audit/web
mkdir -p analytics/reporting/application/{dto,port/in,service}
mkdir -p analytics/reporting/web
mkdir -p analytics/dashboard/application/{dto,service}
mkdir -p analytics/dashboard/web
mkdir -p analytics/customer360/application/{dto,service}
mkdir -p analytics/customer360/web
```

- [ ] **Step 2: Move source files**

| Old prefix | New prefix |
|---|---|
| `domain.audit` | `analytics.audit.domain` |
| `application.audit` | `analytics.audit.application` |
| `infrastructure.audit` | `analytics.audit.infrastructure` |
| `presentation.rest.audit` | `analytics.audit.web` |
| `application.analytics` | `analytics.reporting.application` |
| `presentation.rest.analytics` | `analytics.reporting.web` |
| `application.dashboard` | `analytics.dashboard.application` |
| `presentation.rest.dashboard` | `analytics.dashboard.web` |
| `application.customer360` | `analytics.customer360.application` |
| `presentation.rest.customer360` | `analytics.customer360.web` |

- [ ] **Step 3: Update packages and imports** (sed for each mapping)

- [ ] **Step 4: Move test files (~8 tests)** — update packages/imports

- [ ] **Step 5: Compile** — `cd backend && mvn compile -q` → BUILD SUCCESS

- [ ] **Step 6: Run tests** — `cd backend && mvn test -q` → All pass

- [ ] **Step 7: Commit**

```bash
git add -A backend/src/
git commit -m "refactor(analytics): extract audit, analytics, dashboard, customer360 into analytics module"
```

---

### Task 7: Create `automation` module — extract workflow, campaign, AI assistant, tools, and OpenAI integration

**Files:**
- Move ~87 files from `{domain,application,infrastructure,presentation}/{workflow,campaign}` → `automation/{workflow,campaign}/`
- Move ~113 files from `{domain,application,infrastructure,presentation}/ai` + `infrastructure/integration/openai` → `automation/ai/`
- Move `infrastructure/config/scheduler/WorkflowSchedulingConfig.java` → `automation/workflow/infrastructure/config/`
- Test files: ~28 tests

**Interfaces:**
- Consumes: `com.becommerce.crm.shared.*`, `com.becommerce.crm.sales.activity.*`, `com.becommerce.crm.sales.task.*`, `com.becommerce.crm.communication.template.*`, `com.becommerce.crm.communication.omnichannel.*`, `com.becommerce.crm.analytics.audit.*`, `com.becommerce.crm.analytics.customer360.*`, `com.becommerce.crm.masterdata.contact.*`
- Produces: `com.becommerce.crm.automation.workflow.*`, `com.becommerce.crm.automation.campaign.*`, `com.becommerce.crm.automation.ai.*`

- [ ] **Step 1: Create target directories**

```bash
# Workflow and campaign subdomains
for sub in workflow campaign; do
  mkdir -p automation/$sub/domain/{exception,event}
  mkdir -p automation/$sub/application/{dto,port/{in,out},service}
  mkdir -p automation/$sub/infrastructure/persistence
  mkdir -p automation/$sub/web
done
mkdir -p automation/workflow/infrastructure/{config,listener,scheduler}
mkdir -p automation/campaign/infrastructure/{audience,dispatcher,scheduler}

# AI subdomain
mkdir -p automation/ai/domain
mkdir -p automation/ai/application/{action,context,dto,port/{in,out},service,tool/{tools,write}}
mkdir -p automation/ai/infrastructure/{persistence,openai}
mkdir -p automation/ai/web
```

- [ ] **Step 2: Move source files**

| Old prefix | New prefix |
|---|---|
| `domain.workflow` | `automation.workflow.domain` |
| `application.workflow` | `automation.workflow.application` |
| `infrastructure.workflow` | `automation.workflow.infrastructure` |
| `presentation.rest.workflow` | `automation.workflow.web` |
| `domain.campaign` | `automation.campaign.domain` |
| `application.campaign` | `automation.campaign.application` |
| `infrastructure.campaign` | `automation.campaign.infrastructure` |
| `presentation.rest.campaign` | `automation.campaign.web` |
| `infrastructure.config.scheduler.WorkflowSchedulingConfig` | `automation.workflow.infrastructure.config.WorkflowSchedulingConfig` |
| `domain.ai` | `automation.ai.domain` |
| `application.ai` | `automation.ai.application` |
| `infrastructure.ai` | `automation.ai.infrastructure` |
| `infrastructure.integration.openai` | `automation.ai.infrastructure.openai` |
| `presentation.rest.ai` | `automation.ai.web` |

- [ ] **Step 3: Update packages and imports**

```bash
# Workflow
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.domain\.workflow\./com.becommerce.crm.automation.workflow.domain./g' {} +
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.application\.workflow\./com.becommerce.crm.automation.workflow.application./g' {} +
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.infrastructure\.workflow\./com.becommerce.crm.automation.workflow.infrastructure./g' {} +
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.presentation\.rest\.workflow\./com.becommerce.crm.automation.workflow.web./g' {} +

# Campaign
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.domain\.campaign\./com.becommerce.crm.automation.campaign.domain./g' {} +
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.application\.campaign\./com.becommerce.crm.automation.campaign.application./g' {} +
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.infrastructure\.campaign\./com.becommerce.crm.automation.campaign.infrastructure./g' {} +
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.presentation\.rest\.campaign\./com.becommerce.crm.automation.campaign.web./g' {} +

# AI domain
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.domain\.ai\./com.becommerce.crm.automation.ai.domain./g' {} +

# AI application
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.application\.ai\./com.becommerce.crm.automation.ai.application./g' {} +

# AI infrastructure (must come BEFORE the openai replacement to avoid partial match)
# First do the more specific openai pattern
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.infrastructure\.integration\.openai\./com.becommerce.crm.automation.ai.infrastructure.openai./g' {} +

# Then the general ai infrastructure pattern
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.infrastructure\.ai\./com.becommerce.crm.automation.ai.infrastructure./g' {} +

# AI presentation
find backend/src -name "*.java" -exec sed -i \
  's/com\.becommerce\.crm\.presentation\.rest\.ai\./com.becommerce.crm.automation.ai.web./g' {} +

# WorkflowSchedulingConfig (specific file — handle package declaration separately)
```

- [ ] **Step 4: Move test files (~28 tests)** — update packages/imports

- [ ] **Step 5: Compile** — `cd backend && mvn compile -q` → BUILD SUCCESS

- [ ] **Step 6: Run tests** — `cd backend && mvn test -q` → All pass

- [ ] **Step 7: Commit**

```bash
git add -A backend/src/
git commit -m "refactor(automation): extract workflow, campaign, and AI into automation module

Merge automation and AI into a single module with three subdomains:
workflow, campaign, and ai."
```

---

### Task 8: Cleanup — remove empty old directories and verify final structure

**Files:**
- Delete empty `domain/`, `application/`, `infrastructure/`, `presentation/` directories
- Verify no orphan files remain

- [ ] **Step 1: Verify no files remain in old directories**

```bash
find backend/src/main/java/com/becommerce/crm/domain -type f 2>/dev/null
find backend/src/main/java/com/becommerce/crm/application -type f 2>/dev/null
find backend/src/main/java/com/becommerce/crm/infrastructure -type f 2>/dev/null
find backend/src/main/java/com/becommerce/crm/presentation -type f 2>/dev/null
find backend/src/main/java/com/becommerce/crm/contact -type f 2>/dev/null
```

Expected: No output (all files have been moved)

- [ ] **Step 2: Delete empty old directories**

```bash
rm -rf backend/src/main/java/com/becommerce/crm/domain
rm -rf backend/src/main/java/com/becommerce/crm/application
rm -rf backend/src/main/java/com/becommerce/crm/infrastructure
rm -rf backend/src/main/java/com/becommerce/crm/presentation
rm -rf backend/src/main/java/com/becommerce/crm/contact
# Same for test directories
rm -rf backend/src/test/java/com/becommerce/crm/domain
rm -rf backend/src/test/java/com/becommerce/crm/application
rm -rf backend/src/test/java/com/becommerce/crm/infrastructure
rm -rf backend/src/test/java/com/becommerce/crm/presentation
rm -rf backend/src/test/java/com/becommerce/crm/contact
```

- [ ] **Step 3: Verify final structure matches the ADR**

```bash
find backend/src/main/java/com/becommerce/crm -type d -mindepth 1 -maxdepth 1 | sort
```

Expected output:
```
backend/src/main/java/com/becommerce/crm/analytics
backend/src/main/java/com/becommerce/crm/automation
backend/src/main/java/com/becommerce/crm/communication
backend/src/main/java/com/becommerce/crm/identity
backend/src/main/java/com/becommerce/crm/masterdata
backend/src/main/java/com/becommerce/crm/sales
backend/src/main/java/com/becommerce/crm/shared
```

- [ ] **Step 4: Final compile and full test run**

```bash
cd backend && mvn clean compile -q && mvn test -q
```

Expected: BUILD SUCCESS, all tests pass

- [ ] **Step 5: Commit**

```bash
git add -A backend/src/
git commit -m "refactor: remove empty legacy package directories

All files have been migrated to the new modular structure.
Final structure: shared, identity, masterdata, sales,
communication, analytics, automation."
```
