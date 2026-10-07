"use client";

import { useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { PasswordRequirements } from "@/features/identity/auth/components/PasswordRequirements";
import {
  invitationSignupSchema,
  type InvitationSignupFormData,
} from "../schemas/invitation-signup.schema";
import { InvitationService } from "../services/invitation.service";
import { describeInvitationError, type InvitationError } from "../lib/invitation-errors";

type InvitationSignupFormProps = {
  token: string;
  email: string;
  inviteeName: string | null;
  onRegistered: () => void;
  /** Erros que mudam a tela inteira (convite já usado, conta existente...). */
  onBlockingError: (error: InvitationError) => void;
};

/**
 * Cadastro de quem ainda não tem conta. O e-mail é só exibido: o corpo enviado
 * ao backend tem apenas token, nome e senha.
 */
export function InvitationSignupForm({
  token,
  email,
  inviteeName,
  onRegistered,
  onBlockingError,
}: InvitationSignupFormProps) {
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  // Trava síncrona: dois cliques antes do re-render não geram dois POSTs.
  const inFlight = useRef(false);

  const form = useForm<InvitationSignupFormData>({
    resolver: zodResolver(invitationSignupSchema),
    defaultValues: {
      name: inviteeName ?? "",
      password: "",
      confirmPassword: "",
    },
  });

  const watchPassword = form.watch("password");

  async function onSubmit(data: InvitationSignupFormData) {
    if (inFlight.current) return;
    inFlight.current = true;
    setSubmitting(true);
    setFormError(null);
    try {
      await InvitationService.register({ token, name: data.name.trim(), password: data.password });
      form.reset({ name: data.name, password: "", confirmPassword: "" });
      onRegistered();
    } catch (error) {
      const described = describeInvitationError(error);
      if (described.kind === "rejected") {
        setFormError(described.message);
      } else if (described.kind === "network") {
        setFormError(
          "Não foi possível falar com o servidor. Verifique sua conexão e tente novamente.",
        );
      } else if (described.kind === "unexpected") {
        setFormError("Não foi possível criar sua conta agora. Tente novamente em instantes.");
      } else {
        onBlockingError(described);
      }
      inFlight.current = false;
      setSubmitting(false);
    }
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-3" noValidate>
        <div className="space-y-2">
          <Label htmlFor="invitation-email">E-mail</Label>
          <Input
            id="invitation-email"
            type="email"
            value={email}
            readOnly
            aria-readonly="true"
            tabIndex={-1}
            className="cursor-not-allowed bg-crm-surface-bg"
          />
          <p className="text-xs text-crm-text-secondary">O e-mail é definido pelo convite.</p>
        </div>
        <FormField
          control={form.control}
          name="name"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Nome</FormLabel>
              <FormControl>
                <Input placeholder="Seu nome" autoComplete="name" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="password"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Senha</FormLabel>
              <FormControl>
                <Input
                  placeholder="Digite uma senha forte"
                  type="password"
                  autoComplete="new-password"
                  {...field}
                />
              </FormControl>
              <PasswordRequirements value={watchPassword} />
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="confirmPassword"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Confirmar senha</FormLabel>
              <FormControl>
                <Input
                  placeholder="Repita a senha"
                  type="password"
                  autoComplete="new-password"
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        {formError && (
          <p role="alert" className="text-sm text-crm-danger">
            {formError}
          </p>
        )}
        <Button type="submit" className="w-full" disabled={submitting}>
          {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          {submitting ? "Criando conta..." : "Criar conta e entrar"}
        </Button>
      </form>
    </Form>
  );
}
