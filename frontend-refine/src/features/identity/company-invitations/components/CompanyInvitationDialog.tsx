"use client";

import { useState } from "react";
import { MailPlus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  MODULE_GROUPS,
  MODULE_OPTIONS,
  SEGMENT_DEFAULT_MODULES,
  SEGMENT_OPTIONS,
  type CompanyModuleName,
  type CompanySegment,
} from "@/features/identity/modules/types/modules.types";
import { tenantPlanLabels } from "@/features/identity/tenants/schemas/tenant.schema";
import { InvitationLinkPanel } from "@/features/identity/invitations/components/InvitationLinkPanel";
import { useCreateCompanyInvitation } from "../hooks";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import {
  MailNotConfiguredNotice,
  useCompanyMailStatus,
} from "@/features/masterdata/company/mail/useCompanyMailStatus";
import type { CompanyInvitationLink, CompanyPrefill, InvitedCompanyDetails } from "../types";

const MODULE_LABELS = new Map(MODULE_OPTIONS.map((option) => [option.value, option.label]));
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** Campos que o admin pode adiantar; o convidado confere e completa ao aceitar. */
const PREFILL_FIELDS: { key: keyof InvitedCompanyDetails; label: string }[] = [
  { key: "legalName", label: "Razão social" },
  { key: "tradingName", label: "Nome fantasia" },
  { key: "cnpj", label: "CNPJ ou CPF" },
  { key: "email", label: "E-mail da empresa" },
  { key: "phone", label: "Telefone" },
  { key: "addressZipCode", label: "CEP" },
  { key: "addressStreet", label: "Rua" },
  { key: "addressNumber", label: "Número" },
  { key: "addressComplement", label: "Complemento" },
  { key: "addressNeighborhood", label: "Bairro" },
  { key: "addressCity", label: "Cidade" },
  { key: "addressState", label: "Estado (UF)" },
];

/** Só os campos preenchidos; `undefined` quando nada foi adiantado. */
function filledPrefill(prefill: CompanyPrefill): CompanyPrefill | undefined {
  const entries = Object.entries(prefill)
    .map(([key, value]) => [key, value?.trim() ?? ""] as const)
    .filter(([, value]) => value);
  return entries.length ? Object.fromEntries(entries) : undefined;
}

/**
 * Convite para criar uma empresa (só CRM_ADMIN). Especialidade, módulos, plano
 * e limite de usuários ficam fixos no convite; o convidado só preenche a conta
 * e os dados da empresa.
 */
export function CompanyInvitationDialog() {
  const [open, setOpen] = useState(false);
  const [email, setEmail] = useState("");
  const [name, setName] = useState("");
  const [segment, setSegment] = useState<CompanySegment>("GENERIC");
  const [modules, setModules] = useState<CompanyModuleName[]>([...SEGMENT_DEFAULT_MODULES.GENERIC]);
  const [plan, setPlan] = useState("starter");
  const [maxUsers, setMaxUsers] = useState("5");
  const [sendEmail, setSendEmail] = useState(true);
  const [prefill, setPrefill] = useState<CompanyPrefill>({});
  const [error, setError] = useState<string | null>(null);
  const [created, setCreated] = useState<CompanyInvitationLink | null>(null);
  const create = useCreateCompanyInvitation();
  const { user } = useAuth();
  // O convite sai do e-mail da empresa de quem convida.
  const mailStatus = useCompanyMailStatus(open ? user?.companyId : null);
  const mailMissing = sendEmail && mailStatus.data === false;

  function reset() {
    setEmail("");
    setName("");
    setSegment("GENERIC");
    setModules([...SEGMENT_DEFAULT_MODULES.GENERIC]);
    setPlan("starter");
    setMaxUsers("5");
    setSendEmail(true);
    setPrefill({});
    setError(null);
    setCreated(null);
  }

  function changeSegment(value: CompanySegment) {
    setSegment(value);
    setModules([...SEGMENT_DEFAULT_MODULES[value]]);
  }

  function toggle(module: CompanyModuleName, checked: boolean) {
    setModules((current) =>
      checked
        ? [...current.filter((m) => m !== module), module]
        : current.filter((m) => m !== module),
    );
  }

  function submit() {
    const users = Number(maxUsers);
    if (!EMAIL_PATTERN.test(email.trim())) return setError("Informe um e-mail válido.");
    if (modules.length === 0) return setError("Selecione ao menos um módulo.");
    if (!Number.isInteger(users) || users < 1) return setError("Limite de usuários inválido.");
    setError(null);
    create.mutate(
      {
        email: email.trim(),
        name: name.trim() || undefined,
        segment,
        enabledModules: modules,
        plan,
        maxUsers: users,
        sendEmail,
        company: filledPrefill(prefill),
      },
      { onSuccess: setCreated },
    );
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (!next) reset();
      }}
    >
      <DialogTrigger asChild>
        <Button variant="outline">
          <MailPlus className="mr-1 h-4 w-4" />
          Convidar empresa
        </Button>
      </DialogTrigger>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{created ? "Convite criado" : "Convidar para criar empresa"}</DialogTitle>
          <DialogDescription>
            {created
              ? `Envie o link para ${created.invitation.email}.`
              : "O convidado cria a conta, confere os dados da empresa e entra como administrador."}
          </DialogDescription>
        </DialogHeader>

        {created ? (
          <InvitationLinkPanel
            link={created}
            companyName="sua nova empresa"
            onClose={() => setOpen(false)}
          />
        ) : (
          <div className="space-y-5">
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="company-invite-email">E-mail do convidado</Label>
                <Input
                  id="company-invite-email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="dono@empresa.com"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="company-invite-name">Nome (opcional)</Label>
                <Input
                  id="company-invite-name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="company-invite-segment">Especialidade</Label>
                <Select value={segment} onValueChange={(v) => changeSegment(v as CompanySegment)}>
                  <SelectTrigger id="company-invite-segment">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {SEGMENT_OPTIONS.map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="company-invite-plan">Plano</Label>
                  <Select value={plan} onValueChange={setPlan}>
                    <SelectTrigger id="company-invite-plan">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {Object.entries(tenantPlanLabels).map(([value, label]) => (
                        <SelectItem key={value} value={value}>
                          {label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="company-invite-max-users">Usuários</Label>
                  <Input
                    id="company-invite-max-users"
                    type="number"
                    min={1}
                    value={maxUsers}
                    onChange={(e) => setMaxUsers(e.target.value)}
                  />
                </div>
              </div>
            </div>

            <fieldset className="space-y-3">
              <legend className="text-sm font-medium">Módulos habilitados</legend>
              {MODULE_GROUPS.map((group) => (
                <div key={group.title} className="space-y-2">
                  <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    {group.title}
                  </p>
                  <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                    {group.modules.map((module) => (
                      <div key={module} className="flex items-center gap-2">
                        <Checkbox
                          id={`company-invite-module-${module}`}
                          checked={modules.includes(module)}
                          onCheckedChange={(checked) => toggle(module, checked === true)}
                        />
                        <Label htmlFor={`company-invite-module-${module}`}>
                          {MODULE_LABELS.get(module)}
                        </Label>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </fieldset>

            <details className="rounded-md border border-border p-3">
              <summary className="cursor-pointer text-sm font-medium">
                Adiantar dados da empresa (opcional)
              </summary>
              <p className="mt-2 text-xs text-muted-foreground">
                O convidado recebe estes campos já preenchidos e só confere. O documento pode ser
                CNPJ ou CPF, e também pode ficar em branco.
              </p>
              <div className="mt-3 grid gap-3 sm:grid-cols-2">
                {PREFILL_FIELDS.map((field) => (
                  <div key={field.key} className="space-y-1">
                    <Label htmlFor={`company-invite-prefill-${field.key}`}>{field.label}</Label>
                    <Input
                      id={`company-invite-prefill-${field.key}`}
                      value={prefill[field.key] ?? ""}
                      onChange={(e) => setPrefill({ ...prefill, [field.key]: e.target.value })}
                    />
                  </div>
                ))}
              </div>
            </details>

            <div className="flex items-center gap-2">
              <Checkbox
                id="company-invite-send-email"
                checked={sendEmail}
                onCheckedChange={(checked) => setSendEmail(checked === true)}
              />
              <Label htmlFor="company-invite-send-email">Enviar convite por e-mail</Label>
            </div>

            {mailMissing && <MailNotConfiguredNotice />}

            {error && (
              <p role="alert" className="text-sm text-destructive">
                {error}
              </p>
            )}

            <div className="flex justify-end gap-2">
              <Button type="button" variant="ghost" onClick={() => setOpen(false)}>
                Cancelar
              </Button>
              <Button type="button" onClick={submit} disabled={create.isPending || mailMissing}>
                {create.isPending ? "Criando..." : "Criar convite"}
              </Button>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
