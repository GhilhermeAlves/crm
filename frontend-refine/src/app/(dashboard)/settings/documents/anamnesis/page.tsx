"use client";

import { useState } from "react";
import Link from "next/link";
import { Pencil, Plus, Trash2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

type AnamnesisModel = {
  id: string;
  name: string;
  active: boolean;
};

const INITIAL_MODELS: AnamnesisModel[] = [{ id: "1", name: "Padrão", active: true }];

export default function AnamnesisListPage() {
  const [models, setModels] = useState<AnamnesisModel[]>(INITIAL_MODELS);

  const toggleActive = (id: string) => {
    setModels((prev) => prev.map((m) => (m.id === id ? { ...m, active: !m.active } : m)));
  };

  const handleDelete = (id: string) => {
    setModels((prev) => prev.filter((m) => m.id !== id));
  };

  const handleCreate = () => {
    const newModel: AnamnesisModel = {
      id: crypto.randomUUID(),
      name: `Modelo ${models.length + 1}`,
      active: true,
    };
    setModels((prev) => [...prev, newModel]);
  };

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h2 className="text-xl font-semibold">Modelos de anamnese</h2>

      <section className="space-y-4 rounded-lg border bg-card p-5">
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">Gestão de modelos de anamneses</p>
          <Button size="sm" onClick={handleCreate}>
            <Plus className="mr-1.5 h-3.5 w-3.5" />
            Novo modelo
          </Button>
        </div>

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="text-xs">Nome do modelo</TableHead>
              <TableHead className="text-xs">Modelo ativo?</TableHead>
              <TableHead className="text-right text-xs">Ação</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {models.length === 0 ? (
              <TableRow>
                <TableCell colSpan={3} className="py-8 text-center text-sm text-muted-foreground">
                  Nenhum modelo cadastrado.
                </TableCell>
              </TableRow>
            ) : (
              models.map((m) => (
                <TableRow key={m.id}>
                  <TableCell className="text-sm">{m.name}</TableCell>
                  <TableCell>
                    <Switch checked={m.active} onCheckedChange={() => toggleActive(m.id)} />
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="inline-flex gap-1">
                      <Button variant="ghost" size="icon" className="h-7 w-7" asChild>
                        <Link href={`/settings/documents/anamnesis/${m.id}`}>
                          <Pencil className="h-3.5 w-3.5" />
                        </Link>
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-7 w-7 text-destructive hover:text-destructive"
                        onClick={() => handleDelete(m.id)}
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
    </div>
  );
}
