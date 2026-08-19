import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";
import { AiService } from "../services/ai.service";

export function useSuggestReply() {
  return useMutation({
    mutationFn: (conversationId: string) => AiService.suggest(conversationId),
    onError: (error: Error) => {
      toast.error(error.message || "Não foi possível gerar a sugestão de resposta.");
    },
  });
}

export function useAiPermissions() {
  const { can } = useAuthorization();
  return {
    canSuggest: can("ai:suggest"),
  };
}
