import { format, parseISO } from "date-fns";
import type { CardFeePreset } from "../hooks/useCardFees";

/** Aviso de que as taxas do modelo são só referência. */
export function RatesNotice({ preset }: { preset: CardFeePreset }) {
  const checked = preset.checkedAt ? format(parseISO(preset.checkedAt), "dd/MM/yyyy") : null;
  return (
    <p className="text-xs text-muted-foreground" role="note">
      Modelo “{preset.name}”: taxas de referência
      {checked ? ` conferidas em ${checked}` : ""}
      {preset.estimated ? " (parcelas de 2x a 11x estimadas)" : ""}. Confirme com a sua maquininha.
    </p>
  );
}
