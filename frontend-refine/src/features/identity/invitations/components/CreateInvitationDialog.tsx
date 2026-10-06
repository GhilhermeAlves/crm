"use client";

import { useState, type ReactNode } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { useCreateInvitation } from "../hooks/useInvitations";
import { INVITATION_ROLES } from "../types/invitation.types";
import { roleLabel } from "@/features/identity/members/lib/labels";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";

const inviteSchema = z.object({
  name: z.string().max(255, "Máximo de 255 caracteres").optional(),
  email: z.string().email("E-mail inválido"),
  role: z.string().min(1, "Selecione o papel"),
});

type InviteFormValues = z.infer<typeof inviteSchema>;

const EMPTY: InviteFormValues = { name: "", email: "", role: "AGENT" };

type CreateInvitationDialogProps = {
  companyId: string;
  /** Botão que abre o dialog. Padrão: "Convidar membro". */
  trigger?: ReactNode;
  title?: string;
  description?: string;
  /** Rótulo do botão de envio. Padrão: "Enviar convite". */
  submitLabel?: string;
};

export function CreateInvitationDialog({
  companyId,
  trigger,
  title = "Convidar membro",
  description = "Envie um convite por e-mail com um papel de acesso definido. O membro aceitará pelo link recebido.",
  submitLabel = "Enviar convite",
}: CreateInvitationDialogProps) {
  const [open, setOpen] = useState(false);
  const create = useCreateInvitation(companyId);

  const form = useForm<InviteFormValues>({
    resolver: zodResolver(inviteSchema),
    defaultValues: EMPTY,
  });

  function close() {
    setOpen(false);
    form.reset(EMPTY);
  }

  function onSubmit(values: InviteFormValues) {
    create.mutate(
      { email: values.email, role: values.role, name: values.name?.trim() || undefined },
      { onSuccess: close },
    );
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (!next) form.reset(EMPTY);
      }}
    >
      <DialogTrigger asChild>{trigger ?? <Button>Convidar membro</Button>}</DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
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
                    <Input placeholder="Nome de quem será convidado" {...field} />
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
                    <Input type="email" placeholder="membro@empresa.com" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="role"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Papel</FormLabel>
                  <Select onValueChange={field.onChange} defaultValue={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Selecione o papel" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {INVITATION_ROLES.map((role) => (
                        <SelectItem key={role} value={role}>
                          {roleLabel(role)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
            <DialogFooter>
              <Button type="button" variant="outline" onClick={close}>
                Cancelar
              </Button>
              <Button type="submit" disabled={create.isPending}>
                {create.isPending ? "Enviando..." : submitLabel}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
