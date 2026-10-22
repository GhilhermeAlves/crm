/**
 * Link do WhatsApp (wa.me) a partir de um telefone em qualquer formato.
 * Mantém só os dígitos e acrescenta o DDI 55 quando o número veio sem ele (até 11 dígitos).
 * Devolve null quando não há número utilizável (menos de 10 dígitos).
 */
export function whatsappUrl(phone?: string | null, message?: string): string | null {
  const digits = (phone ?? "").replace(/\D/g, "");
  if (digits.length < 10) return null;
  const url = `https://wa.me/${digits.length <= 11 ? `55${digits}` : digits}`;
  const text = message?.trim();
  return text ? `${url}?text=${encodeURIComponent(text)}` : url;
}
