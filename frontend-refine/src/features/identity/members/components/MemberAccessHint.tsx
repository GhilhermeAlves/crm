"use client";

import { Badge } from "@/components/ui/badge";
import type { Member } from "@/features/identity/members/types/member.types";

/** Aviso de acesso ao lado do papel: dono da plataforma ou membro sem perfil. */
export function MemberAccessHint({
  member,
}: {
  member: Pick<Member, "platformAdmin" | "hasProfile">;
}) {
  if (member.platformAdmin) {
    return (
      <Badge variant="secondary" className="ml-1.5">
        Acesso total (dono da plataforma)
      </Badge>
    );
  }
  if (member.hasProfile === false) {
    return (
      <Badge variant="destructive" className="ml-1.5" title="Use Alterar perfil para dar acesso">
        Sem perfil
      </Badge>
    );
  }
  return null;
}
