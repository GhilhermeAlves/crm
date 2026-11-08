const BRL = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

export function formatCents(cents: number): string {
  return BRL.format(cents / 100);
}

/** "yyyy-MM-dd" → "dd/MM/yyyy" (ou "dd/MM"). Não usa Date: evita deslocar o dia pelo fuso. */
export function formatLocalDate(
  date: string | null | undefined,
  style: "short" | "long" = "long",
): string {
  if (!date) return "—";
  const [y, m, d] = date.slice(0, 10).split("-");
  if (!y || !m || !d) return "—";
  return style === "short" ? `${d}/${m}` : `${d}/${m}/${y}`;
}
