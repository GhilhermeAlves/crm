export const MAX_INSTALLMENTS = 12;

/** Taxa (%) por número de parcelas (1 a 12); null quando não informada. */
export type FeeRates = Record<number, number | null | undefined>;

export type SimulationRow = {
  installments: number;
  ratePercent: number | null;
  patientTotalCents: number | null;
  installmentCents: number | null;
  firstInstallmentCents: number | null;
  netCents: number | null;
  costCents: number | null;
};

export function isValidRate(rate: number | null | undefined): rate is number {
  return typeof rate === "number" && Number.isFinite(rate) && rate >= 0 && rate < 100;
}

/**
 * Simula o parcelamento no crédito de 1x a 12x.
 * - Sem repasse: o paciente paga o valor; a clínica recebe valor × (1 − taxa).
 * - Com repasse: o paciente paga valor ÷ (1 − taxa), para a clínica receber o valor cheio.
 * Parcelas iguais com o resto na primeira (mesma regra de splitInstallments).
 */
export function simulateCredit(
  amountCents: number,
  rates: FeeRates,
  passFeeToPatient: boolean,
): SimulationRow[] {
  return Array.from({ length: MAX_INSTALLMENTS }, (_, i) => {
    const installments = i + 1;
    const rate = rates[installments];
    if (!isValidRate(rate) || !(amountCents >= 0)) {
      return {
        installments,
        ratePercent: rate ?? null,
        patientTotalCents: null,
        installmentCents: null,
        firstInstallmentCents: null,
        netCents: null,
        costCents: null,
      };
    }
    const factor = 1 - rate / 100;
    const patientTotalCents = passFeeToPatient ? Math.round(amountCents / factor) : amountCents;
    const netCents = Math.round(patientTotalCents * factor);
    const installmentCents = Math.floor(patientTotalCents / installments);
    const remainder = patientTotalCents - installmentCents * installments;
    return {
      installments,
      ratePercent: rate,
      patientTotalCents,
      installmentCents,
      firstInstallmentCents: installmentCents + remainder,
      netCents,
      costCents: patientTotalCents - netCents,
    };
  });
}
