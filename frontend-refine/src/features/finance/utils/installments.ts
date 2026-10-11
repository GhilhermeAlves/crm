import { centsToBRL } from "./money";

/**
 * Descreve o parcelamento de um valor em centavos em linguagem natural.
 *
 * O resto da divisão (centavos que não fecham) é somado à 1ª parcela, então
 * `100,00` em 3x vira "3x (1ª de R$ 33,34, demais de R$ 33,33)".
 * Retorna `null` quando não há parcelamento (1x ou menos).
 */
export function describeInstallments(totalCents: number, count: number): string | null {
  if (count <= 1) return null;

  const base = Math.floor(totalCents / count);
  const remainder = totalCents - base * count;
  const first = base + remainder;

  if (remainder === 0) {
    return `${count}x de ${centsToBRL(base)}`;
  }
  return `${count}x (1ª de ${centsToBRL(first)}, demais de ${centsToBRL(base)})`;
}
