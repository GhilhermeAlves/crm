import { describe, expect, it } from "vitest";
import {
  formatConversationTimestamp,
  formatDaySeparator,
  messageDayKey,
  parseServerDate,
} from "@/features/communication/omnichannel/utils/messageDates";

// Datas locais construídas a partir de UTC para o teste valer em qualquer fuso.
const utc = (d: Date) => d.toISOString().replace("Z", "");

describe("messageDates", () => {
  it("lê data sem fuso como UTC", () => {
    expect(parseServerDate("2026-10-19T02:11:49.764").toISOString()).toBe(
      "2026-10-19T02:11:49.764Z",
    );
  });

  it("respeita data que já tem fuso", () => {
    expect(parseServerDate("2026-10-19T02:11:49-03:00").toISOString()).toBe(
      "2026-10-19T05:11:49.000Z",
    );
  });

  it("separa por dia: Hoje, Ontem, dia da semana e data", () => {
    const now = new Date(2026, 9, 19, 15, 0);
    expect(formatDaySeparator(utc(new Date(2026, 9, 19, 8, 0)), now)).toBe("Hoje");
    expect(formatDaySeparator(utc(new Date(2026, 9, 18, 23, 0)), now)).toBe("Ontem");
    expect(formatDaySeparator(utc(new Date(2026, 9, 15, 10, 0)), now)).toBe("quinta-feira");
    expect(formatDaySeparator(utc(new Date(2026, 9, 2, 10, 0)), now)).toBe("02/10/2026");
  });

  it("lista de conversas: hora hoje, Ontem, depois data curta", () => {
    const now = new Date(2026, 9, 19, 15, 0);
    expect(formatConversationTimestamp(utc(new Date(2026, 9, 19, 9, 5)), now)).toBe("09:05");
    expect(formatConversationTimestamp(utc(new Date(2026, 9, 18, 9, 5)), now)).toBe("Ontem");
    expect(formatConversationTimestamp(utc(new Date(2026, 9, 2, 9, 5)), now)).toBe("02/10/26");
  });

  it("agrupa mensagens do mesmo dia local", () => {
    expect(messageDayKey(utc(new Date(2026, 9, 19, 0, 1)))).toBe(
      messageDayKey(utc(new Date(2026, 9, 19, 23, 59))),
    );
  });
});
