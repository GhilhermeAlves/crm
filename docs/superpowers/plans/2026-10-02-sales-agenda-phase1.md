# Agenda de agendamentos — Fase 1 (agenda interna) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Equipe consegue configurar tipos de agendamento e disponibilidade, ver horários livres e marcar/remarcar/cancelar agendamentos numa página `/agenda` estilo Capim.

**Architecture:** Novo bounded context `sales/scheduling` no backend (hexagonal, mesmo padrão de `sales/task`: domain → application/port/in|out → service → infrastructure/persistence → web). O cálculo de horários livres é uma função pura (`SlotCalculator`). Conflitos são garantidos no banco por restrição de exclusão (`btree_gist`) + advisory lock por host. Frontend em `frontend-refine/src/features/sales/scheduling` com calendário próprio (CSS grid + date-fns).

**Tech Stack:** Java 21 / Spring Boot / JPA / Flyway / PostgreSQL 17 / JUnit 5 + Mockito + Testcontainers; Next.js 14 / React 18 / TanStack Query / react-hook-form + zod / date-fns 4 / vitest.

**Spec:** `docs/superpowers/specs/2026-10-02-sales-agenda-design.md` (seção "Fase 1").

## Global Constraints

- Branch: `feature/sales-agenda` (a partir de `feature/frontend-refine`).
- Pacote backend: `com.becommerce.crm.sales.scheduling`.
- Migration desta fase: `V079__scheduling.sql` (última existente é V078). Não editar migrations antigas.
- Todas as tabelas com `company_id NOT NULL`, RLS `ENABLE` + `FORCE` + `tenant_isolation_policy` usando `app.current_tenant_id()` (padrão V039). Grants vêm do loop da V034 — não escrever GRANT.
- Datas no banco em `TIMESTAMPTZ`; no Java `Instant`; no JSON ISO-8601 UTC. Disponibilidade em `LocalTime` + `ZoneId` do usuário (default `America/Sao_Paulo`).
- Mensagens de erro em PT-BR. 400 = validação, 404 = não encontrado/outra empresa, 409 = horário ocupado.
- Serviços: `TenantContext.setCompanyId(companyId)` no início e `TenantContext.clear()` no `finally` (padrão `TaskService`).
- Permissões: `appointment:create|read|update|delete`, `scheduling:configure`, `appointment:page:view`.
- Frontend: sem novas dependências npm.
- Commits terminam com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Estrutura de arquivos

Backend `backend/src/main/java/com/becommerce/crm/sales/scheduling/`:
```
domain/
  AppointmentType.java          tipo de agendamento (duração, buffers, regras)
  LocationKind.java             GOOGLE_MEET | PHONE | IN_PERSON
  AssignmentMode.java           ROUND_ROBIN | CHOOSE_HOST
  WeeklyAvailability.java       janelas semanais + exceções de um usuário (value object)
  AvailabilityWindow.java       record (DayOfWeek, LocalTime start, LocalTime end)
  AvailabilityOverride.java     record (LocalDate date, List<TimeRange> windows) — vazio = dia indisponível
  TimeRange.java                record (LocalTime start, LocalTime end)
  Appointment.java              agendamento
  AppointmentStatus.java        SCHEDULED | CONFIRMED | CANCELED | COMPLETED | NO_SHOW
  AppointmentSource.java        INTERNAL | PUBLIC_LINK
  ScheduleBlock.java            bloqueio (source INTERNAL | GOOGLE)
  BlockSource.java
  Interval.java                 record (Instant start, Instant end) + overlaps()
  SlotCalculator.java           função pura de horários livres
  exception/SchedulingNotFoundException.java     404
  exception/SchedulingValidationException.java   400
  exception/SlotUnavailableException.java        409
application/
  dto/  (records de request/response — ver Task 6)
  port/in/AppointmentTypeUseCase.java, AvailabilityUseCase.java, AppointmentUseCase.java, BlockUseCase.java
  port/out/AppointmentTypeRepository.java, AvailabilityRepository.java, AppointmentRepository.java, ScheduleBlockRepository.java
  service/AppointmentTypeService.java, AvailabilityService.java, AppointmentService.java, BlockService.java, SlotService.java
infrastructure/persistence/
  *JpaEntity.java, *JpaRepository.java, *RepositoryImpl.java (um trio por agregado)
web/
  AppointmentTypeController.java, AvailabilityController.java, AppointmentController.java, BlockController.java
```
Modificar: `shared/web/handler/GlobalExceptionHandler.java`, `identity/infrastructure/persistence/RoleSeedService.java`, `analytics/audit/domain/AuditModule.java`.

Frontend `frontend-refine/src/`:
```
features/sales/scheduling/
  types/scheduling.types.ts
  services/scheduling.service.ts
  hooks/useScheduling.ts
  schemas/appointment.schema.ts (+ .test.ts)
  lib/calendar-layout.ts (+ .test.ts)      posicionamento/sobreposição
  lib/calendar-range.ts (+ .test.ts)       períodos dia/semana/mês e navegação
  components/AgendaToolbar.tsx, AgendaSidebar.tsx, MiniCalendar.tsx, TimeGridView.tsx,
             MonthView.tsx, AppointmentDialog.tsx, BlockDialog.tsx, AppointmentPopover.tsx,
             AppointmentTypesManager.tsx, AvailabilityEditor.tsx
app/(dashboard)/agenda/page.tsx
app/(dashboard)/settings/agenda/page.tsx
```
Modificar: `lib/constants.ts` (ROUTES), `components/layout/navigation.ts`.

---

### Task 1: Migration, permissões e seed de papéis

**Files:**
- Create: `backend/src/main/resources/db/migration/V079__scheduling.sql`
- Modify: `backend/src/main/java/com/becommerce/crm/identity/infrastructure/persistence/RoleSeedService.java` (listas ADMIN, MANAGER, AGENT, VIEWER)
- Modify: `backend/src/main/java/com/becommerce/crm/analytics/audit/domain/AuditModule.java`

**Interfaces:**
- Produces: tabelas `appointment_types`, `appointment_type_hosts`, `availability_rules`, `availability_overrides`, `user_scheduling_settings`, `appointments`, `schedule_blocks`; constraint `ex_appointments_host_no_overlap`; `AuditModule.SCHEDULING`.

- [ ] **Step 1: Escrever a migration**

```sql
-- V079__scheduling.sql
-- Agenda de agendamentos (fase 1). Padrão V039: company_id NOT NULL, RLS FORCE,
-- tenant_isolation_policy. Grants via loop da V034.
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE appointment_types (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id             UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name                   VARCHAR(120) NOT NULL,
    slug                   VARCHAR(80)  NOT NULL,
    description            TEXT,
    duration_minutes       INT NOT NULL,
    buffer_before_minutes  INT NOT NULL DEFAULT 0,
    buffer_after_minutes   INT NOT NULL DEFAULT 0,
    min_notice_hours       INT NOT NULL DEFAULT 2,
    max_days_ahead         INT NOT NULL DEFAULT 60,
    slot_interval_minutes  INT NOT NULL DEFAULT 30,
    color                  VARCHAR(7),
    location_kind          VARCHAR(20) NOT NULL DEFAULT 'GOOGLE_MEET',
    location_detail        VARCHAR(255),
    assignment_mode        VARCHAR(20) NOT NULL DEFAULT 'ROUND_ROBIN',
    public_booking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    active                 BOOLEAN NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_appointment_types_slug UNIQUE (company_id, slug),
    CONSTRAINT chk_at_duration CHECK (duration_minutes BETWEEN 5 AND 480),
    CONSTRAINT chk_at_buffers CHECK (buffer_before_minutes BETWEEN 0 AND 240 AND buffer_after_minutes BETWEEN 0 AND 240),
    CONSTRAINT chk_at_interval CHECK (slot_interval_minutes IN (5, 10, 15, 20, 30, 60)),
    CONSTRAINT chk_at_location CHECK (location_kind IN ('GOOGLE_MEET', 'PHONE', 'IN_PERSON')),
    CONSTRAINT chk_at_assignment CHECK (assignment_mode IN ('ROUND_ROBIN', 'CHOOSE_HOST'))
);

CREATE TABLE appointment_type_hosts (
    appointment_type_id UUID NOT NULL REFERENCES appointment_types(id) ON DELETE CASCADE,
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id          UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    PRIMARY KEY (appointment_type_id, user_id)
);

CREATE TABLE user_scheduling_settings (
    user_id     UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    timezone    VARCHAR(64) NOT NULL DEFAULT 'America/Sao_Paulo',
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE availability_rules (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    weekday     SMALLINT NOT NULL,           -- 1 = segunda ... 7 = domingo (ISO)
    start_time  TIME NOT NULL,
    end_time    TIME NOT NULL,
    CONSTRAINT chk_ar_weekday CHECK (weekday BETWEEN 1 AND 7),
    CONSTRAINT chk_ar_range CHECK (end_time > start_time)
);
CREATE INDEX idx_availability_rules_user ON availability_rules (company_id, user_id);

CREATE TABLE availability_overrides (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date        DATE NOT NULL,
    start_time  TIME,                        -- NULL/NULL = dia indisponível
    end_time    TIME,
    CONSTRAINT chk_ao_range CHECK ((start_time IS NULL AND end_time IS NULL) OR end_time > start_time)
);
CREATE INDEX idx_availability_overrides_user ON availability_overrides (company_id, user_id, date);

CREATE TABLE appointments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id          UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    appointment_type_id UUID REFERENCES appointment_types(id) ON DELETE SET NULL,
    host_id             UUID NOT NULL REFERENCES users(id),
    contact_id          UUID REFERENCES contacts(id) ON DELETE SET NULL,
    opportunity_id      UUID REFERENCES opportunities(id) ON DELETE SET NULL,
    title               VARCHAR(200) NOT NULL,
    start_at            TIMESTAMPTZ NOT NULL,
    end_at              TIMESTAMPTZ NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    source              VARCHAR(20) NOT NULL DEFAULT 'INTERNAL',
    location_kind       VARCHAR(20),
    location_detail     VARCHAR(255),
    meeting_url         VARCHAR(500),
    notes               TEXT,
    cancel_reason       VARCHAR(500),
    public_token        VARCHAR(64) NOT NULL UNIQUE,
    google_event_id     VARCHAR(255),
    created_by          UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_appt_range CHECK (end_at > start_at),
    CONSTRAINT chk_appt_status CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'CANCELED', 'COMPLETED', 'NO_SHOW')),
    CONSTRAINT chk_appt_source CHECK (source IN ('INTERNAL', 'PUBLIC_LINK')),
    CONSTRAINT ex_appointments_host_no_overlap EXCLUDE USING gist (
        host_id WITH =, tstzrange(start_at, end_at, '[)') WITH &&
    ) WHERE (status <> 'CANCELED')
);
CREATE INDEX idx_appointments_host_start ON appointments (company_id, host_id, start_at);
CREATE INDEX idx_appointments_contact ON appointments (contact_id);
CREATE INDEX idx_appointments_opportunity ON appointments (opportunity_id);

CREATE TABLE schedule_blocks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    host_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_at    TIMESTAMPTZ NOT NULL,
    end_at      TIMESTAMPTZ NOT NULL,
    reason      VARCHAR(200),
    source      VARCHAR(20) NOT NULL DEFAULT 'INTERNAL',
    external_id VARCHAR(255),
    created_by  UUID,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_block_range CHECK (end_at > start_at),
    CONSTRAINT chk_block_source CHECK (source IN ('INTERNAL', 'GOOGLE'))
);
CREATE INDEX idx_schedule_blocks_host_start ON schedule_blocks (company_id, host_id, start_at);

DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['appointment_types', 'appointment_type_hosts', 'user_scheduling_settings',
                             'availability_rules', 'availability_overrides', 'appointments', 'schedule_blocks']
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format('CREATE POLICY tenant_isolation_policy ON %I USING (company_id = app.current_tenant_id()) WITH CHECK (company_id = app.current_tenant_id())', t);
    END LOOP;
END $$;

INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('appointment:create',    'Create appointments',         'crm',    'appointment', 'create'),
    ('appointment:read',      'Read appointments',           'crm',    'appointment', 'read'),
    ('appointment:update',    'Update appointments',         'crm',    'appointment', 'update'),
    ('appointment:delete',    'Delete appointments',         'crm',    'appointment', 'delete'),
    ('scheduling:configure',  'Configure appointment types', 'crm',    'scheduling',  'configure'),
    ('appointment:page:view', 'View Agenda page',            'agenda', 'appointment', 'page:view')
ON CONFLICT (name) DO NOTHING;

-- Vincula às empresas existentes (padrão V066); novas empresas recebem via RoleSeedService.
DO $$
DECLARE c RECORD;
BEGIN
    FOR c IN SELECT id FROM companies LOOP
        PERFORM set_config('app.current_company_id', c.id::text, false);
        INSERT INTO role_permissions (role_id, permission_id)
        SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
        WHERE (r.name IN ('ADMIN', 'MANAGER') AND p.name IN ('appointment:create', 'appointment:read',
                   'appointment:update', 'appointment:delete', 'scheduling:configure', 'appointment:page:view'))
           OR (r.name = 'AGENT' AND p.name IN ('appointment:create', 'appointment:read',
                   'appointment:update', 'appointment:page:view'))
           OR (r.name = 'VIEWER' AND p.name IN ('appointment:read', 'appointment:page:view'))
        ON CONFLICT (role_id, permission_id) DO NOTHING;
    END LOOP;
    PERFORM set_config('app.current_company_id', NULL::text, false);
END $$;
```

> Antes de escrever: confirme que `roles` filtra por `app.current_company_id` igual à V066 (`grep -n "current_company_id" backend/src/main/resources/db/migration/V066*`). Se a V066 usa outra variável, use a mesma.

- [ ] **Step 2: Atualizar RoleSeedService**

Nas listas: ADMIN e MANAGER recebem `"appointment:create", "appointment:read", "appointment:update", "appointment:delete", "scheduling:configure", "appointment:page:view"`; AGENT recebe `"appointment:create", "appointment:read", "appointment:update", "appointment:page:view"`; VIEWER recebe `"appointment:read", "appointment:page:view"`. Adicione logo abaixo da linha de `task:*` de cada perfil.

- [ ] **Step 3: Adicionar `SCHEDULING` em `AuditModule`** (após `TASKS`).

- [ ] **Step 4: Validar a migration** com o Postgres de dev: `cd backend && ./mvnw -q flyway:info` ou subindo a app (`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`) e checando log "Successfully applied 1 migration ... v079". Esperado: sem erro.

- [ ] **Step 5: Commit** `feat(scheduling): add V079 scheduling schema and permissions`

---

### Task 2: Domínio — tipos de valor, AppointmentType, WeeklyAvailability

**Files:**
- Create: `domain/Interval.java`, `TimeRange.java`, `AvailabilityWindow.java`, `AvailabilityOverride.java`, `WeeklyAvailability.java`, `AppointmentType.java`, `LocationKind.java`, `AssignmentMode.java`, `exception/SchedulingValidationException.java`, `exception/SchedulingNotFoundException.java`, `exception/SlotUnavailableException.java`
- Test: `backend/src/test/java/com/becommerce/crm/sales/scheduling/domain/AppointmentTypeTest.java`, `WeeklyAvailabilityTest.java`

**Interfaces:**
- Produces:
  - `record Interval(Instant start, Instant end)` com `boolean overlaps(Interval o)` (semiaberto `[start,end)`), `Interval expand(Duration before, Duration after)`; construtor valida `end.isAfter(start)`.
  - `record TimeRange(LocalTime start, LocalTime end)` valida `end > start`.
  - `record AvailabilityWindow(DayOfWeek day, TimeRange range)`.
  - `record AvailabilityOverride(LocalDate date, List<TimeRange> ranges)` — lista vazia = indisponível.
  - `WeeklyAvailability(UUID userId, ZoneId zone, List<AvailabilityWindow> windows, List<AvailabilityOverride> overrides)` com `List<Interval> intervalsOn(LocalDate date)` (override substitui as janelas do dia; converte para Instant no fuso) e `static WeeklyAvailability defaultFor(UUID userId)` (seg–sex 09:00–18:00, `America/Sao_Paulo`). Valida que janelas do mesmo dia não se sobrepõem.
  - `AppointmentType.create(UUID companyId, String name, String slug, Integer durationMinutes, Integer bufferBefore, Integer bufferAfter, Integer minNoticeHours, Integer maxDaysAhead, Integer slotIntervalMinutes, String color, LocationKind locationKind, String locationDetail, AssignmentMode mode, Set<UUID> hostIds)`, `reconstitute(...)` com todos os campos, `update(...)` com os mesmos parâmetros de create menos companyId, `deactivate()`, getters. Regras: nome obrigatório ≤120; slug `^[a-z0-9]+(-[a-z0-9]+)*$` ≤80 (se null, gerado do nome: minúsculas, sem acento via `Normalizer`, espaços→`-`); duração 5–480; buffers 0–240; intervalo ∈ {5,10,15,20,30,60}; minNotice 0–720; maxDaysAhead 1–365; cor `^#[0-9A-Fa-f]{6}$` ou null; ao menos 1 host.
  - Exceções: `SchedulingValidationException(String)`, `SchedulingNotFoundException(String recurso, UUID id)` → mensagem `"<recurso> não encontrado: <id>"`, `SlotUnavailableException()` → `"Este horário não está mais disponível."`.

- [ ] **Step 1: Testes que falham**

```java
package com.becommerce.crm.sales.scheduling.domain;

import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingValidationException;
import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AppointmentTypeTest {
    private final UUID company = UUID.randomUUID();
    private final UUID host = UUID.randomUUID();

    private AppointmentType create(String name, String slug, int duration, int interval) {
        return AppointmentType.create(company, name, slug, duration, 0, 0, 2, 60, interval, null,
                LocationKind.GOOGLE_MEET, null, AssignmentMode.ROUND_ROBIN, Set.of(host));
    }

    @Test void generatesSlugFromNameWithoutAccents() {
        assertEquals("reuniao-de-apresentacao", create("Reunião de Apresentação", null, 30, 30).getSlug());
    }
    @Test void rejectsInvalidDuration() {
        assertThrows(SchedulingValidationException.class, () -> create("X", null, 4, 30));
        assertThrows(SchedulingValidationException.class, () -> create("X", null, 481, 30));
    }
    @Test void rejectsInvalidInterval() {
        assertThrows(SchedulingValidationException.class, () -> create("X", null, 30, 7));
    }
    @Test void rejectsInvalidSlug() {
        assertThrows(SchedulingValidationException.class, () -> create("X", "Com Espaço", 30, 30));
    }
    @Test void requiresAtLeastOneHost() {
        assertThrows(SchedulingValidationException.class, () -> AppointmentType.create(company, "X", null, 30, 0, 0, 2, 60, 30,
                null, LocationKind.PHONE, null, AssignmentMode.ROUND_ROBIN, Set.of()));
    }
}
```

```java
package com.becommerce.crm.sales.scheduling.domain;

import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class WeeklyAvailabilityTest {
    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);

    private WeeklyAvailability twoWindowsMonday(List<AvailabilityOverride> overrides) {
        return new WeeklyAvailability(UUID.randomUUID(), SP, List.of(
                new AvailabilityWindow(DayOfWeek.MONDAY, new TimeRange(LocalTime.of(9, 0), LocalTime.of(12, 0))),
                new AvailabilityWindow(DayOfWeek.MONDAY, new TimeRange(LocalTime.of(13, 0), LocalTime.of(18, 0)))),
                overrides);
    }

    @Test void convertsWindowsToInstantsInUserZone() {
        List<Interval> result = twoWindowsMonday(List.of()).intervalsOn(MONDAY);
        assertEquals(2, result.size());
        assertEquals(Instant.parse("2026-10-05T12:00:00Z"), result.get(0).start()); // 09:00 -03:00
        assertEquals(Instant.parse("2026-10-05T21:00:00Z"), result.get(1).end());
    }
    @Test void emptyOnDayWithoutWindows() {
        assertTrue(twoWindowsMonday(List.of()).intervalsOn(MONDAY.plusDays(1)).isEmpty());
    }
    @Test void overrideReplacesDay() {
        var off = new AvailabilityOverride(MONDAY, List.of());
        assertTrue(twoWindowsMonday(List.of(off)).intervalsOn(MONDAY).isEmpty());
    }
    @Test void rejectsOverlappingWindowsSameDay() {
        assertThrows(RuntimeException.class, () -> new WeeklyAvailability(UUID.randomUUID(), SP, List.of(
                new AvailabilityWindow(DayOfWeek.MONDAY, new TimeRange(LocalTime.of(9, 0), LocalTime.of(12, 0))),
                new AvailabilityWindow(DayOfWeek.MONDAY, new TimeRange(LocalTime.of(11, 0), LocalTime.of(14, 0)))),
                List.of()));
    }
    @Test void defaultIsWeekdaysNineToSix() {
        var d = WeeklyAvailability.defaultFor(UUID.randomUUID());
        assertEquals(1, d.intervalsOn(MONDAY).size());
        assertTrue(d.intervalsOn(LocalDate.of(2026, 10, 4)).isEmpty()); // domingo
    }
}
```

- [ ] **Step 2:** `cd backend && ./mvnw -q test -Dtest='AppointmentTypeTest,WeeklyAvailabilityTest'` → falha de compilação.
- [ ] **Step 3:** Implementar as classes conforme **Interfaces** (estilo de `Task.java`: construtor privado, `create`/`reconstitute`, validações lançando `SchedulingValidationException`, javadoc curto em PT-BR). `intervalsOn`: `date.atTime(range.start()).atZone(zone).toInstant()`.
- [ ] **Step 4:** Rodar de novo → PASS.
- [ ] **Step 5: Commit** `feat(scheduling): appointment type and weekly availability domain`

---

### Task 3: Domínio — Appointment, ScheduleBlock

**Files:**
- Create: `domain/Appointment.java`, `AppointmentStatus.java`, `AppointmentSource.java`, `ScheduleBlock.java`, `BlockSource.java`
- Test: `backend/src/test/java/com/becommerce/crm/sales/scheduling/domain/AppointmentTest.java`

**Interfaces:**
- Produces:
  - `Appointment.create(UUID companyId, UUID appointmentTypeId, UUID hostId, UUID contactId, UUID opportunityId, String title, Instant startAt, Instant endAt, AppointmentSource source, LocationKind locationKind, String locationDetail, String notes, UUID createdBy)` — status `SCHEDULED`, `publicToken` = 32 bytes `SecureRandom` em Base64 URL sem padding; título obrigatório ≤200; `endAt > startAt`; duração ≤ 24h.
  - `reconstitute(...)` com todos os campos da tabela.
  - `reschedule(Instant start, Instant end)` — proibido se CANCELED/COMPLETED/NO_SHOW.
  - `updateDetails(UUID contactId, UUID opportunityId, String title, LocationKind kind, String locationDetail, String notes)`.
  - `confirm()` (de SCHEDULED), `cancel(String reason)` (de SCHEDULED/CONFIRMED), `markCompleted(Instant now)` e `markNoShow(Instant now)` (exigem `startAt <= now` e status SCHEDULED/CONFIRMED).
  - `Interval interval()`; getters para todos os campos.
  - `ScheduleBlock.create(UUID companyId, UUID hostId, Instant start, Instant end, String reason, UUID createdBy)` (source INTERNAL), `reconstitute(...)`, `Interval interval()`, `boolean isEditable()` (= source INTERNAL).

- [ ] **Step 1: Teste que falha**

```java
package com.becommerce.crm.sales.scheduling.domain;

import com.becommerce.crm.sales.scheduling.domain.exception.SchedulingValidationException;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AppointmentTest {
    private static final Instant T0 = Instant.parse("2026-10-05T13:00:00Z");

    private Appointment newAppt() {
        return Appointment.create(UUID.randomUUID(), null, UUID.randomUUID(), null, null, "Demo",
                T0, T0.plusSeconds(1800), AppointmentSource.INTERNAL, LocationKind.PHONE, null, null, null);
    }

    @Test void createsScheduledWithToken() {
        var a = newAppt();
        assertEquals(AppointmentStatus.SCHEDULED, a.getStatus());
        assertTrue(a.getPublicToken().length() >= 40);
    }
    @Test void rejectsEndBeforeStart() {
        assertThrows(SchedulingValidationException.class, () -> Appointment.create(UUID.randomUUID(), null,
                UUID.randomUUID(), null, null, "x", T0, T0, AppointmentSource.INTERNAL, null, null, null, null));
    }
    @Test void cannotRescheduleCanceled() {
        var a = newAppt();
        a.cancel("cliente pediu");
        assertThrows(SchedulingValidationException.class, () -> a.reschedule(T0.plusSeconds(3600), T0.plusSeconds(5400)));
    }
    @Test void noShowOnlyAfterStart() {
        var a = newAppt();
        assertThrows(SchedulingValidationException.class, () -> a.markNoShow(T0.minusSeconds(60)));
        a.markNoShow(T0.plusSeconds(60));
        assertEquals(AppointmentStatus.NO_SHOW, a.getStatus());
    }
    @Test void confirmThenCancel() {
        var a = newAppt();
        a.confirm();
        a.cancel(null);
        assertEquals(AppointmentStatus.CANCELED, a.getStatus());
    }
}
```

- [ ] **Step 2:** `./mvnw -q test -Dtest=AppointmentTest` → falha.
- [ ] **Step 3:** Implementar conforme **Interfaces**.
- [ ] **Step 4:** PASS.
- [ ] **Step 5: Commit** `feat(scheduling): appointment and schedule block domain`

---

### Task 4: SlotCalculator (núcleo de horários livres)

**Files:**
- Create: `domain/SlotCalculator.java`
- Test: `backend/src/test/java/com/becommerce/crm/sales/scheduling/domain/SlotCalculatorTest.java`

**Interfaces:**
- Consumes: `AppointmentType`, `WeeklyAvailability`, `Interval`.
- Produces:
```java
public final class SlotCalculator {
    /** Horários de início livres de um host em [from, to). busy = agendamentos não cancelados + bloqueios (sem buffer). */
    public static List<Instant> freeStarts(AppointmentType type, WeeklyAvailability availability,
                                           List<Interval> busy, Instant from, Instant to, Instant now);
}
```
Algoritmo: para cada `LocalDate` de `from` até `to` no fuso do host → para cada intervalo de `availability.intervalsOn(date)` → candidatos começando no início da janela, passo `slotIntervalMinutes`, enquanto `start + duration <= fimDaJanela`. Descartar se: `start < now + minNoticeHours`; `start > now + maxDaysAhead dias`; fora de `[from,to)`; `Interval(start - bufferBefore, start + duration + bufferAfter)` sobrepõe algum `busy`. Resultado ordenado e sem duplicatas.

- [ ] **Step 1: Testes que falham**

```java
package com.becommerce.crm.sales.scheduling.domain;

import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SlotCalculatorTest {
    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private static final UUID HOST = UUID.randomUUID();
    // segunda 05/10/2026, janela 09:00–11:00 local (12:00–14:00Z)
    private static final WeeklyAvailability AVAIL = new WeeklyAvailability(HOST, SP, List.of(
            new AvailabilityWindow(DayOfWeek.MONDAY, new TimeRange(LocalTime.of(9, 0), LocalTime.of(11, 0)))), List.of());
    private static final Instant FROM = Instant.parse("2026-10-05T03:00:00Z");
    private static final Instant TO = Instant.parse("2026-10-06T03:00:00Z");
    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");

    private AppointmentType type(int duration, int bufferAfter, int interval, int minNoticeHours) {
        return AppointmentType.create(UUID.randomUUID(), "Demo", null, duration, 0, bufferAfter, minNoticeHours, 60,
                interval, null, LocationKind.PHONE, null, AssignmentMode.ROUND_ROBIN, Set.of(HOST));
    }

    @Test void slicesWindowByInterval() {
        var slots = SlotCalculator.freeStarts(type(30, 0, 30, 0), AVAIL, List.of(), FROM, TO, NOW);
        assertEquals(List.of(Instant.parse("2026-10-05T12:00:00Z"), Instant.parse("2026-10-05T12:30:00Z"),
                Instant.parse("2026-10-05T13:00:00Z"), Instant.parse("2026-10-05T13:30:00Z")), slots);
    }
    @Test void slotMustFitInsideWindow() {
        var slots = SlotCalculator.freeStarts(type(60, 0, 30, 0), AVAIL, List.of(), FROM, TO, NOW);
        assertEquals(3, slots.size()); // 09:00, 09:30, 10:00
    }
    @Test void removesBusyIntervals() {
        var busy = List.of(new Interval(Instant.parse("2026-10-05T12:30:00Z"), Instant.parse("2026-10-05T13:00:00Z")));
        var slots = SlotCalculator.freeStarts(type(30, 0, 30, 0), AVAIL, busy, FROM, TO, NOW);
        assertFalse(slots.contains(Instant.parse("2026-10-05T12:30:00Z")));
        assertEquals(3, slots.size());
    }
    @Test void bufferAfterBlocksPreviousSlot() {
        var busy = List.of(new Interval(Instant.parse("2026-10-05T12:30:00Z"), Instant.parse("2026-10-05T13:00:00Z")));
        var slots = SlotCalculator.freeStarts(type(30, 15, 30, 0), AVAIL, busy, FROM, TO, NOW);
        assertFalse(slots.contains(Instant.parse("2026-10-05T12:00:00Z")));
    }
    @Test void respectsMinimumNotice() {
        var now = Instant.parse("2026-10-05T11:00:00Z"); // 08:00 local
        var slots = SlotCalculator.freeStarts(type(30, 0, 30, 2), AVAIL, List.of(), FROM, TO, now);
        assertEquals(List.of(Instant.parse("2026-10-05T13:00:00Z"), Instant.parse("2026-10-05T13:30:00Z")), slots);
    }
    @Test void respectsMaxDaysAhead() {
        var farNow = Instant.parse("2026-06-01T12:00:00Z"); // > 60 dias antes
        assertTrue(SlotCalculator.freeStarts(type(30, 0, 30, 0), AVAIL, List.of(), FROM, TO, farNow).isEmpty());
    }
}
```

- [ ] **Step 2:** `./mvnw -q test -Dtest=SlotCalculatorTest` → falha.
- [ ] **Step 3:** Implementar conforme algoritmo.
- [ ] **Step 4:** PASS.
- [ ] **Step 5: Commit** `feat(scheduling): pure slot calculator`

---

### Task 5: Persistência (JPA) + teste de integração de isolamento e sobreposição

**Files:**
- Create em `infrastructure/persistence/`: `AppointmentTypeJpaEntity/JpaRepository/RepositoryImpl`, `AvailabilityRuleJpaEntity`, `AvailabilityOverrideJpaEntity`, `UserSchedulingSettingsJpaEntity`, respectivos `JpaRepository`, `AvailabilityRepositoryImpl`, `AppointmentJpaEntity/JpaRepository/RepositoryImpl`, `ScheduleBlockJpaEntity/JpaRepository/RepositoryImpl`
- Create em `application/port/out/`: as 4 interfaces
- Test: `backend/src/test/java/com/becommerce/crm/sales/scheduling/infrastructure/persistence/SchedulingIsolationIT.java`, `backend/src/test/resources/scheduling-rls-bootstrap.sql`

**Interfaces:**
- Produces (ports):
```java
public interface AppointmentTypeRepository {
    AppointmentType save(AppointmentType t);
    Optional<AppointmentType> findById(UUID id);
    List<AppointmentType> findByCompanyId(UUID companyId);
    boolean existsByCompanyIdAndSlugAndIdNot(UUID companyId, String slug, UUID excludeId);
}
public interface AvailabilityRepository {
    WeeklyAvailability findByUser(UUID companyId, UUID userId); // sem regras salvas → WeeklyAvailability.defaultFor(userId)
    void replace(UUID companyId, WeeklyAvailability availability); // apaga e regrava regras/overrides/settings
}
public interface AppointmentRepository {
    Appointment save(Appointment a);   // lança SlotUnavailableException se violar ex_appointments_host_no_overlap
    Optional<Appointment> findById(UUID id);
    List<Appointment> findInRange(UUID companyId, Collection<UUID> hostIds, Instant from, Instant to); // inclui CANCELED; ordenado por start_at
    List<Interval> findBusy(UUID companyId, UUID hostId, Instant from, Instant to, UUID excludeAppointmentId); // status <> CANCELED
    long countActiveForHost(UUID companyId, UUID hostId, Instant from, Instant to);
    void lockHost(UUID hostId); // SELECT pg_advisory_xact_lock(hashtextextended(:hostId, 0))
    void delete(Appointment a);
}
public interface ScheduleBlockRepository {
    ScheduleBlock save(ScheduleBlock b);
    Optional<ScheduleBlock> findById(UUID id);
    List<ScheduleBlock> findInRange(UUID companyId, Collection<UUID> hostIds, Instant from, Instant to);
    void delete(ScheduleBlock b);
}
```
- Mapeamento de colunas: snake_case ↔ camelCase como em `TaskJpaEntity`; `Instant` para TIMESTAMPTZ; `appointment_type_hosts` via `@ElementCollection @CollectionTable(name="appointment_type_hosts", joinColumns=@JoinColumn(name="appointment_type_id"))` com `@Column(name="user_id")` — o `company_id` dessa tabela precisa ser gravado: use uma entidade `AppointmentTypeHostJpaEntity` com `@IdClass` (typeId,userId) e `companyId`, gerenciada no `AppointmentTypeRepositoryImpl.save` (apaga e regrava).
- Intervalo: query `where host_id in :hosts and start_at < :to and end_at > :from`.
- Overlap: em `AppointmentRepositoryImpl.save`, use `jpaRepository.saveAndFlush` e capture `DataIntegrityViolationException`; se a causa raiz (`PSQLException`) tiver `getSQLState() == "23P01"`, lance `SlotUnavailableException`.

- [ ] **Step 1: Escrever `SchedulingIsolationIT`** seguindo exatamente o setup de `FollowUpIsolationIT` (Testcontainers `postgres:17-alpine`, role `crm_app_user NOSUPERUSER NOBYPASSRLS`, `TenantAwareDataSource`). O bootstrap `scheduling-rls-bootstrap.sql` deve criar: schema `app` + função `app.current_tenant_id()` (copiar de `followup-rls-bootstrap.sql`), tabelas mínimas `companies`, `users(id, company_id)`, `contacts(id, company_id)`, `opportunities(id, company_id)`, `permissions`, `roles`, `role_permissions`, e então o conteúdo de `V079__scheduling.sql` a partir de `CREATE EXTENSION`. Testes:
  1. `tenantBCannotSeeTenantAAppointments` — insere agendamento em A; com contexto B, `SELECT count(*) FROM appointments` = 0.
  2. `crossTenantInsertIsRejected` — contexto B inserindo `company_id = A` → `SQLException`.
  3. `overlappingAppointmentsForSameHostAreRejected` — dois inserts 10:00–10:30 e 10:15–10:45 para o mesmo host → segundo lança `SQLException` com SQLState `23P01`.
  4. `canceledAppointmentDoesNotBlock` — primeiro com `status='CANCELED'`, segundo no mesmo horário → OK.
  5. `adjacentAppointmentsAreAllowed` — 10:00–10:30 e 10:30–11:00 → OK.
- [ ] **Step 2:** `./mvnw -q verify -Dit.test=SchedulingIsolationIT` (ou `-Dtest=SchedulingIsolationIT` conforme o surefire/failsafe do pom — conferir como `FollowUpIsolationIT` é executado) → deve passar só se a migration estiver correta; corrigir a migration se não.
- [ ] **Step 3:** Implementar ports + JPA (entidades, repositórios Spring Data, impls com `toEntity`/`toDomain` estáticos no padrão `TaskRepositoryImpl`). `lockHost` via `@Query(value = "SELECT pg_advisory_xact_lock(hashtextextended(CAST(:hostId AS text), 0))", nativeQuery = true)` retornando `Object` em `AppointmentJpaRepository`.
- [ ] **Step 4:** `./mvnw -q compile` e IT de novo → PASS.
- [ ] **Step 5: Commit** `feat(scheduling): persistence adapters and RLS/overlap integration test`

---

### Task 6: Serviços de aplicação

**Files:**
- Create: `application/dto/*.java`, `application/port/in/*UseCase.java`, `application/service/AppointmentTypeService.java`, `AvailabilityService.java`, `SlotService.java`, `AppointmentService.java`, `BlockService.java`
- Test: `backend/src/test/java/com/becommerce/crm/sales/scheduling/application/service/AppointmentServiceTest.java`, `SlotServiceTest.java`, `AppointmentTypeServiceTest.java`

**Interfaces:**
- Consumes: ports da Task 5; `ContactRepository`, `OpportunityRepository` (validação igual `TaskService.validateOwnedLinks`); `TenantAuditRecorder`; `Clock` (bean — se não existir, crie `@Bean Clock clock() { return Clock.systemUTC(); }` em `sales/scheduling/infrastructure/SchedulingConfig.java`).
- DTOs (records):
```java
record AppointmentTypeRequest(@NotBlank @Size(max=120) String name, @Size(max=80) String slug, String description,
    @NotNull Integer durationMinutes, Integer bufferBeforeMinutes, Integer bufferAfterMinutes, Integer minNoticeHours,
    Integer maxDaysAhead, Integer slotIntervalMinutes, String color, @NotNull LocationKind locationKind,
    String locationDetail, @NotNull AssignmentMode assignmentMode, @NotEmpty Set<UUID> hostIds) {}
record AppointmentTypeResponse(UUID id, String name, String slug, String description, int durationMinutes,
    int bufferBeforeMinutes, int bufferAfterMinutes, int minNoticeHours, int maxDaysAhead, int slotIntervalMinutes,
    String color, LocationKind locationKind, String locationDetail, AssignmentMode assignmentMode,
    Set<UUID> hostIds, boolean publicBookingEnabled, boolean active) {}
record AvailabilityDto(@NotBlank String timezone, @NotNull List<WindowDto> windows, @NotNull List<OverrideDto> overrides) {
    record WindowDto(@Min(1) @Max(7) int weekday, @NotNull LocalTime start, @NotNull LocalTime end) {}
    record OverrideDto(@NotNull LocalDate date, LocalTime start, LocalTime end) {} // start/end null = indisponível
}
record CreateAppointmentRequest(UUID appointmentTypeId, @NotNull UUID hostId, UUID contactId, UUID opportunityId,
    @Size(max=200) String title, @NotNull Instant startAt, Instant endAt, LocationKind locationKind,
    String locationDetail, String notes, boolean force) {} // endAt null → startAt + duração do tipo (obrigatório se sem tipo)
record UpdateAppointmentRequest(UUID contactId, UUID opportunityId, @NotBlank @Size(max=200) String title,
    LocationKind locationKind, String locationDetail, String notes) {}
record RescheduleRequest(@NotNull Instant startAt, @NotNull Instant endAt, UUID hostId, boolean force) {}
record CancelRequest(@Size(max=500) String reason) {}
record AppointmentResponse(UUID id, UUID appointmentTypeId, UUID hostId, UUID contactId, String contactName,
    UUID opportunityId, String title, Instant startAt, Instant endAt, AppointmentStatus status,
    AppointmentSource source, LocationKind locationKind, String locationDetail, String meetingUrl,
    String notes, String cancelReason, List<String> warnings) {}
record BlockRequest(@NotNull UUID hostId, @NotNull Instant startAt, @NotNull Instant endAt, @Size(max=200) String reason) {}
record BlockResponse(UUID id, UUID hostId, Instant startAt, Instant endAt, String reason, BlockSource source, boolean editable) {}
record SlotResponse(Instant startAt, Instant endAt, List<UUID> availableHostIds) {}
record CalendarResponse(List<AppointmentResponse> appointments, List<BlockResponse> blocks,
    Map<UUID, List<IntervalDto>> availability) { record IntervalDto(Instant start, Instant end) {} }
```
- Use cases:
```java
interface AppointmentTypeUseCase { AppointmentTypeResponse create(UUID companyId, AppointmentTypeRequest r);
    AppointmentTypeResponse update(UUID companyId, UUID id, AppointmentTypeRequest r);
    List<AppointmentTypeResponse> list(UUID companyId); void deactivate(UUID companyId, UUID id); }
interface AvailabilityUseCase { AvailabilityDto get(UUID companyId, UUID userId);
    AvailabilityDto replace(UUID companyId, UUID userId, AvailabilityDto dto); }
interface AppointmentUseCase {
    CalendarResponse calendar(UUID companyId, Instant from, Instant to, Set<UUID> hostIds);
    List<SlotResponse> slots(UUID companyId, UUID appointmentTypeId, Instant from, Instant to);
    AppointmentResponse create(UUID companyId, CreateAppointmentRequest r, UUID createdBy);
    AppointmentResponse update(UUID companyId, UUID id, UpdateAppointmentRequest r);
    AppointmentResponse reschedule(UUID companyId, UUID id, RescheduleRequest r);
    AppointmentResponse changeStatus(UUID companyId, UUID id, AppointmentStatus status, CancelRequest cancel);
    void delete(UUID companyId, UUID id); }
interface BlockUseCase { BlockResponse create(UUID companyId, BlockRequest r, UUID createdBy);
    void delete(UUID companyId, UUID id); }
```
- Regras de `AppointmentService.create`/`reschedule` (em `@Transactional`):
  1. `TenantContext.setCompanyId`; validar contato/oportunidade da empresa; host deve ser host do tipo (se tipo informado).
  2. `appointmentRepository.lockHost(hostId)`.
  3. `busy = findBusy(...excluindo o próprio) + blocos do host no intervalo`; se algum sobrepõe `[start,end)` → `SlotUnavailableException`.
  4. Se `!force` e o horário não estiver contido na disponibilidade do host → `SchedulingValidationException("Horário fora da disponibilidade do responsável.")`. Com `force=true` aceita e devolve `warnings=["OUTSIDE_AVAILABILITY"]`.
  5. Título default: nome do tipo + " - " + nome do contato (ou só o nome do tipo).
  6. Salvar (o save ainda traduz 23P01 → 409); auditar `AuditModule.SCHEDULING`.
- `slots`: hosts do tipo; para cada host `SlotCalculator.freeStarts(type, availability, busy(appts+blocks), from, to, clock.instant())`; agrega por `startAt` com a lista de hosts livres. Intervalo máximo 62 dias, senão 400.
- `calendar`: valida intervalo ≤ 62 dias; hostIds vazio → todos os hosts ativos da empresa que têm agendamentos/bloqueios no período + o usuário atual (o controller passa o usuário atual quando a lista vier vazia); `availability` = `intervalsOn` de cada dia por host (para sombrear horas fora do expediente).
- `AvailabilityService.replace`: usuário só edita a própria, exceto quem tem `scheduling:configure` (checado no controller).
- `AppointmentTypeService`: slug único por empresa (`existsByCompanyIdAndSlugAndIdNot`) senão 400 "Já existe um tipo de agendamento com este endereço."; hosts devem ser usuários da empresa (use `UserRepository` existente do `identity` — `grep -rn "interface UserRepository" backend/src/main/java`).

- [ ] **Step 1: Testes que falham (Mockito, padrão `TaskServiceTest`)** — em `AppointmentServiceTest`:
  - `createRejectsWhenHostBusy` — `findBusy` retorna intervalo sobreposto → `SlotUnavailableException`; `save` nunca chamado.
  - `createRejectsOutsideAvailabilityWithoutForce` — availability seg 09–18; start sábado → `SchedulingValidationException`.
  - `createWithForceOutsideAvailabilityReturnsWarning` — `warnings` contém `"OUTSIDE_AVAILABILITY"`.
  - `createDefaultsEndFromTypeDuration` — tipo 45 min, endAt null → endAt = start + 45min.
  - `createRejectsContactFromOtherCompany` → `ContactNotFoundException`.
  - `rescheduleIgnoresOwnInterval` — `findBusy` chamado com `excludeAppointmentId = id`.
  - `locksHostBeforeCheckingConflicts` — `InOrder`: `lockHost` antes de `findBusy`.
  Em `SlotServiceTest`: `aggregatesHostsPerStart` (dois hosts, um ocupado às 10:00 → slot 10:00 só com o outro) e `rejectsRangeOver62Days`.
  Em `AppointmentTypeServiceTest`: `rejectsDuplicateSlug`.
  Escreva cada teste com dados concretos (UUIDs `randomUUID`, instantes `Instant.parse("2026-10-05T13:00:00Z")`, `Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneOffset.UTC)`).
- [ ] **Step 2:** `./mvnw -q test -Dtest='AppointmentServiceTest,SlotServiceTest,AppointmentTypeServiceTest'` → falha.
- [ ] **Step 3:** Implementar DTOs, use cases e serviços.
- [ ] **Step 4:** PASS.
- [ ] **Step 5: Commit** `feat(scheduling): application services for types, availability, appointments and blocks`

---

### Task 7: Controllers REST + tratamento de erros

**Files:**
- Create: `web/AppointmentTypeController.java`, `AvailabilityController.java`, `AppointmentController.java`, `BlockController.java`
- Modify: `shared/web/handler/GlobalExceptionHandler.java` (3 handlers no padrão dos de Task: 404, 400, 409 com `"error": "Conflict"`)
- Test: `backend/src/test/java/com/becommerce/crm/sales/scheduling/web/AppointmentControllerTest.java` (`@WebMvcTest` — copie a configuração de segurança usada pelo teste de controller mais próximo: `grep -rln "@WebMvcTest" backend/src/test | head -3`)

**Interfaces (rotas, todas com `@CurrentCompanyId` + `@PreAuthorize`):**

| Método | Rota (`/api/v1/companies/{companyId}` +) | Permissão |
|---|---|---|
| GET | `/appointment-types` | `appointment:read` |
| POST/PUT/DELETE | `/appointment-types[/{id}]` | `scheduling:configure` |
| GET | `/appointment-types/{id}/slots?from&to` | `appointment:read` |
| GET/PUT | `/users/{userId}/availability` | `appointment:read` / `appointment:update` (+ no PUT: se `userId != principal.userId()` exigir `scheduling:configure` via `principal`/`SecurityContext`, senão 403) |
| GET | `/calendar?from&to&hostIds=a,b` | `appointment:read` |
| POST | `/appointments` | `appointment:create` → 201 |
| PUT | `/appointments/{id}` | `appointment:update` |
| PATCH | `/appointments/{id}/reschedule` | `appointment:update` |
| POST | `/appointments/{id}/status/{status}` (body opcional `CancelRequest`) | `appointment:update` |
| DELETE | `/appointments/{id}` | `appointment:delete` → 204 |
| POST/DELETE | `/blocks[/{id}]` | `appointment:create` / `appointment:delete` |

- [ ] **Step 1: Testes que falham:** `calendarRequiresReadPermission` (403 sem authority), `createReturns201`, `slotUnavailableMapsTo409` (use case mockado lança `SlotUnavailableException` → 409 e `message` = "Este horário não está mais disponível."), `rangeValidationMapsTo400`.
- [ ] **Step 2:** rodar → falha. **Step 3:** implementar. **Step 4:** `./mvnw -q test` (suíte inteira do backend) → PASS.
- [ ] **Step 5: Commit** `feat(scheduling): REST endpoints and error mapping`

---

### Task 8: Frontend — tipos, service, hooks, schema

**Files:**
- Create: `frontend-refine/src/features/sales/scheduling/types/scheduling.types.ts`, `services/scheduling.service.ts`, `hooks/useScheduling.ts`, `schemas/appointment.schema.ts`, `schemas/appointment.schema.test.ts`

**Interfaces:**
- Produces: tipos TS espelhando os DTOs da Task 6 (datas como `string` ISO); `SchedulingService` com `calendar(companyId, {from,to,hostIds})`, `slots(companyId, typeId, from, to)`, `listTypes`, `createType`, `updateType`, `deactivateType`, `getAvailability(companyId,userId)`, `saveAvailability`, `createAppointment`, `updateAppointment`, `reschedule`, `changeStatus(companyId,id,status,reason?)`, `deleteAppointment`, `createBlock`, `deleteBlock` (padrão `TaskService`, base `/companies`).
- Hooks: `useCalendar(companyId, range, hostIds)` (queryKey `["calendar", companyId, from, to, hostIds.join(",")]`), `useAppointmentTypes`, `useSlots`, `useAvailability`, mutações com `useMutationDefaults` invalidando `["calendar", companyId]`; `useReschedule` com update otimista (`onMutate` move o evento no cache; `onError` restaura) ; `useSchedulingPermissions()` com `canCreate/canUpdate/canDelete/canConfigure`.
- `appointmentFormSchema` (zod): `appointmentTypeId` uuid opcional, `hostId` uuid obrigatório ("Selecione o responsável"), `contactId`/`opportunityId` opcionais, `title` ≤200, `date` (yyyy-MM-dd) obrigatório, `startTime`/`endTime` HH:mm, refine `endTime > startTime` ("O fim deve ser depois do início"), `locationKind`, `locationDetail`, `notes` ≤2000. Função `toCreateRequest(values, timeZone)` que monta `startAt/endAt` ISO a partir de date + hora no fuso do navegador.

- [ ] **Step 1: Teste do schema que falha** (`vitest`): aceita formulário válido; rejeita `endTime <= startTime`; rejeita sem `hostId`; `toCreateRequest` com `date="2026-10-05", startTime="09:00", endTime="09:30"` e TZ `America/Sao_Paulo` → `startAt === "2026-10-05T12:00:00.000Z"`.
- [ ] **Step 2:** `cd frontend-refine && npx vitest run src/features/sales/scheduling` → falha.
- [ ] **Step 3:** implementar (converter fuso com `Date` + `Intl` — sem nova lib: calcule o offset com `new Intl.DateTimeFormat("en-US",{timeZone,...}).formatToParts`).
- [ ] **Step 4:** PASS + `npm run typecheck`.
- [ ] **Step 5: Commit** `feat(scheduling-ui): types, service, hooks and appointment form schema`

---

### Task 9: Frontend — lógica do calendário (pura)

**Files:**
- Create: `features/sales/scheduling/lib/calendar-range.ts`, `calendar-range.test.ts`, `calendar-layout.ts`, `calendar-layout.test.ts`

**Interfaces:**
```ts
export type CalendarView = "day" | "week" | "month";
export function getRange(view: CalendarView, anchor: Date): { from: Date; to: Date; days: Date[] };
// week: segunda a domingo (weekStartsOn: 1); month: semanas completas que cobrem o mês (grid 6x7 ou 5x7)
export function shift(view: CalendarView, anchor: Date, dir: -1 | 1): Date;
export function formatRangeTitle(view: CalendarView, anchor: Date): string; // pt-BR: "28 set – 4 out 2026", "outubro 2026", "sexta, 2 de outubro"

export type LayoutInput = { id: string; start: Date; end: Date };
export type LayoutItem = LayoutInput & { top: number; height: number; column: number; columns: number };
/** Posiciona eventos de UM dia. top/height em % do dia [dayStartHour, dayEndHour). Sobrepostos dividem colunas. */
export function layoutDay(events: LayoutInput[], day: Date, dayStartHour = 0, dayEndHour = 24): LayoutItem[];
```

- [ ] **Step 1: Testes que falham:**
  - `getRange("week", 2026-10-02)` → from = seg 28/09 00:00, to = seg 05/10 00:00, 7 dias.
  - `getRange("month", 2026-10-15)` → começa em 28/09 e termina em 01/11 (5 semanas).
  - `shift("week", d, 1)` soma 7 dias; `shift("month")` soma 1 mês.
  - `layoutDay`: evento 09:00–10:00 → `top = 37.5`, `height ≈ 4.1667`; dois sobrepostos (9–10 e 9:30–10:30) → colunas 0 e 1, `columns = 2`; um terceiro às 11:00 → `column 0, columns 1`; evento que atravessa meia-noite é cortado no fim do dia.
- [ ] **Step 2:** falha. **Step 3:** implementar com `date-fns` (`startOfWeek`, `addDays`, `startOfMonth`, `endOfMonth`, `format` com `ptBR`). Sobreposição: ordenar por início, agrupar clusters conectados, atribuir a menor coluna livre, `columns` = máx. do cluster.
- [ ] **Step 4:** PASS.
- [ ] **Step 5: Commit** `feat(scheduling-ui): calendar range and day layout utilities`

---

### Task 10: Frontend — página /agenda

**Files:**
- Create: `components/AgendaToolbar.tsx`, `AgendaSidebar.tsx`, `MiniCalendar.tsx`, `TimeGridView.tsx`, `MonthView.tsx`, `AppointmentDialog.tsx`, `BlockDialog.tsx`, `AppointmentPopover.tsx`; `src/app/(dashboard)/agenda/page.tsx`
- Modify: `src/lib/constants.ts` (`AGENDA: "/agenda"`, `SETTINGS_AGENDA: "/settings/agenda"`), `src/components/layout/navigation.ts` (item "Agenda", ícone `CalendarDays` do lucide, `permission: "appointment:page:view"`, logo acima de "Tarefas")

**Comportamento (referência Capim):**
- `page.tsx`: lê `view`, `date`, `hosts` da URL (`useSearchParams`, default `week`, hoje, usuário atual); `getRange` → `useCalendar`; layout com `AgendaSidebar` (largura 260px, recolhível pelo botão ☰ da toolbar) + área principal.
- `AgendaToolbar`: botão "Hoje", ‹ ›, `formatRangeTitle`, `Select` Dia/Semana/Mês, ícone engrenagem → `ROUTES.SETTINGS_AGENDA`.
- `AgendaSidebar`: botão "Novo" (dropdown: Agendamento, Bloqueio de agenda, Copiar link de agendamento — este último desabilitado com tooltip "Disponível em breve"), `MiniCalendar` (mês, clique navega; dias com eventos têm ponto), lista de responsáveis (`useUsers`) com checkbox + "Selecionar todos", cor por responsável (paleta fixa de 8 cores indexada pela ordem).
- `TimeGridView` (dia/semana): coluna de horas 00–24 com linhas a cada hora e meia (altura 48px/hora), rolagem inicial para 08:00; cabeçalho dos dias com dia da semana abreviado e número, hoje em círculo de destaque, sábado/domingo em vermelho; fundo cinza onde não há disponibilidade (dados de `calendar.availability`); blocos hachurados (`repeating-linear-gradient`) com motivo; eventos via `layoutDay` mostrando título truncado e hora de início, borda esquerda na cor do responsável, cancelados com opacidade 50% e risco; linha vermelha do "agora".
  - Clique em espaço vazio → `AppointmentDialog` com data/hora arredondada a 15 min.
  - Arrastar evento (pointer events) em passos de 15 min → ao soltar chama `useReschedule` (otimista); arrastar a borda inferior redimensiona. Só se `canUpdate` e status SCHEDULED/CONFIRMED.
- `MonthView`: grade 7 colunas, até 3 eventos por dia + "+N mais" (clique abre visão dia).
- `AppointmentDialog` (react-hook-form + `appointmentFormSchema`): Tipo (Select de `useAppointmentTypes`; ao escolher preenche duração/local), Contato (combobox com busca em `useContacts` filtrado no cliente; link "Novo contato" abre `CreateContactDialog` existente), Oportunidade (opcional), Responsável, Data, De/Até, "Sugerir horários" (chama `useSlots` do tipo para a data e mostra chips clicáveis), Local, Observações. Em 409 mostra toast "Este horário acabou de ser ocupado" e recarrega slots. Se a resposta trouxer `OUTSIDE_AVAILABILITY`, pergunta "Fora do horário de atendimento. Agendar mesmo assim?" e reenvia com `force: true`.
- `AppointmentPopover`: título, etiqueta do tipo, contato, responsável, horário, local/link, observações; ações editar, excluir (AlertDialog de confirmação), Select de status (Agendado/Confirmado/Cancelado — cancelar pede motivo opcional), "Compareceu?" (Sim → COMPLETED, Não → NO_SHOW; só após o início), links "Abrir contato" (`/contacts/{id}`) e "Abrir oportunidade".
- `BlockDialog`: responsável, data, de/até, motivo.

- [ ] **Step 1:** Implementar componentes e página.
- [ ] **Step 2:** `npm run typecheck && npm run lint && npx vitest run src/features/sales/scheduling` → sem erros.
- [ ] **Step 3: Verificação manual** com a skill `run` (backend dev + `npm run dev`): criar tipo via API/settings, marcar pelo clique na grade, arrastar para remarcar, tentar sobrepor (deve dar toast 409), cancelar, ver visão mês e dia, testar largura de celular (390px: sidebar vira drawer, visão padrão vira "day").
- [ ] **Step 4: Commit** `feat(scheduling-ui): agenda page with day/week/month views`

---

### Task 11: Frontend — Configurações da agenda

**Files:**
- Create: `components/AppointmentTypesManager.tsx`, `components/AvailabilityEditor.tsx`, `src/app/(dashboard)/settings/agenda/page.tsx`
- Modify: navegação de Configurações (encontre onde `SETTINGS_AGENT_CONFIG` é listado: `grep -rn "SETTINGS_AGENT_CONFIG" frontend-refine/src`) adicionando "Agenda".

**Comportamento:**
- Aba "Minha disponibilidade" (`AvailabilityEditor`): seletor de fuso (lista `Intl.supportedValuesOf("timeZone")`, default do usuário), para cada dia seg–dom um switch + uma ou mais faixas De/Até (botão "+ faixa"), "Copiar para todos os dias úteis"; lista de exceções por data (indisponível ou faixa especial). Quem tem `canConfigure` vê um Select para editar a disponibilidade de outro usuário.
- Aba "Tipos de agendamento" (só `canConfigure`): lista em cards (cor, nome, duração, responsáveis, ativo) + dialog de criar/editar com todos os campos do `AppointmentTypeRequest`; slug editável mostrando a prévia do link futuro `/agendar/<empresa>/<slug>`; desativar com confirmação.

- [ ] **Step 1:** Implementar. **Step 2:** `npm run typecheck && npm run lint`. **Step 3:** verificação manual (salvar disponibilidade, conferir que a grade da agenda sombreia fora do expediente e que "Sugerir horários" respeita). **Step 4: Commit** `feat(scheduling-ui): agenda settings for availability and appointment types`

---

### Task 12: Fechamento

- [ ] **Step 1:** `cd backend && ./mvnw -q verify` (unit + IT) → verde.
- [ ] **Step 2:** `cd frontend-refine && npm run typecheck && npm run lint && npm test && npm run format:check` → verde (rodar `npm run format` se necessário e commitar).
- [ ] **Step 3:** Atualizar o doc de contexto do backend (o arquivo alterado em `1a1ea74`: `git show --stat 1a1ea74`) com o novo contexto `sales/scheduling`.
- [ ] **Step 4: Commit** `docs(scheduling): document scheduling bounded context`
- [ ] **Step 5:** Usar `superpowers:finishing-a-development-branch` (PR contra `crm-improvements-deploy-phase`, já que `feature/frontend-refine` foi mergeada ou junto dela — confirmar com `git log origin/crm-improvements-deploy-phase..HEAD --oneline` antes).
