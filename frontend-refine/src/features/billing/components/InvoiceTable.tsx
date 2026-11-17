import { DataTable } from "@/components/ui/data-table";
import { formatCents, formatLocalDate } from "../format";
import { INVOICE_STATUS_LABELS, type InvoiceView } from "../types";

export function InvoiceTable({
  invoices,
  isLoading,
  isError,
}: {
  invoices?: InvoiceView[];
  isLoading?: boolean;
  isError?: boolean;
}) {
  if (isError) {
    return <p className="text-sm text-muted-foreground">Não foi possível carregar as faturas.</p>;
  }
  if (isLoading) return <p className="text-sm text-muted-foreground">Carregando…</p>;
  if (!invoices || invoices.length === 0) {
    return <p className="text-sm text-muted-foreground">Nenhuma fatura ainda.</p>;
  }
  return (
    <DataTable
      tableId="invoices"
      data={invoices ?? []}
      bordered={false}
      columns={[
        {
          id: "vencimento",
          header: "Vencimento",
          sortValue: (i) => i.dueDate,
          cell: (i) => <>{formatLocalDate(i.dueDate)}</>,
        },
        {
          id: "valor",
          header: "Valor",
          sortValue: (i) => i.valueCents,
          cell: (i) => <>{formatCents(i.valueCents)}</>,
        },
        { id: "situacao", header: "Situação", cell: (i) => <>{INVOICE_STATUS_LABELS[i.status]}</> },
        {
          id: "pix",
          header: "Pix",
          headerClassName: "text-right",
          className: "text-right",
          cell: (i) => {
            const open = i.status === "PENDING" || i.status === "OVERDUE";
            return (
              <>
                {open && i.invoiceUrl ? (
                  <a
                    href={i.invoiceUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-primary underline"
                  >
                    Abrir
                  </a>
                ) : (
                  "—"
                )}
              </>
            );
          },
        },
      ]}
      keyExtractor={(i) => i.id}
    />
  );
}
