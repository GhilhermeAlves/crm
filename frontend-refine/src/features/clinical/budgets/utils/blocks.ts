export type BudgetBlock = "total" | "procedures" | "dentists" | "discount" | "notes" | "payment";

export const BUDGET_BLOCKS: { key: BudgetBlock; label: string }[] = [
  { key: "total", label: "Valor total" },
  { key: "procedures", label: "Valor por procedimento" },
  { key: "dentists", label: "Dentistas" },
  { key: "discount", label: "Desconto" },
  { key: "notes", label: "Observações" },
  { key: "payment", label: "Forma de pagamento" },
];

export const ALL_BLOCKS: BudgetBlock[] = BUDGET_BLOCKS.map((b) => b.key);

export function parseBlocks(raw: string | null | undefined): BudgetBlock[] {
  if (raw == null) return ALL_BLOCKS;
  const wanted = raw.split(",");
  return ALL_BLOCKS.filter((b) => wanted.includes(b));
}
