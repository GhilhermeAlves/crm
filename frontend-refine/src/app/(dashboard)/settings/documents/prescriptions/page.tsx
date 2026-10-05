"use client";

import { useState } from "react";
import { Switch } from "@/components/ui/switch";
import { Label } from "@/components/ui/label";

export default function PrescriptionsPage() {
  const [includeDefault, setIncludeDefault] = useState(true);

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h2 className="text-xl font-semibold">Receituários</h2>

      <section className="space-y-4 rounded-lg border bg-card p-5">
        <h3 className="text-sm font-semibold">Receituários</h3>

        <div className="flex items-center gap-3">
          <Label className="text-sm text-muted-foreground">
            Incluir medicamentos padrão na listagem?
          </Label>
          <Switch
            checked={includeDefault}
            onCheckedChange={setIncludeDefault}
          />
        </div>
      </section>
    </div>
  );
}
