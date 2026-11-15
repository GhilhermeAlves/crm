import { format } from "date-fns";
import { formatTeeth } from "@/features/clinical/procedures/utils/teeth";
import { PAYMENT_METHOD_LABELS } from "@/features/finance/types/finance.types";
import { centsToBRL } from "@/features/finance/utils/money";
import type { BudgetDetail } from "../types/budget.types";
import type { BudgetBlock } from "../utils/blocks";

type PrintCompany = {
  name: string;
  logoUrl: string | null;
  addressLine: string;
  phone: string;
};

type PrintPatient = {
  name: string;
  birthDate: string | null;
  gender: string | null;
  mobile: string | null;
  cpf: string | null;
  email: string | null;
};

type Props = {
  budget: BudgetDetail;
  blocks: BudgetBlock[];
  patient: PrintPatient;
  company?: PrintCompany | null;
  /** Coluna de dentes: só em clínicas de odontologia. */
  showTeeth?: boolean;
};

const formatDay = (iso: string) => {
  const [year, month, day] = iso.slice(0, 10).split("-");
  return `${day}/${month}/${year}`;
};

function Field({ label, value }: { label: string; value: string | null | undefined }) {
  return (
    <p>
      <span className="font-semibold">{label}:</span> {value || ""}
    </p>
  );
}

/** Impressão do orçamento no modelo do Capim: cabeçalho da clínica com logo, dados do paciente, itens e assinatura. */
export function BudgetPrintView({ budget, blocks, patient, company, showTeeth = true }: Props) {
  const has = (b: BudgetBlock) => blocks.includes(b);
  const showPrices = has("procedures");
  const showDentists = has("dentists");
  const today = format(new Date(), "dd/MM/yyyy");

  return (
    <div className="mx-auto max-w-3xl space-y-6 p-8 text-neutral-900">
      <header className="flex items-start justify-between gap-6 border-b pb-4">
        <div className="flex items-center gap-4">
          {company?.logoUrl && (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={company.logoUrl}
              alt=""
              className="h-16 w-auto max-w-[160px] object-contain"
            />
          )}
          {company && (
            <div className="space-y-0.5 text-sm">
              <p className="text-base font-semibold">{company.name}</p>
              {company.addressLine && <p>{company.addressLine}</p>}
              {company.phone && <p>{company.phone}</p>}
            </div>
          )}
        </div>
        <p className="shrink-0 text-sm">{today}</p>
      </header>

      <section className="grid grid-cols-2 gap-x-6 gap-y-1 text-sm">
        <Field label="Nome" value={patient.name} />
        <Field
          label="Data de Nasc."
          value={patient.birthDate ? formatDay(patient.birthDate) : null}
        />
        <Field label="Sexo" value={patient.gender} />
        <Field label="Celular" value={patient.mobile} />
        <Field label="CPF" value={patient.cpf} />
        <Field label="Email" value={patient.email} />
      </section>

      <h1 className="text-lg font-semibold">{budget.title || "Orçamento"}</h1>

      <table className="w-full text-sm">
        <thead>
          <tr className="border-b text-left">
            {showTeeth && <th className="py-2 pr-3">Dente</th>}
            <th className="py-2">Procedimento</th>
            {showDentists && <th className="py-2">{showTeeth ? "Dentista" : "Profissional"}</th>}
            {showPrices && <th className="py-2 text-right">Valor</th>}
          </tr>
        </thead>
        <tbody>
          {budget.items.map((item) => (
            <tr key={item.procedureId} className="border-b">
              {showTeeth && <td className="py-2 pr-3">{formatTeeth(item.teeth)}</td>}
              <td className="py-2">{item.name}</td>
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
              <span>Valor Total:</span>
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
      <footer className="space-y-12 pt-6 text-sm">
        <p>Declaro para fins de direito que as informações acima prestadas são verdadeiras.</p>
        <div className="mx-auto w-72 border-t pt-1 text-center">{patient.name}</div>
      </footer>
    </div>
  );
}
