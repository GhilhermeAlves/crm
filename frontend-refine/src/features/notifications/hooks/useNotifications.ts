import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useAuthorization } from "@/features/auth/hooks/useAuthorization";
import { NotificationService } from "../services/notification.service";

const POLL_INTERVAL_MS = 15_000;

export function useNotifications(companyId: string | null) {
  return useQuery({
    queryKey: ["notifications", companyId],
    queryFn: () => NotificationService.list(companyId as string),
    enabled: !!companyId,
    refetchInterval: POLL_INTERVAL_MS,
  });
}

export function useUnreadCount(companyId: string | null) {
  return useQuery({
    queryKey: ["notifications-unread", companyId],
    queryFn: () => NotificationService.unreadCount(companyId as string),
    enabled: !!companyId,
    refetchInterval: POLL_INTERVAL_MS,
  });
}

function invalidateNotifications(
  queryClient: ReturnType<typeof useQueryClient>,
  companyId: string | null,
) {
  queryClient.invalidateQueries({ queryKey: ["notifications", companyId] });
  queryClient.invalidateQueries({ queryKey: ["notifications-unread", companyId] });
}

export function useMarkNotificationRead(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => NotificationService.markAsRead(companyId as string, id),
    onSuccess: () => invalidateNotifications(queryClient, companyId),
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao marcar notificação como lida");
    },
  });
}

export function useMarkAllNotificationsRead(companyId: string | null) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => NotificationService.markAllRead(companyId as string),
    onSuccess: () => {
      invalidateNotifications(queryClient, companyId);
      toast.success("Notificações marcadas como lidas");
    },
    onError: (error: Error) => {
      toast.error(error.message || "Erro ao marcar notificações como lidas");
    },
  });
}

export function useNotificationPermissions() {
  const { can } = useAuthorization();
  return {
    canRead: can("notification:read"),
    canUpdate: can("notification:update"),
  };
}
