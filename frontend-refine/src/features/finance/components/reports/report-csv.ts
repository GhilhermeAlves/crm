/**
 * Exportação de relatórios em CSV feita no cliente. Usa `;` como separador
 * (padrão de planilhas pt-BR) e BOM UTF-8 para preservar acentuação no Excel.
 */
export type CsvValue = string | number | null | undefined;

function escapeCsv(value: CsvValue): string {
  const text = value === null || value === undefined ? "" : String(value);
  return /[";\n\r]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
}

export function downloadCsv(fileName: string, rows: CsvValue[][]): void {
  const content = rows.map((row) => row.map(escapeCsv).join(";")).join("\r\n");
  const blob = new Blob(["\uFEFF" + content], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = fileName;
  anchor.click();
  URL.revokeObjectURL(url);
}
