"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { format, parseISO } from "date-fns";
import { useAuth } from "@/features/identity/auth/hooks/useAuth";
import { isAdminLevelRole } from "@/features/identity/members/lib/labels";
import {
  useDeletePatientDocument,
  useDocumentTemplates,
  useIssuePatientDocument,
  useOpenPatientDocumentPdf,
  usePatientDocuments,
  usePreviewPatientDocument,
} from "../hooks/usePatientDocuments";
import { DOCUMENT_TYPE_LABELS, DocumentType } from "../types/document.types";
import Link from "next/link";
import type { Contact } from "@/features/masterdata/contacts/types/contact.types";

export function PatientDocumentsTab({
  companyId,
  contactId,
  patient,
}: {
  companyId: string;
  contactId: string;
  patient: Contact;
}) {
  const { user } = useAuth();
  const { data: templates = [] } = useDocumentTemplates(companyId, true);
  const { data: documents = [] } = usePatientDocuments(companyId, contactId);
  const previewMutation = usePreviewPatientDocument(companyId, contactId);
  const issueMutation = useIssuePatientDocument(companyId, contactId);
  const deleteMutation = useDeletePatientDocument(companyId, contactId);
  const openPdfMutation = useOpenPatientDocumentPdf(companyId, contactId);
  const [open, setOpen] = useState(false);
  const [templateId, setTemplateId] = useState<string>("");
  const [type, setType] = useState<DocumentType>("OTHER");
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [professionalName, setProfessionalName] = useState(user?.name || "");
  const [professionalRegistry, setProfessionalRegistry] = useState("");

  const isAdmin =
    isAdminLevelRole(user?.membershipRole) ||
    (user?.roles ?? []).some((role) => isAdminLevelRole(role));

  const active = templates;
  const grouped = active.reduce<Record<DocumentType, typeof templates>>(
    (acc, t) => {
      acc[t.type] = acc[t.type] || [];
      acc[t.type].push(t);
      return acc;
    },
    {} as Record<DocumentType, typeof templates>,
  );

  const handleSelectTemplate = async (id: string) => {
    setTemplateId(id);
    const t = templates.find((x) => x.id === id);
    if (t) setType(t.type);
    const res = await previewMutation.mutateAsync(id);
    setTitle(res.title);
    setBody(res.body);
    setType(res.type);
  };

  const handleIssue = () => {
    issueMutation.mutate(
      {
        templateId: templateId || null,
        type,
        title,
        body,
        professionalName,
        professionalRegistry: professionalRegistry || null,
      },
      {
        onSuccess: () => {
          setOpen(false);
          setTemplateId("");
          setTitle("");
          setBody("");
          setProfessionalRegistry("");
        },
      },
    );
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">Documentos</h2>
        <Button onClick={() => setOpen(true)}>Emitir documento</Button>
      </div>

      {active.length === 0 && (
        <p className="text-sm text-muted-foreground">
          Nenhum modelo de documento ativo.{" "}
          <Link href="/settings/documents">Cadastrar modelos</Link>
        </p>
      )}

      <div className="space-y-2">
        {[...documents]
          .sort((a, b) => b.issuedAt.localeCompare(a.issuedAt))
          .map((doc) => (
            <Card key={doc.id}>
              <CardContent className="flex items-center justify-between gap-4 pt-6">
                <div>
                  <p className="font-medium">{doc.title}</p>
                  <p className="text-sm text-muted-foreground">
                    {DOCUMENT_TYPE_LABELS[doc.type]} • {doc.professionalName} •{" "}
                    {format(parseISO(doc.issuedAt), "dd/MM/yyyy")}
                  </p>
                </div>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => openPdfMutation.mutate(doc.id)}
                  >
                    Abrir PDF
                  </Button>
                  {isAdmin && (
                    <AlertDialog>
                      <AlertDialogTrigger asChild>
                        <Button variant="ghost" size="sm" className="text-destructive">
                          Excluir
                        </Button>
                      </AlertDialogTrigger>
                      <AlertDialogContent>
                        <AlertDialogHeader>
                          <AlertDialogTitle>Excluir documento?</AlertDialogTitle>
                          <AlertDialogDescription>
                            Esta ação não pode ser desfeita.
                          </AlertDialogDescription>
                        </AlertDialogHeader>
                        <AlertDialogFooter>
                          <AlertDialogCancel>Cancelar</AlertDialogCancel>
                          <AlertDialogAction onClick={() => deleteMutation.mutate(doc.id)}>
                            Excluir
                          </AlertDialogAction>
                        </AlertDialogFooter>
                      </AlertDialogContent>
                    </AlertDialog>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
      </div>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Emitir documento</DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <div className="space-y-2">
              <Label>Modelo</Label>
              <Select value={templateId} onValueChange={handleSelectTemplate}>
                <SelectTrigger>
                  <SelectValue placeholder="Selecione um modelo ativo" />
                </SelectTrigger>
                <SelectContent>
                  {(Object.keys(grouped) as DocumentType[]).map((key) => (
                    <div key={key}>
                      <p className="px-2 py-1 text-xs text-muted-foreground">
                        {DOCUMENT_TYPE_LABELS[key]}
                      </p>
                      {grouped[key].map((t) => (
                        <SelectItem key={t.id} value={t.id}>
                          {t.title}
                        </SelectItem>
                      ))}
                    </div>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Título</Label>
              <Input value={title} onChange={(e) => setTitle(e.target.value)} />
            </div>
            <div className="space-y-2">
              <Label>Corpo</Label>
              <Textarea value={body} onChange={(e) => setBody(e.target.value)} rows={10} />
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="space-y-2">
                <Label>Profissional</Label>
                <Input
                  value={professionalName}
                  onChange={(e) => setProfessionalName(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label>Registro (CRO)</Label>
                <Input
                  value={professionalRegistry}
                  onChange={(e) => setProfessionalRegistry(e.target.value)}
                />
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setOpen(false)}>
              Cancelar
            </Button>
            <Button
              onClick={handleIssue}
              disabled={
                issueMutation.isPending || !title.trim() || !body.trim() || !professionalName.trim()
              }
            >
              Emitir
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
