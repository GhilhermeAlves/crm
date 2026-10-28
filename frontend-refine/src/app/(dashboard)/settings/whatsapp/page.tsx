"use client";

import { PageTitle } from "@/components/common/PageTitle";
import { WhatsAppConnectionCard } from "@/features/communication/omnichannel/whatsapp/WhatsAppConnectionCard";

export default function WhatsAppSettingsPage() {
  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div className="space-y-1">
        <PageTitle>WhatsApp</PageTitle>
        <p className="text-sm text-muted-foreground">Conexão do número da clínica por QR code.</p>
      </div>
      <WhatsAppConnectionCard />
    </div>
  );
}
