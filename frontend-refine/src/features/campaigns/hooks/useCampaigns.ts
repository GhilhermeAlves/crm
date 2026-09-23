import { useMutation, useQuery } from "@tanstack/react-query";
import { useMutationDefaults } from "@/lib/query/mutation-utils";
import { CampaignService, TemplateService } from "../services/campaign.service";
import type {
  AttachChannelRequest,
  CreateCampaignRequest,
  ListCampaignsParams,
  ScheduleCampaignRequest,
  UpdateCampaignRequest,
} from "../types/campaign.types";

export function useCampaigns(companyId: string | null, params?: ListCampaignsParams) {
  return useQuery({
    queryKey: ["campaigns", companyId, params],
    queryFn: () => CampaignService.list(companyId as string, params),
    enabled: !!companyId,
  });
}

export function useCampaign(companyId: string | null, id: string) {
  return useQuery({
    queryKey: ["campaigns", companyId, id],
    queryFn: () => CampaignService.findById(companyId as string, id),
    enabled: !!companyId && !!id,
  });
}

export function useCampaignExecution(
  companyId: string | null,
  id: string,
  options?: { refetchInterval?: number },
) {
  return useQuery({
    queryKey: ["campaigns", companyId, id, "execution"],
    queryFn: () => CampaignService.getExecution(companyId as string, id),
    enabled: !!companyId && !!id,
    refetchInterval: options?.refetchInterval,
    retry: false,
  });
}

export function useCreateCampaign(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (data: CreateCampaignRequest) => CampaignService.create(companyId as string, data),
    onSuccess: onSuccess({
      successMessage: "Campanha criada com sucesso",
      invalidateKeys: [["campaigns", companyId]],
    }),
    onError: onError({ errorMessage: "Erro ao criar campanha" }),
  });
}

export function useUpdateCampaign(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults<
    unknown,
    { id: string; data: UpdateCampaignRequest }
  >();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateCampaignRequest }) =>
      CampaignService.update(companyId as string, id, data),
    onSuccess: onSuccess({
      successMessage: "Campanha atualizada com sucesso",
      invalidateKeys: [["campaigns", companyId]],
      extraInvalidate: (qc, _d, { id }) =>
        qc.invalidateQueries({ queryKey: ["campaigns", companyId, id] }),
    }),
    onError: onError({ errorMessage: "Erro ao atualizar campanha" }),
  });
}

export function useDeleteCampaign(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults();
  return useMutation({
    mutationFn: (id: string) => CampaignService.delete(companyId as string, id),
    onSuccess: onSuccess({
      successMessage: "Campanha excluída com sucesso",
      invalidateKeys: [["campaigns", companyId]],
    }),
    onError: onError({ errorMessage: "Erro ao excluir campanha" }),
  });
}

export function useAttachChannel(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults<
    unknown,
    { id: string; data: AttachChannelRequest }
  >();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: AttachChannelRequest }) =>
      CampaignService.attachChannel(companyId as string, id, data),
    onSuccess: onSuccess({
      invalidateKeys: [["campaigns", companyId]],
      extraInvalidate: (qc, _d, { id }) =>
        qc.invalidateQueries({ queryKey: ["campaigns", companyId, id] }),
    }),
    onError: onError({ errorMessage: "Erro ao vincular canal à campanha" }),
  });
}

export function useScheduleCampaign(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults<
    unknown,
    { id: string; data: ScheduleCampaignRequest }
  >();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ScheduleCampaignRequest }) =>
      CampaignService.schedule(companyId as string, id, data),
    onSuccess: onSuccess({
      successMessage: "Campanha agendada com sucesso",
      invalidateKeys: [["campaigns", companyId]],
      extraInvalidate: (qc, _d, { id }) =>
        qc.invalidateQueries({ queryKey: ["campaigns", companyId, id] }),
    }),
    onError: onError({ errorMessage: "Erro ao agendar campanha" }),
  });
}

function invalidateLifecycleKeys(companyId: string | null): unknown[][] {
  return [["campaigns", companyId]];
}

export function useExecuteCampaign(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults<unknown, string>();
  return useMutation({
    mutationFn: (id: string) => CampaignService.executeNow(companyId as string, id),
    onSuccess: onSuccess({
      successMessage: "Execução da campanha iniciada",
      invalidateKeys: invalidateLifecycleKeys(companyId),
      extraInvalidate: (qc, _d, id) =>
        qc.invalidateQueries({ queryKey: ["campaigns", companyId, id] }),
    }),
    onError: onError({ errorMessage: "Erro ao executar campanha" }),
  });
}

export function usePauseCampaign(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults<unknown, string>();
  return useMutation({
    mutationFn: (id: string) => CampaignService.pause(companyId as string, id),
    onSuccess: onSuccess({
      successMessage: "Campanha pausada",
      invalidateKeys: invalidateLifecycleKeys(companyId),
      extraInvalidate: (qc, _d, id) =>
        qc.invalidateQueries({ queryKey: ["campaigns", companyId, id] }),
    }),
    onError: onError({ errorMessage: "Erro ao pausar campanha" }),
  });
}

export function useResumeCampaign(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults<unknown, string>();
  return useMutation({
    mutationFn: (id: string) => CampaignService.resume(companyId as string, id),
    onSuccess: onSuccess({
      successMessage: "Campanha retomada",
      invalidateKeys: invalidateLifecycleKeys(companyId),
      extraInvalidate: (qc, _d, id) =>
        qc.invalidateQueries({ queryKey: ["campaigns", companyId, id] }),
    }),
    onError: onError({ errorMessage: "Erro ao retomar campanha" }),
  });
}

export function useCancelCampaign(companyId: string | null) {
  const { onSuccess, onError } = useMutationDefaults<unknown, string>();
  return useMutation({
    mutationFn: (id: string) => CampaignService.cancel(companyId as string, id),
    onSuccess: onSuccess({
      successMessage: "Campanha cancelada",
      invalidateKeys: invalidateLifecycleKeys(companyId),
      extraInvalidate: (qc, _d, id) =>
        qc.invalidateQueries({ queryKey: ["campaigns", companyId, id] }),
    }),
    onError: onError({ errorMessage: "Erro ao cancelar campanha" }),
  });
}

export function useTemplates(companyId: string | null) {
  return useQuery({
    queryKey: ["templates", companyId],
    queryFn: () => TemplateService.list(companyId as string, { status: "ACTIVE", pageSize: 100 }),
    enabled: !!companyId,
  });
}
