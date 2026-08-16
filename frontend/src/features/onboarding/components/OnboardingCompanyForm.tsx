"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  onboardingCompanySchema,
  type OnboardingCompanyFormData,
} from "../schemas/onboarding.schema";
import type { OnboardingCompanyRequest } from "../types/onboarding.types";

type OnboardingCompanyFormProps = {
  onSubmit: (data: OnboardingCompanyRequest) => void;
  isLoading?: boolean;
};

export function OnboardingCompanyForm({
  onSubmit,
  isLoading,
}: OnboardingCompanyFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<OnboardingCompanyFormData>({
    resolver: zodResolver(onboardingCompanySchema),
    defaultValues: {
      legalName: "",
      tradingName: "",
      cnpj: "",
      email: "",
      phone: "",
      address: {
        zipCode: "",
        street: "",
        number: "",
        complement: "",
        neighborhood: "",
        city: "",
        state: "",
        country: "Brasil",
      },
    },
  });

  const handleFormSubmit = (data: OnboardingCompanyFormData) => {
    onSubmit({
      ...data,
      website: "",
      stateRegistration: "",
      municipalRegistration: "",
      address: {
        ...data.address,
        complement: data.address.complement ?? "",
      },
    });
  };

  return (
    <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>Dados da Empresa</CardTitle>
          <CardDescription>Informações básicas da sua empresa</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="legalName">Razão Social *</Label>
              <Input id="legalName" {...register("legalName")} />
              {errors.legalName && (
                <p className="text-sm text-destructive">
                  {errors.legalName.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="tradingName">Nome Fantasia *</Label>
              <Input id="tradingName" {...register("tradingName")} />
              {errors.tradingName && (
                <p className="text-sm text-destructive">
                  {errors.tradingName.message}
                </p>
              )}
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <div className="space-y-2">
              <Label htmlFor="cnpj">CNPJ *</Label>
              <Input
                id="cnpj"
                placeholder="00.000.000/0000-00"
                {...register("cnpj")}
              />
              {errors.cnpj && (
                <p className="text-sm text-destructive">
                  {errors.cnpj.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="email">E-mail *</Label>
              <Input id="email" type="email" {...register("email")} />
              {errors.email && (
                <p className="text-sm text-destructive">
                  {errors.email.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="phone">Telefone *</Label>
              <Input
                id="phone"
                placeholder="(00) 00000-0000"
                {...register("phone")}
              />
              {errors.phone && (
                <p className="text-sm text-destructive">
                  {errors.phone.message}
                </p>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Endereço</CardTitle>
          <CardDescription>Endereço da empresa</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-4">
            <div className="space-y-2">
              <Label htmlFor="address.zipCode">CEP *</Label>
              <Input
                id="address.zipCode"
                placeholder="00000-000"
                {...register("address.zipCode")}
              />
              {errors.address?.zipCode && (
                <p className="text-sm text-destructive">
                  {errors.address.zipCode.message}
                </p>
              )}
            </div>
            <div className="col-span-2 space-y-2">
              <Label htmlFor="address.street">Logradouro *</Label>
              <Input id="address.street" {...register("address.street")} />
              {errors.address?.street && (
                <p className="text-sm text-destructive">
                  {errors.address.street.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="address.number">Número *</Label>
              <Input id="address.number" {...register("address.number")} />
              {errors.address?.number && (
                <p className="text-sm text-destructive">
                  {errors.address.number.message}
                </p>
              )}
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-4">
            <div className="space-y-2">
              <Label htmlFor="address.complement">Complemento</Label>
              <Input
                id="address.complement"
                {...register("address.complement")}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="address.neighborhood">Bairro *</Label>
              <Input
                id="address.neighborhood"
                {...register("address.neighborhood")}
              />
              {errors.address?.neighborhood && (
                <p className="text-sm text-destructive">
                  {errors.address.neighborhood.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="address.city">Cidade *</Label>
              <Input id="address.city" {...register("address.city")} />
              {errors.address?.city && (
                <p className="text-sm text-destructive">
                  {errors.address.city.message}
                </p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="address.state">Estado *</Label>
              <Input
                id="address.state"
                placeholder="SP"
                maxLength={2}
                {...register("address.state")}
              />
              {errors.address?.state && (
                <p className="text-sm text-destructive">
                  {errors.address.state.message}
                </p>
              )}
            </div>
          </div>

          <div className="w-full sm:w-1/4 space-y-2">
            <Label htmlFor="address.country">País *</Label>
            <Input id="address.country" {...register("address.country")} />
          </div>
        </CardContent>
      </Card>

      <div className="flex justify-end">
        <Button type="submit" disabled={isLoading}>
          {isLoading ? "Criando empresa..." : "Criar Minha Empresa"}
        </Button>
      </div>
    </form>
  );
}
