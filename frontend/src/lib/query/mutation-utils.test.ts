import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook } from "@testing-library/react";
import { useMutationDefaults } from "./mutation-utils";

const { toastErrorMock, toastSuccessMock, invalidateMock } = vi.hoisted(() => ({
  toastErrorMock: vi.fn(),
  toastSuccessMock: vi.fn(),
  invalidateMock: vi.fn(),
}));

vi.mock("sonner", () => ({
  toast: { error: toastErrorMock, success: toastSuccessMock },
}));

vi.mock("@tanstack/react-query", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@tanstack/react-query")>();
  return {
    ...actual,
    useQueryClient: () => ({ invalidateQueries: invalidateMock }),
  };
});

describe("useMutationDefaults (Task 2.11)", () => {
  beforeEach(() => {
    toastErrorMock.mockReset();
    toastSuccessMock.mockReset();
    invalidateMock.mockReset();
  });

  it("invalida as queryKeys e emite toast de sucesso no onSuccess", () => {
    const { result } = renderHook(() => useMutationDefaults());

    result.current.onSuccess({ invalidateKeys: [["leads", "c1"]], successMessage: "Feito" })(
      "data",
      "vars",
    );

    expect(invalidateMock).toHaveBeenCalledWith({ queryKey: ["leads", "c1"] });
    expect(toastSuccessMock).toHaveBeenCalledWith("Feito");
  });

  it("suporta successMessage dinâmico baseado nas variables", () => {
    const { result } = renderHook(() => useMutationDefaults());

    result.current.onSuccess({
      successMessage: (_data, variables) => `Ativado ${variables}`,
    })({}, "workflow-1");

    expect(toastSuccessMock).toHaveBeenCalledWith("Ativado workflow-1");
  });

  it("chama extraInvalidate com queryClient, data e variables", () => {
    const { result } = renderHook(() => useMutationDefaults<unknown, { id: string }>());
    const extra = vi.fn();

    result.current.onSuccess({ invalidateKeys: [["leads", "c1"]], extraInvalidate: extra })(
      "data",
      { id: "lead-1" },
    );

    expect(extra).toHaveBeenCalledWith(
      expect.objectContaining({ invalidateQueries: invalidateMock }),
      "data",
      { id: "lead-1" },
    );
  });

  it("não emite toast de sucesso quando successMessage é omitido", () => {
    const { result } = renderHook(() => useMutationDefaults());

    result.current.onSuccess({ invalidateKeys: [["tasks", "c1"]] })("data", "vars");

    expect(invalidateMock).toHaveBeenCalledWith({ queryKey: ["tasks", "c1"] });
    expect(toastSuccessMock).not.toHaveBeenCalled();
  });

  it("usa a mensagem do response.data.message no onError", () => {
    const { result } = renderHook(() => useMutationDefaults());

    result.current.onError({})({
      response: { data: { message: "Erro do backend" } },
    } as unknown as Error);

    expect(toastErrorMock).toHaveBeenCalledWith("Erro do backend");
  });

  it("usa error.message quando não há mensagem do backend, sem expor fallback", () => {
    const { result } = renderHook(() => useMutationDefaults());

    result.current.onError({})(new Error("boom"));

    expect(toastErrorMock).toHaveBeenCalledWith("boom");
  });

  it("usa errorMessage quando o erro não contém mensagem", () => {
    const { result } = renderHook(() => useMutationDefaults());

    result.current.onError({ errorMessage: "Erro ao criar lead" })({} as Error);

    expect(toastErrorMock).toHaveBeenCalledWith("Erro ao criar lead");
  });

  it("cai em 'Algo deu errado' sem mensagem e sem fallback", () => {
    const { result } = renderHook(() => useMutationDefaults());

    result.current.onError({})({} as Error);

    expect(toastErrorMock).toHaveBeenCalledWith("Algo deu errado");
  });

  it("suprime o toast no onError quando silentError está ativo", () => {
    const { result } = renderHook(() => useMutationDefaults());

    result.current.onError({ silentError: true })(new Error("boom"));

    expect(toastErrorMock).not.toHaveBeenCalled();
  });
});
