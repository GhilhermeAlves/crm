"use client";

import { RoleForm } from "@/features/rbac/components/RoleForm";

export default function NewRolePage() {
  return (
    <div className="mx-auto max-w-2xl">
      <RoleForm mode="create" />
    </div>
  );
}
