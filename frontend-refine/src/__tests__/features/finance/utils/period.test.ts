import { describe, expect, it } from "vitest";
import { periodLabel, resolvePeriod } from "@/features/finance/utils/period";

// Sábado, 10/10/2026 — data fixa para tornar os testes determinísticos.
const NOW = new Date(2026, 9, 10, 12, 0, 0);

describe("resolvePeriod", () => {
  it("resolves 'today' to the current day", () => {
    expect(resolvePeriod("today", undefined, NOW)).toEqual({
      from: "2026-10-10",
      to: "2026-10-10",
    });
  });

  it("resolves 'week' from Monday to Sunday", () => {
    expect(resolvePeriod("week", undefined, NOW)).toEqual({
      from: "2026-10-05",
      to: "2026-10-11",
    });
  });

  it("resolves 'month' to the whole current month", () => {
    expect(resolvePeriod("month", undefined, NOW)).toEqual({
      from: "2026-10-01",
      to: "2026-10-31",
    });
  });

  it("resolves 'last-month' to the whole previous month", () => {
    expect(resolvePeriod("last-month", undefined, NOW)).toEqual({
      from: "2026-09-01",
      to: "2026-09-30",
    });
  });

  it("resolves '3-months' to the last three calendar months", () => {
    expect(resolvePeriod("3-months", undefined, NOW)).toEqual({
      from: "2026-08-01",
      to: "2026-10-31",
    });
  });

  it("resolves 'custom' to the provided range", () => {
    expect(resolvePeriod("custom", { from: "2026-09-01", to: "2026-09-30" }, NOW)).toEqual({
      from: "2026-09-01",
      to: "2026-09-30",
    });
  });
});

describe("periodLabel", () => {
  it("labels 'today' with the formatted date", () => {
    expect(periodLabel("today", undefined, NOW)).toBe("Hoje (10/10/2026)");
  });

  it("labels 'month' with the month name and year", () => {
    expect(periodLabel("month", undefined, NOW)).toBe("Este mês (outubro/2026)");
  });

  it("labels 'custom' with the range", () => {
    expect(periodLabel("custom", { from: "2026-09-01", to: "2026-09-30" }, NOW)).toBe(
      "01/09/2026 – 30/09/2026",
    );
  });
});
