"use client";

import { useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, Building2, CheckCircle2, Loader2, MailX } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useMe } from "@/features/identity/auth/hooks/useAuthMutations";
import { loginWithGateway, logoutWithGateway } from "@/lib/gateway-auth";
import { ROUTES, invitationPath } from "@/lib/constants";
import { InvitationService } from "../services/invitation.service";
import type { InvitationPreview } from "../types/invitation.types";
import {
  INVITATION_STATUS_MESSAGE,
  describeInvitationError,
  type InvitationError,
} from "../lib/invitation-errors";
import { InvitationSignupForm } from "./InvitationSignupForm";

const ROLE_LABEL: Record<string, string> = {
  ADMIN: "Administrador",
  MANAGER: "Gerente",
  AGENT: "Agente",
  VIEWER: "Visualizador",
};

/** Erro que substitui a tela inteira; `null` enquanto o convite segue utilizável. */
type BlockingError = Exclude<InvitationError, { kind: "account-exists" } | { kind: "rejected" }>;

/**
 * Página oficial do convite (/convite/{token}). Pública: consulta a prévia e
 * decide entre criar conta, entrar com a conta existente ou explicar por que o
 * convite não pode mais ser usado. Regras e autorização ficam no backend.
 */
export function InvitationLanding({ token }: { token: string }) {
  const preview = useQuery({
    queryKey: ["invitation-preview", token],
    queryFn: () => InvitationService.preview(token),
    retry: false,
    refetchOnWindowFocus: false,
  });
  const [blocking, setBlocking] = useState<BlockingError | null>(null);
  const [accountExists, setAccountExists] = useState(false);
  const [registered, setRegistered] = useState(false);

  if (preview.isLoading) {
    return (
      <Centered icon={<Loader2 className="h-6 w-6 animate-spin" />} text="Carregando convite..." />
    );
  }

  if (preview.isError) {
    const error = describeInvitationError(preview.error);
    return <ErrorView error={toBlocking(error)} onRetry={() => preview.refetch()} />;
  }

  const data = preview.data as InvitationPreview;

  if (blocking) {
    return <ErrorView error={blocking} />;
  }
  if (data.status !== "PENDING") {
    return <ErrorView error={{ kind: "unusable", status: data.status }} />;
  }

  if (registered) {
    return (
      <Centered
        icon={<CheckCircle2 className="h-8 w-8 text-crm-success" />}
        title="Conta criada"
        text="Sua conta foi criada e você já faz parte da empresa. Redirecionando para o login..."
      >
        <Button className="w-full" onClick={() => loginWithGateway(ROUTES.DASHBOARD)}>
          Entrar no CRM
        </Button>
      </Centered>
    );
  }

  return (
    <div className="space-y-5">
      <InvitationSummary preview={data} />
      {data.hasAccount || accountExists ? (
        <ExistingAccount token={token} preview={data} onBlockingError={setBlocking} />
      ) : (
        <InvitationSignupForm
          token={token}
          email={data.email}
          inviteeName={data.inviteeName}
          onRegistered={() => {
            setRegistered(true);
            // Conta criada no Keycloak + CRM e convite aceito: segue para o
            // login do gateway (senha recém-criada), que leva ao CRM.
            loginWithGateway(ROUTES.DASHBOARD);
          }}
          onBlockingError={(error) => {
            if (error.kind === "account-exists") {
              setAccountExists(true);
            } else {
              setBlocking(toBlocking(error));
            }
          }}
        />
      )}
    </div>
  );
}

function toBlocking(error: InvitationError): BlockingError {
  if (error.kind === "account-exists" || error.kind === "rejected") {
    return { kind: "unexpected" };
  }
  return error;
}

function InvitationSummary({ preview }: { preview: InvitationPreview }) {
  return (
    <div className="space-y-2 text-center">
      <div className="mx-auto flex h-10 w-10 items-center justify-center rounded-full bg-crm-surface-bg">
        <Building2 className="h-5 w-5 text-crm-primary" />
      </div>
      <h1 className="text-xl font-semibold tracking-tight text-crm-text">
        Convite para {preview.companyName}
      </h1>
      <dl className="space-y-1 text-sm text-crm-text-secondary">
        {preview.inviteeName && (
          <div>
            <dt className="sr-only">Nome</dt>
            <dd>{preview.inviteeName}</dd>
          </div>
        )}
        <div>
          <dt className="sr-only">E-mail</dt>
          <dd className="break-all font-medium text-crm-text">{preview.email}</dd>
        </div>
        <div>
          <dt className="inline">Perfil: </dt>
          <dd className="inline">{ROLE_LABEL[preview.role] ?? preview.role}</dd>
        </div>
      </dl>
    </div>
  );
}

function ExistingAccount({
  token,
  preview,
  onBlockingError,
}: {
  token: string;
  preview: InvitationPreview;
  onBlockingError: (error: BlockingError) => void;
}) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const me = useMe(true);
  const [action, setAction] = useState<"accept" | "decline" | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

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
          Esta conta já existe. Entre com sua conta para continuar com o convite.
        </p>
        <Button className="w-full" onClick={() => loginWithGateway(invitationPath(token))}>
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
          . Saia e abra o link do convite novamente para entrar com a conta correta.
        </p>
        <Button variant="outline" className="w-full" onClick={() => logoutWithGateway()}>
          Sair
        </Button>
      </div>
    );
  }

  const handle = async (next: "accept" | "decline") => {
    if (action) return;
    setAction(next);
    setActionError(null);
    try {
      if (next === "accept") {
        await InvitationService.accept(token);
      } else {
        await InvitationService.decline(token);
      }
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["me"] }),
        queryClient.invalidateQueries({ queryKey: ["me", "companies"] }),
      ]);
      router.push(ROUTES.DASHBOARD);
    } catch (error) {
      const described = describeInvitationError(error);
      if (described.kind === "rejected") {
        setActionError(described.message);
      } else if (described.kind === "network") {
        setActionError(
          "Não foi possível falar com o servidor. Verifique sua conexão e tente novamente.",
        );
      } else {
        onBlockingError(toBlocking(described));
      }
      setAction(null);
    }
  };

  return (
    <div className="space-y-4">
      <p className="text-center text-sm text-crm-text-secondary">
        Ao aceitar, a empresa {preview.companyName} passa a aparecer na sua lista de empresas.
      </p>
      {actionError && (
        <p role="alert" className="text-center text-sm text-crm-danger">
          {actionError}
        </p>
      )}
      <div className="flex flex-col-reverse gap-3 sm:flex-row">
        <Button
          variant="outline"
          className="w-full"
          disabled={action !== null}
          onClick={() => handle("decline")}
        >
          <MailX className="mr-2 h-4 w-4" /> Recusar
        </Button>
        <Button className="w-full" disabled={action !== null} onClick={() => handle("accept")}>
          {action === "accept" && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          {action === "accept" ? "Aceitando..." : "Aceitar convite"}
        </Button>
      </div>
    </div>
  );
}

function ErrorView({ error, onRetry }: { error: BlockingError; onRetry?: () => void }) {
  let title = "Não foi possível abrir o convite";
  let text = "Ocorreu um erro inesperado. Tente novamente em instantes.";
  let showLogin = false;

  if (error.kind === "unusable") {
    title = INVITATION_STATUS_MESSAGE[error.status];
    if (error.status === "ACCEPTED") {
      text = "Se você já aceitou, entre na sua conta para acessar o CRM.";
      showLogin = true;
    } else if (error.status === "EXPIRED") {
      text = "Peça um novo convite ao administrador da empresa.";
    } else {
      text = "Se precisar de acesso, fale com o administrador da empresa.";
    }
  } else if (error.kind === "not-found") {
    title = "Convite inválido";
    text = "Este link de convite não é válido. Confira o link recebido por e-mail.";
  } else if (error.kind === "network") {
    text = "Não foi possível falar com o servidor. Verifique sua conexão e tente novamente.";
  }

  return (
    <Centered
      icon={<AlertTriangle className="h-8 w-8 text-crm-warning" />}
      title={title}
      text={text}
    >
      {showLogin && (
        <Button className="w-full" onClick={() => loginWithGateway(ROUTES.DASHBOARD)}>
          Entrar
        </Button>
      )}
      {onRetry && error.kind !== "unusable" && error.kind !== "not-found" && (
        <Button variant="outline" className="w-full" onClick={onRetry}>
          Tentar novamente
        </Button>
      )}
    </Centered>
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
