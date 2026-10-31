"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useCreateOwnerInvitation } from "../hooks/useInvitations";
import type { InvitationLink } from "../types/invitation.types";
import { InvitationLinkPanel } from "./InvitationLinkPanel";

const ownerInviteSchema = z.object({
  name: z.string().max(255, "Máximo de 255 caracteres").optional(),
  email: z.string().email("E-mail inválido"),
});

type OwnerInviteFormValues = z.infer<typeof ownerInviteSchema>;

const EMPTY: OwnerInviteFormValues = { name: "", email: "" };

type OwnerInvitationDialogProps = {
  companyId: string;
  companyName: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

export function OwnerInvitationDialog({
  companyId,
  companyName,
  open,
  onOpenChange,
}: OwnerInvitationDialogProps) {
  const [sendEmail, setSendEmail] = useState(true);
  const [created, setCreated] = useState<InvitationLink | null>(null);
  const create = useCreateOwnerInvitation();

  const form = useForm<OwnerInviteFormValues>({
    resolver: zodResolver(ownerInviteSchema),
    defaultValues: EMPTY,
  });

  function reset() {
    setSendEmail(true);
    setCreated(null);
    form.reset(EMPTY);
  }

  function close() {
    reset();
    onOpenChange(false);
  }

  function onSubmit(values: OwnerInviteFormValues) {
    create.mutate(
      {
        companyId,
        data: {
          email: values.email,
          name: values.name?.trim() || undefined,
          sendEmail,
        },
      },
      { onSuccess: (data) => setCreated(data) },
    );
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (!next) reset();
        onOpenChange(next);
      }}
    >
      <DialogContent>
        {created ? (
          <>
            <DialogHeader>
              <DialogTitle>Convite de dono criado</DialogTitle>
              <DialogDescription>
                Compartilhe o link com <strong>{created.invitation.email}</strong> para que essa
                pessoa crie o acesso de dono de {companyName}.
              </DialogDescription>
            </DialogHeader>
            <InvitationLinkPanel link={created} companyName={companyName} onClose={close} />
          </>
        ) : (
          <>
            <DialogHeader>
              <DialogTitle>Convidar dono da empresa</DialogTitle>
              <DialogDescription>
                Envie um convite para o dono de <strong>{companyName}</strong>. Ao aceitar, a pessoa
                entra como dono com acesso total à empresa.
              </DialogDescription>
            </DialogHeader>
            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                <FormField
                  control={form.control}
                  name="name"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Nome (opcional)</FormLabel>
                      <FormControl>
                        <Input placeholder="Nome do dono" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="email"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>E-mail</FormLabel>
                      <FormControl>
                        <Input type="email" placeholder="dono@empresa.com" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <div className="flex items-start gap-2">
                  <Checkbox
                    id="owner-send-email"
                    checked={sendEmail}
                    onCheckedChange={(checked) => setSendEmail(checked === true)}
                  />
                  <div className="space-y-1">
                    <Label htmlFor="owner-send-email">Enviar convite por e-mail</Label>
                    <p className="text-xs text-muted-foreground">
                      Desmarque para gerar apenas o link e enviar por WhatsApp.
                    </p>
                  </div>
                </div>
                <DialogFooter>
                  <Button type="button" variant="outline" onClick={close}>
                    Cancelar
                  </Button>
                  <Button type="submit" disabled={create.isPending}>
                    {create.isPending ? "Enviando..." : "Enviar convite"}
                  </Button>
                </DialogFooter>
              </form>
            </Form>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
