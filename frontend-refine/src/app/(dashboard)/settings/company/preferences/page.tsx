"use client";

import { Switch } from "@/components/ui/switch";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import {
  useContactRules,
  useSetAutoBudgetFromProcedures,
  useSetRequireContactCpf,
} from "@/features/identity/tenants/hooks/useTenants";

export default function PreferencesPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const { data: rules, isLoading } = useContactRules(companyId);
  const setRequireCpf = useSetRequireContactCpf(companyId);
  const setAutoBudget = useSetAutoBudgetFromProcedures(companyId);

  const requireCpf = setRequireCpf.isPending
    ? (setRequireCpf.variables ?? false)
    : (rules?.requireContactCpf ?? false);
  const autoBudget = setAutoBudget.isPending
    ? (setAutoBudget.variables ?? false)
    : (rules?.autoBudgetFromProcedures ?? false);

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

      <section className="space-y-4 rounded-lg border bg-card p-5">
        <h3 className="text-sm font-semibold text-primary">Orçamentos</h3>

        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <Label htmlFor="auto-budget" className="text-sm font-medium">
              Orçamento automático dos procedimentos
            </Label>
            <p className="text-xs text-muted-foreground">
              Todo procedimento planejado entra no orçamento em aberto do paciente, sem precisar
              aprovar. Ao marcar como realizado, vira conta a receber no Financeiro.
            </p>
          </div>
          <Switch
            id="auto-budget"
            checked={autoBudget}
            disabled={isLoading || setAutoBudget.isPending || !companyId}
            onCheckedChange={(checked) => setAutoBudget.mutate(checked)}
          />
        </div>
      </section>
    </div>
  );
}
