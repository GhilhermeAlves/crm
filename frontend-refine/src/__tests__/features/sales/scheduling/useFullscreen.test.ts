import { describe, it, expect, vi, afterEach } from "vitest";
import { renderHook, act } from "@testing-library/react";
import { useFullscreen } from "@/features/sales/scheduling/hooks/useFullscreen";

function stubDocument(enabled: boolean, target: Element = document.body) {
  let current: Element | null = null;
  Object.defineProperty(document, "fullscreenEnabled", { configurable: true, value: enabled });
  Object.defineProperty(document, "fullscreenElement", {
    configurable: true,
    get: () => current,
  });
  const exit = vi.fn(async () => {
    current = null;
    document.dispatchEvent(new Event("fullscreenchange"));
  });
  Object.defineProperty(document, "exitFullscreen", { configurable: true, value: exit });
  const request = vi.fn(async () => {
    current = target;
    document.dispatchEvent(new Event("fullscreenchange"));
  });
  return { exit, request };
}

afterEach(() => {
  for (const key of ["fullscreenEnabled", "fullscreenElement", "exitFullscreen"]) {
    delete (document as unknown as Record<string, unknown>)[key];
  }
});

describe("useFullscreen", () => {
  it("entra e sai da tela cheia", async () => {
    const el = document.createElement("div");
    const { exit, request } = stubDocument(true, el);
    el.requestFullscreen = request as unknown as typeof el.requestFullscreen;
    const { result } = renderHook(() => useFullscreen<HTMLDivElement>());
    (result.current.ref as { current: HTMLDivElement | null }).current = el;

    expect(result.current.supported).toBe(true);
    await act(async () => result.current.toggle());
    expect(request).toHaveBeenCalled();
    expect(result.current.isFullscreen).toBe(true);

    await act(async () => result.current.toggle());
    expect(exit).toHaveBeenCalled();
    expect(result.current.isFullscreen).toBe(false);
  });

  it("esconde o botão quando o navegador não permite", () => {
    stubDocument(false);
    const { result } = renderHook(() => useFullscreen<HTMLDivElement>());
    expect(result.current.supported).toBe(false);
  });
});
