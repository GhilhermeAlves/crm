import { Controller, useFormState } from "react-hook-form";
import type { Control } from "react-hook-form";

import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { maskCnpj, maskPhone } from "@/lib/masks";
import type { TenantFormData } from "../../schemas/tenant.schema";

type CompanyInfoFieldsProps = {
  control: Control<TenantFormData>;
};

export function CompanyInfoFields({ control }: CompanyInfoFieldsProps) {
  const { errors } = useFormState({ control });

  return (
    <>
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-2">
          <Label htmlFor="legalName">Razão Social *</Label>
          <Controller
            name="legalName"
            control={control}
            render={({ field }) => <Input id="legalName" {...field} />}
          />
          {errors.legalName && (
            <p className="text-sm text-destructive">{errors.legalName.message}</p>
          )}
        </div>
        <div className="space-y-2">
          <Label htmlFor="tradingName">Nome Fantasia *</Label>
          <Controller
            name="tradingName"
            control={control}
            render={({ field }) => <Input id="tradingName" {...field} />}
          />
          {errors.tradingName && (
            <p className="text-sm text-destructive">{errors.tradingName.message}</p>
          )}
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="space-y-2">
          <Label htmlFor="cnpj">CNPJ *</Label>
          <Controller
            name="cnpj"
            control={control}
            render={({ field }) => (
              <Input
                id="cnpj"
                placeholder="00.000.000/0000-00"
                inputMode="numeric"
                value={field.value}
                onChange={(e) => field.onChange(maskCnpj(e.target.value))}
                ref={field.ref}
              />
            )}
          />
          {errors.cnpj && <p className="text-sm text-destructive">{errors.cnpj.message}</p>}
        </div>
        <div className="space-y-2">
          <Label htmlFor="stateRegistration">Inscrição Estadual</Label>
          <Controller
            name="stateRegistration"
            control={control}
            render={({ field }) => <Input id="stateRegistration" {...field} />}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="municipalRegistration">Inscrição Municipal</Label>
          <Controller
            name="municipalRegistration"
            control={control}
            render={({ field }) => <Input id="municipalRegistration" {...field} />}
          />
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="space-y-2">
          <Label htmlFor="email">E-mail *</Label>
          <Controller
            name="email"
            control={control}
            render={({ field }) => <Input id="email" type="email" {...field} />}
          />
          {errors.email && <p className="text-sm text-destructive">{errors.email.message}</p>}
        </div>
        <div className="space-y-2">
          <Label htmlFor="phone">Telefone *</Label>
          <Controller
            name="phone"
            control={control}
            render={({ field }) => (
              <Input
                id="phone"
                placeholder="(00) 00000-0000"
                inputMode="numeric"
                value={field.value}
                onChange={(e) => field.onChange(maskPhone(e.target.value))}
                ref={field.ref}
              />
            )}
          />
          {errors.phone && <p className="text-sm text-destructive">{errors.phone.message}</p>}
        </div>
        <div className="space-y-2">
          <Label htmlFor="website">Website</Label>
          <Controller
            name="website"
            control={control}
            render={({ field }) => <Input id="website" placeholder="https://" {...field} />}
          />
          {errors.website && <p className="text-sm text-destructive">{errors.website.message}</p>}
        </div>
      </div>
    </>
  );
}
