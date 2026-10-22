"use client";

import { useRef, useState } from "react";
import { Pencil, Plus, Trash2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
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
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useCompanyId } from "@/features/identity/auth/hooks/useCompanyId";
import { usePermission } from "@/features/identity/auth/hooks/useAuthorization";
import {
  useCreateDocumentTemplate,
  useDeleteDocumentTemplate,
  useDocumentTemplates,
  useUpdateDocumentTemplate,
} from "@/features/clinical/documents/hooks/usePatientDocuments";
import { DOCUMENT_TYPE_LABELS, DocumentType } from "@/features/clinical/documents/types/document.types";

const VARIABLES = [
  "paciente.nome",
  "paciente.cpf",
  "paciente.nascimento",
  "paciente.prontuario",
  "data",
  "data.extenso",
  "cidade",
  "empresa.nome",
  "profissional.nome",
];

export default function DocumentTemplatesPage() {
  const companyId = useCompanyId();
  const canManage = usePermission()("anamnesis:manage");
  const { data: templates = [], isLoading } = useDocumentTemplates(companyId, false);
  const createMutation = useCreateDocumentTemplate(companyId);
  const updateMutation = useUpdateDocumentTemplate(companyId);
  const deleteMutation = useDeleteDocumentTemplate(companyId);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [type, setType] = useState<DocumentType>("OTHER");
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [active, setActive] = useState(true);
  const [pendingDelete, setPendingDelete] = useState<string | null>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const openCreate = () => {
    setEditingId(null);
    setType("OTHER");
    setTitle("");
    setBody("");
    setActive(true);
    setDialogOpen(true);
  };

  const openEdit = (template: any) => {
    setEditingId(template.id);
    setType(template.type);
    setTitle(template.title);
    setBody(template.body);
    setActive(template.active);
    setDialogOpen(true);
  };

  const insertVariable = (variable: string) => {
    const el = textareaRef.current;
    if (!el) return;
    const start = el.selectionStart || 0;
    const end = el.selectionEnd || 0;
    const inserted = `{{${variable}}}`;
    const next = body.slice(0, start) + inserted + body.slice(end);
    setBody(next);
    requestAnimationFrame(() => {
      el.focus();
      el.selectionStart = el.selectionEnd = start + inserted.length;
    });
  };

  const handleSave = () => {
    if (!title.trim() || !body.trim()) return;
    const payload = { type, title: title.trim(), body, active };
    if (editingId) {
      updateMutation.mutate({ templateId: editingId, payload });
    } else {
      createMutation.mutate(payload);
    }
    setDialogOpen(false);
  };

  const handleToggle = (template: any, value: boolean) => {
    updateMutation.mutate({
      templateId: template.id,
      payload: { type: template.type, title: template.title, body: template.body, active: value },
    });
  };

  const handleDelete = (id: string) => {
    deleteMutation.mutate(id);
    setPendingDelete(null);
  };

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <h2 className="text-xl font-semibold">Modelos de documentos</h2>

      <section className="space-y-4 rounded-lg border bg-card p-5">
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">Gerenciamento de modelos para emissão de documentos</p>
          {canManage && (
            <Button size="sm" onClick={openCreate}>
              <Plus className="mr-1.5 h-3.5 w-3.5" />
              Novo modelo
            </Button>
          )}
        </div>

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="text-xs">Título</TableHead>
              <TableHead className="text-xs">Tipo</TableHead>
              <TableHead className="text-xs">Ativo</TableHead>
              <TableHead className="text-right text-xs">Ações</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={4} className="py-6 text-center text-sm text-muted-foreground">
                  Carregando...
                </TableCell>
              </TableRow>
            ) : templates.length === 0 ? (
              <TableRow>
                <TableCell colSpan={4} className="py-8 text-center text-sm text-muted-foreground">
                  Nenhum modelo cadastrado.
                </TableCell>
              </TableRow>
            ) : (
              templates.map((t: any) => (
                <TableRow key={t.id}>
                  <TableCell className="text-sm">{t.title}</TableCell>
                  <TableCell className="text-sm">{DOCUMENT_TYPE_LABELS?.[t.type] || t.type}</TableCell>
                  <TableCell>
                    <Switch checked={!!t.active} onCheckedChange={(v) => handleToggle(t, v)} disabled={!canManage} />
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="inline-flex gap-1">
                      {canManage && (
                        <>
                          <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => openEdit(t)}>
                            <Pencil className="h-3.5 w-3.5" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-7 w-7 text-destructive hover:text-destructive"
                            onClick={() => setPendingDelete(t.id)}
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </Button>
                        </>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </section>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>{editingId ? "Editar modelo" : "Novo modelo de documento"}</DialogTitle>
          </DialogHeader>

          <div className="space-y-4 py-2">
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label>Título</Label>
                <Input value={title} onChange={(e) => setTitle(e.target.value)} />
              </div>
              <div className="space-y-1.5">
                <Label>Tipo</Label>
                <Select value={type} onValueChange={(v) => setType(v as DocumentType)}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {Object.entries(DOCUMENT_TYPE_LABELS || {}).map(([k, v]) => (
                      <SelectItem key={k} value={k}>
                        {v}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <Label>Conteúdo</Label>
                <div className="flex flex-wrap gap-1">
                  {VARIABLES.map((v) => (
                    <Button key={v} variant="outline" size="sm" onClick={() => insertVariable(v)}>
                      {"{{"}{v}{"}}"}
                    </Button>
                  ))}
                </div>
              </div>
              <Textarea ref={textareaRef} rows={10} maxLength={9000} value={body} onChange={(e) => setBody(e.target.value)} />
            </div>
            <div className="flex items-center gap-2">
              <Switch checked={active} onCheckedChange={setActive} />
              <Label>Ativo</Label>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogOpen(false)}>
              Cancelar
            </Button>
            <Button onClick={handleSave} disabled={!title.trim() || !body.trim()}>
              Salvar
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <AlertDialog open={!!pendingDelete} onOpenChange={() => setPendingDelete(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Excluir modelo</AlertDialogTitle>
            <AlertDialogDescription>Deseja excluir este modelo?</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction onClick={() => pendingDelete && handleDelete(pendingDelete)}>Excluir</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
