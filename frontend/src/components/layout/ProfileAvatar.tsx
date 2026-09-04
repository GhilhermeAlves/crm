"use client";

import { useAuth } from "@/features/auth/hooks/useAuth";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";

export function ProfileAvatar() {
  const { user } = useAuth();

  const initials = user?.name
    ?.split(" ")
    .map((n) => n[0])
    .join("")
    .toUpperCase()
    .slice(0, 2);

  return (
    <Avatar className="h-8 w-8">
      <AvatarFallback className="text-xs">{initials || "?"}</AvatarFallback>
    </Avatar>
  );
}
