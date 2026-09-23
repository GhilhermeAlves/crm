"use client";

import { useState } from "react";
import { MessageSquareOff, Plus } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorCard } from "@/components/common/ErrorCard";
import { SkeletonTable } from "@/components/feedback/SkeletonTable";
import { ChannelFormDialog } from "@/features/omnichannel/components/ChannelFormDialog";
import { ChannelStatusBadge } from "@/features/omnichannel/components/ChannelStatusBadge";
import {
  useChannels,
  useCreateChannel,
  useDeleteChannel,
  useOmnichannelPermissions,
  useUpdateChannel,
} from "@/features/omnichannel/hooks/useOmnichannel";
import {
  CHANNEL_PROVIDER_LABELS,
  type Channel,
} from "@/features/omnichannel/types/omnichannel.types";

export default function ChannelsPage() {
  const { data: channels, isLoading, error, refetch } = useChannels();
  const createChannel = useCreateChannel();
  const updateChannel = useUpdateChannel();
  const deleteChannel = useDeleteChannel();
  const { canCreate, canUpdate, canDelete } = useOmnichannelPermissions();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Channel | null>(null);
  const [toDelete, setToDelete] = useState<Channel | null>(null);

  const openCreate = () => {
    setEditing(null);
    setDialogOpen(true);
  };

  const openEdit = (channel: Channel) => {
    setEditing(channel);
    setDialogOpen(true);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Canais</h1>
          <p className="text-sm text-muted-foreground">
            Conecte números de WhatsApp para atender conversas no Inbox.
          </p>
        </div>
        {canCreate && (
          <Button onClick={openCreate}>
            <Plus className="mr-1 h-4 w-4" /> Novo canal
          </Button>
        )}
      </div>

      {isLoading ? (
        <SkeletonTable rows={5} columns={4} />
      ) : error ? (
        <ErrorCard message={error.message} onRetry={() => refetch()} />
      ) : (
        <Card>
          <CardContent className="p-0">
            {channels && channels.length > 0 ? (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Nome</TableHead>
                    <TableHead>Provedor</TableHead>
                    <TableHead>ID externo</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="text-right">Ações</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {channels.map((channel) => (
                    <TableRow key={channel.id}>
                      <TableCell className="font-medium">{channel.name}</TableCell>
                      <TableCell>{CHANNEL_PROVIDER_LABELS[channel.provider]}</TableCell>
                      <TableCell className="text-muted-foreground">
                        {channel.externalId ?? "—"}
                      </TableCell>
                      <TableCell>
                        <ChannelStatusBadge status={channel.status} />
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          {canUpdate && (
                            <Button variant="outline" size="sm" onClick={() => openEdit(channel)}>
                              Editar
                            </Button>
                          )}
                          {canDelete && (
                            <Button
                              variant="outline"
                              size="sm"
                              className="text-destructive"
                              onClick={() => setToDelete(channel)}
                            >
                              Excluir
                            </Button>
                          )}
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            ) : (
              <EmptyState
                icon={<MessageSquareOff className="h-8 w-8" />}
                title="Nenhum canal configurado"
                description="Crie um canal para começar a receber e responder mensagens."
              />
            )}
          </CardContent>
        </Card>
      )}

      <ChannelFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        channel={editing}
        isLoading={createChannel.isPending || updateChannel.isPending}
        onSubmit={(values) => {
          if (editing) {
            updateChannel.mutate({ id: editing.id, data: values });
          } else {
            createChannel.mutate(values);
          }
          setDialogOpen(false);
        }}
      />

      <ConfirmDialog
        open={!!toDelete}
        onOpenChange={(open) => !open && setToDelete(null)}
        title="Excluir canal"
        description={`Excluir o canal "${toDelete?.name}"? Essa ação não pode ser desfeita.`}
        confirmLabel="Excluir"
        variant="destructive"
        isLoading={deleteChannel.isPending}
        onConfirm={() => {
          if (toDelete) deleteChannel.mutate(toDelete.id);
          setToDelete(null);
        }}
      />
    </div>
  );
}
