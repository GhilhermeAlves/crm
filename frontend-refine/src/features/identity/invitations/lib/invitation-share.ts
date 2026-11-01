import { toast } from "sonner";
import { resolveInviteUrl } from "./invite-url";

export function buildInvitationMessage(url: string, companyName?: string | null): string {
  const company = companyName?.trim() || "o CRM";
  return `Olá! Você foi convidado para acessar ${company} no CRM. Crie seu acesso pelo link: ${url}`;
}

export function buildInvitationWhatsAppUrl(url: string, companyName?: string | null): string {
  return `https://wa.me/?text=${encodeURIComponent(
    buildInvitationMessage(resolveInviteUrl(url), companyName),
  )}`;
}

export function openInvitationWhatsApp(url: string, companyName?: string | null): void {
  if (typeof window === "undefined") return;
  window.open(buildInvitationWhatsAppUrl(url, companyName), "_blank", "noopener,noreferrer");
}

export async function copyInvitationLink(url: string): Promise<void> {
  const resolved = resolveInviteUrl(url);
  try {
    if (!resolved || typeof navigator === "undefined" || !navigator.clipboard?.writeText) {
      throw new Error("clipboard unavailable");
    }
    await navigator.clipboard.writeText(resolved);
    toast.success("Link do convite copiado.");
  } catch {
    toast.error(
      "Não foi possível copiar o link automaticamente. Selecione o link e copie manualmente.",
    );
  }
}
