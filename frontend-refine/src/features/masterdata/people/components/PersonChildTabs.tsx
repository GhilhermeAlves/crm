"use client";

import { useState } from "react";
import { Loader2, Pencil, Plus, Star, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { toHttpError } from "@/lib/http-error";
import { PeopleService } from "../services/people.service";
import {
  useBankAccounts,
  usePeopleMutation,
  usePersonHistory,
  useRequiredKeys,
} from "../hooks/usePeople";
import {
  ADDRESS_TYPE_LABELS,
  CONTACT_TYPE_LABELS,
  DOCUMENT_TYPE_LABELS,
  RELATIONSHIP_LABELS,
  formatCep,
  formatDate,
  formatDateTime,
  formatPhone,
} from "../lib/people-labels";
import type {
  Address,
  AddressRequest,
  BankAccountRequest,
  ContactChannel,
  ContactType,
  DocumentRequest,
  DocumentType,
  FieldContext,
  PersonDetail,
  PersonSummary,
  RelationshipType,
} from "../types/people.types";
import { AddressFields } from "./AddressFields";
import { Field, NativeSelect, Pill, SectionCard } from "./FormBits";
import { PersonPicker } from "./PersonPicker";

function contextsOf(p: PersonDetail): FieldContext[] {
  const c: FieldContext[] = [p.personType === "J" ? "PJ" : "PF"];
  if (p.roles.includes("CUSTOMER")) c.push("CUSTOMER");
  if (p.roles.includes("SUPPLIER")) c.push("SUPPLIER");
  if (p.roles.includes("EMPLOYEE")) c.push("EMPLOYEE");
  return c;
}

// =================================================================== endereços

export function AddressesTab({ person, canEdit }: { person: PersonDetail; canEdit: boolean }) {
  const [editing, setEditing] = useState<Address | "new" | null>(null);
  const [form, setForm] = useState<AddressRequest>({});
  const required = useRequiredKeys(contextsOf(person));
  const save = usePeopleMutation<{ id?: string; data: AddressRequest }>(
    (c, v) =>
      v.id
        ? PeopleService.updateAddress(c, person.id, v.id, v.data)
        : PeopleService.addAddress(c, person.id, v.data),
    "Endereço salvo",
  );
  const remove = usePeopleMutation<string>(
    (c, id) => PeopleService.deleteAddress(c, person.id, id),
    "Endereço removido",
  );

  const open = (a: Address | "new") => {
    setEditing(a);
    setForm(a === "new" ? { addressType: "RESIDENTIAL", countryId: 1 } : { ...a });
  };

  return (
    <SectionCard
      title="Endereços"
      description="Um endereço principal por tipo."
      actions={
        canEdit && (
          <Button size="sm" variant="outline" onClick={() => open("new")}>
            <Plus className="mr-1 h-4 w-4" /> Endereço
          </Button>
        )
      }
    >
      {person.addresses.length === 0 ? (
        <p className="text-sm text-muted-foreground">Nenhum endereço cadastrado.</p>
      ) : (
        <ul className="grid gap-3 md:grid-cols-2">
          {person.addresses.map((a) => (
            <li key={a.id} className="rounded-md border p-3 text-sm">
              <div className="mb-1 flex items-center gap-2">
                <span className="font-semibold">{ADDRESS_TYPE_LABELS[a.addressType]}</span>
                {a.primary && <Pill tone="blue">Principal</Pill>}
                {canEdit && (
                  <span className="ml-auto flex gap-1">
                    <Button
                      size="icon"
                      variant="ghost"
                      aria-label="Editar endereço"
                      onClick={() => open(a)}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      size="icon"
                      variant="ghost"
                      aria-label="Remover endereço"
                      onClick={() => remove.mutate(a.id)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </span>
                )}
              </div>
              <div>
                {[a.street, a.addressNumber, a.complement].filter(Boolean).join(", ") || "—"}
              </div>
              <div className="text-muted-foreground">
                {[
                  a.neighborhoodName,
                  a.cityName && `${a.cityName}/${a.uf ?? ""}`,
                  a.postalCode && formatCep(a.postalCode),
                ]
                  .filter(Boolean)
                  .join(" · ")}
              </div>
            </li>
          ))}
        </ul>
      )}
      <Dialog open={editing !== null} onOpenChange={(o) => !o && setEditing(null)}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>{editing === "new" ? "Novo endereço" : "Editar endereço"}</DialogTitle>
          </DialogHeader>
          <AddressFields value={form} onChange={setForm} idPrefix="addr" requiredKeys={required} />
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={form.primary ?? editing === "new"}
              onChange={(e) => setForm({ ...form, primary: e.target.checked })}
            />
            Principal deste tipo
          </label>
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditing(null)}>
              Cancelar
            </Button>
            <Button
              disabled={save.isPending}
              onClick={() =>
                save.mutate(
                  { id: editing === "new" || !editing ? undefined : editing.id, data: form },
                  { onSuccess: () => setEditing(null) },
                )
              }
            >
              {save.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Salvar
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </SectionCard>
  );
}

// =================================================================== contatos

export function ContactsTab({ person, canEdit }: { person: PersonDetail; canEdit: boolean }) {
  const [editing, setEditing] = useState<ContactChannel | "new" | null>(null);
  const [type, setType] = useState<ContactType>("MOBILE");
  const [value, setValue] = useState("");
  const [name, setName] = useState("");
  const [whatsapp, setWhatsapp] = useState(true);
  const [primary, setPrimary] = useState(true);
  const save = usePeopleMutation<void>((c) => {
    const data = { contactType: type, value, contactName: name || null, whatsapp, primary };
    return editing && editing !== "new"
      ? PeopleService.updateChannel(c, person.id, editing.id, data)
      : PeopleService.addChannel(c, person.id, data);
  }, "Contato salvo");
  const remove = usePeopleMutation<string>(
    (c, id) => PeopleService.deleteChannel(c, person.id, id),
    "Contato removido",
  );

  const open = (ch: ContactChannel | "new") => {
    setEditing(ch);
    setType(ch === "new" ? "MOBILE" : ch.contactType);
    setValue(ch === "new" ? "" : ch.value);
    setName(ch === "new" ? "" : (ch.contactName ?? ""));
    setWhatsapp(ch === "new" ? true : ch.whatsapp);
    setPrimary(ch === "new" ? true : ch.primary);
  };

  return (
    <SectionCard
      title="Contatos"
      description="O celular WhatsApp principal é o usado no atendimento."
      actions={
        canEdit && (
          <Button size="sm" variant="outline" onClick={() => open("new")}>
            <Plus className="mr-1 h-4 w-4" /> Contato
          </Button>
        )
      }
    >
      {person.contacts.length === 0 ? (
        <p className="text-sm text-muted-foreground">Nenhum contato cadastrado.</p>
      ) : (
        <ul className="divide-y text-sm">
          {person.contacts.map((c) => (
            <li key={c.id} className="flex flex-wrap items-center gap-2 py-2">
              <span className="w-20 font-medium">{CONTACT_TYPE_LABELS[c.contactType]}</span>
              <span>
                {c.contactType === "MOBILE" || c.contactType === "PHONE"
                  ? formatPhone(c.value)
                  : c.value}
              </span>
              {c.contactName && <span className="text-muted-foreground">({c.contactName})</span>}
              {c.whatsapp && <Pill tone="green">WhatsApp</Pill>}
              {c.primary && <Pill tone="blue">Principal</Pill>}
              {canEdit && (
                <span className="ml-auto flex gap-1">
                  <Button
                    size="icon"
                    variant="ghost"
                    aria-label="Editar contato"
                    onClick={() => open(c)}
                  >
                    <Pencil className="h-4 w-4" />
                  </Button>
                  <Button
                    size="icon"
                    variant="ghost"
                    aria-label="Remover contato"
                    onClick={() => remove.mutate(c.id)}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </span>
              )}
            </li>
          ))}
        </ul>
      )}
      <Dialog open={editing !== null} onOpenChange={(o) => !o && setEditing(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editing === "new" ? "Novo contato" : "Editar contato"}</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Tipo" htmlFor="ch-type">
              <NativeSelect
                id="ch-type"
                value={type}
                onChange={(e) => setType(e.target.value as ContactType)}
              >
                {Object.entries(CONTACT_TYPE_LABELS).map(([k, l]) => (
                  <option key={k} value={k}>
                    {l}
                  </option>
                ))}
              </NativeSelect>
            </Field>
            <Field label="Contato" htmlFor="ch-value" required>
              <Input id="ch-value" value={value} onChange={(e) => setValue(e.target.value)} />
            </Field>
            <Field label="Pessoa de referência" htmlFor="ch-name" className="sm:col-span-2">
              <Input
                id="ch-name"
                value={name}
                maxLength={100}
                onChange={(e) => setName(e.target.value)}
              />
            </Field>
          </div>
          <div className="flex flex-wrap gap-6 text-sm">
            {(type === "MOBILE" || type === "PHONE") && (
              <label className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={whatsapp}
                  onChange={(e) => setWhatsapp(e.target.checked)}
                />{" "}
                WhatsApp
              </label>
            )}
            <label className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={primary}
                onChange={(e) => setPrimary(e.target.checked)}
              />{" "}
              Principal deste tipo
            </label>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditing(null)}>
              Cancelar
            </Button>
            <Button
              disabled={save.isPending || !value.trim()}
              onClick={() => save.mutate(undefined, { onSuccess: () => setEditing(null) })}
            >
              Salvar
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </SectionCard>
  );
}

// =================================================================== documentos

export function DocumentsTab({ person, canEdit }: { person: PersonDetail; canEdit: boolean }) {
  const empty: DocumentRequest = {
    documentType: "RG",
    documentNumber: "",
    issuer: null,
    issuerUf: null,
    issueDate: null,
    expiryDate: null,
    storageObjectId: null,
  };
  const [form, setForm] = useState<DocumentRequest | null>(null);
  const save = usePeopleMutation<DocumentRequest>(
    (c, d) => PeopleService.addDocument(c, person.id, d),
    "Documento salvo",
  );
  const remove = usePeopleMutation<string>(
    (c, id) => PeopleService.deleteDocument(c, person.id, id),
    "Documento removido",
  );
  return (
    <SectionCard
      title="Documentos"
      actions={
        canEdit && (
          <Button size="sm" variant="outline" onClick={() => setForm(empty)}>
            <Plus className="mr-1 h-4 w-4" /> Documento
          </Button>
        )
      }
    >
      {person.documents.length === 0 ? (
        <p className="text-sm text-muted-foreground">Nenhum documento adicional.</p>
      ) : (
        <ul className="divide-y text-sm">
          {person.documents.map((d) => (
            <li key={d.id} className="flex flex-wrap items-center gap-3 py-2">
              <span className="w-32 font-medium">{DOCUMENT_TYPE_LABELS[d.documentType]}</span>
              <span>{d.documentNumber}</span>
              <span className="text-muted-foreground">
                {[d.issuer, d.issuerUf].filter(Boolean).join("/")}
                {d.expiryDate ? ` · validade ${formatDate(d.expiryDate)}` : ""}
              </span>
              {canEdit && (
                <Button
                  className="ml-auto"
                  size="icon"
                  variant="ghost"
                  aria-label="Remover documento"
                  onClick={() => remove.mutate(d.id)}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              )}
            </li>
          ))}
        </ul>
      )}
      <Dialog open={form !== null} onOpenChange={(o) => !o && setForm(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Novo documento</DialogTitle>
          </DialogHeader>
          {form && (
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Tipo" htmlFor="doc-type">
                <NativeSelect
                  id="doc-type"
                  value={form.documentType}
                  onChange={(e) =>
                    setForm({ ...form, documentType: e.target.value as DocumentType })
                  }
                >
                  {Object.entries(DOCUMENT_TYPE_LABELS).map(([k, l]) => (
                    <option key={k} value={k}>
                      {l}
                    </option>
                  ))}
                </NativeSelect>
              </Field>
              <Field label="Número" htmlFor="doc-number" required>
                <Input
                  id="doc-number"
                  value={form.documentNumber}
                  maxLength={30}
                  onChange={(e) => setForm({ ...form, documentNumber: e.target.value })}
                />
              </Field>
              <Field label="Órgão emissor" htmlFor="doc-issuer">
                <Input
                  id="doc-issuer"
                  value={form.issuer ?? ""}
                  maxLength={20}
                  onChange={(e) => setForm({ ...form, issuer: e.target.value })}
                />
              </Field>
              <Field label="UF" htmlFor="doc-uf">
                <Input
                  id="doc-uf"
                  value={form.issuerUf ?? ""}
                  maxLength={2}
                  onChange={(e) => setForm({ ...form, issuerUf: e.target.value })}
                />
              </Field>
              <Field label="Emissão" htmlFor="doc-issue">
                <Input
                  id="doc-issue"
                  type="date"
                  value={form.issueDate ?? ""}
                  onChange={(e) => setForm({ ...form, issueDate: e.target.value || null })}
                />
              </Field>
              <Field label="Validade" htmlFor="doc-exp">
                <Input
                  id="doc-exp"
                  type="date"
                  value={form.expiryDate ?? ""}
                  onChange={(e) => setForm({ ...form, expiryDate: e.target.value || null })}
                />
              </Field>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setForm(null)}>
              Cancelar
            </Button>
            <Button
              disabled={save.isPending || !form?.documentNumber.trim()}
              onClick={() => form && save.mutate(form, { onSuccess: () => setForm(null) })}
            >
              Salvar
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </SectionCard>
  );
}

// =================================================================== relacionamentos

export function RelationshipsTab({ person, canEdit }: { person: PersonDetail; canEdit: boolean }) {
  const [open, setOpen] = useState(false);
  const [other, setOther] = useState<PersonSummary | null>(null);
  const [type, setType] = useState<RelationshipType>("GUARDIAN");
  const [direction, setDirection] = useState<"OUT" | "IN">("OUT");
  const add = usePeopleMutation<void>(
    (c) =>
      PeopleService.addRelationship(c, person.id, {
        relatedPersonId: other!.id,
        relationshipType: type,
        direction,
      }),
    "Relacionamento salvo",
  );
  const remove = usePeopleMutation<string>(
    (c, id) => PeopleService.deleteRelationship(c, person.id, id),
    "Relacionamento removido",
  );

  return (
    <SectionCard
      title="Relacionamentos"
      description="Responsável, cônjuge, sócio ou representante legal — sempre outra pessoa cadastrada."
      actions={
        canEdit && (
          <Button size="sm" variant="outline" onClick={() => setOpen(true)}>
            <Plus className="mr-1 h-4 w-4" /> Vincular pessoa
          </Button>
        )
      }
    >
      {person.relationships.length === 0 ? (
        <p className="text-sm text-muted-foreground">Nenhum relacionamento.</p>
      ) : (
        <ul className="divide-y text-sm">
          {person.relationships.map((r) => (
            <li key={r.id} className="flex items-center gap-3 py-2">
              <span className="w-44 text-muted-foreground">
                {r.direction === "OUT"
                  ? RELATIONSHIP_LABELS[r.relationshipType].out
                  : RELATIONSHIP_LABELS[r.relationshipType].in}
              </span>
              <a
                className="font-medium text-primary hover:underline"
                href={`/people/${r.relatedPersonId}`}
              >
                {r.relatedName}
              </a>
              {canEdit && (
                <Button
                  className="ml-auto"
                  size="icon"
                  variant="ghost"
                  aria-label="Remover relacionamento"
                  onClick={() => remove.mutate(r.id)}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              )}
            </li>
          ))}
        </ul>
      )}
      <Dialog
        open={open}
        onOpenChange={(o) => {
          setOpen(o);
          if (!o) setOther(null);
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Vincular pessoa</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <Field label="Vínculo" htmlFor="rel-type">
              <NativeSelect
                id="rel-type"
                value={type}
                onChange={(e) => setType(e.target.value as RelationshipType)}
              >
                {Object.entries(RELATIONSHIP_LABELS).map(([k, l]) => (
                  <option key={k} value={k}>
                    {l.out}
                  </option>
                ))}
              </NativeSelect>
            </Field>
            <fieldset className="flex flex-wrap gap-4 text-sm">
              <legend className="sr-only">Direção</legend>
              <label className="flex items-center gap-2">
                <input
                  type="radio"
                  checked={direction === "OUT"}
                  onChange={() => setDirection("OUT")}
                />
                A pessoa escolhida é {RELATIONSHIP_LABELS[type].out.toLowerCase()} de{" "}
                {person.name.split(" ")[0]}
              </label>
              <label className="flex items-center gap-2">
                <input
                  type="radio"
                  checked={direction === "IN"}
                  onChange={() => setDirection("IN")}
                />
                {person.name.split(" ")[0]} é {RELATIONSHIP_LABELS[type].out.toLowerCase()} da
                pessoa escolhida
              </label>
            </fieldset>
            <Field label="Pessoa" htmlFor="rel-person">
              <PersonPicker
                id="rel-person"
                excludeId={person.id}
                selected={other}
                onSelect={setOther}
              />
            </Field>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setOpen(false)}>
              Cancelar
            </Button>
            <Button
              disabled={!other || add.isPending}
              onClick={() =>
                add.mutate(undefined, {
                  onSuccess: () => {
                    setOpen(false);
                    setOther(null);
                  },
                })
              }
            >
              Vincular
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </SectionCard>
  );
}

// =================================================================== dados bancários

export function BankTab({ person }: { person: PersonDetail }) {
  const { data, isLoading, error } = useBankAccounts(person.id, true);
  const [form, setForm] = useState<BankAccountRequest | null>(null);
  const save = usePeopleMutation<BankAccountRequest>(
    (c, d) => PeopleService.addBankAccount(c, person.id, d),
    "Conta salva",
  );
  const remove = usePeopleMutation<string>(
    (c, id) => PeopleService.deleteBankAccount(c, person.id, id),
    "Conta removida",
  );
  const empty: BankAccountRequest = {
    bankCode: "",
    branchNumber: "",
    accountNumber: "",
    accountDigit: "",
    accountType: "CHECKING",
    pixKey: "",
    pixKeyType: null,
    holderName: person.name,
  };
  return (
    <SectionCard
      title="Dados bancários"
      description="Guardados com criptografia. Visíveis só para quem tem permissão."
      actions={
        <Button size="sm" variant="outline" onClick={() => setForm(empty)}>
          <Plus className="mr-1 h-4 w-4" /> Conta / PIX
        </Button>
      }
    >
      {isLoading ? (
        <Loader2 className="h-4 w-4 animate-spin" />
      ) : error ? (
        <p className="text-sm text-destructive">{toHttpError(error).message}</p>
      ) : (data ?? []).length === 0 ? (
        <p className="text-sm text-muted-foreground">Nenhuma conta cadastrada.</p>
      ) : (
        <ul className="divide-y text-sm">
          {(data ?? []).map((b) => (
            <li key={b.id} className="flex flex-wrap items-center gap-3 py-2">
              {b.primary && <Star className="h-4 w-4 text-amber-500" aria-label="Principal" />}
              <span>
                Banco {b.bankCode ?? "—"} · Ag. {b.branchNumber ?? "—"} · Conta{" "}
                {b.accountNumber ?? "—"}
                {b.accountDigit ? `-${b.accountDigit}` : ""}
              </span>
              {b.pixKey && <span className="text-muted-foreground">PIX: {b.pixKey}</span>}
              <Button
                className="ml-auto"
                size="icon"
                variant="ghost"
                aria-label="Remover conta"
                onClick={() => remove.mutate(b.id)}
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            </li>
          ))}
        </ul>
      )}
      <Dialog open={form !== null} onOpenChange={(o) => !o && setForm(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Conta bancária / PIX</DialogTitle>
          </DialogHeader>
          {form && (
            <div className="grid gap-4 sm:grid-cols-3">
              <Field label="Banco (código)" htmlFor="bk-bank">
                <Input
                  id="bk-bank"
                  maxLength={3}
                  value={form.bankCode ?? ""}
                  onChange={(e) => setForm({ ...form, bankCode: e.target.value })}
                />
              </Field>
              <Field label="Agência" htmlFor="bk-branch">
                <Input
                  id="bk-branch"
                  value={form.branchNumber ?? ""}
                  onChange={(e) => setForm({ ...form, branchNumber: e.target.value })}
                />
              </Field>
              <Field label="Tipo" htmlFor="bk-type">
                <NativeSelect
                  id="bk-type"
                  value={form.accountType ?? ""}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      accountType: (e.target.value || null) as BankAccountRequest["accountType"],
                    })
                  }
                >
                  <option value="CHECKING">Corrente</option>
                  <option value="SAVINGS">Poupança</option>
                  <option value="PAYMENT">Pagamento</option>
                  <option value="OTHER">Outra</option>
                </NativeSelect>
              </Field>
              <Field label="Conta" htmlFor="bk-acc" className="sm:col-span-2">
                <Input
                  id="bk-acc"
                  value={form.accountNumber ?? ""}
                  onChange={(e) => setForm({ ...form, accountNumber: e.target.value })}
                />
              </Field>
              <Field label="Dígito" htmlFor="bk-dig">
                <Input
                  id="bk-dig"
                  maxLength={2}
                  value={form.accountDigit ?? ""}
                  onChange={(e) => setForm({ ...form, accountDigit: e.target.value })}
                />
              </Field>
              <Field label="Tipo de chave PIX" htmlFor="bk-pixt">
                <NativeSelect
                  id="bk-pixt"
                  value={form.pixKeyType ?? ""}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      pixKeyType: (e.target.value || null) as BankAccountRequest["pixKeyType"],
                    })
                  }
                >
                  <option value="">—</option>
                  <option value="CPF">CPF</option>
                  <option value="CNPJ">CNPJ</option>
                  <option value="EMAIL">E-mail</option>
                  <option value="PHONE">Telefone</option>
                  <option value="RANDOM">Aleatória</option>
                </NativeSelect>
              </Field>
              <Field label="Chave PIX" htmlFor="bk-pix" className="sm:col-span-2">
                <Input
                  id="bk-pix"
                  value={form.pixKey ?? ""}
                  onChange={(e) => setForm({ ...form, pixKey: e.target.value })}
                />
              </Field>
              <Field label="Titular" htmlFor="bk-holder" className="sm:col-span-3">
                <Input
                  id="bk-holder"
                  value={form.holderName ?? ""}
                  onChange={(e) => setForm({ ...form, holderName: e.target.value })}
                />
              </Field>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setForm(null)}>
              Cancelar
            </Button>
            <Button
              disabled={save.isPending}
              onClick={() => form && save.mutate(form, { onSuccess: () => setForm(null) })}
            >
              Salvar
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </SectionCard>
  );
}

// =================================================================== histórico

const OPERATION_LABELS: Record<string, string> = {
  CREATE: "Cadastro criado",
  UPDATE: "Alteração",
  SOFT_DELETE: "Exclusão",
  RESTORE: "Restaurado",
  MERGE: "Unificação",
  ROLE_ENABLED: "Papel habilitado",
  ROLE_DISABLED: "Papel removido",
};

export function HistoryTab({ personId }: { personId: string }) {
  const { data, isLoading } = usePersonHistory(personId);
  const show = (v: Record<string, unknown> | null) =>
    v
      ? Object.entries(v)
          .map(([k, val]) => `${k}: ${val === null ? "—" : String(val)}`)
          .join(" · ")
      : "";
  return (
    <SectionCard title="Histórico" description="Quem alterou, quando e o que mudou.">
      {isLoading ? (
        <Loader2 className="h-4 w-4 animate-spin" />
      ) : (data ?? []).length === 0 ? (
        <p className="text-sm text-muted-foreground">
          Sem alterações registradas pelo cadastro novo.
        </p>
      ) : (
        <ol className="space-y-3 text-sm">
          {(data ?? []).map((h) => (
            <li key={h.id} className="rounded-md border p-3">
              <div className="flex flex-wrap gap-2">
                <span className="font-semibold">
                  {OPERATION_LABELS[h.operation] ?? h.operation}
                </span>
                <span className="text-muted-foreground">
                  {formatDateTime(h.occurredAt)} · {h.userName}
                </span>
              </div>
              {h.oldValues && (
                <div className="text-muted-foreground line-through decoration-1">
                  {show(h.oldValues)}
                </div>
              )}
              {h.newValues && <div>{show(h.newValues)}</div>}
              {h.reason && <div className="text-muted-foreground">Motivo: {h.reason}</div>}
            </li>
          ))}
        </ol>
      )}
    </SectionCard>
  );
}
