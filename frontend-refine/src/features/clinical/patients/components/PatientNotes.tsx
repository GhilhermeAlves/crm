"use client";

import { useMemo, useState } from "react";
import { format, formatDistanceToNow, parseISO } from "date-fns";
import { ptBR } from "date-fns/locale";
import { Loader2, Pencil, Plus, StickyNote, Trash2 } from "lucide-react";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Textarea } from "@/components/ui/textarea";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { usePermission } from "@/features/identity/auth/hooks/useAuthorization";
import { isAdminLevelRole } from "@/features/identity/members/lib/labels";
import {
  useContactNotes,
  useCreateContactNote,
  useDeleteContactNote,
  useUpdateContactNote,
} from "../hooks/usePatient";
import type { ContactNote } from "../types/patient.types";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

const MAX_LENGTH = 5000;

function NoteItem({
  note,
  canEdit,
  onSave,
  onDelete,
  isSaving,
}: {
  note: ContactNote;
  canEdit: boolean;
  onSave: (body: string) => void;
  onDelete: () => void;
  isSaving: boolean;
}) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(note.body);

  const createdAt = parseISO(note.createdAt);
  const relative = formatDistanceToNow(createdAt, { addSuffix: true, locale: ptBR });
  const absolute = format(createdAt, "dd/MM/yyyy 'às' HH:mm");

  return (
    <li className="rounded-lg border p-4">
      <div className="mb-2 flex items-start justify-between gap-2">
        <div className="min-w-0">
          <p className="text-sm font-medium">{note.createdByName || "Usuário"}</p>
          <p className="text-xs text-muted-foreground" title={absolute}>
            {relative}
          </p>
        </div>
        {canEdit && !editing && (
          <div className="flex shrink-0 gap-1">
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="h-8 w-8"
              aria-label="Editar anotação"
              onClick={() => {
                setDraft(note.body);
                setEditing(true);
              }}
            >
              <Pencil className="h-4 w-4" />
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="h-8 w-8 text-destructive hover:text-destructive"
              aria-label="Excluir anotação"
              onClick={onDelete}
            >
              <Trash2 className="h-4 w-4" />
            </Button>
          </div>
        )}
      </div>

      {editing ? (
        <div className="space-y-2">
          <Textarea
            value={draft}
            maxLength={MAX_LENGTH}
            rows={3}
            onChange={(e) => setDraft(e.target.value)}
          />
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" size="sm" onClick={() => setEditing(false)}>
              Cancelar
            </Button>
            <Button
              type="button"
              size="sm"
              disabled={isSaving || !draft.trim()}
              onClick={() => {
                onSave(draft.trim());
                setEditing(false);
              }}
            >
              Salvar
            </Button>
          </div>
        </div>
      ) : (
        <p className="whitespace-pre-wrap text-sm">{note.body}</p>
      )}
    </li>
  );
}

export function PatientNotes({ contact }: { contact: Contact }) {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const contactId = contact.id;
  const can = usePermission();
  const canUpdate = can("contact:update");

  const { data: notes = [], isLoading } = useContactNotes(companyId, contactId);
  const createNote = useCreateContactNote(companyId, contactId);
  const updateNote = useUpdateContactNote(companyId, contactId);
  const deleteNote = useDeleteContactNote(companyId, contactId);

  const [draft, setDraft] = useState("");
  const [pendingDelete, setPendingDelete] = useState<string | null>(null);

  const isAdmin =
    isAdminLevelRole(user?.membershipRole) ||
    (user?.roles ?? []).some((role) => isAdminLevelRole(role));

  const sorted = useMemo(
    () => [...notes].sort((a, b) => b.createdAt.localeCompare(a.createdAt)),
    [notes],
  );

  const handleAdd = () => {
    const body = draft.trim();
    if (!body) return;
    createNote.mutate({ body }, { onSuccess: () => setDraft("") });
  };

  return (
    <div className="space-y-6">
      <h1 className="text-lg font-semibold">Anotações</h1>

      {contact.notes && contact.notes.trim() !== "" && (
        <Card>
          <CardContent className="flex gap-3 p-4">
            <StickyNote className="h-4 w-4 shrink-0 text-muted-foreground" />
            <div className="min-w-0">
              <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
                Observação do cadastro
              </p>
              <p className="mt-1 whitespace-pre-wrap text-sm">{contact.notes}</p>
            </div>
          </CardContent>
        </Card>
      )}

      {canUpdate && (
        <div className="space-y-2">
          <Textarea
            value={draft}
            maxLength={MAX_LENGTH}
            rows={3}
            placeholder="Escreva uma anotação sobre o paciente…"
            onChange={(e) => setDraft(e.target.value)}
          />
          <div className="flex items-center justify-between">
            <span className="text-xs tabular-nums text-muted-foreground">
              {draft.length}/{MAX_LENGTH}
            </span>
            <Button
              size="sm"
              className="gap-1.5"
              disabled={createNote.isPending || !draft.trim()}
              onClick={handleAdd}
            >
              {createNote.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Plus className="h-4 w-4" />
              )}
              Adicionar anotação
            </Button>
          </div>
        </div>
      )}

      {isLoading ? (
        <div className="flex items-center justify-center gap-2 py-20 text-muted-foreground">
          <Loader2 className="h-5 w-5 animate-spin" /> Carregando anotações…
        </div>
      ) : sorted.length === 0 ? (
        <p className="py-16 text-center text-sm text-muted-foreground">
          O paciente não possui anotações adicionadas.
        </p>
      ) : (
        <ul className="space-y-3">
          {sorted.map((note) => (
            <NoteItem
              key={note.id}
              note={note}
              canEdit={canUpdate && (note.createdBy === user?.id || isAdmin)}
              isSaving={updateNote.isPending}
              onSave={(body) => updateNote.mutate({ noteId: note.id, data: { body } })}
              onDelete={() => setPendingDelete(note.id)}
            />
          ))}
        </ul>
      )}

      <AlertDialog open={!!pendingDelete} onOpenChange={(open) => !open && setPendingDelete(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Excluir anotação?</AlertDialogTitle>
            <AlertDialogDescription>
              Esta anotação será removida definitivamente. Esta ação não pode ser desfeita.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              onClick={() => {
                if (pendingDelete) deleteNote.mutate(pendingDelete);
                setPendingDelete(null);
              }}
            >
              Excluir
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
