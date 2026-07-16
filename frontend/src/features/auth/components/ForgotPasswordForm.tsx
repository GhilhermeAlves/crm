"use client";

import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2, CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { forgotPasswordSchema, type ForgotPasswordFormData } from "../schemas/auth.schema";
import { useForgotPassword } from "../hooks/useAuthMutations";
import { ROUTES } from "@/lib/constants";
import { useState } from "react";

export function ForgotPasswordForm() {
  const [sent, setSent] = useState(false);
  const forgotPasswordMutation = useForgotPassword();

  const form = useForm<ForgotPasswordFormData>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: {
      email: "",
    },
  });

  function onSubmit(data: ForgotPasswordFormData) {
    forgotPasswordMutation.mutate(data, {
      onSuccess: () => setSent(true),
      onError: () => setSent(true),
    });
  }

  if (sent) {
    return (
      <div className="space-y-4 text-center">
        <CheckCircle2 className="mx-auto h-12 w-12 text-green-500" />
        <h3 className="text-lg font-semibold">Verifique seu email</h3>
        <p className="text-sm text-muted-foreground">
          Se o email existir em nossa base, você receberá um link para redefinir sua senha.
        </p>
        <Link href={ROUTES.LOGIN}>
          <Button variant="outline" className="w-full">
            Voltar para o login
          </Button>
        </Link>
      </div>
    );
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        <p className="text-sm text-muted-foreground">
          Informe seu email para receber um link de recuperação de senha.
        </p>
        <FormField
          control={form.control}
          name="email"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Email</FormLabel>
              <FormControl>
                <Input placeholder="seu@email.com" type="email" autoComplete="email" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <Button type="submit" className="w-full" disabled={forgotPasswordMutation.isPending}>
          {forgotPasswordMutation.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          Enviar link de recuperação
        </Button>
      </form>
      <div className="mt-4 text-center text-sm text-muted-foreground">
        Lembrou a senha?{" "}
        <Link href={ROUTES.LOGIN} className="font-medium text-primary hover:underline">
          Voltar para o login
        </Link>
      </div>
    </Form>
  );
}
