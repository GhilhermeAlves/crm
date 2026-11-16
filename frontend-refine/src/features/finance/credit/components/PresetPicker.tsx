"use client";

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useCardFeePresets, type CardFeePreset } from "../hooks/useCardFees";

type Props = {
  onPick: (preset: CardFeePreset) => void;
  id?: string;
};

/** Seletor "Usar modelo": preenche as taxas com as de uma maquininha do mercado. */
export function PresetPicker({ onPick, id }: Props) {
  const { data: presets = [], isLoading } = useCardFeePresets();

  return (
    <Select
      value=""
      onValueChange={(value) => {
        const preset = presets.find((p) => p.id === value);
        if (preset) onPick(preset);
      }}
      disabled={isLoading || presets.length === 0}
    >
      <SelectTrigger id={id} className="w-full sm:w-72" aria-label="Usar modelo de maquininha">
        <SelectValue
          placeholder={isLoading ? "Carregando modelos…" : "Usar modelo de maquininha"}
        />
      </SelectTrigger>
      <SelectContent>
        {presets.map((p) => (
          <SelectItem key={p.id} value={p.id}>
            {p.name}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
