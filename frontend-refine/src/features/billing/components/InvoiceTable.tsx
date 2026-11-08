import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { formatCents, formatLocalDate } from "../format";
import { INVOICE_STATUS_LABELS, type InvoiceView } from "../types";

export function InvoiceTable({
  invoices,
  isLoading,
}: {
  invoices?: InvoiceView[];
  isLoading?: boolean;
}) {
  if (isLoading) return <p className="text-sm text-muted-foreground">Carregando…</p>;
  if (!invoices || invoices.length === 0) {
    return <p className="text-sm text-muted-foreground">Nenhuma fatura ainda.</p>;
  }
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Vencimento</TableHead>
          <TableHead>Valor</TableHead>
          <TableHead>Situação</TableHead>
          <TableHead className="text-right">Pix</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {invoices.map((i) => {
          const open = i.status === "PENDING" || i.status === "OVERDUE";
          return (
            <TableRow key={i.id}>
              <TableCell>{formatLocalDate(i.dueDate)}</TableCell>
              <TableCell>{formatCents(i.valueCents)}</TableCell>
              <TableCell>{INVOICE_STATUS_LABELS[i.status]}</TableCell>
              <TableCell className="text-right">
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
              </TableCell>
            </TableRow>
          );
        })}
      </TableBody>
    </Table>
  );
}
