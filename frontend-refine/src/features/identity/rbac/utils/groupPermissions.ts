export type PermissionOrigin = "role" | "allow" | "deny" | "none";

type P = { id: string; name: string };

export function groupPermissions<T extends P>(list: T[]): Record<string, T[]> {
  const out: Record<string, T[]> = {};
  for (const p of [...list].sort((a, b) => a.name.localeCompare(b.name))) {
    const mod = p.name.split(":")[0];
    (out[mod] ??= []).push(p);
  }
  return out;
}

export function originOf(
  name: string,
  fromRoles: Set<string>,
  overrides: Map<string, string>,
): PermissionOrigin {
  const eff = overrides.get(name);
  if (eff === "DENY") return "deny";
  if (eff === "ALLOW") return "allow";
  return fromRoles.has(name) ? "role" : "none";
}
