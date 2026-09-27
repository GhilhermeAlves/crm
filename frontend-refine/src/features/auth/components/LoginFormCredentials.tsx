"use client";

import { useState } from "react";
import { Loader2, LogIn } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useLoginMutation } from "../hooks/useLoginMutation";

export function LoginFormCredentials() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const { mutate, isPending, error } = useLoginMutation();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    mutate({ email, password });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="space-y-2">
        <label
          htmlFor="email"
          className="text-sm font-medium text-crm-text"
        >
          Email
        </label>
        <input
          id="email"
          type="email"
          placeholder="seu@email.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          disabled={isPending}
          required
          className="w-full px-3 py-2 border border-crm-border rounded-md bg-crm-background text-crm-text placeholder-crm-text-secondary focus:outline-none focus:ring-2 focus:ring-crm-primary"
        />
      </div>

      <div className="space-y-2">
        <label
          htmlFor="password"
          className="text-sm font-medium text-crm-text"
        >
          Senha
        </label>
        <input
          id="password"
          type="password"
          placeholder="••••••••"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          disabled={isPending}
          required
          className="w-full px-3 py-2 border border-crm-border rounded-md bg-crm-background text-crm-text placeholder-crm-text-secondary focus:outline-none focus:ring-2 focus:ring-crm-primary"
        />
      </div>

      {error && (
        <div className="rounded-md bg-red-50 p-3 text-sm text-red-800">
          {error.message}
        </div>
      )}

      <Button
        type="submit"
        variant="crm"
        className="w-full"
        disabled={isPending}
      >
        {isPending ? (
          <Loader2 className="h-4 w-4 animate-spin" />
        ) : (
          <LogIn className="h-4 w-4" />
        )}
        Entrar com e-mail e senha
      </Button>

      <p className="text-center text-xs text-crm-text-secondary">
        Login seguro — os dados ficam entre você e nossos servidores.
      </p>
    </form>
  );
}
