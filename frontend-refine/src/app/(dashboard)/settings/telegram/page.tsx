"use client";

import { PageTitle } from "@/components/common/PageTitle";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { TelegramBotCard } from "@/features/communication/telegram/TelegramBotCard";

export default function TelegramSettingsPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div className="space-y-1">
        <PageTitle>Telegram (Leo)</PageTitle>
        <p className="text-sm text-muted-foreground">
          Converse com o Leo pelo Telegram, por texto ou áudio.
        </p>
      </div>
      {companyId && <TelegramBotCard companyId={companyId} />}
    </div>
  );
}
