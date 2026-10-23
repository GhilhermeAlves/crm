import { format } from "date-fns";
import { formatTeeth } from "@/features/clinical/procedures/utils/teeth";
import { PAYMENT_METHOD_LABELS } from "@/features/finance/types/finance.types";
import { centsToBRL } from "@/features/finance/utils/money";
import type { BudgetDetail } from "../types/budget.types";
import type { BudgetBlock } from "../utils/blocks";

type Props = {
  budget: BudgetDetail;
  blocks: BudgetBlock[];
  patientName: string;
};

const formatDay = (iso: string) => {
  const [year, month, day] = iso.slice(0, 10).split("-");
  return `${day}/${month}/${year}`;
};

export function BudgetPrintView({ budget, blocks, patientName }: Props) {
  const has = (b: BudgetBlock) => blocks.includes(b);
  const showPrices = has("procedures");
  const showDentists = has("dentists");
  const updated = new Date(budget.updatedAt);
  const createdAt = Number.isNaN(updated.getTime()) ? "" : format(updated, "dd/MM/yyyy");

  return (
    <div className="mx-auto max-w-3xl space-y-6 p-8">
      <header className="space-y-1 border-b pb-4">
        <h1 className="text-2xl font-semibold">{budget.title}</h1>
        <p className="text-sm">Paciente: {patientName}</p>
        {createdAt && <p className="text-sm text-neutral-600">Data: {createdAt}</p>}
      </header>

      <table className="w-full text-sm">
        <thead>
          <tr className="border-b text-left">
            <th className="py-2">Procedimento</th>
            <th className="py-2">Dentes</th>
            {showDentists && <th className="py-2">Dentista</th>}
            {showPrices && <th className="py-2 text-right">Valor</th>}
          </tr>
        </thead>
        <tbody>
          {budget.items.map((item) => (
            <tr key={item.procedureId} className="border-b">
              <td className="py-2">{item.name}</td>
              <td className="py-2">{formatTeeth(item.teeth)}</td>
              {showDentists && <td className="py-2">{item.professionalName ?? "—"}</td>}
              {showPrices && <td className="py-2 text-right">{centsToBRL(item.priceCents)}</td>}
            </tr>
          ))}
        </tbody>
      </table>

      {(has("discount") || has("total")) && (
        <div className="ml-auto w-64 space-y-1 text-sm">
          {has("discount") && (
            <>
              <div className="flex justify-between">
                <span>Subtotal</span>
                <span>{centsToBRL(budget.subtotalCents)}</span>
              </div>
              <div className="flex justify-between">
                <span>Desconto</span>
                <span>{centsToBRL(budget.discountCents)}</span>
              </div>
            </>
          )}
          {has("total") && (
            <div className="flex justify-between border-t pt-1 font-semibold">
              <span>Total</span>
              <span>{centsToBRL(budget.totalCents)}</span>
            </div>
          )}
        </div>
      )}

      {has("notes") && budget.notes && (
        <section className="space-y-1 text-sm">
          <h2 className="font-semibold">Observações</h2>
          <p className="whitespace-pre-wrap">{budget.notes}</p>
        </section>
      )}

      {has("payment") && budget.status === "APPROVED" && budget.paymentMethod && (
        <section className="space-y-2 text-sm">
          <h2 className="font-semibold">Forma de pagamento</h2>
          <p>
            {PAYMENT_METHOD_LABELS[budget.paymentMethod]}
            {budget.installments ? ` em ${budget.installments}x` : ""}
          </p>
          <ul className="space-y-1">
            {budget.installmentsList.map((inst, i) => (
              <li key={inst.id} className="flex justify-between">
                <span>
                  {i + 1}ª parcela — {formatDay(inst.dueDate)}
                </span>
                <span>{centsToBRL(inst.amountCents)}</span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}
