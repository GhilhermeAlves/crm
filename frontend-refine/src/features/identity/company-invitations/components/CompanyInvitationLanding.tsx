"use client";

import { useRef, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, Building2, CheckCircle2, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PasswordRequirements } from "@/features/identity/auth/components/PasswordRequirements";
import { PASSWORD_PATTERN } from "@/features/identity/auth/schemas/auth.schema";
import { useMe } from "@/features/identity/auth/hooks/useAuthMutations";
import { INVITATION_STATUS_MESSAGE } from "@/features/identity/invitations/lib/invitation-errors";
import { SEGMENT_OPTIONS } from "@/features/identity/modules/types/modules.types";
import { loginWithGateway, logoutWithGateway } from "@/lib/gateway-auth";
import { ROUTES, companyInvitationPath } from "@/lib/constants";
import { CompanyInvitationService } from "../service";
import type { CompanyInvitationPreview, InvitedCompanyDetails } from "../types";

const SEGMENT_LABEL = new Map<string, string>(
  SEGMENT_OPTIONS.map((option) => [option.value, option.label]),
);

const EMPTY_COMPANY: InvitedCompanyDetails = {
  legalName: "",
  tradingName: "",
  cnpj: "",
  email: "",
  phone: "",
  addressZipCode: "",
  addressStreet: "",
  addressNumber: "",
  addressComplement: "",
  addressNeighborhood: "",
  addressCity: "",
  addressState: "",
};

const COMPANY_FIELDS: { key: keyof InvitedCompanyDetails; label: string; optional?: boolean }[] = [
  { key: "legalName", label: "Razão social" },
  { key: "tradingName", label: "Nome fantasia" },
  { key: "cnpj", label: "CNPJ" },
  { key: "email", label: "E-mail da empresa" },
  { key: "phone", label: "Telefone" },
  { key: "addressZipCode", label: "CEP" },
  { key: "addressStreet", label: "Rua" },
  { key: "addressNumber", label: "Número" },
  { key: "addressComplement", label: "Complemento", optional: true },
  { key: "addressNeighborhood", label: "Bairro" },
  { key: "addressCity", label: "Cidade" },
  { key: "addressState", label: "Estado (UF)" },
];

type HttpError = {
  response?: { status?: number; data?: { message?: unknown; invitationStatus?: unknown } };
};

function errorMessage(error: unknown): string {
  const response = (error as HttpError)?.response;
  if (!response) return "Não foi possível falar com o servidor. Tente novamente.";
  const message = response.data?.message;
  if (typeof message === "string" && message.trim()) return message;
  return "Não foi possível concluir agora. Confira os dados e tente novamente.";
}

/**
 * Convite de criação de empresa (/convite-empresa/{token}). Pública: quem não
 * tem conta cria a conta e a empresa de uma vez; quem tem entra e informa os
 * dados da empresa. Especialidade, módulos e plano vêm fixos do convite.
 */
export function CompanyInvitationLanding({ token }: { token: string }) {
  const preview = useQuery({
    queryKey: ["company-invitation-preview", token],
    queryFn: () => CompanyInvitationService.preview(token),
    retry: false,
    refetchOnWindowFocus: false,
  });
  const [done, setDone] = useState(false);

  if (preview.isLoading) {
    return (
      <Centered icon={<Loader2 className="h-6 w-6 animate-spin" />} text="Carregando convite..." />
    );
  }
  if (preview.isError || !preview.data) {
    const status = (preview.error as HttpError)?.response?.status;
    return (
      <Centered
        icon={<AlertTriangle className="h-8 w-8 text-crm-warning" />}
        title="Convite inválido"
        text={
          status === 404
            ? "Este link de convite não é válido. Confira o link recebido."
            : "Não foi possível abrir o convite. Tente novamente em instantes."
        }
      />
    );
  }

  const data = preview.data;
  if (data.status !== "PENDING") {
    return (
      <Centered
        icon={<AlertTriangle className="h-8 w-8 text-crm-warning" />}
        title={INVITATION_STATUS_MESSAGE[data.status]}
        text="Se precisar de um novo convite, fale com o administrador do CRM."
      >
        {data.status === "ACCEPTED" && (
          <Button className="w-full" onClick={() => loginWithGateway(ROUTES.DASHBOARD)}>
            Entrar
          </Button>
        )}
      </Centered>
    );
  }

  if (done) {
    return (
      <Centered
        icon={<CheckCircle2 className="h-8 w-8 text-crm-success" />}
        title="Empresa criada"
        text="Sua empresa está pronta. Entre para começar a usar o CRM."
      >
        <Button className="w-full" onClick={() => loginWithGateway(ROUTES.DASHBOARD)}>
          Entrar no CRM
        </Button>
      </Centered>
    );
  }

  return (
    <div className="space-y-5">
      <Summary preview={data} />
      {data.hasAccount ? (
        <ExistingAccount token={token} preview={data} />
      ) : (
        <SignupForm
          token={token}
          preview={data}
          onDone={() => {
            setDone(true);
            loginWithGateway(ROUTES.DASHBOARD);
          }}
        />
      )}
    </div>
  );
}

function Summary({ preview }: { preview: CompanyInvitationPreview }) {
  return (
    <div className="space-y-2 text-center">
      <div className="mx-auto flex h-10 w-10 items-center justify-center rounded-full bg-crm-surface-bg">
        <Building2 className="h-5 w-5 text-crm-primary" />
      </div>
      <h1 className="text-xl font-semibold tracking-tight text-crm-text">
        Convite para criar sua empresa
      </h1>
      <p className="break-all text-sm font-medium text-crm-text">{preview.email}</p>
      <p className="text-sm text-crm-text-secondary">
        {SEGMENT_LABEL.get(preview.segment) ?? preview.segment} · até {preview.maxUsers} usuários ·{" "}
        {preview.enabledModules.length} módulos
      </p>
    </div>
  );
}

function CompanyFields({
  value,
  onChange,
}: {
  value: InvitedCompanyDetails;
  onChange: (next: InvitedCompanyDetails) => void;
}) {
  return (
    <fieldset className="space-y-3">
      <legend className="text-sm font-medium text-crm-text">Dados da empresa</legend>
      <div className="grid gap-3 sm:grid-cols-2">
        {COMPANY_FIELDS.map((field) => (
          <div key={field.key} className="space-y-1">
            <Label htmlFor={`company-${field.key}`}>
              {field.label}
              {field.optional ? " (opcional)" : ""}
            </Label>
            <Input
              id={`company-${field.key}`}
              value={value[field.key] ?? ""}
              onChange={(e) => onChange({ ...value, [field.key]: e.target.value })}
            />
          </div>
        ))}
      </div>
    </fieldset>
  );
}

function missingCompanyField(company: InvitedCompanyDetails): string | null {
  const missing = COMPANY_FIELDS.find((f) => !f.optional && !company[f.key]?.trim());
  return missing ? `Preencha: ${missing.label}.` : null;
}

function SignupForm({
  token,
  preview,
  onDone,
}: {
  token: string;
  preview: CompanyInvitationPreview;
  onDone: () => void;
}) {
  const [name, setName] = useState(preview.inviteeName ?? "");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [company, setCompany] = useState<InvitedCompanyDetails>({
    ...EMPTY_COMPANY,
    email: preview.email,
  });
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  // Trava síncrona: dois cliques antes do re-render não geram dois POSTs.
  const inFlight = useRef(false);

  async function submit() {
    if (inFlight.current) return;
    if (name.trim().length < 3) return setError("Nome deve ter no mínimo 3 caracteres.");
    if (!PASSWORD_PATTERN.test(password))
      return setError("Sua senha ainda não atende aos requisitos.");
    if (password !== confirm) return setError("Senhas não conferem.");
    const missing = missingCompanyField(company);
    if (missing) return setError(missing);
    setError(null);
    inFlight.current = true;
    setSubmitting(true);
    try {
      await CompanyInvitationService.register({ token, name: name.trim(), password, company });
      onDone();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      inFlight.current = false;
      setSubmitting(false);
    }
  }

  return (
    <div className="space-y-5">
      <fieldset className="space-y-3">
        <legend className="text-sm font-medium text-crm-text">Sua conta</legend>
        <div className="space-y-1">
          <Label htmlFor="signup-name">Seu nome</Label>
          <Input id="signup-name" value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div className="grid gap-3 sm:grid-cols-2">
          <div className="space-y-1">
            <Label htmlFor="signup-password">Senha</Label>
            <Input
              id="signup-password"
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          <div className="space-y-1">
            <Label htmlFor="signup-confirm">Confirmar senha</Label>
            <Input
              id="signup-confirm"
              type="password"
              autoComplete="new-password"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
            />
          </div>
        </div>
        <PasswordRequirements value={password} />
      </fieldset>
      <CompanyFields value={company} onChange={setCompany} />
      {error && (
        <p role="alert" className="text-sm text-crm-danger">
          {error}
        </p>
      )}
      <Button className="w-full" onClick={submit} disabled={submitting}>
        {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
        {submitting ? "Criando..." : "Criar conta e empresa"}
      </Button>
    </div>
  );
}

function ExistingAccount({ token, preview }: { token: string; preview: CompanyInvitationPreview }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const me = useMe(true);
  const [company, setCompany] = useState<InvitedCompanyDetails>({
    ...EMPTY_COMPANY,
    email: preview.email,
  });
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (me.isLoading) {
    return (
      <Centered
        icon={<Loader2 className="h-5 w-5 animate-spin" />}
        text="Verificando sua sessão..."
      />
    );
  }

  const user = me.data ?? null;
  if (!user) {
    return (
      <div className="space-y-4 text-center">
        <p className="text-sm text-crm-text-secondary">
          Este e-mail já tem conta. Entre com ela para criar a empresa.
        </p>
        <Button className="w-full" onClick={() => loginWithGateway(companyInvitationPath(token))}>
          Entrar
        </Button>
      </div>
    );
  }

  if (user.email?.toLowerCase() !== preview.email.toLowerCase()) {
    return (
      <div className="space-y-4 text-center">
        <p className="text-sm text-crm-text-secondary">
          Você está conectado como <span className="font-medium text-crm-text">{user.email}</span>,
          mas este convite é para <span className="font-medium text-crm-text">{preview.email}</span>
          . Saia e abra o link novamente com a conta correta.
        </p>
        <Button variant="outline" className="w-full" onClick={() => logoutWithGateway()}>
          Sair
        </Button>
      </div>
    );
  }

  async function submit() {
    if (submitting) return;
    const missing = missingCompanyField(company);
    if (missing) return setError(missing);
    setError(null);
    setSubmitting(true);
    try {
      await CompanyInvitationService.accept(token, company);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["me"] }),
        queryClient.invalidateQueries({ queryKey: ["me", "companies"] }),
      ]);
      router.push(ROUTES.DASHBOARD);
    } catch (err) {
      setError(errorMessage(err));
      setSubmitting(false);
    }
  }

  return (
    <div className="space-y-5">
      <CompanyFields value={company} onChange={setCompany} />
      {error && (
        <p role="alert" className="text-sm text-crm-danger">
          {error}
        </p>
      )}
      <Button className="w-full" onClick={submit} disabled={submitting}>
        {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
        {submitting ? "Criando..." : "Criar empresa"}
      </Button>
    </div>
  );
}

function Centered({
  icon,
  title,
  text,
  children,
}: {
  icon: ReactNode;
  title?: string;
  text: string;
  children?: ReactNode;
}) {
  return (
    <div className="flex flex-col items-center gap-3 py-6 text-center" role="status">
      <div className="text-crm-text-secondary">{icon}</div>
      {title && <h1 className="text-xl font-semibold text-crm-text">{title}</h1>}
      <p className="text-sm text-crm-text-secondary">{text}</p>
      {children && <div className="flex w-full flex-col gap-2 pt-2">{children}</div>}
    </div>
  );
}
