"use client";

import { useState } from "react";
import { Switch } from "@/components/ui/switch";
import { Label } from "@/components/ui/label";

export default function PreferencesPage() {
  const [requireCpf, setRequireCpf] = useState(false);

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h2 className="text-xl font-semibold">Preferências</h2>

      <section className="space-y-4 rounded-lg border bg-card p-5">
        <h3 className="text-sm font-semibold text-primary">
          Obrigatoriedade do CPF
        </h3>

        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <Label className="text-sm font-medium">
              Exigir CPF no cadastro de contatos
            </Label>
            <p className="text-xs text-muted-foreground">
              Quando ativado, o CPF será obrigatório em todos os cadastros de
              contatos.
            </p>
          </div>
          <Switch checked={requireCpf} onCheckedChange={setRequireCpf} />
        </div>
      </section>
    </div>
  );
}
