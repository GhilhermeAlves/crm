"use client";

import { useParams, useRouter } from "next/navigation";

import { ErrorCard } from "@/components/common/ErrorCard";
import { SkeletonTable } from "@/components/feedback/SkeletonTable";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { AnamnesisModelEditor } from "@/features/masterdata/anamnesis/components/AnamnesisModelEditor";
import {
  useAnamnesisModel,
  useUpdateAnamnesisModel,
} from "@/features/masterdata/anamnesis/hooks/useAnamnesis";
import type { UpdateAnamnesisModelRequest } from "@/features/masterdata/anamnesis/types/anamnesis.types";
import { ROUTES } from "@/lib/constants";

export default function AnamnesisEditorPage() {
  const params = useParams<{ id: string }>();
  const modelId = params?.id ?? null;
  const router = useRouter();
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;

  const { data, isLoading, error, refetch } = useAnamnesisModel(companyId, modelId);
  const updateModel = useUpdateAnamnesisModel(companyId, modelId);

  const handleBack = () => router.push(ROUTES.SETTINGS_DOCUMENTS_ANAMNESIS);

  if (isLoading) {
    return (
      <div className="mx-auto max-w-4xl">
        <SkeletonTable rows={4} columns={3} />
      </div>
    );
  }

  if (error) {
    return (
      <div className="mx-auto max-w-4xl">
        <ErrorCard message={error.message} onRetry={() => refetch()} />
      </div>
    );
  }

  if (!data) {
    return null;
  }

  return (
    <AnamnesisModelEditor
      model={data}
      isSaving={updateModel.isPending}
      onSave={(request: UpdateAnamnesisModelRequest) => updateModel.mutate(request)}
      onBack={handleBack}
    />
  );
}
