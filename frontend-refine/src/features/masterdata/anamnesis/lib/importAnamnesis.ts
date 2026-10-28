import type {
  AnamnesisQuestionRequest,
  AnamnesisQuestionType,
  AnamnesisSectionRequest,
} from "../types/anamnesis.types";

/** Resultado da importação: nome sugerido e seções prontas para o editor. */
export type ImportedAnamnesis = {
  name: string;
  description?: string;
  sections: AnamnesisSectionRequest[];
};

const QUESTION_TYPES: AnamnesisQuestionType[] = [
  "SHORT_TEXT",
  "LONG_TEXT",
  "YES_NO",
  "YES_NO_WITH_TEXT",
  "NUMBER",
  "SINGLE_SELECT",
  "MULTI_SELECT",
];

const IGNORED_INPUTS = new Set([
  "hidden",
  "submit",
  "button",
  "reset",
  "image",
  "file",
  "password",
]);
const YES = /^(sim|s|yes|y)$/i;
const NO = /^(n[ãa]o|n|no)$/i;

/** Valor seguro dentro de um seletor de atributo entre aspas. */
function attr(value: string): string {
  return value.replace(/["\\]/g, "\\$&");
}

function clean(text: string | null | undefined): string {
  return (text ?? "")
    .replace(/\s+/g, " ")
    .replace(/[:*]\s*$/, "")
    .trim();
}

// ---------------------------------------------------------------------------
// JSON
// ---------------------------------------------------------------------------

type JsonOption = string | { label?: string; text?: string; value?: string };
type JsonQuestion = {
  text?: string;
  label?: string;
  pergunta?: string;
  type?: string;
  tipo?: string;
  required?: boolean;
  obrigatoria?: boolean;
  highlight?: boolean;
  options?: JsonOption[];
  opcoes?: JsonOption[];
};
type JsonSection = {
  title?: string;
  titulo?: string;
  professional?: boolean;
  questions?: JsonQuestion[];
  perguntas?: JsonQuestion[];
};

function jsonType(raw: string | undefined, hasOptions: boolean): AnamnesisQuestionType {
  const value = (raw ?? "").toUpperCase();
  if ((QUESTION_TYPES as string[]).includes(value)) return value as AnamnesisQuestionType;
  const aliases: Record<string, AnamnesisQuestionType> = {
    TEXT: "SHORT_TEXT",
    TEXTO: "SHORT_TEXT",
    TEXTAREA: "LONG_TEXT",
    BOOLEAN: "YES_NO",
    SIM_NAO: "YES_NO",
    NUMERO: "NUMBER",
    RADIO: "SINGLE_SELECT",
    SELECT: "SINGLE_SELECT",
    CHECKBOX: "MULTI_SELECT",
  };
  return aliases[value] ?? (hasOptions ? "SINGLE_SELECT" : "SHORT_TEXT");
}

function jsonQuestion(q: JsonQuestion): AnamnesisQuestionRequest | null {
  const text = clean(q.text ?? q.label ?? q.pergunta);
  if (!text) return null;
  const options = (q.options ?? q.opcoes ?? [])
    .map((o) => clean(typeof o === "string" ? o : (o.label ?? o.text ?? o.value)))
    .filter(Boolean);
  const type = jsonType(q.type ?? q.tipo, options.length > 0);
  return {
    text,
    type,
    required: Boolean(q.required ?? q.obrigatoria),
    highlight: Boolean(q.highlight),
    options:
      type === "SINGLE_SELECT" || type === "MULTI_SELECT"
        ? options.map((label) => ({ label }))
        : undefined,
  };
}

export function parseAnamnesisJson(raw: string, fallbackName: string): ImportedAnamnesis {
  let data: unknown;
  try {
    data = JSON.parse(raw);
  } catch {
    throw new Error("O arquivo não é um JSON válido.");
  }
  const root = (Array.isArray(data) ? { sections: data } : data) as {
    name?: string;
    nome?: string;
    description?: string;
    descricao?: string;
    sections?: JsonSection[];
    secoes?: JsonSection[];
    questions?: JsonQuestion[];
    perguntas?: JsonQuestion[];
  };
  let rawSections = root.sections ?? root.secoes;
  const looseQuestions = root.questions ?? root.perguntas;
  if (!rawSections && looseQuestions) rawSections = [{ title: "Geral", questions: looseQuestions }];
  const sections = (rawSections ?? [])
    .map((s, index) => ({
      title: clean(s.title ?? s.titulo) || `Seção ${index + 1}`,
      professional: Boolean(s.professional),
      questions: (s.questions ?? s.perguntas ?? [])
        .map(jsonQuestion)
        .filter((q): q is AnamnesisQuestionRequest => q !== null),
    }))
    .filter((s) => s.questions.length > 0);
  if (sections.length === 0) throw new Error("Nenhuma pergunta encontrada no JSON.");
  return {
    name: clean(root.name ?? root.nome) || fallbackName,
    description: clean(root.description ?? root.descricao) || undefined,
    sections,
  };
}

// ---------------------------------------------------------------------------
// HTML
// ---------------------------------------------------------------------------

function labelFor(doc: Document, el: Element): string {
  const id = el.getAttribute("id");
  if (id) {
    const label = doc.querySelector(`label[for="${attr(id)}"]`);
    if (label) return clean(label.textContent);
  }
  const wrapping = el.closest("label");
  if (wrapping) {
    const copy = wrapping.cloneNode(true) as Element;
    copy.querySelectorAll("input,select,textarea").forEach((c) => c.remove());
    const text = clean(copy.textContent);
    if (text) return text;
  }
  return clean(el.getAttribute("aria-label") ?? el.getAttribute("placeholder"));
}

/** Pergunta de um grupo de radios/checkboxes: legend, rótulo do bloco ou nome do campo. */
function groupQuestionText(doc: Document, inputs: Element[], name: string): string {
  const first = inputs[0];
  const fieldset = first.closest("fieldset");
  if (fieldset && fieldset.querySelectorAll(`[name="${attr(name)}"]`).length === inputs.length) {
    const legend = fieldset.querySelector("legend");
    if (legend) return clean(legend.textContent);
  }
  // Bloco comum: o primeiro texto antes das opções (label sem "for", p, span, h*).
  let container = first.parentElement;
  while (container && !inputs.every((i) => container?.contains(i))) {
    container = container.parentElement;
  }
  if (container) {
    for (const child of Array.from(container.children)) {
      if (child.querySelector("input,select,textarea") || child.matches("input,select,textarea")) {
        break;
      }
      const text = clean(child.textContent);
      if (text) return text;
    }
    const prev = container.previousElementSibling;
    if (prev && !prev.querySelector("input,select,textarea")) {
      const text = clean(prev.textContent);
      if (text) return text;
    }
  }
  return clean(name.replace(/\[\]$/, "").replace(/[_-]+/g, " "));
}

function sectionTitleFor(el: Element): string | null {
  const fieldset = el.closest("fieldset");
  const legend = fieldset?.querySelector(":scope > legend");
  if (legend) {
    // Fieldset que é a própria pergunta (só um grupo de opções) não vira seção.
    const names = new Set(
      Array.from(fieldset!.querySelectorAll("input,select,textarea")).map((i) =>
        i.getAttribute("name"),
      ),
    );
    if (names.size > 1) return clean(legend.textContent);
  }
  // Último título (h1–h4) antes do campo, na ordem do documento.
  const headings = Array.from(el.ownerDocument.querySelectorAll("h1,h2,h3,h4"));
  let title: string | null = null;
  for (const h of headings) {
    if (h.compareDocumentPosition(el) & Node.DOCUMENT_POSITION_FOLLOWING)
      title = clean(h.textContent);
  }
  return title;
}

export function parseAnamnesisHtml(raw: string, fallbackName: string): ImportedAnamnesis {
  const doc = new DOMParser().parseFromString(raw, "text/html");
  const form = doc.querySelector("form") ?? doc.body;
  const controls = Array.from(form.querySelectorAll("input,select,textarea")).filter(
    (el) => !IGNORED_INPUTS.has((el.getAttribute("type") ?? "text").toLowerCase()),
  );

  const sections: AnamnesisSectionRequest[] = [];
  const sectionByTitle = new Map<string, AnamnesisSectionRequest>();
  const seenGroups = new Set<string>();

  const push = (el: Element, question: AnamnesisQuestionRequest) => {
    const title = sectionTitleFor(el) ?? "Geral";
    let section = sectionByTitle.get(title);
    if (!section) {
      section = { title, questions: [] };
      sectionByTitle.set(title, section);
      sections.push(section);
    }
    section.questions!.push(question);
  };

  for (const el of controls) {
    const tag = el.tagName.toLowerCase();
    const type = (el.getAttribute("type") ?? "text").toLowerCase();
    const name = el.getAttribute("name") ?? el.getAttribute("id") ?? "";
    const required = el.hasAttribute("required");

    if (tag === "input" && (type === "radio" || type === "checkbox")) {
      if (!name || seenGroups.has(name)) continue;
      seenGroups.add(name);
      const group = controls.filter(
        (c) => c.getAttribute("name") === name && c.getAttribute("type") === type,
      );
      const labels = group.map((i) => labelFor(doc, i) || clean(i.getAttribute("value")));
      const text = groupQuestionText(doc, group, name);
      if (type === "checkbox" && group.length === 1) {
        push(el, { text: labels[0] || text, type: "YES_NO", required });
        continue;
      }
      const yesNo =
        type === "radio" &&
        labels.length === 2 &&
        labels.some((l) => YES.test(l)) &&
        labels.some((l) => NO.test(l));
      push(el, {
        text,
        type: yesNo ? "YES_NO" : type === "radio" ? "SINGLE_SELECT" : "MULTI_SELECT",
        required: group.some((i) => i.hasAttribute("required")),
        options: yesNo ? undefined : labels.filter(Boolean).map((label) => ({ label })),
      });
      continue;
    }

    const text = labelFor(doc, el) || clean(name.replace(/[_-]+/g, " "));
    if (!text) continue;
    if (tag === "select") {
      const options = Array.from(el.querySelectorAll("option"))
        .filter((o) => o.getAttribute("value") !== "" && !o.hasAttribute("disabled"))
        .map((o) => clean(o.textContent))
        .filter(Boolean);
      push(el, {
        text,
        type: el.hasAttribute("multiple") ? "MULTI_SELECT" : "SINGLE_SELECT",
        required,
        options: options.map((label) => ({ label })),
      });
    } else if (tag === "textarea") {
      push(el, { text, type: "LONG_TEXT", required });
    } else {
      push(el, { text, type: type === "number" ? "NUMBER" : "SHORT_TEXT", required });
    }
  }

  const filled = sections.filter((s) => (s.questions?.length ?? 0) > 0);
  if (filled.length === 0) throw new Error("Nenhum campo de formulário encontrado no HTML.");
  const title = clean(doc.querySelector("h1")?.textContent ?? doc.title);
  return { name: title || fallbackName, sections: filled };
}

/** Decide pelo conteúdo: começa com { ou [ → JSON; senão HTML. */
export function parseAnamnesisImport(raw: string, fileName = ""): ImportedAnamnesis {
  const fallbackName = fileName.replace(/\.(json|html?)$/i, "").trim() || "Anamnese importada";
  const trimmed = raw.trim();
  if (!trimmed) throw new Error("Cole o conteúdo ou escolha um arquivo.");
  return trimmed.startsWith("{") || trimmed.startsWith("[")
    ? parseAnamnesisJson(trimmed, fallbackName)
    : parseAnamnesisHtml(trimmed, fallbackName);
}
