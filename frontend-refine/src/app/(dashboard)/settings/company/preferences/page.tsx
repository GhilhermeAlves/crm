"use client";

import { Switch } from "@/components/ui/switch";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import {
  useContactRules,
  useSetRequireContactCpf,
} from "@/features/identity/tenants/hooks/useTenants";

export default function PreferencesPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const { data: rules, isLoading } = useContactRules(companyId);
  const setRequireCpf = useSetRequireContactCpf(companyId);

  const requireCpf = setRequireCpf.isPending
    ? (setRequireCpf.variables ?? false)
    : (rules?.requireContactCpf ?? false);

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h2 className="text-xl font-semibold">Preferências</h2>

      <section className="space-y-4 rounded-lg border bg-card p-5">
        <h3 className="text-sm font-semibold text-primary">Obrigatoriedade do CPF</h3>

        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <Label htmlFor="require-cpf" className="text-sm font-medium">
              Exigir CPF no cadastro de contatos
            </Label>
            <p className="text-xs text-muted-foreground">
              Quando ativado, o CPF será obrigatório ao cadastrar e ao editar contatos.
            </p>
          </div>
          <Switch
            id="require-cpf"
            checked={requireCpf}
            disabled={isLoading || setRequireCpf.isPending || !companyId}
            onCheckedChange={(checked) => setRequireCpf.mutate(checked)}
          />
        </div>
      </section>
    </div>
  );
}
