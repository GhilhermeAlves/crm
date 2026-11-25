"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Loader2, Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { ROUTES } from "@/lib/constants";
import { PeopleService } from "../services/people.service";
import {
  useJobTitles,
  usePeopleMutation,
  usePeoplePermissions,
  useTeamUsers,
} from "../hooks/usePeople";
import {
  CONTRACT_LABELS,
  EMPLOYEE_STATUS_LABELS,
  SPECIALTY_LABELS,
  SUPPLIER_TYPE_LABELS,
  formatDate,
} from "../lib/people-labels";
import type {
  ContractType,
  CustomerRequest,
  EmployeeRequest,
  EmployeeStatus,
  PersonDetail,
  SpecialtyCode,
  SpecialtyRequest,
  SupplierRequest,
  SupplierType,
} from "../types/people.types";
import { Field, NativeSelect, Pill, SectionCard } from "./FormBits";

function RemoveRole({
  label,
  onConfirm,
  pending,
}: {
  label: string;
  onConfirm: () => void;
  pending: boolean;
}) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button size="sm" variant="ghost" className="text-destructive" onClick={() => setOpen(true)}>
        Remover papel
      </Button>
      <ConfirmDialog
        open={open}
        onOpenChange={setOpen}
        title={`Remover o papel ${label}?`}
        description="Os dados da pessoa continuam cadastrados. O histórico registra a remoção."
        confirmLabel="Remover"
        variant="destructive"
        isLoading={pending}
        onConfirm={() => {
          onConfirm();
          setOpen(false);
        }}
      />
    </>
  );
}

// =================================================================== cliente / paciente

export function CustomerTab({ person, canEdit }: { person: PersonDetail; canEdit: boolean }) {
  const c = person.customer;
  const [form, setForm] = useState<CustomerRequest>({});
  useEffect(() => {
    setForm({
      creditLimit: c?.creditLimit ?? null,
      blocked: c?.blocked ?? false,
      blockReason: c?.blockReason ?? null,
      customerSince: c?.customerSince ?? null,
      notes: c?.notes ?? null,
      patient: person.healthSegment
        ? {
            recordNumber: c?.patient?.recordNumber ?? null,
            healthPlan: c?.patient?.healthPlan ?? null,
            healthPlanNumber: c?.patient?.healthPlanNumber ?? null,
            referralSource: c?.patient?.referralSource ?? null,
          }
        : null,
    });
  }, [c, person.healthSegment]);
  const save = usePeopleMutation<CustomerRequest>(
    (cid, d) => PeopleService.saveCustomer(cid, person.id, d),
    c ? "Cliente salvo" : "Papel Cliente habilitado",
  );
  const disable = usePeopleMutation<void>(
    (cid) => PeopleService.disableCustomer(cid, person.id),
    "Papel Cliente removido",
  );
  const pt = form.patient;
  const setPt = (patch: Partial<NonNullable<CustomerRequest["patient"]>>) =>
    setForm({
      ...form,
      patient: {
        recordNumber: null,
        healthPlan: null,
        healthPlanNumber: null,
        referralSource: null,
        ...pt,
        ...patch,
      },
    });

  return (
    <SectionCard
      title={person.healthSegment ? "Cliente · Paciente" : "Cliente"}
      description={person.healthSegment ? "O paciente é um cliente com dados clínicos." : undefined}
      actions={
        <>
          {c?.patient && (
            <Button asChild size="sm" variant="outline">
              <Link href={`${ROUTES.PATIENTS}/${person.id}`}>Abrir ficha clínica</Link>
            </Button>
          )}
          {c && canEdit && (
            <RemoveRole
              label="Cliente"
              pending={disable.isPending}
              onConfirm={() => disable.mutate()}
            />
          )}
        </>
      }
    >
      {c && (
        <div className="mb-4 flex flex-wrap gap-4 text-sm text-muted-foreground">
          <span>Código {c.customerCode ?? "—"}</span>
          <span>Cliente desde {formatDate(c.customerSince)}</span>
          {c.blocked && <Pill tone="red">Bloqueado</Pill>}
        </div>
      )}
      <div className="grid gap-4 sm:grid-cols-4">
        {person.healthSegment && (
          <>
            <Field label="Nº do prontuário" htmlFor="cu-rec">
              <Input
                id="cu-rec"
                disabled={!canEdit}
                value={pt?.recordNumber ?? ""}
                maxLength={40}
                onChange={(e) => setPt({ recordNumber: e.target.value })}
              />
            </Field>
            <Field label="Convênio" htmlFor="cu-plan">
              <Input
                id="cu-plan"
                disabled={!canEdit}
                value={pt?.healthPlan ?? ""}
                maxLength={80}
                onChange={(e) => setPt({ healthPlan: e.target.value })}
              />
            </Field>
            <Field label="Nº da carteirinha" htmlFor="cu-plann">
              <Input
                id="cu-plann"
                disabled={!canEdit}
                value={pt?.healthPlanNumber ?? ""}
                maxLength={40}
                onChange={(e) => setPt({ healthPlanNumber: e.target.value })}
              />
            </Field>
            <Field label="Como conheceu" htmlFor="cu-ref">
              <Input
                id="cu-ref"
                disabled={!canEdit}
                value={pt?.referralSource ?? ""}
                maxLength={80}
                onChange={(e) => setPt({ referralSource: e.target.value })}
              />
            </Field>
          </>
        )}
        <Field label="Cliente desde" htmlFor="cu-since">
          <Input
            id="cu-since"
            type="date"
            disabled={!canEdit}
            value={form.customerSince ?? ""}
            onChange={(e) => setForm({ ...form, customerSince: e.target.value || null })}
          />
        </Field>
        <Field label="Limite de crédito (R$)" htmlFor="cu-credit">
          <Input
            id="cu-credit"
            type="number"
            min={0}
            step="0.01"
            disabled={!canEdit}
            value={form.creditLimit ?? ""}
            onChange={(e) =>
              setForm({
                ...form,
                creditLimit: e.target.value === "" ? null : Number(e.target.value),
              })
            }
          />
        </Field>
        <Field label="Situação" htmlFor="cu-blocked">
          <NativeSelect
            id="cu-blocked"
            disabled={!canEdit}
            value={form.blocked ? "1" : "0"}
            onChange={(e) => setForm({ ...form, blocked: e.target.value === "1" })}
          >
            <option value="0">Liberado</option>
            <option value="1">Bloqueado</option>
          </NativeSelect>
        </Field>
        {form.blocked && (
          <Field label="Motivo do bloqueio" htmlFor="cu-reason" required>
            <Input
              id="cu-reason"
              disabled={!canEdit}
              value={form.blockReason ?? ""}
              onChange={(e) => setForm({ ...form, blockReason: e.target.value })}
            />
          </Field>
        )}
        <Field label="Observações comerciais" htmlFor="cu-notes" className="sm:col-span-4">
          <Textarea
            id="cu-notes"
            rows={2}
            disabled={!canEdit}
            value={form.notes ?? ""}
            onChange={(e) => setForm({ ...form, notes: e.target.value })}
          />
        </Field>
      </div>
      {canEdit && (
        <div className="mt-4 flex justify-end">
          <Button disabled={save.isPending} onClick={() => save.mutate(form)}>
            {save.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {c
              ? "Salvar"
              : person.healthSegment
                ? "Habilitar como cliente (paciente)"
                : "Habilitar como cliente"}
          </Button>
        </div>
      )}
    </SectionCard>
  );
}

// =================================================================== fornecedor

export function SupplierTab({ person }: { person: PersonDetail }) {
  const { canSuppliers } = usePeoplePermissions();
  const s = person.supplier;
  const [form, setForm] = useState<SupplierRequest>({});
  useEffect(() => {
    setForm({
      supplierType: s?.supplierType ?? "OTHER",
      paymentTermsDays: s?.paymentTermsDays ?? null,
      blocked: s?.blocked ?? false,
      blockReason: s?.blockReason ?? null,
      notes: s?.notes ?? null,
    });
  }, [s]);
  const save = usePeopleMutation<SupplierRequest>(
    (c, d) => PeopleService.saveSupplier(c, person.id, d),
    s ? "Fornecedor salvo" : "Papel Fornecedor habilitado",
  );
  const disable = usePeopleMutation<void>(
    (c) => PeopleService.disableSupplier(c, person.id),
    "Papel Fornecedor removido",
  );
  return (
    <SectionCard
      title="Fornecedor"
      actions={
        s &&
        canSuppliers && (
          <RemoveRole
            label="Fornecedor"
            pending={disable.isPending}
            onConfirm={() => disable.mutate()}
          />
        )
      }
    >
      {s && <p className="mb-4 text-sm text-muted-foreground">Código {s.supplierCode ?? "—"}</p>}
      <div className="grid gap-4 sm:grid-cols-3">
        <Field label="Tipo" htmlFor="su-type">
          <NativeSelect
            id="su-type"
            disabled={!canSuppliers}
            value={form.supplierType ?? "OTHER"}
            onChange={(e) => setForm({ ...form, supplierType: e.target.value as SupplierType })}
          >
            {Object.entries(SUPPLIER_TYPE_LABELS).map(([k, l]) => (
              <option key={k} value={k}>
                {l}
              </option>
            ))}
          </NativeSelect>
        </Field>
        <Field label="Prazo de pagamento (dias)" htmlFor="su-terms">
          <Input
            id="su-terms"
            type="number"
            min={0}
            disabled={!canSuppliers}
            value={form.paymentTermsDays ?? ""}
            onChange={(e) =>
              setForm({
                ...form,
                paymentTermsDays: e.target.value === "" ? null : Number(e.target.value),
              })
            }
          />
        </Field>
        <Field label="Situação" htmlFor="su-blocked">
          <NativeSelect
            id="su-blocked"
            disabled={!canSuppliers}
            value={form.blocked ? "1" : "0"}
            onChange={(e) => setForm({ ...form, blocked: e.target.value === "1" })}
          >
            <option value="0">Liberado</option>
            <option value="1">Bloqueado</option>
          </NativeSelect>
        </Field>
        {form.blocked && (
          <Field label="Motivo do bloqueio" htmlFor="su-reason" required className="sm:col-span-3">
            <Input
              id="su-reason"
              value={form.blockReason ?? ""}
              onChange={(e) => setForm({ ...form, blockReason: e.target.value })}
            />
          </Field>
        )}
        <Field label="Observações" htmlFor="su-notes" className="sm:col-span-3">
          <Textarea
            id="su-notes"
            rows={2}
            disabled={!canSuppliers}
            value={form.notes ?? ""}
            onChange={(e) => setForm({ ...form, notes: e.target.value })}
          />
        </Field>
      </div>
      {canSuppliers && (
        <div className="mt-4 flex justify-end">
          <Button disabled={save.isPending} onClick={() => save.mutate(form)}>
            {s ? "Salvar" : "Habilitar como fornecedor"}
          </Button>
        </div>
      )}
    </SectionCard>
  );
}

// =================================================================== funcionário

export function EmployeeTab({ person }: { person: PersonDetail }) {
  const { canEmployees, canSalary } = usePeoplePermissions();
  const e = person.employee;
  const { data: titles } = useJobTitles();
  const { data: users } = useTeamUsers(canEmployees);
  const [form, setForm] = useState<EmployeeRequest>({});
  const [spec, setSpec] = useState<SpecialtyRequest>({
    specialty: "DENTAL",
    council: "CRO",
    councilNumber: "",
    councilUf: "",
  });
  useEffect(() => {
    setForm({
      registrationNumber: e?.registrationNumber ?? null,
      jobTitleId: e?.jobTitleId ?? null,
      hireDate: e?.hireDate ?? null,
      terminationDate: e?.terminationDate ?? null,
      contractType: e?.contractType ?? null,
      status: e?.status ?? "ACTIVE",
      salary: e?.salary ?? null,
      userId: e?.userId ?? null,
      notes: e?.notes ?? null,
    });
  }, [e]);
  const save = usePeopleMutation<EmployeeRequest>(
    (c, d) => PeopleService.saveEmployee(c, person.id, d),
    e ? "Funcionário salvo" : "Papel Funcionário habilitado",
  );
  const disable = usePeopleMutation<void>(
    (c) => PeopleService.disableEmployee(c, person.id),
    "Papel Funcionário removido",
  );
  const addSpec = usePeopleMutation<SpecialtyRequest>(
    (c, d) => PeopleService.addSpecialty(c, person.id, d),
    "Especialidade adicionada",
  );
  const delSpec = usePeopleMutation<string>(
    (c, id) => PeopleService.deleteSpecialty(c, person.id, id),
    "Especialidade removida",
  );

  return (
    <div className="space-y-6">
      <SectionCard
        title="Funcionário"
        actions={
          e &&
          canEmployees && (
            <RemoveRole
              label="Funcionário"
              pending={disable.isPending}
              onConfirm={() => disable.mutate()}
            />
          )
        }
      >
        <div className="grid gap-4 sm:grid-cols-3">
          <Field label="Matrícula" htmlFor="em-reg">
            <Input
              id="em-reg"
              disabled={!canEmployees}
              value={form.registrationNumber ?? ""}
              maxLength={20}
              onChange={(ev) => setForm({ ...form, registrationNumber: ev.target.value })}
            />
          </Field>
          <Field label="Cargo" htmlFor="em-title" hint="Cargos em Pessoas → Funcionários → Cargos.">
            <NativeSelect
              id="em-title"
              disabled={!canEmployees}
              value={form.jobTitleId ?? ""}
              onChange={(ev) => setForm({ ...form, jobTitleId: ev.target.value || null })}
            >
              <option value="">—</option>
              {(titles ?? []).map((t) => (
                <option key={t.id} value={t.id} disabled={!t.active}>
                  {t.title}
                </option>
              ))}
            </NativeSelect>
          </Field>
          <Field label="Situação" htmlFor="em-status">
            <NativeSelect
              id="em-status"
              disabled={!canEmployees}
              value={form.status ?? "ACTIVE"}
              onChange={(ev) => setForm({ ...form, status: ev.target.value as EmployeeStatus })}
            >
              {Object.entries(EMPLOYEE_STATUS_LABELS).map(([k, l]) => (
                <option key={k} value={k}>
                  {l}
                </option>
              ))}
            </NativeSelect>
          </Field>
          <Field label="Admissão" htmlFor="em-hire">
            <Input
              id="em-hire"
              type="date"
              disabled={!canEmployees}
              value={form.hireDate ?? ""}
              onChange={(ev) => setForm({ ...form, hireDate: ev.target.value || null })}
            />
          </Field>
          <Field label="Desligamento" htmlFor="em-term">
            <Input
              id="em-term"
              type="date"
              disabled={!canEmployees}
              value={form.terminationDate ?? ""}
              onChange={(ev) => setForm({ ...form, terminationDate: ev.target.value || null })}
            />
          </Field>
          <Field label="Contrato" htmlFor="em-contract">
            <NativeSelect
              id="em-contract"
              disabled={!canEmployees}
              value={form.contractType ?? ""}
              onChange={(ev) =>
                setForm({ ...form, contractType: (ev.target.value || null) as ContractType | null })
              }
            >
              <option value="">—</option>
              {Object.entries(CONTRACT_LABELS).map(([k, l]) => (
                <option key={k} value={k}>
                  {l}
                </option>
              ))}
            </NativeSelect>
          </Field>
          <Field
            label="Usuário do sistema"
            htmlFor="em-user"
            hint="O login dessa pessoa na Equipe (opcional)."
          >
            <NativeSelect
              id="em-user"
              disabled={!canEmployees}
              value={form.userId ?? ""}
              onChange={(ev) => setForm({ ...form, userId: ev.target.value || null })}
            >
              <option value="">Sem usuário</option>
              {(users ?? []).map((u) => (
                <option key={u.id} value={u.id}>
                  {u.name}
                </option>
              ))}
            </NativeSelect>
          </Field>
          {canSalary && (
            <Field label="Salário (R$)" htmlFor="em-salary">
              <Input
                id="em-salary"
                type="number"
                min={0}
                step="0.01"
                value={form.salary ?? ""}
                onChange={(ev) =>
                  setForm({
                    ...form,
                    salary: ev.target.value === "" ? null : Number(ev.target.value),
                  })
                }
              />
            </Field>
          )}
          <Field label="Observações" htmlFor="em-notes" className="sm:col-span-3">
            <Textarea
              id="em-notes"
              rows={2}
              disabled={!canEmployees}
              value={form.notes ?? ""}
              onChange={(ev) => setForm({ ...form, notes: ev.target.value })}
            />
          </Field>
        </div>
        {canEmployees && (
          <div className="mt-4 flex justify-end">
            <Button disabled={save.isPending} onClick={() => save.mutate(form)}>
              {e ? "Salvar" : "Habilitar como funcionário"}
            </Button>
          </div>
        )}
      </SectionCard>

      {e && (
        <SectionCard
          title="Especialidades e conselho"
          description="Quem tem especialidade aparece como profissional na Agenda e nos Procedimentos."
        >
          {e.specialties.length === 0 ? (
            <p className="mb-4 text-sm text-muted-foreground">
              Sem especialidade: não é profissional de saúde.
            </p>
          ) : (
            <ul className="mb-4 divide-y text-sm">
              {e.specialties.map((s) => (
                <li key={s.id} className="flex items-center gap-3 py-2">
                  <span className="font-medium">{SPECIALTY_LABELS[s.specialty]}</span>
                  <span className="text-muted-foreground">
                    {[s.council, s.councilNumber].filter(Boolean).join(" ")}
                    {s.councilUf ? `/${s.councilUf}` : ""}
                  </span>
                  {s.primary && <Pill tone="blue">Principal</Pill>}
                  {canEmployees && (
                    <Button
                      className="ml-auto"
                      size="icon"
                      variant="ghost"
                      aria-label="Remover especialidade"
                      onClick={() => delSpec.mutate(s.id)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  )}
                </li>
              ))}
            </ul>
          )}
          {canEmployees && (
            <div className="grid gap-3 sm:grid-cols-5 sm:items-end">
              <Field label="Especialidade" htmlFor="sp-sp">
                <NativeSelect
                  id="sp-sp"
                  value={spec.specialty}
                  onChange={(ev) =>
                    setSpec({ ...spec, specialty: ev.target.value as SpecialtyCode })
                  }
                >
                  {Object.entries(SPECIALTY_LABELS).map(([k, l]) => (
                    <option key={k} value={k}>
                      {l}
                    </option>
                  ))}
                </NativeSelect>
              </Field>
              <Field label="Conselho" htmlFor="sp-council">
                <Input
                  id="sp-council"
                  value={spec.council ?? ""}
                  maxLength={10}
                  onChange={(ev) => setSpec({ ...spec, council: ev.target.value })}
                />
              </Field>
              <Field label="Número" htmlFor="sp-number">
                <Input
                  id="sp-number"
                  value={spec.councilNumber ?? ""}
                  maxLength={20}
                  onChange={(ev) => setSpec({ ...spec, councilNumber: ev.target.value })}
                />
              </Field>
              <Field label="UF" htmlFor="sp-uf">
                <Input
                  id="sp-uf"
                  value={spec.councilUf ?? ""}
                  maxLength={2}
                  onChange={(ev) => setSpec({ ...spec, councilUf: ev.target.value })}
                />
              </Field>
              <Button
                variant="outline"
                disabled={addSpec.isPending}
                onClick={() => addSpec.mutate(spec)}
              >
                <Plus className="mr-1 h-4 w-4" /> Adicionar
              </Button>
            </div>
          )}
        </SectionCard>
      )}
    </div>
  );
}
