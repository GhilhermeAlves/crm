const BRL_FORMATTER = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

/** Formata um valor em centavos como moeda brasileira (ex.: 123456 → "R$ 1.234,56"). */
export function centsToBRL(cents: number): string {
  return BRL_FORMATTER.format(cents / 100);
}

/**
 * Converte um valor monetário digitado para centavos. Aceita "1.234,56", "1234.56",
 * "0,05" e "R$ 1.234,56". Retorna `null` para entrada vazia ou inválida.
 */
export function parseBRLToCents(value: string): number | null {
  if (!value) return null;

  const negative = value.includes("-");
  const digits = value.replace(/R\$/gi, "").replace(/[\s\u00A0-]/g, "");

  if (!/^[0-9.,]+$/.test(digits) || !/[0-9]/.test(digits)) return null;

  const normalized = digits.includes(",") ? digits.replace(/\./g, "").replace(",", ".") : digits;

  const amount = Number.parseFloat(normalized);
  if (Number.isNaN(amount)) return null;

  const cents = Math.round(amount * 100);
  return negative ? -cents : cents;
}
