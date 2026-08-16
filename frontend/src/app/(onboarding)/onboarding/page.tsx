"use client";

import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { useCreateCompany } from "@/features/onboarding/hooks/useOnboarding";
import { OnboardingCompanyForm } from "@/features/onboarding/components/OnboardingCompanyForm";
import type { OnboardingCompanyRequest } from "@/features/onboarding/types/onboarding.types";
import { ROUTES } from "@/lib/constants";

export default function OnboardingPage() {
  const router = useRouter();
  const { user } = useAuth();
  const createCompany = useCreateCompany();

  const handleSubmit = (data: OnboardingCompanyRequest) => {
    createCompany.mutate(data, {
      onSuccess: () => {
        router.push(ROUTES.DASHBOARD);
      },
    });
  };

  return (
    <main className="flex min-h-screen items-center justify-center bg-crm-background p-4">
      <div className="w-full max-w-3xl space-y-6">
        <div className="space-y-2 text-center">
          <h1 className="text-3xl font-semibold tracking-tight text-crm-text">
            Crie a sua empresa
          </h1>
          <p className="text-sm text-crm-text-secondary">
            Olá, {user?.name} — você ainda não possui uma empresa. Crie a
            primeira para começar a usar o CRM.
          </p>
        </div>
        <OnboardingCompanyForm
          onSubmit={handleSubmit}
          isLoading={createCompany.isPending}
        />
      </div>
    </main>
  );
}
