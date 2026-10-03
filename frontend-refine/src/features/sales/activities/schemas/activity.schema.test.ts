import { describe, it, expect } from "vitest";
import { createActivitySchema } from "../schemas/activity.schema";

describe("createActivitySchema", () => {
  it("accepts valid activity", () => {
    const result = createActivitySchema.safeParse({
      type: "CALL",
      subject: "Ligação de qualificação",
    });
    expect(result.success).toBe(true);
  });

  it("rejects empty subject", () => {
    const result = createActivitySchema.safeParse({
      type: "MEETING",
      subject: "",
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].path).toContain("subject");
    }
  });

  it("rejects unknown type", () => {
    const result = createActivitySchema.safeParse({
      type: "FAKE",
      subject: "x",
    });
    expect(result.success).toBe(false);
  });

  it("rejects subject over 255 chars", () => {
    const result = createActivitySchema.safeParse({
      type: "NOTE",
      subject: "a".repeat(256),
    });
    expect(result.success).toBe(false);
  });
});
