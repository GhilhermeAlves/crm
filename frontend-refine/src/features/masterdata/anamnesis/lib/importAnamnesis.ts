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
  "DATE",
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
//
// JSONs de anamnese vêm de muitas fontes (outros sistemas, IA, planilhas), cada
// uma com seus nomes de campo. Aceitamos os sinônimos mais comuns em português
// e inglês e procuramos as seções/perguntas mesmo dentro de um objeto "envelope"
// (ex.: { "anamnese": { ... } } ou { "data": { ... } }).

type Json = Record<string, unknown>;

const SECTION_KEYS = [
  "sections",
  "secoes",
  "seções",
  "grupos",
  "groups",
  "blocos",
  "etapas",
  "pages",
  "paginas",
  "steps",
  "categorias",
];
const QUESTION_KEYS = [
  "questions",
  "perguntas",
  "questoes",
  "questões",
  "campos",
  "fields",
  "items",
  "itens",
];
const TEXT_KEYS = [
  "text",
  "label",
  "pergunta",
  "question",
  "enunciado",
  "descricao_pergunta",
  "titulo",
  "title",
  "nome",
  "name",
];
const SECTION_TITLE_KEYS = ["title", "titulo", "título", "name", "nome", "label", "secao", "seção"];
const OPTION_KEYS = ["options", "opcoes", "opções", "alternativas", "choices", "valores", "values"];
const TYPE_KEYS = ["type", "tipo", "inputType", "tipo_campo", "tipoCampo"];
const REQUIRED_KEYS = ["required", "obrigatoria", "obrigatória", "obrigatorio", "obrigatório"];

const isObject = (v: unknown): v is Json =>
  typeof v === "object" && v !== null && !Array.isArray(v);

/** Primeiro valor presente entre as chaves (ignorando maiúsculas/minúsculas). */
function pick(obj: Json, keys: string[]): unknown {
  const lower = new Map(Object.keys(obj).map((k) => [k.toLowerCase(), k]));
  for (const key of keys) {
    const real = lower.get(key.toLowerCase());
    if (real !== undefined && obj[real] !== undefined && obj[real] !== null) return obj[real];
  }
  return undefined;
}

function pickString(obj: Json, keys: string[]): string {
  const v = pick(obj, keys);
  return typeof v === "string" || typeof v === "number" ? clean(String(v)) : "";
}

function pickArray(obj: Json, keys: string[]): unknown[] | undefined {
  const v = pick(obj, keys);
  return Array.isArray(v) ? v : undefined;
}

function truthy(v: unknown): boolean {
  return v === true || (typeof v === "string" && /^(true|sim|s|yes|1)$/i.test(v.trim())) || v === 1;
}

function jsonType(raw: string, hasOptions: boolean): AnamnesisQuestionType {
  const value = raw
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toUpperCase()
    .replace(/[\s-/]+/g, "_");
  if ((QUESTION_TYPES as string[]).includes(value)) return value as AnamnesisQuestionType;
  const aliases: Record<string, AnamnesisQuestionType> = {
    TEXT: "SHORT_TEXT",
    TEXTO: "SHORT_TEXT",
    STRING: "SHORT_TEXT",
    INPUT: "SHORT_TEXT",
    EMAIL: "SHORT_TEXT",
    TEL: "SHORT_TEXT",
    TELEFONE: "SHORT_TEXT",
    TEXTO_CURTO: "SHORT_TEXT",
    TEXTAREA: "LONG_TEXT",
    TEXTO_LONGO: "LONG_TEXT",
    PARAGRAFO: "LONG_TEXT",
    PARAGRAPH: "LONG_TEXT",
    LONG: "LONG_TEXT",
    BOOLEAN: "YES_NO",
    BOOL: "YES_NO",
    SIM_NAO: "YES_NO",
    YESNO: "YES_NO",
    NUMERO: "NUMBER",
    NUMERIC: "NUMBER",
    INTEGER: "NUMBER",
    INT: "NUMBER",
    RADIO: "SINGLE_SELECT",
    SELECT: "SINGLE_SELECT",
    DROPDOWN: "SINGLE_SELECT",
    ESCOLHA_UNICA: "SINGLE_SELECT",
    UNICA: "SINGLE_SELECT",
    SINGLE: "SINGLE_SELECT",
    CHOICE: "SINGLE_SELECT",
    CHECKBOX: "MULTI_SELECT",
    CHECKBOXES: "MULTI_SELECT",
    MULTIPLA: "MULTI_SELECT",
    MULTIPLA_ESCOLHA: "MULTI_SELECT",
    MULTIPLE: "MULTI_SELECT",
    MULTI: "MULTI_SELECT",
    MULTISELECT: "MULTI_SELECT",
    MULTIPLE_CHOICE: "MULTI_SELECT",
    DATA: "DATE",
    DATETIME: "DATE",
  };
  return aliases[value] ?? (hasOptions ? "SINGLE_SELECT" : "SHORT_TEXT");
}

function jsonQuestion(raw: unknown): AnamnesisQuestionRequest | null {
  if (typeof raw === "string") {
    const text = clean(raw);
    return text ? { text, type: "SHORT_TEXT", required: false, highlight: false } : null;
  }
  if (!isObject(raw)) return null;
  const text = pickString(raw, TEXT_KEYS);
  if (!text) return null;
  const options = (pickArray(raw, OPTION_KEYS) ?? [])
    .map((o) =>
      clean(
        typeof o === "string" || typeof o === "number"
          ? String(o)
          : isObject(o)
            ? pickString(o, ["label", "text", "texto", "nome", "name", "value", "valor"])
            : "",
      ),
    )
    .filter(Boolean);
  let type = jsonType(pickString(raw, TYPE_KEYS), options.length > 0);
  // Seleção sem opções não é aceita pelo servidor: um checkbox sozinho
  // (ex.: "Declaro que as informações são verdadeiras") vira Sim/Não.
  if ((type === "SINGLE_SELECT" || type === "MULTI_SELECT") && options.length === 0) {
    type = "YES_NO";
  }
  return {
    text,
    type,
    required: truthy(pick(raw, REQUIRED_KEYS)),
    highlight: truthy(pick(raw, ["highlight", "destaque"])),
    options:
      type === "SINGLE_SELECT" || type === "MULTI_SELECT"
        ? options.map((label) => ({ label }))
        : undefined,
  };
}

function toSections(root: Json): AnamnesisSectionRequest[] {
  let rawSections = pickArray(root, SECTION_KEYS);
  const loose = pickArray(root, QUESTION_KEYS);
  if (!rawSections && loose) rawSections = [{ titulo: "Geral", perguntas: loose }];
  return (rawSections ?? [])
    .filter(isObject)
    .map((s, index) => ({
      title: pickString(s, SECTION_TITLE_KEYS) || `Seção ${index + 1}`,
      professional: truthy(pick(s, ["professional", "profissional"])),
      questions: (pickArray(s, QUESTION_KEYS) ?? [])
        .map(jsonQuestion)
        .filter((q): q is AnamnesisQuestionRequest => q !== null),
    }))
    .filter((s) => s.questions.length > 0);
}

/** Procura, em largura, o primeiro objeto que tenha seções ou perguntas. */
function findRoot(data: unknown): Json | null {
  const queue: unknown[] = [Array.isArray(data) ? { sections: data } : data];
  for (let i = 0; i < queue.length && i < 200; i++) {
    const node = queue[i];
    if (!isObject(node)) continue;
    if (toSections(node).length > 0) return node;
    for (const value of Object.values(node)) {
      if (isObject(value)) queue.push(value);
    }
  }
  return null;
}

export function parseAnamnesisJson(raw: string, fallbackName: string): ImportedAnamnesis {
  let data: unknown;
  try {
    data = JSON.parse(raw.replace(/^﻿/, ""));
  } catch {
    throw new Error("O arquivo não é um JSON válido.");
  }
  // Lista na raiz: de seções (têm perguntas dentro) ou de perguntas soltas.
  if (Array.isArray(data)) {
    const asSections = data.some((d) => isObject(d) && pickArray(d, QUESTION_KEYS));
    data = asSections ? { sections: data } : { questions: data };
  }
  const root = findRoot(data);
  if (!root) {
    throw new Error(
      'Nenhuma pergunta encontrada no JSON. Use, por exemplo: { "nome": "...", "secoes": [ { "titulo": "...", "perguntas": [ { "texto": "...", "tipo": "texto" } ] } ] }.',
    );
  }
  const top = isObject(data) ? data : root;
  return {
    name:
      pickString(root, ["name", "nome", "titulo", "título", "title"]) ||
      pickString(top, ["name", "nome", "titulo", "título", "title"]) ||
      fallbackName,
    description:
      pickString(root, ["description", "descricao", "descrição"]) ||
      pickString(top, ["description", "descricao", "descrição"]) ||
      undefined,
    sections: toSections(root),
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
      const inputType = type === "number" ? "NUMBER" : type === "date" ? "DATE" : "SHORT_TEXT";
      push(el, { text, type: inputType, required });
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
