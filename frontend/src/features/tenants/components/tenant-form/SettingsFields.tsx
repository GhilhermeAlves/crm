import { Controller, useFormState } from "react-hook-form";
import type { Control } from "react-hook-form";

import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { tenantPlanLabels, tenantStatusLabels } from "../../schemas/tenant.schema";
import type { TenantFormData } from "../../schemas/tenant.schema";

type SettingsFieldsProps = {
  control: Control<TenantFormData>;
};

export function SettingsFields({ control }: SettingsFieldsProps) {
  const { errors } = useFormState({ control });

  return (
    <>
      <div className="grid gap-4 sm:grid-cols-4">
        <div className="space-y-2">
          <Label>Status *</Label>
          <Controller
            name="status"
            control={control}
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(tenantStatusLabels).map(([value, label]) => (
                    <SelectItem key={value} value={value}>
                      {label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
          {errors.status && <p className="text-sm text-destructive">{errors.status.message}</p>}
        </div>
        <div className="space-y-2">
          <Label>Plano *</Label>
          <Controller
            name="plan"
            control={control}
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(tenantPlanLabels).map(([value, label]) => (
                    <SelectItem key={value} value={value}>
                      {label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
          {errors.plan && <p className="text-sm text-destructive">{errors.plan.message}</p>}
        </div>
        <div className="space-y-2">
          <Label htmlFor="maxUsers">Limite de Usuários *</Label>
          <Controller
            name="maxUsers"
            control={control}
            render={({ field }) => (
              <Input
                id="maxUsers"
                type="number"
                {...field}
                onChange={(e) => field.onChange(e.target.valueAsNumber)}
              />
            )}
          />
          {errors.maxUsers && <p className="text-sm text-destructive">{errors.maxUsers.message}</p>}
        </div>
        <div className="space-y-2">
          <Label htmlFor="maxStorageMb">Armazenamento (MB) *</Label>
          <Controller
            name="maxStorageMb"
            control={control}
            render={({ field }) => (
              <Input
                id="maxStorageMb"
                type="number"
                {...field}
                onChange={(e) => field.onChange(e.target.valueAsNumber)}
              />
            )}
          />
          {errors.maxStorageMb && (
            <p className="text-sm text-destructive">{errors.maxStorageMb.message}</p>
          )}
        </div>
        <div className="space-y-2">
          <Label htmlFor="maxContacts">Limite de Contatos *</Label>
          <Controller
            name="maxContacts"
            control={control}
            render={({ field }) => (
              <Input
                id="maxContacts"
                type="number"
                {...field}
                onChange={(e) => field.onChange(e.target.valueAsNumber)}
              />
            )}
          />
          {errors.maxContacts && (
            <p className="text-sm text-destructive">{errors.maxContacts.message}</p>
          )}
        </div>
      </div>

      <div className="space-y-2">
        <Label htmlFor="notes">Observações</Label>
        <Controller
          name="notes"
          control={control}
          render={({ field }) => <Textarea id="notes" rows={3} {...field} />}
        />
      </div>
    </>
  );
}
