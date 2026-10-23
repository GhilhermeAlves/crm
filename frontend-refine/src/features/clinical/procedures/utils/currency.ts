/** Centavos → máscara BRL sem símbolo ("1.234,56"); nulo vira campo vazio. */
export function centsToMask(cents: number | null): string {
  if (cents == null) return "";
  return (cents / 100).toLocaleString("pt-BR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

/** Texto digitado → centavos (só os dígitos contam); sem dígitos vira nulo. */
export function maskToCents(text: string): number | null {
  const digits = text.replace(/\D/g, "");
  if (!digits) return null;
  return Number(digits.slice(0, 13));
}
