"use client";

import { Suspense, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ROUTES } from "@/lib/constants";
import { AddressFields } from "@/features/masterdata/people/components/AddressFields";
import { Field, NativeSelect, SectionCard } from "@/features/masterdata/people/components/FormBits";
import { IdentityFields } from "@/features/masterdata/people/components/IdentityFields";
import {
  useCreatePerson,
  useHealthSegment,
  useJobTitles,
  usePeoplePermissions,
  useRequiredKeys,
} from "@/features/masterdata/people/hooks/usePeople";
import { SUPPLIER_TYPE_LABELS } from "@/features/masterdata/people/lib/people-labels";
import { useActiveCompanyModules } from "@/features/identity/modules/hooks/useCompanyModules";
import type {
  AddressRequest,
  FieldContext,
  PersonRequest,
  SupplierType,
} from "@/features/masterdata/people/types/people.types";

type Role = "CUSTOMER" | "SUPPLIER" | "EMPLOYEE";

function NewPersonForm() {
  const router = useRouter();
  const params = useSearchParams();
  const initialRole = params.get("role") as Role | null;
  const health = useHealthSegment();
  const { isEnabled } = useActiveCompanyModules();
  const perms = usePeoplePermissions();
  const create = useCreatePerson();
  const { data: jobTitles } = useJobTitles();

  const [person, setPerson] = useState<PersonRequest>({
    personType: initialRole === "SUPPLIER" ? "J" : "F",
    name: "",
    countryId: 1,
    nationality: "Brasileira",
  });
  const [mobile, setMobile] = useState("");
  const [email, setEmail] = useState("");
  const [address, setAddress] = useState<AddressRequest>({
    addressType: "RESIDENTIAL",
    countryId: 1,
  });
  const [roles, setRoles] = useState<Role[]>(initialRole ? [initialRole] : []);
  const [patient, setPatient] = useState({
    recordNumber: "",
    healthPlan: "",
    healthPlanNumber: "",
    referralSource: "",
  });
  const [supplierType, setSupplierType] = useState<SupplierType>("OTHER");
  const [jobTitleId, setJobTitleId] = useState("");
  const [hireDate, setHireDate] = useState("");

  const contexts = useMemo<FieldContext[]>(
    () => [person.personType === "J" ? "PJ" : "PF", ...roles],
    [person.personType, roles],
  );
  const required = useRequiredKeys(contexts);
  const toggle = (r: Role) =>
    setRoles((cur) => (cur.includes(r) ? cur.filter((x) => x !== r) : [...cur, r]));

  const submit = () => {
    const hasAddress = !!(
      address.postalCode ||
      address.street ||
      address.cityId ||
      address.cityName
    );
    create.mutate(
      {
        ...person,
        roles,
        mobile: mobile || null,
        email: email || null,
        address: hasAddress ? address : null,
        customer: roles.includes("CUSTOMER")
          ? {
              patient: health
                ? {
                    recordNumber: patient.recordNumber || null,
                    healthPlan: patient.healthPlan || null,
                    healthPlanNumber: patient.healthPlanNumber || null,
                    referralSource: patient.referralSource || null,
                  }
                : null,
            }
          : null,
        supplier: roles.includes("SUPPLIER") ? { supplierType } : null,
        employee: roles.includes("EMPLOYEE")
          ? { jobTitleId: jobTitleId || null, hireDate: hireDate || null }
          : null,
      },
      { onSuccess: (p) => router.push(`${ROUTES.PEOPLE}/${p.id}`) },
    );
  };

  const req = (k: string) => required.includes(k) && !person.provisional;

  return (
    <form
      className="space-y-6"
      onSubmit={(e) => {
        e.preventDefault();
        submit();
      }}
    >
      <h1 className="text-2xl font-bold tracking-tight">Nova pessoa</h1>

      <SectionCard title="Papéis" description="Uma pessoa pode ter vários papéis ao mesmo tempo.">
        <div className="flex flex-wrap gap-6 text-sm">
          <label className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={roles.includes("CUSTOMER")}
              onChange={() => toggle("CUSTOMER")}
            />
            {health ? "Cliente (paciente)" : "Cliente"}
          </label>
          {isEnabled("SUPPLIERS") && perms.canSuppliers && (
            <label className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={roles.includes("SUPPLIER")}
                onChange={() => toggle("SUPPLIER")}
              />
              Fornecedor
            </label>
          )}
          {isEnabled("EMPLOYEES") && perms.canEmployees && (
            <label className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={roles.includes("EMPLOYEE")}
                onChange={() => toggle("EMPLOYEE")}
              />
              Funcionário
            </label>
          )}
        </div>
      </SectionCard>

      <SectionCard title="Dados da pessoa">
        <IdentityFields value={person} onChange={setPerson} requiredKeys={required} />
      </SectionCard>

      <SectionCard title="Contato principal">
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Celular (WhatsApp)" htmlFor="n-mobile" required={req("PERSON_MOBILE")}>
            <Input
              id="n-mobile"
              inputMode="tel"
              value={mobile}
              onChange={(e) => setMobile(e.target.value)}
              placeholder="(00) 00000-0000"
            />
          </Field>
          <Field label="E-mail" htmlFor="n-email" required={req("PERSON_EMAIL")}>
            <Input
              id="n-email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </Field>
        </div>
      </SectionCard>

      <SectionCard title="Endereço principal">
        <AddressFields
          value={address}
          onChange={setAddress}
          idPrefix="n-addr"
          requiredKeys={person.provisional ? [] : required}
        />
      </SectionCard>

      {roles.includes("CUSTOMER") && health && (
        <SectionCard title="Paciente" description="O paciente é um cliente com dados clínicos.">
          <div className="grid gap-4 sm:grid-cols-4">
            <Field label="Nº do prontuário" htmlFor="n-rec" required={req("PATIENT_RECORD_NUMBER")}>
              <Input
                id="n-rec"
                value={patient.recordNumber}
                maxLength={40}
                onChange={(e) => setPatient({ ...patient, recordNumber: e.target.value })}
              />
            </Field>
            <Field label="Convênio" htmlFor="n-plan" required={req("PATIENT_HEALTH_PLAN")}>
              <Input
                id="n-plan"
                value={patient.healthPlan}
                maxLength={80}
                onChange={(e) => setPatient({ ...patient, healthPlan: e.target.value })}
              />
            </Field>
            <Field label="Nº da carteirinha" htmlFor="n-plann">
              <Input
                id="n-plann"
                value={patient.healthPlanNumber}
                maxLength={40}
                onChange={(e) => setPatient({ ...patient, healthPlanNumber: e.target.value })}
              />
            </Field>
            <Field label="Como conheceu" htmlFor="n-ref" required={req("PATIENT_REFERRAL_SOURCE")}>
              <Input
                id="n-ref"
                value={patient.referralSource}
                maxLength={80}
                onChange={(e) => setPatient({ ...patient, referralSource: e.target.value })}
              />
            </Field>
          </div>
          {req("PATIENT_GUARDIAN_MINOR") && (
            <p className="mt-3 text-xs text-muted-foreground">
              Menor de idade precisa de responsável: depois de salvar, vincule na aba
              Relacionamentos.
            </p>
          )}
        </SectionCard>
      )}

      {roles.includes("SUPPLIER") && (
        <SectionCard title="Fornecedor">
          <Field label="Tipo de fornecedor" htmlFor="n-stype" className="max-w-xs">
            <NativeSelect
              id="n-stype"
              value={supplierType}
              onChange={(e) => setSupplierType(e.target.value as SupplierType)}
            >
              {Object.entries(SUPPLIER_TYPE_LABELS).map(([k, l]) => (
                <option key={k} value={k}>
                  {l}
                </option>
              ))}
            </NativeSelect>
          </Field>
        </SectionCard>
      )}

      {roles.includes("EMPLOYEE") && (
        <SectionCard
          title="Funcionário"
          description="Especialidades e conselho ficam na aba Funcionário depois de salvar."
        >
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Cargo" htmlFor="n-job" required={req("EMPLOYEE_JOB_TITLE")}>
              <NativeSelect
                id="n-job"
                value={jobTitleId}
                onChange={(e) => setJobTitleId(e.target.value)}
              >
                <option value="">—</option>
                {(jobTitles ?? [])
                  .filter((j) => j.active)
                  .map((j) => (
                    <option key={j.id} value={j.id}>
                      {j.title}
                    </option>
                  ))}
              </NativeSelect>
            </Field>
            <Field label="Admissão" htmlFor="n-hire" required={req("EMPLOYEE_HIRE_DATE")}>
              <Input
                id="n-hire"
                type="date"
                value={hireDate}
                onChange={(e) => setHireDate(e.target.value)}
              />
            </Field>
          </div>
        </SectionCard>
      )}

      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" onClick={() => router.back()}>
          Cancelar
        </Button>
        <Button type="submit" disabled={create.isPending || !person.name.trim()}>
          {create.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          Salvar
        </Button>
      </div>
    </form>
  );
}

export default function NewPersonPage() {
  return (
    <Suspense fallback={null}>
      <NewPersonForm />
    </Suspense>
  );
}
