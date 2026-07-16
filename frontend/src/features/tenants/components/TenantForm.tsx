"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
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
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from "@/components/ui/card";
import { ROUTES } from "@/lib/constants";
import {
  tenantSchema,
  tenantStatusLabels,
  tenantPlanLabels,
  type TenantFormData,
} from "../schemas/tenant.schema";
import type { Tenant, CreateTenantRequest } from "../types/tenant.types";

type TenantFormProps = {
  initialData?: Tenant;
  onSubmit: (data: CreateTenantRequest) => void;
  isLoading?: boolean;
};

export function TenantForm({ initialData, onSubmit, isLoading }: TenantFormProps) {
  const router = useRouter();

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<TenantFormData>({
    resolver: zodResolver(tenantSchema),
    defaultValues: {
      legalName: initialData?.legalName ?? "",
      tradingName: initialData?.tradingName ?? "",
      cnpj: initialData?.cnpj ?? "",
      stateRegistration: initialData?.stateRegistration ?? "",
      municipalRegistration: initialData?.municipalRegistration ?? "",
      email: initialData?.email ?? "",
      phone: initialData?.phone ?? "",
      website: initialData?.website ?? "",
      status: initialData?.status ?? "onboarding",
      plan: initialData?.plan ?? "starter",
      maxUsers: initialData?.maxUsers ?? 5,
      maxStorageMb: initialData?.maxStorageMb ?? 1024,
      logoUrl: initialData?.logoUrl ?? null,
      notes: initialData?.notes ?? "",
      address: {
        zipCode: initialData?.address?.zipCode ?? "",
        street: initialData?.address?.street ?? "",
        number: initialData?.address?.number ?? "",
        complement: initialData?.address?.complement ?? "",
        neighborhood: initialData?.address?.neighborhood ?? "",
        city: initialData?.address?.city ?? "",
        state: initialData?.address?.state ?? "",
        country: initialData?.address?.country ?? "Brasil",
      },
    },
  });

  const statusValue = watch("status");
  const planValue = watch("plan");

  const handleFormSubmit = (data: TenantFormData) => {
    onSubmit({
      ...data,
      logoUrl: data.logoUrl ?? null,
      stateRegistration: data.stateRegistration ?? "",
      municipalRegistration: data.municipalRegistration ?? "",
      website: data.website ?? "",
      notes: data.notes ?? "",
      address: {
        ...data.address,
        complement: data.address.complement ?? "",
      },
    });
  };

  return (
    <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-6">
      {/* Dados da Empresa */}
      <Card>
        <CardHeader>
          <CardTitle>Dados da Empresa</CardTitle>
          <CardDescription>Informações básicas da empresa</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="legalName">Razão Social *</Label>
              <Input id="legalName" {...register("legalName")} />
              {errors.legalName && (
                <p className="text-sm text-destructive">{errors.legalName.message}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="tradingName">Nome Fantasia *</Label>
              <Input id="tradingName" {...register("tradingName")} />
              {errors.tradingName && (
                <p className="text-sm text-destructive">{errors.tradingName.message}</p>
              )}
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <div className="space-y-2">
              <Label htmlFor="cnpj">CNPJ *</Label>
              <Input id="cnpj" placeholder="00.000.000/0000-00" {...register("cnpj")} />
              {errors.cnpj && (
                <p className="text-sm text-destructive">{errors.cnpj.message}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="stateRegistration">Inscrição Estadual</Label>
              <Input id="stateRegistration" {...register("stateRegistration")} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="municipalRegistration">Inscrição Municipal</Label>
              <Input id="municipalRegistration" {...register("municipalRegistration")} />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <div className="space-y-2">
              <Label htmlFor="email">E-mail *</Label>
              <Input id="email" type="email" {...register("email")} />
              {errors.email && (
                <p className="text-sm text-destructive">{errors.email.message}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="phone">Telefone *</Label>
              <Input id="phone" placeholder="(00) 00000-0000" {...register("phone")} />
              {errors.phone && (
                <p className="text-sm text-destructive">{errors.phone.message}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="website">Website</Label>
              <Input id="website" placeholder="https://" {...register("website")} />
              {errors.website && (
                <p className="text-sm text-destructive">{errors.website.message}</p>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Configurações */}
      <Card>
        <CardHeader>
          <CardTitle>Configurações</CardTitle>
          <CardDescription>Plano, limites e status</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-4">
            <div className="space-y-2">
              <Label>Status *</Label>
              <Select
                value={statusValue}
                onValueChange={(val) => setValue("status", val as TenantFormData["status"])}
              >
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
              {errors.status && (
                <p className="text-sm text-destructive">{errors.status.message}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label>Plano *</Label>
              <Select
                value={planValue}
                onValueChange={(val) => setValue("plan", val as TenantFormData["plan"])}
              >
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
              {errors.plan && (
                <p className="text-sm text-destructive">{errors.plan.message}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="maxUsers">Limite de Usuários *</Label>
              <Input id="maxUsers" type="number" {...register("maxUsers", { valueAsNumber: true })} />
              {errors.maxUsers && (
                <p className="text-sm text-destructive">{errors.maxUsers.message}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="maxStorageMb">Armazenamento (MB) *</Label>
              <Input
                id="maxStorageMb"
                type="number"
                {...register("maxStorageMb", { valueAsNumber: true })}
              />
              {errors.maxStorageMb && (
                <p className="text-sm text-destructive">{errors.maxStorageMb.message}</p>
              )}
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="notes">Observações</Label>
            <Textarea id="notes" rows={3} {...register("notes")} />
          </div>
        </CardContent>
      </Card>

      {/* Endereço */}
      <Card>
        <CardHeader>
          <CardTitle>Endereço</CardTitle>
          <CardDescription>Endereço da empresa</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-4">
            <div className="space-y-2">
              <Label htmlFor="address.zipCode">CEP *</Label>
              <Input id="address.zipCode" placeholder="00000-000" {...register("address.zipCode")} />
              {errors.address?.zipCode && (
                <p className="text-sm text-destructive">{errors.address.zipCode.message}</p>
              )}
            </div>
            <div className="col-span-2 space-y-2">
              <Label htmlFor="address.street">Logradouro *</Label>
              <Input id="address.street" {...register("address.street")} />
              {errors.address?.street && (
                <p className="text-sm text-destructive">{errors.address.street.message}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="address.number">Número *</Label>
              <Input id="address.number" {...register("address.number")} />
              {errors.address?.number && (
                <p className="text-sm text-destructive">{errors.address.number.message}</p>
              )}
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-4">
            <div className="space-y-2">
              <Label htmlFor="address.complement">Complemento</Label>
              <Input id="address.complement" {...register("address.complement")} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="address.neighborhood">Bairro *</Label>
              <Input id="address.neighborhood" {...register("address.neighborhood")} />
              {errors.address?.neighborhood && (
                <p className="text-sm text-destructive">{errors.address.neighborhood.message}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="address.city">Cidade *</Label>
              <Input id="address.city" {...register("address.city")} />
              {errors.address?.city && (
                <p className="text-sm text-destructive">{errors.address.city.message}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="address.state">Estado *</Label>
              <Input id="address.state" placeholder="SP" maxLength={2} {...register("address.state")} />
              {errors.address?.state && (
                <p className="text-sm text-destructive">{errors.address.state.message}</p>
              )}
            </div>
          </div>

          <div className="w-full sm:w-1/4 space-y-2">
            <Label htmlFor="address.country">País *</Label>
            <Input id="address.country" {...register("address.country")} />
          </div>
        </CardContent>
      </Card>

      {/* Actions */}
      <div className="flex justify-end gap-3">
        <Button type="button" variant="outline" onClick={() => router.push(ROUTES.TENANTS)}>
          Cancelar
        </Button>
        <Button type="submit" disabled={isLoading}>
          {isLoading ? "Salvando..." : initialData ? "Atualizar" : "Criar Empresa"}
        </Button>
      </div>
    </form>
  );
}
