import { describe, expect, it } from "vitest";
import {
  parseAnamnesisHtml,
  parseAnamnesisImport,
  parseAnamnesisJson,
} from "@/features/masterdata/anamnesis/lib/importAnamnesis";

const HTML = `
<h1>Ficha de Anamnese Capilar</h1>
<form id="anamneseForm">
  <fieldset>
    <legend>Dados pessoais</legend>
    <label for="nome">Nome completo:</label>
    <input id="nome" name="nome" required>
    <label for="idade">Idade</label>
    <input id="idade" name="idade" type="number">
  </fieldset>
  <h2>Histórico capilar</h2>
  <div>
    <p>Tipo de cabelo</p>
    <label><input type="checkbox" name="tipo_cabelo[]" value="liso"> Liso</label>
    <label><input type="checkbox" name="tipo_cabelo[]" value="ondulado"> Ondulado</label>
    <label><input type="checkbox" name="tipo_cabelo[]" value="cacheado"> Cacheado</label>
  </div>
  <div>
    <span>Usa medicamento contínuo?</span>
    <label><input type="radio" name="medicamento" value="sim"> Sim</label>
    <label><input type="radio" name="medicamento" value="nao"> Não</label>
  </div>
  <label for="queda">Intensidade da queda</label>
  <select id="queda" name="queda">
    <option value="">Selecione</option>
    <option>Leve</option>
    <option>Moderada</option>
  </select>
  <label for="obs">Observações</label>
  <textarea id="obs" name="obs"></textarea>
  <input type="hidden" name="paciente_id" value="123">
  <button type="submit">Enviar</button>
</form>`;

describe("parseAnamnesisHtml", () => {
  it("transforma o formulário em seções e perguntas", () => {
    const result = parseAnamnesisHtml(HTML, "fallback");

    expect(result.name).toBe("Ficha de Anamnese Capilar");
    expect(result.sections.map((s) => s.title)).toEqual(["Dados pessoais", "Histórico capilar"]);

    const [dados, historico] = result.sections;
    expect(dados.questions).toEqual([
      { text: "Nome completo", type: "SHORT_TEXT", required: true },
      { text: "Idade", type: "NUMBER", required: false },
    ]);
    expect(historico.questions).toEqual([
      {
        text: "Tipo de cabelo",
        type: "MULTI_SELECT",
        required: false,
        options: [{ label: "Liso" }, { label: "Ondulado" }, { label: "Cacheado" }],
      },
      { text: "Usa medicamento contínuo?", type: "YES_NO", required: false, options: undefined },
      {
        text: "Intensidade da queda",
        type: "SINGLE_SELECT",
        required: false,
        options: [{ label: "Leve" }, { label: "Moderada" }],
      },
      { text: "Observações", type: "LONG_TEXT", required: false },
    ]);
  });

  it("recusa HTML sem campos", () => {
    expect(() => parseAnamnesisHtml("<p>oi</p>", "x")).toThrow(/Nenhum campo/);
  });
});

describe("parseAnamnesisJson", () => {
  it("aceita nomes em português e tipos abreviados", () => {
    const result = parseAnamnesisJson(
      JSON.stringify({
        nome: "Pré-consulta",
        secoes: [
          {
            titulo: "Saúde",
            perguntas: [
              { pergunta: "Tem alergia?", tipo: "sim_nao", obrigatoria: true },
              { pergunta: "Qual couro cabeludo?", opcoes: ["Oleoso", "Seco"] },
            ],
          },
        ],
      }),
      "x",
    );
    expect(result.name).toBe("Pré-consulta");
    expect(result.sections[0].questions?.[0]).toMatchObject({ type: "YES_NO", required: true });
    expect(result.sections[0].questions?.[1]).toMatchObject({
      type: "SINGLE_SELECT",
      options: [{ label: "Oleoso" }, { label: "Seco" }],
    });
  });

  it("detecta JSON pelo conteúdo e usa o nome do arquivo como padrão", () => {
    const result = parseAnamnesisImport('{"questions":[{"text":"Peso"}]}', "triagem.json");
    expect(result.name).toBe("triagem");
    expect(result.sections[0]).toMatchObject({ title: "Geral" });
  });
});

describe("parseAnamnesisJson — formatos de outros sistemas", () => {
  it("aceita campos/label/obrigatorio e tipos em português", () => {
    const json = JSON.stringify({
      titulo: "Anamnese Tricológica",
      descricao: "Teste",
      secoes: [
        {
          titulo: "Queixa principal",
          campos: [
            { label: "Qual a sua queixa?", tipo: "textarea", obrigatorio: true },
            { label: "Há quanto tempo?", tipo: "radio", opcoes: ["< 6 meses", "> 1 ano"] },
          ],
        },
        { titulo: "Histórico", campos: [{ label: "Usa medicamento?", tipo: "boolean" }] },
      ],
    });
    const result = parseAnamnesisJson(json, "fallback");
    expect(result.name).toBe("Anamnese Tricológica");
    expect(result.sections).toHaveLength(2);
    expect(result.sections[0].questions?.[0]).toMatchObject({
      text: "Qual a sua queixa?",
      type: "LONG_TEXT",
      required: true,
    });
    expect(result.sections[0].questions?.[1].type).toBe("SINGLE_SELECT");
    expect(result.sections[1].questions?.[0].type).toBe("YES_NO");
  });

  it("encontra as perguntas dentro de um envelope e aceita BOM", () => {
    const json =
      "﻿" +
      JSON.stringify({
        anamnese: {
          nome: "Capilar",
          fields: [{ question: "Idade", type: "number" }, "Observações"],
        },
      });
    const result = parseAnamnesisImport(json, "arquivo.json");
    expect(result.name).toBe("Capilar");
    expect(result.sections[0].questions?.map((q) => q.type)).toEqual(["NUMBER", "SHORT_TEXT"]);
  });

  it("lista de perguntas soltas na raiz", () => {
    const result = parseAnamnesisJson(
      JSON.stringify([{ pergunta: "Tem alergia?", tipo: "sim/não" }]),
      "Lista",
    );
    expect(result.name).toBe("Lista");
    expect(result.sections[0].questions?.[0].type).toBe("YES_NO");
  });

  it("checkbox sem opções vira Sim/Não e multiselect é múltipla escolha", () => {
    const result = parseAnamnesisJson(
      JSON.stringify({
        schema_version: 1,
        form: {
          title: "Ficha",
          sections: [
            {
              title: "Final",
              fields: [
                {
                  id: "a",
                  label: "Tipo de cabelo",
                  type: "multiselect",
                  options: [{ label: "Seco", value: "seco" }],
                },
                {
                  id: "b",
                  label: "Declaro que as informações são verdadeiras",
                  type: "checkbox",
                  required: true,
                },
              ],
            },
          ],
        },
      }),
      "x",
    );
    expect(result.name).toBe("Ficha");
    expect(result.sections[0].questions?.map((q) => q.type)).toEqual(["MULTI_SELECT", "YES_NO"]);
  });
});
