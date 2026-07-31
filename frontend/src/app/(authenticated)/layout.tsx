"use client";

import { ProtectedRoute } from "@/components/ProtectedRoute";

export default function AuthenticatedGroupLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <ProtectedRoute>{children}</ProtectedRoute>;
}
