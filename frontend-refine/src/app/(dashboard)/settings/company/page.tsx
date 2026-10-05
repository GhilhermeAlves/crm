"use client";

import { useEffect, useRef, useState } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Upload } from "lucide-react";
import { toast } from "sonner";

import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { useTenant, useUpdateTenant } from "@/features/identity/tenants/hooks/useTenants";
import { fetchAddressByCep } from "@/features/identity/tenants/services/cep.service";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { maskCnpj, maskPhone, maskCep } from "@/lib/masks";

const companySchema = z.object({
  cnpj: z.string().min(1, "CNPJ/CPF é obrigatório"),
  legalName: z.string().max(200).optional().default(""),
  phone: z.string().min(1, "Telefone é obrigatório"),
  zipCode: z.string().min(1, "CEP é obrigatório"),
  street: z.string().min(1, "Rua é obrigatória"),
  number: z.string().min(1, "Número é obrigatório"),
  complement: z.string().optional().default(""),
  neighborhood: z.string().min(1, "Bairro é obrigatório"),
  city: z.string().min(1, "Cidade é obrigatória"),
  state: z.string().min(2, "Estado é obrigatório").max(2),
});

type CompanyFormData = z.infer<typeof companySchema>;

export default function CompanyDataPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? "";
  const { data: tenant, isLoading } = useTenant(companyId);
  const updateTenant = useUpdateTenant();
  const [editing, setEditing] = useState(false);
  const [fetchingCep, setFetchingCep] = useState(false);
  const lastCep = useRef("");

  const {
    control,
    handleSubmit,
    reset,
    setValue,
    clearErrors,
    formState: { errors, isDirty },
  } = useForm<CompanyFormData>({
    resolver: zodResolver(companySchema),
    defaultValues: {
      cnpj: "",
      legalName: "",
      phone: "",
      zipCode: "",
      street: "",
      number: "",
      complement: "",
      neighborhood: "",
      city: "",
      state: "",
    },
  });

  useEffect(() => {
    if (tenant) {
      reset({
        cnpj: tenant.cnpj ?? "",
        legalName: tenant.legalName ?? "",
        phone: tenant.phone ?? "",
        zipCode: tenant.address?.zipCode ?? "",
        street: tenant.address?.street ?? "",
        number: tenant.address?.number ?? "",
        complement: tenant.address?.complement ?? "",
        neighborhood: tenant.address?.neighborhood ?? "",
        city: tenant.address?.city ?? "",
        state: tenant.address?.state ?? "",
      });
    }
  }, [tenant, reset]);

  const handleCepBlur = async (cepValue: string) => {
    const digits = cepValue.replace(/\D/g, "");
    if (digits.length !== 8 || digits === lastCep.current) return;
    lastCep.current = digits;
    setFetchingCep(true);
    try {
      const result = await fetchAddressByCep(cepValue);
      if (result) {
        setValue("street", result.street);
        setValue("neighborhood", result.neighborhood);
        setValue("city", result.city);
        setValue("state", result.state);
        setValue("complement", result.complement);
        clearErrors(["street", "neighborhood", "city", "state"]);
      }
    } catch {
      // keep existing values on network error
    } finally {
      setFetchingCep(false);
    }
  };

  const onSubmit = (data: CompanyFormData) => {
    updateTenant.mutate(
      {
        id: companyId,
        data: {
          cnpj: data.cnpj,
          legalName: data.legalName,
          phone: data.phone,
          address: {
            zipCode: data.zipCode,
            street: data.street,
            number: data.number,
            complement: data.complement ?? "",
            neighborhood: data.neighborhood,
            city: data.city,
            state: data.state,
            country: "Brasil",
          },
        },
      },
      {
        onSuccess: () => {
          setEditing(false);
          toast.success("Dados da empresa atualizados");
        },
      },
    );
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <p className="text-sm text-muted-foreground">Carregando...</p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h2 className="text-xl font-semibold">Dados da empresa</h2>

      <section className="space-y-4 rounded-lg border bg-card p-5">
        <h3 className="text-sm font-semibold text-primary">
          Informações de cadastro
        </h3>

        <div className="grid gap-1 text-sm">
          <div>
            <span className="font-medium">CNPJ/CPF:</span>{" "}
            {tenant?.cnpj || "—"}
          </div>
          <div>
            <span className="font-medium">Razão social:</span>{" "}
            {tenant?.legalName || "—"}
          </div>
        </div>

        <div className="space-y-2">
          <p className="text-xs text-muted-foreground">
            O logo da empresa deverá ter um tamanho máximo de 10MB, nos formatos
            JPG ou PNG.
          </p>
          <div className="flex h-32 items-center justify-center rounded-lg border-2 border-dashed bg-muted/20">
            <div className="flex flex-col items-center gap-1 text-sm text-muted-foreground">
              <Upload className="h-5 w-5" />
              <span>
                Arraste ou{" "}
                <button
                  type="button"
                  className="font-medium text-primary underline"
                >
                  escolha o arquivo
                </button>{" "}
                para enviar
              </span>
              <span className="text-xs">Formatos aceitos: jpg, jpeg, png</span>
            </div>
          </div>
        </div>

        {!editing ? (
          <>
            <div className="grid gap-1 text-sm">
              <div>
                <span className="font-medium">Nome da empresa:</span>{" "}
                {tenant?.tradingName || tenant?.legalName || "—"}
              </div>
              <div>
                <span className="font-medium">Telefone:</span>{" "}
                {tenant?.phone || "—"}
              </div>
              <div>
                <span className="font-medium">CEP:</span>{" "}
                {tenant?.address?.zipCode || "—"}
              </div>
              <div>
                <span className="font-medium">Rua:</span>{" "}
                {tenant?.address?.street || "—"}
              </div>
              <div>
                <span className="font-medium">Número:</span>{" "}
                {tenant?.address?.number || "—"}
              </div>
              <div>
                <span className="font-medium">Complemento:</span>{" "}
                {tenant?.address?.complement || "—"}
              </div>
              <div>
                <span className="font-medium">Bairro:</span>{" "}
                {tenant?.address?.neighborhood || "—"}
              </div>
              <div>
                <span className="font-medium">Cidade:</span>{" "}
                {tenant?.address?.city || "—"}
              </div>
              <div>
                <span className="font-medium">Estado:</span>{" "}
                {tenant?.address?.state || "—"}
              </div>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setEditing(true)}
            >
              Editar
            </Button>
          </>
        ) : (
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label>CNPJ/CPF</Label>
                <Controller
                  name="cnpj"
                  control={control}
                  render={({ field }) => (
                    <Input
                      inputMode="numeric"
                      value={field.value}
                      onChange={(e) => field.onChange(maskCnpj(e.target.value))}
                      ref={field.ref}
                    />
                  )}
                />
                {errors.cnpj && (
                  <p className="text-xs text-destructive">
                    {errors.cnpj.message}
                  </p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label>Razão Social</Label>
                <Controller
                  name="legalName"
                  control={control}
                  render={({ field }) => <Input {...field} />}
                />
              </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label>Telefone</Label>
                <Controller
                  name="phone"
                  control={control}
                  render={({ field }) => (
                    <Input
                      inputMode="numeric"
                      placeholder="(00) 00000-0000"
                      value={field.value}
                      onChange={(e) => field.onChange(maskPhone(e.target.value))}
                      ref={field.ref}
                    />
                  )}
                />
                {errors.phone && (
                  <p className="text-xs text-destructive">
                    {errors.phone.message}
                  </p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label>CEP</Label>
                <Controller
                  name="zipCode"
                  control={control}
                  render={({ field }) => (
                    <Input
                      inputMode="numeric"
                      placeholder="00000-000"
                      disabled={fetchingCep}
                      value={field.value}
                      onChange={(e) => field.onChange(maskCep(e.target.value))}
                      onBlur={() => handleCepBlur(field.value)}
                      ref={field.ref}
                    />
                  )}
                />
                {fetchingCep && (
                  <p className="text-xs text-muted-foreground">
                    Buscando endereço...
                  </p>
                )}
                {errors.zipCode && (
                  <p className="text-xs text-destructive">
                    {errors.zipCode.message}
                  </p>
                )}
              </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-3">
              <div className="col-span-2 space-y-1.5">
                <Label>Rua</Label>
                <Controller
                  name="street"
                  control={control}
                  render={({ field }) => <Input {...field} />}
                />
                {errors.street && (
                  <p className="text-xs text-destructive">
                    {errors.street.message}
                  </p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label>Número</Label>
                <Controller
                  name="number"
                  control={control}
                  render={({ field }) => <Input {...field} />}
                />
                {errors.number && (
                  <p className="text-xs text-destructive">
                    {errors.number.message}
                  </p>
                )}
              </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-4">
              <div className="space-y-1.5">
                <Label>Complemento</Label>
                <Controller
                  name="complement"
                  control={control}
                  render={({ field }) => <Input {...field} />}
                />
              </div>
              <div className="space-y-1.5">
                <Label>Bairro</Label>
                <Controller
                  name="neighborhood"
                  control={control}
                  render={({ field }) => <Input {...field} />}
                />
                {errors.neighborhood && (
                  <p className="text-xs text-destructive">
                    {errors.neighborhood.message}
                  </p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label>Cidade</Label>
                <Controller
                  name="city"
                  control={control}
                  render={({ field }) => <Input {...field} />}
                />
                {errors.city && (
                  <p className="text-xs text-destructive">
                    {errors.city.message}
                  </p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label>Estado</Label>
                <Controller
                  name="state"
                  control={control}
                  render={({ field }) => (
                    <Input placeholder="MG" maxLength={2} {...field} />
                  )}
                />
                {errors.state && (
                  <p className="text-xs text-destructive">
                    {errors.state.message}
                  </p>
                )}
              </div>
            </div>

            <div className="flex gap-2">
              <Button type="submit" size="sm" disabled={updateTenant.isPending}>
                {updateTenant.isPending ? "Salvando..." : "Salvar"}
              </Button>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => {
                  setEditing(false);
                  if (tenant) {
                    reset({
                      cnpj: tenant.cnpj ?? "",
                      legalName: tenant.legalName ?? "",
                      phone: tenant.phone ?? "",
                      zipCode: tenant.address?.zipCode ?? "",
                      street: tenant.address?.street ?? "",
                      number: tenant.address?.number ?? "",
                      complement: tenant.address?.complement ?? "",
                      neighborhood: tenant.address?.neighborhood ?? "",
                      city: tenant.address?.city ?? "",
                      state: tenant.address?.state ?? "",
                    });
                  }
                }}
              >
                Cancelar
              </Button>
            </div>
          </form>
        )}
      </section>
    </div>
  );
}
