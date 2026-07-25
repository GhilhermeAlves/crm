"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Loader2, LogIn } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { loginSchema, type LoginFormData } from "../schemas/auth.schema";
import { useLogin } from "../hooks/useAuthMutations";
import { useAuth } from "../hooks/useAuth";
import { ROUTES } from "@/lib/constants";

export function LoginForm() {
  const loginMutation = useLogin();
  const { loginKeycloak } = useAuth();
  const [rememberMe, setRememberMe] = useState(false);

  const form = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "",
      password: "",
    },
  });

  useEffect(() => {
    const savedEmail = localStorage.getItem("remembered_email");
    if (savedEmail) {
      form.setValue("email", savedEmail);
      setRememberMe(true);
    }
  }, [form]);

  function onSubmit(data: LoginFormData) {
    if (rememberMe) {
      localStorage.setItem("remembered_email", data.email);
    } else {
      localStorage.removeItem("remembered_email");
    }
    loginMutation.mutate(data);
  }

  return (
    <div className="space-y-6">
      <Button
        type="button"
        variant="outline"
        className="w-full"
        onClick={() => loginKeycloak()}
      >
        <LogIn className="mr-2 h-4 w-4" />
        Entrar com Keycloak
      </Button>

      <div className="relative">
        <div className="absolute inset-0 flex items-center">
          <span className="w-full border-t" />
        </div>
        <div className="relative flex justify-center text-xs uppercase">
          <span className="bg-background px-2 text-muted-foreground">
            ou continue com email e senha
          </span>
        </div>
      </div>

      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
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
          <FormField
            control={form.control}
            name="password"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Senha</FormLabel>
                <FormControl>
                  <Input
                    placeholder="Sua senha"
                    type="password"
                    autoComplete="current-password"
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Checkbox
                id="rememberMe"
                checked={rememberMe}
                onCheckedChange={(checked) => setRememberMe(checked === true)}
              />
              <Label htmlFor="rememberMe" className="text-sm cursor-pointer">
                Lembrar usuário
              </Label>
            </div>
            <Link href={ROUTES.FORGOT_PASSWORD} className="text-sm text-primary hover:underline">
              Esqueceu a senha?
            </Link>
          </div>
          <Button type="submit" className="w-full" disabled={loginMutation.isPending}>
            {loginMutation.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Entrar
          </Button>
        </form>
      </Form>
    </div>
  );
}
