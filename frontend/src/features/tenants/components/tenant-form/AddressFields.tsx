import { useRef, useState } from "react";
import { Controller, useFormState } from "react-hook-form";
import type {
  Control,
  UseFormClearErrors,
  UseFormSetError,
  UseFormSetValue,
} from "react-hook-form";

import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { maskCep } from "@/lib/masks";
import { fetchAddressByCep } from "../../services/cep.service";
import type { TenantFormData } from "../../schemas/tenant.schema";

type AddressFieldsProps = {
  control: Control<TenantFormData>;
  setValue: UseFormSetValue<TenantFormData>;
  clearErrors: UseFormClearErrors<TenantFormData>;
  setError: UseFormSetError<TenantFormData>;
};

export function AddressFields({ control, setValue, clearErrors, setError }: AddressFieldsProps) {
  const { errors } = useFormState({ control });
  const lastCepSearched = useRef("");
  const [isFetchingCep, setIsFetchingCep] = useState(false);

  const handleCepBlur = async (cepValue: string) => {
    const digits = maskCep(cepValue).replace(/\D/g, "");
    if (digits.length !== 8) return;
    // Evita chamadas duplicadas para o mesmo CEP (de-bounce por blur + guarda).
    if (digits === lastCepSearched.current) return;
    lastCepSearched.current = digits;

    clearErrors("address.zipCode");
    setIsFetchingCep(true);
    try {
      const result = await fetchAddressByCep(cepValue);
      if (result) {
        setValue("address.street", result.street);
        setValue("address.neighborhood", result.neighborhood);
        setValue("address.city", result.city);
        setValue("address.state", result.state);
        setValue("address.complement", result.complement);
        // address.number permanece informado manualmente pelo usuário.
      } else {
        setError("address.zipCode", { message: "CEP não encontrado" });
      }
    } catch {
      // Falha de rede/HTTP: preserva os dados já preenchidos sem erro forçado.
    } finally {
      setIsFetchingCep(false);
    }
  };

  return (
    <>
      <div className="grid gap-4 sm:grid-cols-4">
        <div className="space-y-2">
          <Label htmlFor="address.zipCode">CEP *</Label>
          <Controller
            name="address.zipCode"
            control={control}
            render={({ field }) => (
              <Input
                id="address.zipCode"
                placeholder="00000-000"
                inputMode="numeric"
                value={field.value}
                disabled={isFetchingCep}
                ref={field.ref}
                onChange={(e) => {
                  clearErrors("address.zipCode");
                  field.onChange(maskCep(e.target.value));
                }}
                onBlur={() => handleCepBlur(field.value)}
              />
            )}
          />
          {isFetchingCep && <p className="text-sm text-muted-foreground">Buscando endereço...</p>}
          {!isFetchingCep && errors.address?.zipCode && (
            <p className="text-sm text-destructive">{errors.address.zipCode.message}</p>
          )}
        </div>
        <div className="col-span-2 space-y-2">
          <Label htmlFor="address.street">Logradouro *</Label>
          <Controller
            name="address.street"
            control={control}
            render={({ field }) => <Input id="address.street" {...field} />}
          />
          {errors.address?.street && (
            <p className="text-sm text-destructive">{errors.address.street.message}</p>
          )}
        </div>
        <div className="space-y-2">
          <Label htmlFor="address.number">Número *</Label>
          <Controller
            name="address.number"
            control={control}
            render={({ field }) => <Input id="address.number" {...field} />}
          />
          {errors.address?.number && (
            <p className="text-sm text-destructive">{errors.address.number.message}</p>
          )}
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-4">
        <div className="space-y-2">
          <Label htmlFor="address.complement">Complemento</Label>
          <Controller
            name="address.complement"
            control={control}
            render={({ field }) => <Input id="address.complement" {...field} />}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="address.neighborhood">Bairro *</Label>
          <Controller
            name="address.neighborhood"
            control={control}
            render={({ field }) => <Input id="address.neighborhood" {...field} />}
          />
          {errors.address?.neighborhood && (
            <p className="text-sm text-destructive">{errors.address.neighborhood.message}</p>
          )}
        </div>
        <div className="space-y-2">
          <Label htmlFor="address.city">Cidade *</Label>
          <Controller
            name="address.city"
            control={control}
            render={({ field }) => <Input id="address.city" {...field} />}
          />
          {errors.address?.city && (
            <p className="text-sm text-destructive">{errors.address.city.message}</p>
          )}
        </div>
        <div className="space-y-2">
          <Label htmlFor="address.state">Estado *</Label>
          <Controller
            name="address.state"
            control={control}
            render={({ field }) => (
              <Input id="address.state" placeholder="SP" maxLength={2} {...field} />
            )}
          />
          {errors.address?.state && (
            <p className="text-sm text-destructive">{errors.address.state.message}</p>
          )}
        </div>
      </div>

      <div className="w-full space-y-2 sm:w-1/4">
        <Label htmlFor="address.country">País *</Label>
        <Controller
          name="address.country"
          control={control}
          render={({ field }) => <Input id="address.country" {...field} />}
        />
      </div>
    </>
  );
}
