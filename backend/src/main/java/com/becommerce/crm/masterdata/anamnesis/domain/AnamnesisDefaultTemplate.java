package com.becommerce.crm.masterdata.anamnesis.domain;

import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisTemplateDraft.QuestionDraft;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisTemplateDraft.SectionDraft;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Definição do modelo <strong>Anamnese Odontológica Padrão</strong>.
 *
 * <p>Serve apenas como ponto de partida: cada empresa recebe sua própria
 * instância editável (ver {@code AnamnesisService.ensureDefaultTemplate}), e
 * edições posteriores não afetam outras empresas nem o modelo padrão de origem.
 *
 * <p>Regra de modelagem: "Sim/Não com campo complementar" é expresso como
 * {@code YES_NO} (ou {@code YES_NO_WITH_TEXT}) com {@code allowComplement=true} e
 * um {@code complementTrigger} — assim o campo complementar aparece quando a
 * resposta for "Sim", "Não" ou uma opção específica, e não engessa o modelo.
 */
public final class AnamnesisDefaultTemplate {

    public static final String DEFAULT_NAME = "Anamnese Odontológica Padrão";
    public static final String DEFAULT_DESCRIPTION =
            "Modelo odontológico padrão, pronto para uso e totalmente editável pela clínica.";

    private final UUID companyId;
    private final UUID modelId = UUID.randomUUID();
    private final List<SectionDraft> sections = new ArrayList<>();
    private SectionDraft current;
    private int sectionOrder;

    private AnamnesisDefaultTemplate(UUID companyId) {
        this.companyId = companyId;
    }

    /** Monta o rascunho completo (modelo + 6 seções) para uma empresa. */
    public static AnamnesisTemplateDraft build(UUID companyId, UUID createdBy) {
        AnamnesisDefaultTemplate builder = new AnamnesisDefaultTemplate(companyId);
        builder.section1Identificacao();
        builder.section2Historico();
        builder.section3Medicamentos();
        builder.section4Habitos();
        builder.section5Especiais();
        builder.section6AvaliacaoProfissional();
        AnamnesisModel model = AnamnesisModel.createDefault(
                builder.modelId, companyId, DEFAULT_NAME, DEFAULT_DESCRIPTION, createdBy);
        return new AnamnesisTemplateDraft(model, builder.sections);
    }

    // ------------------------------------------------------------------
    // Seção 1 — Identificação e motivo da consulta
    // ------------------------------------------------------------------
    private void section1Identificacao() {
        section("Identificação e motivo da consulta", false);
        longText("Qual é o motivo principal da consulta?");
        longText("Qual é a sua principal queixa ou preocupação com a saúde bucal?");
        yesNo("Está sentindo dor ou desconforto na boca ou nos dentes?",
                "Localização, intensidade, duração e características da dor", true);
        longText("Se respondeu sim à pergunta anterior, detalhe a localização, intensidade, duração e "
                + "características da dor.");
        shortText("Quando os sintomas começaram?");
        yesNo("Já realizou tratamento odontológico anteriormente?",
                "Descreva o tratamento realizado", false);
        shortText("Quando foi sua última consulta odontológica?");
        longText("Existe algum tratamento odontológico que deseja realizar prioritariamente?");
    }

    // ------------------------------------------------------------------
    // Seção 2 — Histórico médico e condições de saúde
    // ------------------------------------------------------------------
    private void section2Historico() {
        section("Histórico médico e condições de saúde", false);
        yesNo("Está realizando algum tratamento médico atualmente?",
                "Qual tratamento e desde quando?", false);
        yesNo("Possui alguma doença ou condição de saúde diagnosticada?",
                "Qual(is)?", true);
        yesNo("Tem diabetes?", "Informações relevantes (tipo, controle, medicamentos)", true);
        yesNo("Tem hipertensão arterial?", "Informações relevantes (controle, medicamentos)", true);
        yesNo("Possui alguma doença ou condição cardíaca?", "Qual(is)?", true);
        yesNo("Possui problemas de coagulação ou histórico de sangramento excessivo?",
                "Descreva", true);
        yesNo("Já realizou cirurgias ou internações hospitalares?", "Qual(is) e quando?", false);
        yesNo("Possui alguma doença infecciosa ou outra condição de saúde relevante para o "
                        + "atendimento odontológico?", "Qual(is)?", true);
        longText("Existe alguma outra condição de saúde que o cirurgião-dentista deva conhecer?");
    }

    // ------------------------------------------------------------------
    // Seção 3 — Medicamentos e alergias
    // ------------------------------------------------------------------
    private void section3Medicamentos() {
        section("Medicamentos e alergias", false);
        yesNo("Faz uso contínuo ou recente de medicamentos?", "Quais?", false);
        longText("Informe os nomes dos medicamentos e as respectivas dosagens, se souber.");
        yesNo("Possui alergia a algum medicamento?", "Qual(is)?", true);
        yesNo("Possui alergia ou reação conhecida a anestésicos locais?", "Descreva", true);
        yesNo("Possui alergia a látex ou a outras substâncias utilizadas em procedimentos clínicos?",
                "Descreva", true);
        yesNo("Já apresentou reação adversa a medicamentos ou anestesia durante algum atendimento?",
                "Descreva", true);
        yesNo("Utiliza anticoagulantes ou medicamentos que possam afetar a coagulação?", "Quais?", true);
        yesNo("Utiliza medicamentos para osteoporose ou outros medicamentos que possam interferir em "
                + "procedimentos odontológicos?", "Quais?", true);
    }

    // ------------------------------------------------------------------
    // Seção 4 — Hábitos e saúde bucal
    // ------------------------------------------------------------------
    private void section4Habitos() {
        section("Hábitos e saúde bucal", false);
        select("Quantas vezes escova os dentes por dia?",
                "Uma vez ao dia",
                "Duas vezes ao dia",
                "Três vezes ao dia",
                "Quatro vezes ao dia ou mais",
                "Irregularmente");
        yesNo("Utiliza fio dental regularmente?");
        yesNo("Apresenta sangramento gengival?", "Com que frequência?", false);
        yesNo("Sente sensibilidade nos dentes?", "Onde e quando?", false);
        yesNo("Range ou aperta os dentes, inclusive durante o sono?");
        yesNo("Apresenta dor na mandíbula, estalos ou dificuldade para abrir a boca?", "Descreva", false);
        yesNo("Fuma ou utiliza produtos com nicotina?", "Com que frequência?", true);
        yesNo("Consome bebidas alcoólicas?", "Com que frequência?", false);
        yesNo("Possui próteses, implantes, aparelhos ortodônticos ou outros dispositivos odontológicos?",
                "Qual(is)?", false);
        yesNo("Já teve experiências negativas ou medo intenso durante atendimentos odontológicos?",
                "Descreva", false);
    }

    // ------------------------------------------------------------------
    // Seção 5 — Condições especiais e avaliação de risco
    // ------------------------------------------------------------------
    private void section5Especiais() {
        section("Condições especiais e avaliação de risco", false);
        select("Está grávida ou existe possibilidade de gravidez?", "Sim", "Não", "Não se aplica");
        select("Está amamentando?", "Sim", "Não", "Não se aplica");
        yesNo("Possui alguma limitação ou condição que exija cuidados especiais durante o atendimento?",
                "Descreva", false);
        yesNo("Já apresentou desmaio, mal-estar ou outra reação importante durante atendimento "
                + "odontológico ou médico?", "Descreva", true);
        longText("Há alguma informação adicional importante para a segurança do atendimento?");
    }

    // ------------------------------------------------------------------
    // Seção 6 — Avaliação profissional (preenchida pelo cirurgião-dentista)
    // ------------------------------------------------------------------
    private void section6AvaliacaoProfissional() {
        section("Avaliação profissional", true);
        longText("Observações clínicas.");
        longText("Condições relevantes identificadas na anamnese.");
        longText("Necessidade de avaliação ou liberação médica, quando clinicamente indicada.");
        longText("Hipóteses diagnósticas ou avaliação clínica.");
        longText("Plano de tratamento proposto.");
        longText("Encaminhamentos e recomendações.");
        shortText("Nome e registro profissional do responsável.");
        shortText("Data da avaliação.");
    }

    // ------------------------------------------------------------------
    // Construção
    // ------------------------------------------------------------------
    private void section(String title, boolean professional) {
        AnamnesisSection section = AnamnesisSection.create(
                UUID.randomUUID(), modelId, companyId, title, professional, sectionOrder++);
        current = new SectionDraft(section, new ArrayList<>());
        sections.add(current);
    }

    private void shortText(String text) {
        addQuestion(text, AnamnesisQuestionType.SHORT_TEXT, false, false, false, null,
                AnamnesisComplementTrigger.YES, null, List.of());
    }

    private void longText(String text) {
        addQuestion(text, AnamnesisQuestionType.LONG_TEXT, false, false, false, null,
                AnamnesisComplementTrigger.YES, null, List.of());
    }

    private void yesNo(String text) {
        addQuestion(text, AnamnesisQuestionType.YES_NO, false, false, false, null,
                AnamnesisComplementTrigger.YES, null, List.of());
    }

    private void yesNo(String text, String complementLabel, boolean highlight) {
        addQuestion(text, AnamnesisQuestionType.YES_NO, false, highlight, true, complementLabel,
                AnamnesisComplementTrigger.YES, null, List.of());
    }

    private void select(String text, String... optionLabels) {
        List<String> labels = List.of(optionLabels);
        List<AnamnesisQuestionOption> options = new ArrayList<>();
        AnamnesisQuestion question = question(text, AnamnesisQuestionType.SINGLE_SELECT, false, false,
                false, null, AnamnesisComplementTrigger.YES, null);
        int order = 0;
        for (String label : labels) {
            options.add(AnamnesisQuestionOption.create(UUID.randomUUID(), question.getId(), companyId, label,
                    label, order++));
        }
        current.questions().add(new QuestionDraft(question, options));
    }

    private void addQuestion(String text, AnamnesisQuestionType type, boolean required, boolean highlight,
                             boolean allowComplement, String complementLabel,
                             AnamnesisComplementTrigger trigger, String optionValue,
                             List<AnamnesisQuestionOption> options) {
        AnamnesisQuestion question = question(text, type, required, highlight, allowComplement,
                complementLabel, trigger, optionValue);
        current.questions().add(new QuestionDraft(question, options));
    }

    private AnamnesisQuestion question(String text, AnamnesisQuestionType type, boolean required,
                                       boolean highlight, boolean allowComplement, String complementLabel,
                                       AnamnesisComplementTrigger trigger, String optionValue) {
        return AnamnesisQuestion.create(UUID.randomUUID(), current.section().getId(), modelId, companyId,
                text, type, required, highlight, allowComplement, complementLabel, trigger, optionValue,
                current.questions().size());
    }
}
