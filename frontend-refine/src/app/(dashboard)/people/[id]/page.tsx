"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { AlertTriangle, ArrowLeft, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { ErrorCard } from "@/components/common/ErrorCard";
import { ROUTES } from "@/lib/constants";
import { useActiveCompanyModules } from "@/features/identity/modules/hooks/useCompanyModules";
import { Pill, SectionCard } from "@/features/masterdata/people/components/FormBits";
import { IdentityFields } from "@/features/masterdata/people/components/IdentityFields";
import {
  AddressesTab,
  BankTab,
  ContactsTab,
  DocumentsTab,
  HistoryTab,
  RelationshipsTab,
} from "@/features/masterdata/people/components/PersonChildTabs";
import {
  CustomerTab,
  EmployeeTab,
  SupplierTab,
} from "@/features/masterdata/people/components/PersonRoleTabs";
import {
  usePeopleMutation,
  usePeoplePermissions,
  usePerson,
  useRequiredKeys,
  useUpdatePerson,
} from "@/features/masterdata/people/hooks/usePeople";
import { PeopleService } from "@/features/masterdata/people/services/people.service";
import { ROLE_LABELS, formatTaxId, initials } from "@/features/masterdata/people/lib/people-labels";
import type {
  FieldContext,
  PersonDetail,
  PersonRequest,
} from "@/features/masterdata/people/types/people.types";

function toRequest(p: PersonDetail): PersonRequest {
  return {
    personType: p.personType,
    name: p.name,
    tradeName: p.tradeName,
    taxId: p.taxId,
    rgOrIe: p.rgOrIe,
    rgIssuer: p.rgIssuer,
    birthOrFoundationDate: p.birthOrFoundationDate,
    gender: p.gender,
    maritalStatus: p.maritalStatus,
    professionalStatus: p.professionalStatus,
    nationality: p.nationality,
    countryId: p.countryId ?? 1,
    provisional: p.provisional,
    notes: p.notes,
  };
}

function DataTab({
  person,
  canEdit,
  canSensitive,
}: {
  person: PersonDetail;
  canEdit: boolean;
  canSensitive: boolean;
}) {
  const [form, setForm] = useState<PersonRequest>(() => toRequest(person));
  useEffect(() => setForm(toRequest(person)), [person]);
  const contexts: FieldContext[] = [
    form.personType === "J" ? "PJ" : "PF",
    ...(person.roles.filter((r) => r !== "PATIENT") as FieldContext[]),
  ];
  const required = useRequiredKeys(contexts);
  const save = useUpdatePerson(person.id);
  return (
    <SectionCard title="Dados da pessoa">
      <fieldset disabled={!canEdit} className="space-y-4">
        <IdentityFields
          value={form}
          onChange={setForm}
          excludeId={person.id}
          requiredKeys={required}
          canSensitive={canSensitive}
        />
      </fieldset>
      {canEdit && (
        <div className="mt-4 flex justify-end">
          <Button disabled={save.isPending || !form.name.trim()} onClick={() => save.mutate(form)}>
            {save.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Salvar
          </Button>
        </div>
      )}
    </SectionCard>
  );
}

export default function PersonPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const router = useRouter();
  const perms = usePeoplePermissions();
  const { isEnabled } = useActiveCompanyModules();
  const { data: person, isLoading, isError, refetch } = usePerson(id);
  const [tab, setTab] = useState("data");
  const [deleting, setDeleting] = useState(false);
  const [reason, setReason] = useState("");

  const status = usePeopleMutation<boolean>(
    (c, active) => PeopleService.setStatus(c, id, active),
    "Situação alterada",
  );
  const remove = usePeopleMutation<string>(
    (c, r) => PeopleService.remove(c, id, r),
    "Cadastro excluído",
  );
  const restore = usePeopleMutation<void>(
    (c) => PeopleService.restore(c, id),
    "Cadastro restaurado",
  );

  if (isLoading) {
    return (
      <div className="flex items-center justify-center gap-2 py-20 text-muted-foreground">
        <Loader2 className="h-5 w-5 animate-spin" /> Carregando…
      </div>
    );
  }
  if (isError || !person) {
    return <ErrorCard message="Cadastro não encontrado ou sem acesso." onRetry={() => refetch()} />;
  }

  const canEdit = perms.canUpdate && !person.deleted;
  const isCustomer = person.roles.includes("CUSTOMER");
  const isSupplier = person.roles.includes("SUPPLIER");
  const isEmployee = person.roles.includes("EMPLOYEE");
  const showSupplier = isEnabled("SUPPLIERS") && (isSupplier || perms.canSuppliers);
  const showEmployee = isEnabled("EMPLOYEES") && (isEmployee || perms.canEmployees);

  return (
    <div className="space-y-6">
      <Link
        href={ROUTES.PEOPLE}
        className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" /> Pessoas
      </Link>

      <div className="flex flex-wrap items-center justify-between gap-4 rounded-lg border bg-card p-5 shadow-sm">
        <div className="flex items-center gap-4">
          <div className="flex h-14 w-14 items-center justify-center rounded-full bg-gradient-to-br from-blue-500 to-blue-800 text-lg font-bold text-white">
            {initials(person.name)}
          </div>
          <div className="space-y-1">
            <div className="flex flex-wrap items-baseline gap-2">
              <h1 className="text-xl font-bold">{person.name}</h1>
              <span className="text-sm text-muted-foreground">
                {person.personType === "J" ? "Pessoa jurídica" : "Pessoa física"}
                {person.taxId
                  ? ` · ${person.personType === "J" ? "CNPJ" : "CPF"} ${formatTaxId(person.taxId)}`
                  : ""}
              </span>
            </div>
            <div className="flex flex-wrap gap-1.5">
              {person.roles.map((r) => (
                <Pill
                  key={r}
                  tone={
                    r === "PATIENT"
                      ? "blue"
                      : r === "SUPPLIER"
                        ? "amber"
                        : r === "EMPLOYEE"
                          ? "purple"
                          : "green"
                  }
                >
                  {ROLE_LABELS[r]}
                </Pill>
              ))}
              {person.hasContact && <Pill>Contato no CRM</Pill>}
              {person.provisional && <Pill tone="amber">Provisório</Pill>}
              {!person.active && !person.deleted && <Pill>Inativo</Pill>}
              {person.deleted && <Pill tone="red">Excluído</Pill>}
            </div>
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          {person.deleted ? (
            perms.canDelete && (
              <Button variant="outline" onClick={() => restore.mutate()}>
                Restaurar
              </Button>
            )
          ) : (
            <>
              {perms.canUpdate && (
                <Button variant="outline" onClick={() => status.mutate(!person.active)}>
                  {person.active ? "Inativar" : "Ativar"}
                </Button>
              )}
              {perms.canDelete && (
                <Button
                  variant="outline"
                  className="text-destructive"
                  onClick={() => setDeleting(true)}
                >
                  Excluir
                </Button>
              )}
            </>
          )}
        </div>
      </div>

      {person.deleted && person.deleteReason && (
        <p className="text-sm text-muted-foreground">Motivo da exclusão: {person.deleteReason}</p>
      )}
      {person.openIssues > 0 && (
        <div
          role="alert"
          className="flex items-center gap-3 rounded-md border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900 dark:bg-amber-950 dark:text-amber-100"
        >
          <AlertTriangle className="h-4 w-4 shrink-0" aria-hidden="true" />
          Este cadastro tem pendência de CPF da migração (repetido ou inválido).
        </div>
      )}

      <Tabs value={tab} onValueChange={setTab}>
        <div className="overflow-x-auto">
          <TabsList>
            <TabsTrigger value="data">Dados</TabsTrigger>
            <TabsTrigger value="addresses">Endereços ({person.addresses.length})</TabsTrigger>
            <TabsTrigger value="contacts">Contatos ({person.contacts.length})</TabsTrigger>
            {perms.canSensitive && <TabsTrigger value="documents">Documentos</TabsTrigger>}
            <TabsTrigger value="relationships">Relacionamentos</TabsTrigger>
            <TabsTrigger value="customer">
              {person.healthSegment ? "Cliente / Paciente" : "Cliente"}
            </TabsTrigger>
            {showSupplier && <TabsTrigger value="supplier">Fornecedor</TabsTrigger>}
            {showEmployee && <TabsTrigger value="employee">Funcionário</TabsTrigger>}
            {perms.canBank && <TabsTrigger value="bank">Dados bancários</TabsTrigger>}
            <TabsTrigger value="history">Histórico</TabsTrigger>
          </TabsList>
        </div>
        <TabsContent value="data">
          <DataTab person={person} canEdit={canEdit} canSensitive={perms.canSensitive} />
        </TabsContent>
        <TabsContent value="addresses">
          <AddressesTab person={person} canEdit={canEdit} />
        </TabsContent>
        <TabsContent value="contacts">
          <ContactsTab person={person} canEdit={canEdit} />
        </TabsContent>
        {perms.canSensitive && (
          <TabsContent value="documents">
            <DocumentsTab person={person} canEdit={canEdit} />
          </TabsContent>
        )}
        <TabsContent value="relationships">
          <RelationshipsTab person={person} canEdit={canEdit} />
        </TabsContent>
        <TabsContent value="customer">
          {isCustomer || canEdit ? (
            <CustomerTab person={person} canEdit={canEdit} />
          ) : (
            <p className="text-sm text-muted-foreground">Esta pessoa não é cliente.</p>
          )}
        </TabsContent>
        {showSupplier && (
          <TabsContent value="supplier">
            <SupplierTab person={person} />
          </TabsContent>
        )}
        {showEmployee && (
          <TabsContent value="employee">
            <EmployeeTab person={person} />
          </TabsContent>
        )}
        {perms.canBank && (
          <TabsContent value="bank">
            <BankTab person={person} />
          </TabsContent>
        )}
        <TabsContent value="history">
          <HistoryTab personId={person.id} />
        </TabsContent>
      </Tabs>

      <Dialog open={deleting} onOpenChange={setDeleting}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Excluir {person.name}?</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground">
            A exclusão é lógica: agenda, prontuário, orçamentos e financeiro continuam guardados e o
            cadastro pode ser restaurado.
          </p>
          <label htmlFor="del-reason" className="text-sm font-medium">
            Motivo<span className="text-destructive">*</span>
          </label>
          <Input id="del-reason" value={reason} onChange={(e) => setReason(e.target.value)} />
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleting(false)}>
              Cancelar
            </Button>
            <Button
              variant="destructive"
              disabled={!reason.trim() || remove.isPending}
              onClick={() =>
                remove.mutate(reason.trim(), {
                  onSuccess: () => {
                    setDeleting(false);
                    router.push(ROUTES.PEOPLE);
                  },
                })
              }
            >
              Excluir
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
