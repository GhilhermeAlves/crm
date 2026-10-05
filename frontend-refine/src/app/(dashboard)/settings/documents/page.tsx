"use client";

import { useState } from "react";
import { Pencil, Plus, Trash2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

type DocumentTemplate = {
  id: string;
  title: string;
  content: string;
};

const INITIAL_TEMPLATES: DocumentTemplate[] = [
  {
    id: "1",
    title: "TERMO PARA TRATAMENTO DE DADOS",
    content:
      "Autorizo o uso dos meus dados pessoais para fins de atendimento e comunicação pela empresa.",
  },
  {
    id: "2",
    title: "TERMO DE CONSENTIMENTO",
    content:
      "Declaro que fui devidamente informado(a) sobre o procedimento e autorizo sua realização.",
  },
];

export default function DocumentTemplatesPage() {
  const [templates, setTemplates] = useState<DocumentTemplate[]>(INITIAL_TEMPLATES);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<DocumentTemplate | null>(null);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");

  const openCreate = () => {
    setEditingTemplate(null);
    setTitle("");
    setContent("");
    setDialogOpen(true);
  };

  const openEdit = (template: DocumentTemplate) => {
    setEditingTemplate(template);
    setTitle(template.title);
    setContent(template.content);
    setDialogOpen(true);
  };

  const handleSave = () => {
    if (!title.trim()) return;
    if (editingTemplate) {
      setTemplates((prev) =>
        prev.map((t) =>
          t.id === editingTemplate.id ? { ...t, title: title.trim(), content } : t,
        ),
      );
    } else {
      setTemplates((prev) => [
        ...prev,
        { id: crypto.randomUUID(), title: title.trim(), content },
      ]);
    }
    setDialogOpen(false);
  };

  const handleDelete = (id: string) => {
    setTemplates((prev) => prev.filter((t) => t.id !== id));
  };

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h2 className="text-xl font-semibold">Modelos de documentos</h2>

      <section className="space-y-4 rounded-lg border bg-card p-5">
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Gestão de modelos de documentos
          </p>
          <Button size="sm" onClick={openCreate}>
            <Plus className="mr-1.5 h-3.5 w-3.5" />
            Novo modelo
          </Button>
        </div>

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="text-xs">Título</TableHead>
              <TableHead className="text-right text-xs">Ação</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {templates.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={2}
                  className="py-8 text-center text-sm text-muted-foreground"
                >
                  Nenhum modelo cadastrado.
                </TableCell>
              </TableRow>
            ) : (
              templates.map((t) => (
                <TableRow key={t.id}>
                  <TableCell className="text-sm">{t.title}</TableCell>
                  <TableCell className="text-right">
                    <div className="inline-flex gap-1">
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-7 w-7"
                        onClick={() => openEdit(t)}
                      >
                        <Pencil className="h-3.5 w-3.5" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-7 w-7 text-destructive hover:text-destructive"
                        onClick={() => handleDelete(t.id)}
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </section>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>
              {editingTemplate ? "Editar modelo" : "Novo modelo de documento"}
            </DialogTitle>
          </DialogHeader>

          <div className="space-y-4 py-2">
            <div className="space-y-1.5">
              <Label>Título</Label>
              <Input
                placeholder="Ex.: Termo de consentimento"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label>Conteúdo</Label>
              <Textarea
                placeholder="Escreva o conteúdo do documento..."
                rows={6}
                value={content}
                onChange={(e) => setContent(e.target.value)}
              />
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogOpen(false)}>
              Cancelar
            </Button>
            <Button onClick={handleSave} disabled={!title.trim()}>
              Salvar
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
