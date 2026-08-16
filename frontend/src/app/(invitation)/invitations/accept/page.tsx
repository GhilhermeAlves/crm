"use client";

import { Suspense, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { InvitationService } from "@/features/invitations/services/invitation.service";
import { toast } from "sonner";
import { LoadingScreen } from "@/components/LoadingScreen";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { MailCheck, MailX } from "lucide-react";
import { ROUTES } from "@/lib/constants";

function AcceptInvitationContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { user } = useAuth();
  const token = searchParams.get("token") ?? "";
  const [action, setAction] = useState<"accept" | "decline" | null>(null);

  const handle = async (next: "accept" | "decline") => {
    setAction(next);
    try {
      if (next === "accept") {
        await InvitationService.accept(token);
        toast.success("Convite aceito! Você agora faz parte da empresa.");
      } else {
        await InvitationService.decline(token);
        toast.success("Convite recusado com sucesso.");
      }
      router.push(ROUTES.DASHBOARD);
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : "Não foi possível processar o convite.";
      toast.error(message);
      setAction(null);
    }
  };

  // Sem token: link inválido/incompleto.
  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Convite de empresa</CardTitle>
          <CardDescription>
            {token
              ? `Você foi convidado para ingressar em uma empresa. O convite está
                vinculado ao e-mail ${user?.email ?? "da sua conta"}.`
              : "Link de convite inválido ou incompleto."}
          </CardDescription>
        </CardHeader>
        {token ? (
          <>
            <CardContent className="space-y-2 text-sm text-muted-foreground">
              <p>
                Ao aceitar, uma nova associação ativa será criada para o seu
                usuário. Você poderá alternar de empresa depois.
              </p>
            </CardContent>
            <CardFooter className="flex justify-end gap-3">
              <Button
                variant="outline"
                disabled={action !== null}
                onClick={() => handle("decline")}
              >
                <MailX className="mr-2 h-4 w-4" /> Recusar
              </Button>
              <Button
                disabled={action !== null}
                onClick={() => handle("accept")}
              >
                <MailCheck className="mr-2 h-4 w-4" />
                {action === "accept" ? "Aceitando..." : "Aceitar convite"}
              </Button>
            </CardFooter>
          </>
        ) : (
          <CardFooter>
            <Button
              variant="outline"
              onClick={() => router.push(ROUTES.DASHBOARD)}
            >
              Voltar ao início
            </Button>
          </CardFooter>
        )}
      </Card>
    </div>
  );
}

export default function AcceptInvitationPage() {
  return (
    <Suspense fallback={<LoadingScreen />}>
      <AcceptInvitationContent />
    </Suspense>
  );
}
