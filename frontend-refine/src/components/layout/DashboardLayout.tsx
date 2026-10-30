"use client";

import { type ReactNode } from "react";
import { Sidebar } from "./Sidebar";
import { Header } from "./Header";
import { AiAssistantLauncher } from "@/features/automation/ai/components/AiAssistantLauncher";
import { useActiveCompanyModules } from "@/features/identity/modules/hooks/useCompanyModules";
import { ModuleGate } from "./ModuleGate";
import { useRecordRecentShortcut } from "./recentShortcuts";

type DashboardLayoutProps = {
  children: ReactNode;
};

export function DashboardLayout({ children }: DashboardLayoutProps) {
  const { isEnabled } = useActiveCompanyModules();
  useRecordRecentShortcut();
  return (
    <div className="flex h-dvh overflow-hidden">
      <Sidebar />
      <div className="flex flex-1 flex-col overflow-hidden">
        <Header />
        <main className="flex-1 overflow-y-auto p-4 lg:p-6">
          <ModuleGate>{children}</ModuleGate>
        </main>
      </div>
      {isEnabled("ASSISTANT") && <AiAssistantLauncher />}
    </div>
  );
}
