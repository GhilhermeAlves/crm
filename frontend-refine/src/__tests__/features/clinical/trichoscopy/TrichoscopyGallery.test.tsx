import { describe, it, expect, vi, beforeEach } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { TrichoscopyGallery } from "@/features/clinical/trichoscopy/components/TrichoscopyGallery";
import type { TrichoscopyImageResponse } from "@/features/clinical/trichoscopy/types/trichoscopy.types";

const { listState, removeMutate } = vi.hoisted(() => ({
  listState: { value: [] as TrichoscopyImageResponse[] },
  removeMutate: vi.fn(),
}));

vi.mock("@/features/clinical/trichoscopy/hooks/useTrichoscopy", () => ({
  useTrichoscopyList: () => ({ data: listState.value, isLoading: false }),
  useDeleteTrichoscopy: () => ({ mutate: removeMutate, isPending: false }),
}));

const image: TrichoscopyImageResponse = {
  id: "img1",
  contactId: "c1",
  storageObjectId: "s1",
  takenBy: null,
  takenAt: "2026-10-24T10:30:00",
  notes: "Região frontal",
  downloadUrl: "/api/v1/companies/co1/storage/s1/download",
};

describe("TrichoscopyGallery", () => {
  beforeEach(() => {
    removeMutate.mockReset();
    listState.value = [image];
  });

  it("mostra a imagem com data e observações", () => {
    render(<TrichoscopyGallery companyId="co1" contactId="c1" canEdit />);
    const img = screen.getByAltText("Imagem tricoscópica de 24/10/2026");
    expect(img.getAttribute("src")).toBe(image.downloadUrl);
    expect(screen.getByText("24/10/2026 10:30")).toBeTruthy();
    expect(screen.getByText("Região frontal")).toBeTruthy();
  });

  it("exclui a imagem só depois de confirmar", () => {
    render(<TrichoscopyGallery companyId="co1" contactId="c1" canEdit />);
    fireEvent.click(screen.getByRole("button", { name: "Excluir imagem" }));
    expect(removeMutate).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Excluir" }));
    expect(removeMutate).toHaveBeenCalledWith("img1");
  });

  it("sem permissão de edição não oferece exclusão", () => {
    render(<TrichoscopyGallery companyId="co1" contactId="c1" canEdit={false} />);
    expect(screen.queryByRole("button", { name: "Excluir imagem" })).toBeNull();
  });

  it("avisa quando não há imagens", () => {
    listState.value = [];
    render(<TrichoscopyGallery companyId="co1" contactId="c1" canEdit />);
    expect(screen.getByText("Nenhuma imagem registrada.")).toBeTruthy();
  });
});
