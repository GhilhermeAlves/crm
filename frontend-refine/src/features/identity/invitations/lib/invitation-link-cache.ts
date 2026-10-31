/**
 * Cache em memória dos links devolvidos pelo backend nesta sessão.
 *
 * O token só existe na resposta de criação/regeneração; a listagem nunca o
 * devolve. Guardar em memória permite oferecer "Enviar por WhatsApp" logo após
 * criar/gerar o link sem persistir o token em storage. Recarregar a página
 * perde o cache — por isso a UI avisa que o link não pode ser recuperado.
 */
const links = new Map<string, string>();

export function rememberInvitationLink(invitationId: string, url: string): void {
  if (invitationId && url) links.set(invitationId, url);
}

export function getInvitationLink(invitationId: string): string | null {
  return links.get(invitationId) ?? null;
}
