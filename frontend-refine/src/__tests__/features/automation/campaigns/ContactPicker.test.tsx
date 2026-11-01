import { describe, it, expect, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { ContactPicker } from "@/features/automation/campaigns/components/ContactPicker";

vi.mock("@/features/masterdata/contacts/hooks/useContacts", () => ({
  useContacts: () => ({
    isLoading: false,
    data: [
      { id: "c1", firstName: "Ana", lastName: "Souza", mobile: "11999990001", phone: null },
      { id: "c2", firstName: "Bruno", lastName: "Lima", mobile: null, phone: "1133334444" },
      { id: "c3", firstName: "Carla", lastName: "Dias", mobile: null, phone: null },
    ],
  }),
}));

describe("ContactPicker", () => {
  it("marca um contato e bloqueia quem não tem telefone", () => {
    const onChange = vi.fn();
    render(<ContactPicker companyId="co1" selected={[]} onChange={onChange} />);

    fireEvent.click(screen.getByLabelText(/Ana Souza/));
    expect(onChange).toHaveBeenCalledWith(["c1"]);
    expect(screen.getByText("sem telefone")).toBeTruthy();
    expect(
      (
        screen
          .getByLabelText(/Carla Dias/)
          .closest("label")
          ?.querySelector("button") as HTMLButtonElement
      ).disabled,
    ).toBe(true);
  });

  it("seleciona todos os visíveis com telefone e filtra pela busca", () => {
    const onChange = vi.fn();
    render(<ContactPicker companyId="co1" selected={[]} onChange={onChange} />);

    fireEvent.click(screen.getByRole("button", { name: "Selecionar todos" }));
    expect(onChange).toHaveBeenLastCalledWith(["c1", "c2"]);

    fireEvent.change(screen.getByLabelText("Buscar contato"), { target: { value: "bruno" } });
    expect(screen.queryByText("Ana Souza")).toBeNull();
    expect(screen.getByText("Bruno Lima")).toBeTruthy();
  });
});
