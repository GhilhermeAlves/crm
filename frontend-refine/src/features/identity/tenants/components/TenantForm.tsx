"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/constants";
import { tenantSchema, type TenantFormData } from "../schemas/tenant.schema";
import type { Tenant, CreateTenantRequest } from "../types/tenant.types";
import { AddressFields } from "./tenant-form/AddressFields";
import { CompanyInfoFields } from "./tenant-form/CompanyInfoFields";
import { SettingsFields } from "./tenant-form/SettingsFields";
import { TenantFormSection } from "./tenant-form/TenantFormSection";

type TenantFormProps = {
  initialData?: Tenant;
  onSubmit: (data: CreateTenantRequest) => void;
  isLoading?: boolean;
};

const STEPS = [
  { id: "company", title: "Dados da Empresa", description: "Informações básicas da empresa" },
  { id: "settings", title: "Configurações", description: "Plano, limites e status" },
  { id: "address", title: "Endereço", description: "Endereço da empresa" },
] as const;

const STEP_FIELDS: Record<(typeof STEPS)[number]["id"], string[]> = {
  company: ["legalName", "tradingName", "cnpj", "email", "phone"],
  settings: ["status", "plan", "maxUsers", "maxStorageMb", "maxContacts"],
  address: [
    "address.zipCode",
    "address.street",
    "address.number",
    "address.neighborhood",
    "address.city",
    "address.state",
  ],
};

export function TenantForm({ initialData, onSubmit, isLoading }: TenantFormProps) {
  const router = useRouter();
  const [currentStep, setCurrentStep] = useState(0);
  const lastStepIndex = STEPS.length - 1;

  const { control, setValue, setError, clearErrors, handleSubmit, trigger } =
    useForm<TenantFormData>({
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
        maxContacts: initialData?.maxContacts ?? 500,
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

  const handleNext = async () => {
    const fields = STEP_FIELDS[STEPS[currentStep].id];
    const valid = await trigger(fields as (keyof TenantFormData)[]);
    if (valid) {
      setCurrentStep((step) => Math.min(step + 1, lastStepIndex));
    }
  };

  const handlePrevious = () => {
    setCurrentStep((step) => Math.max(step - 1, 0));
  };

  return (
    <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-6">
      {/* Indicador de steps */}
      <div className="flex items-center justify-center gap-2 sm:gap-4">
        {STEPS.map((step, index) => {
          const isActive = index === currentStep;
          const isDone = index < currentStep;
          return (
            <div key={step.id} className="flex items-center gap-2 sm:gap-4">
              {index > 0 && (
                <div
                  className={`h-0.5 w-6 rounded-full sm:w-12 ${
                    index <= currentStep ? "bg-primary" : "bg-border"
                  }`}
                />
              )}
              <button
                type="button"
                onClick={() => setCurrentStep(index)}
                className={`flex items-center gap-2 rounded-full px-2 py-1 transition-colors ${
                  isActive ? "text-primary" : "text-muted-foreground hover:text-foreground"
                }`}
              >
                <span
                  className={`flex h-8 w-8 items-center justify-center rounded-full text-sm font-medium ${
                    isDone
                      ? "bg-primary text-primary-foreground"
                      : isActive
                        ? "bg-primary text-primary-foreground"
                        : "bg-muted text-muted-foreground"
                  }`}
                >
                  {isDone ? "✓" : index + 1}
                </span>
                <span className="hidden text-sm font-medium sm:inline">{step.title}</span>
              </button>
            </div>
          );
        })}
      </div>

      {/* Dados da Empresa */}
      {currentStep === 0 && (
        <TenantFormSection title={STEPS[0].title} description={STEPS[0].description}>
          <CompanyInfoFields control={control} />
        </TenantFormSection>
      )}

      {/* Configurações */}
      {currentStep === 1 && (
        <TenantFormSection title={STEPS[1].title} description={STEPS[1].description}>
          <SettingsFields control={control} />
        </TenantFormSection>
      )}

      {/* Endereço */}
      {currentStep === 2 && (
        <TenantFormSection title={STEPS[2].title} description={STEPS[2].description}>
          <AddressFields
            control={control}
            setValue={setValue}
            clearErrors={clearErrors}
            setError={setError}
          />
        </TenantFormSection>
      )}

      {/* Actions */}
      <div className="flex justify-between gap-3">
        {currentStep > 0 ? (
          <Button type="button" variant="outline" onClick={handlePrevious}>
            Anterior
          </Button>
        ) : (
          <Button type="button" variant="outline" onClick={() => router.push(ROUTES.TENANTS)}>
            Cancelar
          </Button>
        )}

        {currentStep < lastStepIndex ? (
          <Button type="button" onClick={handleNext}>
            Próximo
          </Button>
        ) : (
          <Button type="submit" disabled={isLoading}>
            {isLoading ? "Salvando..." : initialData ? "Atualizar" : "Criar Empresa"}
          </Button>
        )}
      </div>
    </form>
  );
}
