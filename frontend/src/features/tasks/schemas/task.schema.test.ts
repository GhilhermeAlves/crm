import { describe, it, expect } from "vitest";
import { createTaskSchema } from "../schemas/task.schema";

describe("createTaskSchema", () => {
  it("accepts minimal valid task", () => {
    const result = createTaskSchema.safeParse({ title: "Ligar para cliente" });
    expect(result.success).toBe(true);
  });

  it("rejects empty title", () => {
    const result = createTaskSchema.safeParse({ title: "" });
    expect(result.success).toBe(false);
  });

  it("rejects unknown priority", () => {
    const result = createTaskSchema.safeParse({
      title: "x",
      priority: "URGENT",
    });
    expect(result.success).toBe(false);
  });

  it("rejects invalid uuid for opportunityId", () => {
    const result = createTaskSchema.safeParse({
      title: "x",
      opportunityId: "not-a-uuid",
    });
    expect(result.success).toBe(false);
  });
});
