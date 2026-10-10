"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { GripVertical, Plus, Trash2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  ANAMNESIS_COMPLEMENT_TRIGGER_LABELS,
  ANAMNESIS_QUESTION_TYPE_LABELS,
  isSelectable,
  type AnamnesisComplementTrigger,
  type AnamnesisModel,
  type AnamnesisQuestionType,
  type UpdateAnamnesisModelRequest,
} from "../types/anamnesis.types";

type OptionDraft = { id: string; label: string };

type QuestionDraft = {
  id: string;
  text: string;
  type: AnamnesisQuestionType;
  required: boolean;
  highlight: boolean;
  allowComplement: boolean;
  complementLabel: string;
  complementTrigger: AnamnesisComplementTrigger;
  complementOptionValue: string;
  options: OptionDraft[];
};

type SectionDraft = {
  id: string;
  title: string;
  professional: boolean;
  questions: QuestionDraft[];
};

type EditorState = {
  name: string;
  description: string;
  sections: SectionDraft[];
};

const QUESTION_TYPES = Object.keys(ANAMNESIS_QUESTION_TYPE_LABELS) as AnamnesisQuestionType[];
const TRIGGERS = Object.keys(ANAMNESIS_COMPLEMENT_TRIGGER_LABELS) as AnamnesisComplementTrigger[];

function toEditorState(model: AnamnesisModel): EditorState {
  return {
    name: model.name,
    description: model.description ?? "",
    sections: model.sections.map((section) => ({
      id: section.id,
      title: section.title,
      professional: section.professional,
      questions: section.questions.map((question) => ({
        id: question.id,
        text: question.text,
        type: question.type,
        required: question.required,
        highlight: question.highlight,
        allowComplement: question.allowComplement,
        complementLabel: question.complementLabel ?? "",
        complementTrigger: question.complementTrigger,
        complementOptionValue: question.complementOptionValue ?? "",
        options: question.options.map((option) => ({ id: option.id, label: option.label })),
      })),
    })),
  };
}

function emptyQuestion(): QuestionDraft {
  return {
    id: crypto.randomUUID(),
    text: "",
    type: "SHORT_TEXT",
    required: false,
    highlight: false,
    allowComplement: false,
    complementLabel: "",
    complementTrigger: "YES",
    complementOptionValue: "",
    options: [],
  };
}

function toRequest(state: EditorState): UpdateAnamnesisModelRequest {
  return {
    name: state.name.trim(),
    description: state.description.trim() || undefined,
    sections: state.sections.map((section) => ({
      id: section.id,
      title: section.title.trim(),
      professional: section.professional,
      questions: section.questions.map((question) => {
        const selectable = isSelectable(question.type);
        return {
          id: question.id,
          text: question.text.trim(),
          type: question.type,
          required: question.required,
          highlight: question.highlight,
          allowComplement: question.allowComplement,
          complementLabel: question.allowComplement
            ? question.complementLabel.trim() || undefined
            : undefined,
          complementTrigger: question.complementTrigger,
          complementOptionValue:
            question.allowComplement && question.complementTrigger === "OPTION"
              ? question.complementOptionValue
              : undefined,
          options: selectable
            ? question.options
                .filter((option) => option.label.trim())
                .map((option) => ({
                  id: option.id,
                  label: option.label.trim(),
                  value: option.label.trim(),
                }))
            : undefined,
        };
      }),
    })),
  };
}

type Props = {
  model: AnamnesisModel;
  isSaving: boolean;
  onSave: (request: UpdateAnamnesisModelRequest) => void;
  onBack: () => void;
};

export function AnamnesisModelEditor({ model, isSaving, onSave, onBack }: Props) {
  const [state, setState] = useState<EditorState>(() => toEditorState(model));

  useEffect(() => {
    setState(toEditorState(model));
  }, [model]);

  const patchSection = (sectionId: string, patch: Partial<SectionDraft>) =>
    setState((prev) => ({
      ...prev,
      sections: prev.sections.map((s) => (s.id === sectionId ? { ...s, ...patch } : s)),
    }));

  const patchQuestion = (sectionId: string, questionId: string, patch: Partial<QuestionDraft>) =>
    setState((prev) => ({
      ...prev,
      sections: prev.sections.map((s) =>
        s.id === sectionId
          ? {
              ...s,
              questions: s.questions.map((q) => (q.id === questionId ? { ...q, ...patch } : q)),
            }
          : s,
      ),
    }));

  const addSection = () =>
    setState((prev) => ({
      ...prev,
      sections: [
        ...prev.sections,
        { id: crypto.randomUUID(), title: "", professional: false, questions: [] },
      ],
    }));

  const deleteSection = (sectionId: string) =>
    setState((prev) => ({ ...prev, sections: prev.sections.filter((s) => s.id !== sectionId) }));

  const addQuestion = (sectionId: string) =>
    setState((prev) => ({
      ...prev,
      sections: prev.sections.map((s) =>
        s.id === sectionId ? { ...s, questions: [...s.questions, emptyQuestion()] } : s,
      ),
    }));

  const deleteQuestion = (sectionId: string, questionId: string) =>
    setState((prev) => ({
      ...prev,
      sections: prev.sections.map((s) =>
        s.id === sectionId
          ? { ...s, questions: s.questions.filter((q) => q.id !== questionId) }
          : s,
      ),
    }));

  const addOption = (sectionId: string, questionId: string) =>
    patchQuestion(sectionId, questionId, {
      options: [
        ...(findQuestion(state, sectionId, questionId)?.options ?? []),
        { id: crypto.randomUUID(), label: "" },
      ],
    });

  const updateOption = (sectionId: string, questionId: string, optionId: string, label: string) => {
    const question = findQuestion(state, sectionId, questionId);
    if (!question) return;
    patchQuestion(sectionId, questionId, {
      options: question.options.map((o) => (o.id === optionId ? { ...o, label } : o)),
    });
  };

  const deleteOption = (sectionId: string, questionId: string, optionId: string) => {
    const question = findQuestion(state, sectionId, questionId);
    if (!question) return;
    patchQuestion(sectionId, questionId, {
      options: question.options.filter((o) => o.id !== optionId),
    });
  };

  const handleSave = () => {
    if (!state.name.trim()) {
      toast.error("Informe o nome do modelo.");
      return;
    }
    for (const section of state.sections) {
      if (!section.title.trim()) {
        toast.error("Toda seção precisa de um título.");
        return;
      }
      for (const question of section.questions) {
        if (!question.text.trim()) {
          toast.error(`Há uma pergunta sem texto na seção “${section.title}”.`);
          return;
        }
        if (isSelectable(question.type) && question.options.every((o) => !o.label.trim())) {
          toast.error(`A pergunta “${question.text}” precisa de pelo menos uma opção.`);
          return;
        }
        if (question.allowComplement && question.complementTrigger === "OPTION") {
          const values = question.options.map((o) => o.label.trim()).filter(Boolean);
          if (!values.includes(question.complementOptionValue)) {
            toast.error(`Selecione a opção que dispara o complemento em “${question.text}”.`);
            return;
          }
        }
      }
    }
    onSave(toRequest(state));
  };

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 className="text-xl font-semibold">Editar modelo de anamnese</h2>
          {model.isDefault && (
            <p className="text-xs text-muted-foreground">
              Modelo padrão da empresa — edite livremente, as alterações não afetam outras empresas.
            </p>
          )}
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={onBack}>
            Voltar
          </Button>
          <Button size="sm" onClick={handleSave} disabled={isSaving}>
            {isSaving ? "Salvando…" : "Salvar modelo"}
          </Button>
        </div>
      </div>

      <section className="space-y-4 rounded-lg border bg-card p-5">
        <div className="space-y-1.5">
          <Label htmlFor="model-name">Nome do modelo</Label>
          <Input
            id="model-name"
            value={state.name}
            onChange={(e) => setState((prev) => ({ ...prev, name: e.target.value }))}
            maxLength={120}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="model-description">Descrição</Label>
          <Textarea
            id="model-description"
            value={state.description}
            onChange={(e) => setState((prev) => ({ ...prev, description: e.target.value }))}
            rows={2}
            maxLength={4000}
          />
        </div>
      </section>

      {state.sections.map((section, sIdx) => (
        <section key={section.id} className="space-y-4 rounded-lg border bg-card p-5">
          <div className="flex items-center gap-2">
            <GripVertical className="h-4 w-4 shrink-0 text-muted-foreground" />
            <Input
              value={section.title}
              onChange={(e) => patchSection(section.id, { title: e.target.value })}
              placeholder="Nome da seção"
              className="flex-1 font-medium"
              maxLength={160}
            />
            <span className="shrink-0 text-xs text-muted-foreground">
              Seção {sIdx + 1} · {section.questions.length}{" "}
              {section.questions.length === 1 ? "pergunta" : "perguntas"}
            </span>
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-destructive hover:text-destructive"
              onClick={() => deleteSection(section.id)}
              aria-label="Excluir seção"
            >
              <Trash2 className="h-3.5 w-3.5" />
            </Button>
          </div>

          <div className="flex items-center gap-2">
            <Switch
              checked={section.professional}
              onCheckedChange={(checked) => patchSection(section.id, { professional: checked })}
            />
            <span className="text-xs text-muted-foreground">
              Seção preenchida pelo profissional (cirurgião-dentista)
            </span>
          </div>

          {section.questions.map((question) => {
            const selectable = isSelectable(question.type);
            return (
              <div key={question.id} className="space-y-3 rounded-md border bg-muted/20 p-3">
                <div className="flex flex-wrap items-center gap-2">
                  <GripVertical className="h-4 w-4 shrink-0 text-muted-foreground" />
                  <Input
                    value={question.text}
                    onChange={(e) =>
                      patchQuestion(section.id, question.id, { text: e.target.value })
                    }
                    placeholder="Digite a pergunta"
                    className="min-w-[12rem] flex-1"
                    maxLength={4000}
                  />
                  <Select
                    value={question.type}
                    onValueChange={(value: AnamnesisQuestionType) =>
                      patchQuestion(section.id, question.id, { type: value })
                    }
                  >
                    <SelectTrigger className="w-48">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {QUESTION_TYPES.map((type) => (
                        <SelectItem key={type} value={type}>
                          {ANAMNESIS_QUESTION_TYPE_LABELS[type]}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-7 w-7 text-destructive hover:text-destructive"
                    onClick={() => deleteQuestion(section.id, question.id)}
                    aria-label="Excluir pergunta"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                </div>

                <div className="flex flex-wrap items-center gap-4 pl-6">
                  <label className="flex items-center gap-2 text-xs text-muted-foreground">
                    <Switch
                      checked={question.required}
                      onCheckedChange={(checked) =>
                        patchQuestion(section.id, question.id, { required: checked })
                      }
                    />
                    Obrigatória
                  </label>
                  <label className="flex items-center gap-2 text-xs text-muted-foreground">
                    <Switch
                      checked={question.highlight}
                      onCheckedChange={(checked) =>
                        patchQuestion(section.id, question.id, { highlight: checked })
                      }
                    />
                    Destacar (informação clínica relevante)
                  </label>
                  <label className="flex items-center gap-2 text-xs text-muted-foreground">
                    <Switch
                      checked={question.allowComplement}
                      onCheckedChange={(checked) =>
                        patchQuestion(section.id, question.id, { allowComplement: checked })
                      }
                    />
                    Campo complementar
                  </label>
                </div>

                {question.allowComplement && (
                  <div className="ml-6 space-y-2 rounded-md border-l-2 border-primary/30 bg-background p-3">
                    <p className="text-xs font-semibold text-primary">Campo complementar</p>
                    <div className="grid gap-2 sm:grid-cols-2">
                      <div className="space-y-1.5">
                        <Label className="text-xs">Rótulo do campo</Label>
                        <Input
                          value={question.complementLabel}
                          onChange={(e) =>
                            patchQuestion(section.id, question.id, {
                              complementLabel: e.target.value,
                            })
                          }
                          placeholder="Ex.: Qual?"
                          maxLength={255}
                        />
                      </div>
                      <div className="space-y-1.5">
                        <Label className="text-xs">Exibir quando</Label>
                        <Select
                          value={question.complementTrigger}
                          onValueChange={(value: AnamnesisComplementTrigger) =>
                            patchQuestion(section.id, question.id, { complementTrigger: value })
                          }
                        >
                          <SelectTrigger>
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            {TRIGGERS.map((trigger) => (
                              <SelectItem key={trigger} value={trigger}>
                                {ANAMNESIS_COMPLEMENT_TRIGGER_LABELS[trigger]}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                    </div>
                    {question.complementTrigger === "OPTION" && (
                      <div className="space-y-1.5">
                        <Label className="text-xs">Opção que dispara o complemento</Label>
                        {question.options.length === 0 ? (
                          <p className="text-xs text-muted-foreground">
                            Adicione opções de resposta abaixo para poder escolher.
                          </p>
                        ) : (
                          <Select
                            value={question.complementOptionValue}
                            onValueChange={(value) =>
                              patchQuestion(section.id, question.id, {
                                complementOptionValue: value,
                              })
                            }
                          >
                            <SelectTrigger>
                              <SelectValue placeholder="Selecione uma opção" />
                            </SelectTrigger>
                            <SelectContent>
                              {question.options
                                .filter((o) => o.label.trim())
                                .map((o) => (
                                  <SelectItem key={o.id} value={o.label.trim()}>
                                    {o.label.trim()}
                                  </SelectItem>
                                ))}
                            </SelectContent>
                          </Select>
                        )}
                      </div>
                    )}
                  </div>
                )}

                {selectable && (
                  <div className="ml-6 space-y-2">
                    <p className="text-xs font-semibold text-muted-foreground">
                      Opções de resposta
                    </p>
                    {question.options.map((option) => (
                      <div key={option.id} className="flex items-center gap-2">
                        <Input
                          value={option.label}
                          onChange={(e) =>
                            updateOption(section.id, question.id, option.id, e.target.value)
                          }
                          placeholder="Opção"
                          maxLength={255}
                        />
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-7 w-7 text-destructive hover:text-destructive"
                          onClick={() => deleteOption(section.id, question.id, option.id)}
                          aria-label="Excluir opção"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </Button>
                      </div>
                    ))}
                    <Button
                      variant="link"
                      size="sm"
                      className="h-auto p-0 text-xs"
                      onClick={() => addOption(section.id, question.id)}
                    >
                      + Adicionar opção
                    </Button>
                  </div>
                )}
              </div>
            );
          })}

          <Button
            variant="outline"
            size="sm"
            className="w-full"
            onClick={() => addQuestion(section.id)}
          >
            <Plus className="mr-1.5 h-3.5 w-3.5" />
            Nova pergunta
          </Button>
        </section>
      ))}

      <div className="flex justify-center">
        <Button variant="outline" size="sm" onClick={addSection}>
          <Plus className="mr-1.5 h-3.5 w-3.5" />
          Nova seção
        </Button>
      </div>
    </div>
  );
}

function findQuestion(state: EditorState, sectionId: string, questionId: string) {
  return state.sections
    .find((section) => section.id === sectionId)
    ?.questions.find((question) => question.id === questionId);
}
