import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";
import { OmnichannelService } from "../services/omnichannel.service";
import type { ChannelRequest, FollowUpRequest } from "../types/omnichannel.types";

// Canais --------------------------------------------------------------------

export function useChannels() {
  return useQuery({
    queryKey: ["omnichannel", "channels"],
    queryFn: () => OmnichannelService.listChannels(),
  });
}

export function useCreateChannel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: ChannelRequest) => OmnichannelService.createChannel(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["omnichannel", "channels"] });
      toast.success("Canal criado");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao criar canal");
    },
  });
}

export function useUpdateChannel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ChannelRequest }) =>
      OmnichannelService.updateChannel(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["omnichannel", "channels"] });
      toast.success("Canal atualizado");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao atualizar canal");
    },
  });
}

export function useSetChannelStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: "ACTIVE" | "INACTIVE" | "ERROR" }) =>
      OmnichannelService.setChannelStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["omnichannel", "channels"] });
      toast.success("Status atualizado");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao atualizar status");
    },
  });
}

export function useDeleteChannel() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => OmnichannelService.deleteChannel(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["omnichannel", "channels"] });
      toast.success("Canal excluído");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao excluir canal");
    },
  });
}

// Inbox ---------------------------------------------------------------------

export function useConversations() {
  return useQuery({
    queryKey: ["omnichannel", "conversations"],
    queryFn: () => OmnichannelService.listConversations(0, 50),
  });
}

export function useConversation(conversationId: string | null) {
  return useQuery({
    queryKey: ["omnichannel", "conversation", conversationId],
    queryFn: () => OmnichannelService.getConversation(conversationId as string),
    enabled: !!conversationId,
  });
}

export function useSendMessage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ conversationId, body }: { conversationId: string; body: string }) =>
      OmnichannelService.sendMessage(conversationId, body),
    onSuccess: (message) => {
      queryClient.invalidateQueries({
        queryKey: ["omnichannel", "conversation", message.conversationId],
      });
      queryClient.invalidateQueries({
        queryKey: ["omnichannel", "conversations"],
      });
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao enviar mensagem");
    },
  });
}

export function useMarkRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (conversationId: string) => OmnichannelService.markRead(conversationId),
    onSuccess: (_, conversationId) => {
      queryClient.invalidateQueries({
        queryKey: ["omnichannel", "conversation", conversationId],
      });
      queryClient.invalidateQueries({
        queryKey: ["omnichannel", "conversations"],
      });
    },
  });
}

function invalidateConversationScope(
  queryClient: ReturnType<typeof useQueryClient>,
  conversationId: string,
) {
  queryClient.invalidateQueries({ queryKey: ["omnichannel", "conversations"] });
  queryClient.invalidateQueries({ queryKey: ["omnichannel", "conversation", conversationId] });
}

export function useTakeoverConversation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (conversationId: string) => OmnichannelService.takeover(conversationId),
    onSuccess: (_, conversationId) => {
      invalidateConversationScope(queryClient, conversationId);
      toast.success("Você assumiu a conversa. A IA autônoma foi suspensa.");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao assumir a conversa");
    },
  });
}

export function useReleaseConversation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (conversationId: string) => OmnichannelService.release(conversationId),
    onSuccess: (_, conversationId) => {
      invalidateConversationScope(queryClient, conversationId);
      toast.success("Atendimento automático (IA) restabelecido.");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao restabelecer o atendimento automático");
    },
  });
}

export function useOmnichannelPermissions() {
  const { can } = useAuthorization();
  return {
    canRead: can("omnichannel:read"),
    canSend: can("omnichannel:send"),
    canCreate: can("omnichannel:create"),
    canUpdate: can("omnichannel:update"),
    canDelete: can("omnichannel:delete"),
    canTakeover: can("omnichannel:takeover"),
    canFollowUpRead: can("omnichannel:followup:read"),
    canFollowUpManage: can("omnichannel:followup"),
  };
}

// Follow-ups (Sprint 4) ------------------------------------------------------

export function useConversationFollowUps(conversationId: string | null) {
  return useQuery({
    queryKey: ["omnichannel", "conversation", conversationId, "follow-ups"],
    queryFn: () => OmnichannelService.listFollowUps(conversationId as string, 0, 30),
    enabled: !!conversationId,
  });
}

export function useCreateFollowUp(conversationId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: FollowUpRequest) => OmnichannelService.createFollowUp(data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["omnichannel", "conversation", conversationId, "follow-ups"],
      });
      toast.success("Follow-up agendado");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao agendar follow-up");
    },
  });
}

export function useCancelFollowUp(conversationId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (followUpId: string) => OmnichannelService.cancelFollowUp(followUpId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["omnichannel", "conversation", conversationId, "follow-ups"],
      });
      toast.success("Follow-up cancelado");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao cancelar follow-up");
    },
  });
}
