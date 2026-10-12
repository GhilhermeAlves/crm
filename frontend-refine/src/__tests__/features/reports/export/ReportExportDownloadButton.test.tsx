import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";

import { ReportExportDownloadButton } from "@/features/reports/export/ReportExportDownloadButton";
import { downloadReportExport } from "@/features/reports/export/report-export.service";

vi.mock("@/features/reports/export/report-export.service", () => ({
  downloadReportExport: vi.fn().mockResolvedValue(undefined),
}));

const METADATA =
  '{"kind":"REPORT_EXPORT","exportId":"exp-1","fileName":"agenda.pdf","format":"PDF"}';

describe("ReportExportDownloadButton", () => {
  it("não aparece em notificações comuns", () => {
    const { container } = render(
      <ReportExportDownloadButton companyId="c1" metadata='{"kind":"TASK"}' />,
    );
    expect(container.innerHTML).toBe("");
    render(<ReportExportDownloadButton companyId="c1" metadata={null} />);
    expect(screen.queryByRole("button", { name: /Baixar/ })).toBeNull();
  });

  it("baixa o arquivo e avisa para marcar como lida", async () => {
    const onDownloaded = vi.fn();
    render(
      <ReportExportDownloadButton companyId="c1" metadata={METADATA} onDownloaded={onDownloaded} />,
    );

    fireEvent.click(screen.getByRole("button", { name: /Baixar/ }));

    await waitFor(() => expect(onDownloaded).toHaveBeenCalled());
    expect(downloadReportExport).toHaveBeenCalledWith("c1", "exp-1", "agenda.pdf");
  });
});
