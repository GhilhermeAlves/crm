// Datas do Inbox. O backend grava em UTC e serializa sem fuso
// ("2026-10-19T02:11:49.764"); sem o "Z", o navegador leria como horário
// local e mostraria 3h a mais.

const HAS_ZONE = /(Z|[+-]\d{2}:?\d{2})$/i;

export function parseServerDate(value: string): Date {
  return new Date(HAS_ZONE.test(value) ? value : `${value}Z`);
}

function startOfDay(date: Date): number {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
}

function daysAgo(date: Date, now: Date): number {
  return Math.round((startOfDay(now) - startOfDay(date)) / 86_400_000);
}

/** Horário da mensagem: "14:05". */
export function formatMessageTime(value: string): string {
  return parseServerDate(value).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
}

/** Chave do dia (local) para agrupar mensagens. */
export function messageDayKey(value: string): string {
  const d = parseServerDate(value);
  return `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;
}

/** Separador de dia no chat: "Hoje", "Ontem", "segunda-feira" (última semana) ou "12/10/2026". */
export function formatDaySeparator(value: string, now: Date = new Date()): string {
  const date = parseServerDate(value);
  const diff = daysAgo(date, now);
  if (diff === 0) return "Hoje";
  if (diff === 1) return "Ontem";
  if (diff > 1 && diff < 7) return date.toLocaleDateString("pt-BR", { weekday: "long" });
  return date.toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric" });
}

/** Horário na lista de conversas: "14:05" (hoje), "Ontem" ou "12/10/26". */
export function formatConversationTimestamp(value: string, now: Date = new Date()): string {
  const date = parseServerDate(value);
  const diff = daysAgo(date, now);
  if (diff === 0) return formatMessageTime(value);
  if (diff === 1) return "Ontem";
  return date.toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit", year: "2-digit" });
}
