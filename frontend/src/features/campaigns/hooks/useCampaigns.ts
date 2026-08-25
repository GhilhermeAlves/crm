import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
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
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateCampaignRequest) => CampaignService.create(companyId as string, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["campaigns", companyId] });
      toast.success("Campanha criada com sucesso");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao criar campanha");
    },
  });
}

export function useUpdateCampaign(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateCampaignRequest }) =>
      CampaignService.update(companyId as string, id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: ["campaigns", companyId] });
      queryClient.invalidateQueries({ queryKey: ["campaigns", companyId, id] });
      toast.success("Campanha atualizada com sucesso");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao atualizar campanha");
    },
  });
}

export function useDeleteCampaign(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => CampaignService.delete(companyId as string, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["campaigns", companyId] });
      toast.success("Campanha excluída com sucesso");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao excluir campanha");
    },
  });
}

export function useAttachChannel(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: AttachChannelRequest }) =>
      CampaignService.attachChannel(companyId as string, id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: ["campaigns", companyId] });
      queryClient.invalidateQueries({ queryKey: ["campaigns", companyId, id] });
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao vincular canal à campanha");
    },
  });
}

export function useScheduleCampaign(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ScheduleCampaignRequest }) =>
      CampaignService.schedule(companyId as string, id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: ["campaigns", companyId] });
      queryClient.invalidateQueries({ queryKey: ["campaigns", companyId, id] });
      toast.success("Campanha agendada com sucesso");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao agendar campanha");
    },
  });
}

function invalidateLifecycle(
  queryClient: ReturnType<typeof useQueryClient>,
  companyId: string | null,
) {
  queryClient.invalidateQueries({ queryKey: ["campaigns", companyId] });
}

export function useExecuteCampaign(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => CampaignService.executeNow(companyId as string, id),
    onSuccess: (_, id) => {
      invalidateLifecycle(queryClient, companyId);
      queryClient.invalidateQueries({ queryKey: ["campaigns", companyId, id] });
      toast.success("Execução da campanha iniciada");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao executar campanha");
    },
  });
}

export function usePauseCampaign(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => CampaignService.pause(companyId as string, id),
    onSuccess: (_, id) => {
      invalidateLifecycle(queryClient, companyId);
      queryClient.invalidateQueries({ queryKey: ["campaigns", companyId, id] });
      toast.success("Campanha pausada");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao pausar campanha");
    },
  });
}

export function useResumeCampaign(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => CampaignService.resume(companyId as string, id),
    onSuccess: (_, id) => {
      invalidateLifecycle(queryClient, companyId);
      queryClient.invalidateQueries({ queryKey: ["campaigns", companyId, id] });
      toast.success("Campanha retomada");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao retomar campanha");
    },
  });
}

export function useCancelCampaign(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => CampaignService.cancel(companyId as string, id),
    onSuccess: (_, id) => {
      invalidateLifecycle(queryClient, companyId);
      queryClient.invalidateQueries({ queryKey: ["campaigns", companyId, id] });
      toast.success("Campanha cancelada");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao cancelar campanha");
    },
  });
}

export function useTemplates(companyId: string | null) {
  return useQuery({
    queryKey: ["templates", companyId],
    queryFn: () => TemplateService.list(companyId as string, { status: "ACTIVE", pageSize: 100 }),
    enabled: !!companyId,
  });
}
