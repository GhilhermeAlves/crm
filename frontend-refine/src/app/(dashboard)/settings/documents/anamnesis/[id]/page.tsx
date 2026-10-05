"use client";

import { useState } from "react";
import Link from "next/link";
import { ChevronUp, GripVertical, MoreVertical, Plus, Trash2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

type QuestionType = "text" | "yes_no" | "yes_no_text" | "number" | "select";

const QUESTION_TYPE_LABELS: Record<QuestionType, string> = {
  text: "Somente texto",
  yes_no: "Sim/Não",
  yes_no_text: "Sim/Não e Texto",
  number: "Número",
  select: "Seleção",
};

type SubQuestion = {
  id: string;
  text: string;
  highlightOnYes: boolean;
};

type Question = {
  id: string;
  text: string;
  type: QuestionType;
  subQuestion: SubQuestion | null;
};

type Section = {
  id: string;
  title: string;
  questions: Question[];
};

const INITIAL_SECTIONS: Section[] = [
  {
    id: "s1",
    title: "Início",
    questions: [
      { id: "q1", text: "Qual é o motivo da consulta?", type: "text", subQuestion: null },
    ],
  },
  {
    id: "s2",
    title: "Histórico médico",
    questions: [
      {
        id: "q2",
        text: "Está realizando algum tratamento médico?",
        type: "yes_no_text",
        subQuestion: {
          id: "sq1",
          text: "Qual?",
          highlightOnYes: true,
        },
      },
      {
        id: "q3",
        text: "Está fazendo uso de algum medicamento?",
        type: "yes_no_text",
        subQuestion: {
          id: "sq2",
          text: "Qual?",
          highlightOnYes: true,
        },
      },
      {
        id: "q4",
        text: "Possui alergia a algum medicamento?",
        type: "yes_no_text",
        subQuestion: {
          id: "sq3",
          text: "Qual?",
          highlightOnYes: true,
        },
      },
      {
        id: "q5",
        text: "Tem diabetes?",
        type: "yes_no",
        subQuestion: null,
      },
    ],
  },
];

export default function AnamnesisEditorPage() {
  const [modelName, setModelName] = useState("Padrão");
  const [sections, setSections] = useState<Section[]>(INITIAL_SECTIONS);

  const addSection = () => {
    setSections((prev) => [
      ...prev,
      {
        id: crypto.randomUUID(),
        title: "",
        questions: [],
      },
    ]);
  };

  const updateSectionTitle = (sectionId: string, title: string) => {
    setSections((prev) => prev.map((s) => (s.id === sectionId ? { ...s, title } : s)));
  };

  const deleteSection = (sectionId: string) => {
    setSections((prev) => prev.filter((s) => s.id !== sectionId));
  };

  const addQuestion = (sectionId: string) => {
    setSections((prev) =>
      prev.map((s) =>
        s.id === sectionId
          ? {
              ...s,
              questions: [
                ...s.questions,
                {
                  id: crypto.randomUUID(),
                  text: "",
                  type: "text" as QuestionType,
                  subQuestion: null,
                },
              ],
            }
          : s,
      ),
    );
  };

  const updateQuestion = (sectionId: string, questionId: string, updates: Partial<Question>) => {
    setSections((prev) =>
      prev.map((s) =>
        s.id === sectionId
          ? {
              ...s,
              questions: s.questions.map((q) => (q.id === questionId ? { ...q, ...updates } : q)),
            }
          : s,
      ),
    );
  };

  const deleteQuestion = (sectionId: string, questionId: string) => {
    setSections((prev) =>
      prev.map((s) =>
        s.id === sectionId
          ? { ...s, questions: s.questions.filter((q) => q.id !== questionId) }
          : s,
      ),
    );
  };

  const toggleSubQuestion = (sectionId: string, questionId: string) => {
    setSections((prev) =>
      prev.map((s) =>
        s.id === sectionId
          ? {
              ...s,
              questions: s.questions.map((q) =>
                q.id === questionId
                  ? {
                      ...q,
                      subQuestion: q.subQuestion
                        ? null
                        : { id: crypto.randomUUID(), text: "Qual?", highlightOnYes: true },
                    }
                  : q,
              ),
            }
          : s,
      ),
    );
  };

  const updateSubQuestion = (
    sectionId: string,
    questionId: string,
    updates: Partial<SubQuestion>,
  ) => {
    setSections((prev) =>
      prev.map((s) =>
        s.id === sectionId
          ? {
              ...s,
              questions: s.questions.map((q) =>
                q.id === questionId && q.subQuestion
                  ? { ...q, subQuestion: { ...q.subQuestion, ...updates } }
                  : q,
              ),
            }
          : s,
      ),
    );
  };

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold">Editar modelo de anamnese</h2>
        <Button asChild variant="outline" size="sm">
          <Link href="/settings/documents/anamnesis">Voltar</Link>
        </Button>
      </div>

      <section className="space-y-4 rounded-lg border bg-card p-5">
        <div className="space-y-1.5">
          <Label>Nome do modelo</Label>
          <Input value={modelName} onChange={(e) => setModelName(e.target.value)} maxLength={100} />
          <p className="text-xs text-primary">
            É o nome que aparece na lista de modelos e na ficha do paciente.
          </p>
        </div>
      </section>

      {sections.map((section, sIdx) => (
        <section key={section.id} className="space-y-3 rounded-lg border bg-card p-5">
          <div className="flex items-center gap-2">
            <GripVertical className="h-4 w-4 shrink-0 cursor-grab text-muted-foreground" />
            <Input
              value={section.title}
              onChange={(e) => updateSectionTitle(section.id, e.target.value)}
              placeholder="Nome da seção"
              className="flex-1 font-medium"
              maxLength={100}
            />
            <span className="shrink-0 text-xs text-muted-foreground">
              Seção {sIdx + 1} · {section.questions.length}{" "}
              {section.questions.length === 1 ? "pergunta" : "perguntas"}
            </span>
            <ChevronUp className="h-4 w-4 shrink-0 text-muted-foreground" />
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" className="h-7 w-7">
                  <MoreVertical className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem
                  className="text-destructive"
                  onClick={() => deleteSection(section.id)}
                >
                  <Trash2 className="mr-2 h-3.5 w-3.5" />
                  Excluir seção
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>

          {section.questions.map((question) => (
            <div key={question.id} className="space-y-2">
              <div className="flex items-center gap-2">
                <GripVertical className="h-4 w-4 shrink-0 cursor-grab text-muted-foreground" />
                <Input
                  value={question.text}
                  onChange={(e) =>
                    updateQuestion(section.id, question.id, { text: e.target.value })
                  }
                  placeholder="Digite a pergunta"
                  className="flex-1"
                  maxLength={255}
                />
                <Select
                  value={question.type}
                  onValueChange={(val: QuestionType) =>
                    updateQuestion(section.id, question.id, { type: val })
                  }
                >
                  <SelectTrigger className="w-40">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {Object.entries(QUESTION_TYPE_LABELS).map(([value, label]) => (
                      <SelectItem key={value} value={value}>
                        {label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="icon" className="h-7 w-7">
                      <MoreVertical className="h-4 w-4" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={() => toggleSubQuestion(section.id, question.id)}>
                      {question.subQuestion
                        ? "Remover pergunta auxiliar"
                        : "Adicionar pergunta auxiliar"}
                    </DropdownMenuItem>
                    <DropdownMenuItem
                      className="text-destructive"
                      onClick={() => deleteQuestion(section.id, question.id)}
                    >
                      <Trash2 className="mr-2 h-3.5 w-3.5" />
                      Excluir
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>

              {question.subQuestion && (
                <div className="ml-8 space-y-2 rounded-md border-l-2 border-primary/20 bg-muted/30 p-3">
                  <p className="text-xs font-semibold text-primary">Pergunta auxiliar</p>
                  <Input
                    value={question.subQuestion.text}
                    onChange={(e) =>
                      updateSubQuestion(section.id, question.id, {
                        text: e.target.value,
                      })
                    }
                    placeholder="Pergunta auxiliar"
                    maxLength={255}
                  />
                  <div className="flex items-center gap-2">
                    <Switch
                      checked={question.subQuestion.highlightOnYes}
                      onCheckedChange={(checked) =>
                        updateSubQuestion(section.id, question.id, {
                          highlightOnYes: checked,
                        })
                      }
                    />
                    <span className="text-xs text-muted-foreground">
                      Destacar no prontuário quando a resposta for &quot;Sim&quot;
                    </span>
                  </div>
                </div>
              )}
            </div>
          ))}

          <div className="flex items-center justify-center gap-6 pt-2">
            <button
              onClick={addSection}
              className="text-xs font-medium text-primary hover:underline"
            >
              ≡ Nova seção
            </button>
            <button
              onClick={() => addQuestion(section.id)}
              className="text-xs font-medium text-primary hover:underline"
            >
              + Nova pergunta
            </button>
          </div>
        </section>
      ))}

      {sections.length === 0 && (
        <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed p-8 text-center">
          <p className="text-sm text-muted-foreground">Nenhuma seção criada ainda.</p>
          <Button size="sm" variant="outline" onClick={addSection}>
            <Plus className="mr-1.5 h-3.5 w-3.5" />
            Nova seção
          </Button>
        </div>
      )}

      <div className="flex justify-end">
        <Button>Salvar modelo</Button>
      </div>
    </div>
  );
}
