import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { PasswordRequirements } from "./PasswordRequirements";

describe("PasswordRequirements (Sprint 6.9 — indicador de senha forte)", () => {
  it("marca como atendido o requisito de tamanho mínimo e número para 12345678", () => {
    render(<PasswordRequirements value="12345678" />);
    expect(screen.getByLabelText("Atendido: Pelo menos 8 caracteres")).not.toBeNull();
    expect(screen.getByLabelText("Atendido: Um número")).not.toBeNull();
    expect(screen.getByLabelText("Não atendido: Uma letra maiúscula")).not.toBeNull();
    expect(screen.getByLabelText("Não atendido: Uma letra minúscula")).not.toBeNull();
    expect(screen.getByLabelText("Não atendido: Um caractere especial")).not.toBeNull();
    expect(screen.getByText(/Força da senha:/)).not.toBeNull();
    expect(screen.getByText("Fraca")).not.toBeNull();
  });

  it("marca todos os requisitos como atendidos para Senha!123 (Forte)", () => {
    render(<PasswordRequirements value="Senha!123" />);
    expect(screen.getByLabelText("Atendido: Pelo menos 8 caracteres")).not.toBeNull();
    expect(screen.getByLabelText("Atendido: Uma letra maiúscula")).not.toBeNull();
    expect(screen.getByLabelText("Atendido: Uma letra minúscula")).not.toBeNull();
    expect(screen.getByLabelText("Atendido: Um número")).not.toBeNull();
    expect(screen.getByLabelText("Atendido: Um caractere especial")).not.toBeNull();
    expect(screen.getByText("Forte")).not.toBeNull();
  });

  it("mostra força Média quando metade dos requisitos é satisfeita (Senha1234)", () => {
    render(<PasswordRequirements value="Senha1234" />);
    expect(screen.getByText("Média")).not.toBeNull();
  });

  it("exibe cada requisito com seu rótulo visível", () => {
    render(<PasswordRequirements value="" />);
    expect(screen.getByText("Pelo menos 8 caracteres")).not.toBeNull();
    expect(screen.getByText("Uma letra maiúscula")).not.toBeNull();
    expect(screen.getByText("Uma letra minúscula")).not.toBeNull();
    expect(screen.getByText("Um número")).not.toBeNull();
    expect(screen.getByText("Um caractere especial")).not.toBeNull();
  });
});