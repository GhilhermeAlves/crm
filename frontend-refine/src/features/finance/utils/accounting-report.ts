import { format, parse, subMonths } from "date-fns";
import { ptBR } from "date-fns/locale";

/** Formata um mês `yyyy-MM` como "Setembro 2026" (mês com inicial maiúscula, pt-BR). */
export function formatMonthLabel(month: string): string {
  const date = parse(month, "yyyy-MM", new Date());
  const label = format(date, "MMMM yyyy", { locale: ptBR });
  return label.charAt(0).toUpperCase() + label.slice(1);
}

/** Chave `yyyy-MM` do mês anterior ao informado (padrão: mês anterior ao atual). */
export function previousMonthKey(now: Date = new Date()): string {
  return format(subMonths(now, 1), "yyyy-MM");
}

/** Dispara o download de um Blob com o nome de arquivo informado. */
export function downloadBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = fileName;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}
