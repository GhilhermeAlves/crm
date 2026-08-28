"use client";

import { Check, X } from "lucide-react";
import { cn } from "@/lib/utils";
import {
  PASSWORD_REQUIREMENTS,
  passwordStrength,
} from "../schemas/auth.schema";

interface PasswordRequirementsProps {
  value: string;
}

const STRENGTH_STYLE: Record<string, string> = {
  Fraca: "text-crm-danger",
  Média: "text-amber-600 dark:text-amber-400",
  Forte: "text-emerald-600 dark:text-emerald-400",
};

/**
 * Mostrador de requisitos e de força da senha (Sprint 6.9 UX).
 *
 * <p>Reproduz a política do crm-backend
 * ({@code Password} — min 8, maiúscula, minúscula, número e símbolo
 * {@code @#$%^&+=!}) para o usuário entender o que falta enquanto digita,
 * evitando que uma senha inválida seja enviada ao {@code /register}.</p>
 */
export function PasswordRequirements({ value }: PasswordRequirementsProps) {
  const strength = passwordStrength(value);
  const satisfied = PASSWORD_REQUIREMENTS.filter((requirement) => requirement.check(value)).length;

  return (
    <div className="space-y-2 text-sm text-crm-text-secondary" aria-label="Requisitos da senha">
      <ul className="list-none space-y-1">
        {PASSWORD_REQUIREMENTS.map((requirement) => {
          const ok = requirement.check(value);
          return (
            <li
              key={requirement.label}
              aria-label={`${ok ? "Atendido" : "Não atendido"}: ${requirement.label}`}
              className={cn("flex items-center gap-2", ok ? "text-crm-text" : "text-crm-text-secondary")}
            >
              {ok ? (
                <Check className="h-4 w-4 shrink-0 text-emerald-600 dark:text-emerald-400" aria-hidden="true" />
              ) : (
                <X className="h-4 w-4 shrink-0 text-crm-danger" aria-hidden="true" />
              )}
              <span>{requirement.label}</span>
            </li>
          );
        })}
      </ul>
      <p
        aria-live="polite"
        className={cn(
          "flex items-center gap-2 font-medium",
          satisfied === PASSWORD_REQUIREMENTS.length ? "text-emerald-600 dark:text-emerald-400" : "text-crm-text-secondary",
        )}
      >
        Força da senha: <span className={cn("font-semibold", STRENGTH_STYLE[strength])}>{strength}</span>
      </p>
    </div>
  );
}