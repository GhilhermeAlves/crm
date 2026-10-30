import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { LiveViewScreen } from "@/features/clinical/trichoscopy/components/LiveViewScreen";

const { frameMock } = vi.hoisted(() => ({ frameMock: vi.fn() }));

vi.mock("@/features/clinical/trichoscopy/services/live-view.service", () => ({
  LiveViewService: { frame: frameMock },
}));

describe("LiveViewScreen", () => {
  beforeEach(() => {
    frameMock.mockReset();
    URL.createObjectURL = vi.fn(() => "blob:frame");
    URL.revokeObjectURL = vi.fn();
  });

  it("mostra o quadro recebido", async () => {
    frameMock.mockResolvedValue({ status: "frame", blob: new Blob(["x"]), seq: 1 });

    render(<LiveViewScreen token="abc" />);

    await waitFor(() =>
      expect(screen.getByAltText("Imagem ao vivo do tricoscópio").getAttribute("src")).toBe(
        "blob:frame",
      ),
    );
    expect(frameMock).toHaveBeenCalledWith("abc", 0);
  });

  it("avisa quando a transmissão acabou", async () => {
    frameMock.mockResolvedValue({ status: "ended" });

    render(<LiveViewScreen token="abc" />);

    expect(await screen.findByText(/A transmissão foi encerrada/)).toBeTruthy();
  });
});
