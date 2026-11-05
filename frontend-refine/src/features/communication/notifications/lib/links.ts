import { ROUTES } from "@/lib/constants";

/**
 * Página aberta ao clicar na notificação, quando o metadata aponta para um
 * registro (ex.: formulário respondido pelo paciente → aba Anamnese).
 */
export function notificationHref(metadata: string | null | undefined): string | null {
  if (!metadata) return null;
  try {
    const data = JSON.parse(metadata) as { kind?: string; contactId?: string; channel?: string };
    if (data.kind === "PATIENT_FORM" && data.contactId) {
      return `${ROUTES.PATIENTS}/${data.contactId}/anamnese`;
    }
    // O que o Leo fez pelo Telegram: abre o assistente, onde fica a conversa.
    if (data.channel === "telegram") {
      return ROUTES.ASSISTANT;
    }
  } catch {
    return null;
  }
  return null;
}
