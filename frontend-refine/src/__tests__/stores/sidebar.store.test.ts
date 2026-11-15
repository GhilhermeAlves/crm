import { beforeEach, describe, expect, it } from "vitest";
import { rehydrateSidebarStore, useSidebarStore } from "@/stores/sidebar.store";

describe("sidebar store", () => {
  beforeEach(() => {
    localStorage.clear();
    useSidebarStore.setState({ collapsed: false, mobileOpen: false });
  });

  it("alterna collapsed e persiste só ele", () => {
    useSidebarStore.getState().toggle();
    useSidebarStore.getState().setMobileOpen(true);
    const saved = JSON.parse(localStorage.getItem("sidebar-ui") ?? "{}");
    expect(saved.state).toEqual({ collapsed: true });
  });

  it("migra a chave antiga sidebar-collapsed", () => {
    localStorage.setItem("sidebar-collapsed", "true");
    rehydrateSidebarStore();
    expect(useSidebarStore.getState().collapsed).toBe(true);
    expect(localStorage.getItem("sidebar-collapsed")).toBeNull();
  });
});
