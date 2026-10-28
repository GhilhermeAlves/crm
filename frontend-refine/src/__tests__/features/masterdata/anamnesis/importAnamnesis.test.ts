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
