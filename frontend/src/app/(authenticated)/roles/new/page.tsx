"use client";

import { RoleForm } from "@/features/rbac/components/RoleForm";

export default function NewRolePage() {
  return (
    <div className="max-w-2xl mx-auto">
      <RoleForm mode="create" />
    </div>
  );
}
