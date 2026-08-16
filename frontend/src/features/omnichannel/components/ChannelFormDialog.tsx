"use client";

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
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
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  CHANNEL_PROVIDER_LABELS,
  CHANNEL_STATUS_LABELS,
  type Channel,
  type ChannelProvider,
  type ChannelStatus,
} from "../types/omnichannel.types";

const channelSchema = z.object({
  name: z.string().min(1, "Nome é obrigatório").max(120),
  type: z.literal("WHATSAPP"),
  provider: z.enum(["WHATSAPP_CLOUD_API", "FAKE"] as const),
  externalId: z.string().max(120).optional(),
  config: z.string().max(4000).optional(),
  secretsRef: z.string().max(200).optional(),
  status: z.enum(["ACTIVE", "INACTIVE", "ERROR"] as const).optional(),
});

type FormValues = z.infer<typeof channelSchema>;

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  channel?: Channel | null;
  isLoading?: boolean;
  onSubmit: (values: FormValues) => void;
};

export function ChannelFormDialog({
  open,
  onOpenChange,
  channel,
  isLoading,
  onSubmit,
}: Props) {
  const form = useForm<FormValues>({
    resolver: zodResolver(channelSchema),
    defaultValues: {
      name: channel?.name ?? "",
      type: "WHATSAPP",
      provider: channel?.provider ?? "WHATSAPP_CLOUD_API",
      externalId: channel?.externalId ?? "",
      config: channel?.config ?? "",
      secretsRef: channel?.secretsRef ?? "",
      status: channel?.status ?? "ACTIVE",
    },
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{channel ? "Editar canal" : "Novo canal"}</DialogTitle>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Nome</FormLabel>
                  <FormControl>
                    <Input placeholder="Ex.: Vendas WhatsApp" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="provider"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Provedor</FormLabel>
                  <Select
                    value={field.value}
                    onValueChange={(v) => field.onChange(v as ChannelProvider)}
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Provedor" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {(
                        Object.keys(
                          CHANNEL_PROVIDER_LABELS,
                        ) as ChannelProvider[]
                      ).map((p) => (
                        <SelectItem key={p} value={p}>
                          {CHANNEL_PROVIDER_LABELS[p]}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="externalId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>ID externo (phone_number_id)</FormLabel>
                  <FormControl>
                    <Input
                      placeholder="Identificador do número no provedor"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="config"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Configuração (JSON)</FormLabel>
                  <FormControl>
                    <Textarea placeholder='{"wabaId": "..."}' {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="secretsRef"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Referência de secret</FormLabel>
                  <FormControl>
                    <Input placeholder="vault:token-whatsapp" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            {channel && (
              <FormField
                control={form.control}
                name="status"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Status</FormLabel>
                    <Select
                      value={field.value}
                      onValueChange={(v) => field.onChange(v as ChannelStatus)}
                    >
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Status" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {(
                          Object.keys(CHANNEL_STATUS_LABELS) as ChannelStatus[]
                        ).map((s) => (
                          <SelectItem key={s} value={s}>
                            {CHANNEL_STATUS_LABELS[s]}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
            )}
            <div className="flex justify-end gap-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
              >
                Cancelar
              </Button>
              <Button type="submit" disabled={isLoading}>
                {isLoading ? "Salvando…" : "Salvar"}
              </Button>
            </div>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
