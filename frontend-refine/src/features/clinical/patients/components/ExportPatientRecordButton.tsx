"use client";

import { useState } from "react";
import { format, parseISO } from "date-fns";
import { FileDown, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { evolutionService } from "@/features/clinical/evolutions/services/evolution.service";
import type { PatientEvolutionResponse } from "@/features/clinical/evolutions/types/evolution.types";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";
import type { ContactNote } from "../types/patient.types";

interface Props {
  companyId: string | null;
  contact: Contact;
  notes: ContactNote[];
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

const formatDateTime = (value: string) => format(parseISO(value), "dd/MM/yyyy HH:mm");

function entry(date: string, author: string | null, body: string, extra = ""): string {
  return `<article><header><strong>${escapeHtml(formatDateTime(date))}</strong>${
    author ? ` · ${escapeHtml(author)}` : ""
  }${extra}</header><p>${escapeHtml(body)}</p></article>`;
}

function buildRecordHtml(
  contact: Contact,
  notes: ContactNote[],
  evolutions: PatientEvolutionResponse[],
): string {
  const name = `${contact.firstName} ${contact.lastName ?? ""}`.trim();
  const info = [
    contact.cpf && `CPF: ${contact.cpf}`,
    contact.birthDate && `Nascimento: ${format(parseISO(contact.birthDate), "dd/MM/yyyy")}`,
    (contact.mobile || contact.phone) && `Telefone: ${contact.mobile || contact.phone}`,
    contact.email && `E-mail: ${contact.email}`,
  ].filter(Boolean) as string[];

  const sortedNotes = [...notes].sort((a, b) => a.createdAt.localeCompare(b.createdAt));
  const sortedEvolutions = [...evolutions].sort((a, b) => a.createdAt.localeCompare(b.createdAt));

  return `<!doctype html><html lang="pt-BR"><head><meta charset="utf-8">
<title>Registro do paciente — ${escapeHtml(name)}</title>
<style>
  body { font-family: system-ui, sans-serif; color: #111; margin: 32px; font-size: 13px; }
  h1 { font-size: 20px; margin: 0 0 4px; }
  h2 { font-size: 15px; margin: 28px 0 8px; border-bottom: 1px solid #ccc; padding-bottom: 4px; }
  .meta { color: #555; margin: 0 0 4px; }
  article { margin: 0 0 12px; page-break-inside: avoid; }
  header { color: #333; margin-bottom: 2px; }
  p { white-space: pre-wrap; margin: 0; }
  .tag { font-size: 11px; color: #666; }
</style></head><body>
<h1>${escapeHtml(name)}</h1>
${info.map((line) => `<p class="meta">${escapeHtml(line)}</p>`).join("")}
<p class="meta">Exportado em ${escapeHtml(format(new Date(), "dd/MM/yyyy HH:mm"))}</p>
${
  contact.notes?.trim()
    ? `<h2>Observação do cadastro</h2><p>${escapeHtml(contact.notes.trim())}</p>`
    : ""
}
<h2>Evoluções do caso (${sortedEvolutions.length})</h2>
${
  sortedEvolutions.length === 0
    ? "<p>Nenhuma evolução registrada.</p>"
    : sortedEvolutions
        .map((e) =>
          entry(
            e.createdAt,
            e.finalizedByName ?? e.createdByName,
            e.body,
            e.status === "DRAFT" ? ' <span class="tag">(rascunho)</span>' : "",
          ),
        )
        .join("")
}
<h2>Anotações (${sortedNotes.length})</h2>
${
  sortedNotes.length === 0
    ? "<p>Nenhuma anotação registrada.</p>"
    : sortedNotes
        .map((n) =>
          entry(
            n.createdAt,
            n.createdByName || "Sistema",
            n.body,
            n.tooth ? ` <span class="tag">(dente ${n.tooth})</span>` : "",
          ),
        )
        .join("")
}
</body></html>`;
}

/** Imprime (ou salva em PDF) o registro do paciente: dados, evoluções e anotações. */
export function ExportPatientRecordButton({ companyId, contact, notes }: Props) {
  const [busy, setBusy] = useState(false);

  const handleExport = async () => {
    if (!companyId) return;
    setBusy(true);
    try {
      const evolutions = await evolutionService.list(companyId, contact.id).catch(() => []);
      const iframe = document.createElement("iframe");
      iframe.style.position = "fixed";
      iframe.style.width = "0";
      iframe.style.height = "0";
      iframe.style.border = "0";
      document.body.appendChild(iframe);
      const doc = iframe.contentDocument;
      if (!doc || !iframe.contentWindow) throw new Error("print");
      doc.open();
      doc.write(buildRecordHtml(contact, notes, evolutions));
      doc.close();
      iframe.contentWindow.focus();
      iframe.contentWindow.print();
      setTimeout(() => iframe.remove(), 1000);
    } catch {
      toast.error("Não foi possível exportar o registro do paciente.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Button
      type="button"
      size="sm"
      variant="outline"
      className="gap-1.5"
      disabled={busy || !companyId}
      onClick={() => void handleExport()}
    >
      {busy ? <Loader2 className="h-4 w-4 animate-spin" /> : <FileDown className="h-4 w-4" />}
      Exportar registro
    </Button>
  );
}
