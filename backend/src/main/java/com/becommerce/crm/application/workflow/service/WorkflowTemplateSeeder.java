package com.becommerce.crm.application.workflow.service;

import com.becommerce.crm.application.workflow.port.output.WorkflowRepository;
import com.becommerce.crm.domain.workflow.ActionType;
import com.becommerce.crm.domain.workflow.ConditionOperator;
import com.becommerce.crm.domain.workflow.TriggerEvent;
import com.becommerce.crm.domain.workflow.Workflow;
import com.becommerce.crm.domain.workflow.WorkflowAction;
import com.becommerce.crm.domain.workflow.WorkflowCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Templates pré-definidos de workflow (Item 9). Seeds de regras úteis quando uma
 * empresa é provisionada (company creation / onboarding), assim como o
 * {@code RoleSeedService} seeda os papéis padrão.
 *
 * <p>Os templates são criados INATIVOS: o admin decide quando ativar (UX
 * previsível — nenhum comportamento automático surpreendente no primeiro dia).
 * Reexecução é idempotente: regras com o mesmo nome não são duplicadas.
 *
 * <p>Observação: a condição do template de estágio referencia o nome "Proposta";
 * empresas com nomenclatura própria devem ajustar a condição ao editar.
 */
@Service
public class WorkflowTemplateSeeder {

    private static final Logger log = LoggerFactory.getLogger(WorkflowTemplateSeeder.class);

    private final WorkflowRepository workflowRepository;

    public WorkflowTemplateSeeder(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    public void seedTemplates(UUID companyId) {
        List<Workflow> existing = workflowRepository.findByCompanyId(companyId);
        Set<String> existingNames = existing.stream().map(Workflow::getName).collect(Collectors.toSet());

        seed(companyId, followUpProposta(companyId), existingNames);
        seed(companyId, contatoInicial(companyId), existingNames);
        seed(companyId, agradecimentoGanha(companyId), existingNames);
        seed(companyId, followUpParada(companyId), existingNames);
    }

    private void seed(UUID companyId, Workflow template, Set<String> existingNames) {
        if (existingNames.contains(template.getName())) {
            return;
        }
        workflowRepository.save(template);
        log.info("Workflow template seeded for company={}: {}", companyId, template.getName());
    }

    private Workflow followUpProposta(UUID companyId) {
        Workflow wf = Workflow.create(companyId, "Follow-up após proposta",
                "Cria um follow-up 2 dias após a oportunidade entrar no estágio Proposta.",
                TriggerEvent.OPPORTUNITY_STAGE_CHANGED);
        wf.addCondition(WorkflowCondition.create(companyId, wf.getId(), "opportunity.stage",
                ConditionOperator.EQUALS, "Proposta", 0));
        wf.addAction(WorkflowAction.create(companyId, wf.getId(), ActionType.CREATE_TASK, 0,
                "{\"title\":\"Fazer follow-up da proposta\",\"dueInDays\":2,\"priority\":\"MEDIUM\"}"));
        return wf;
    }

    private Workflow contatoInicial(UUID companyId) {
        Workflow wf = Workflow.create(companyId, "Contato inicial após oportunidade",
                "Cria uma tarefa de contato quando uma oportunidade é criada.",
                TriggerEvent.OPPORTUNITY_CREATED);
        wf.addAction(WorkflowAction.create(companyId, wf.getId(), ActionType.CREATE_TASK, 0,
                "{\"title\":\"Entrar em contato com o cliente\",\"dueInDays\":1,\"priority\":\"MEDIUM\"}"));
        return wf;
    }

    private Workflow agradecimentoGanha(UUID companyId) {
        Workflow wf = Workflow.create(companyId, "Agradecimento por oportunidade ganha",
                "Cria uma tarefa de agradecimento quando uma oportunidade é ganha.",
                TriggerEvent.OPPORTUNITY_WON);
        wf.addAction(WorkflowAction.create(companyId, wf.getId(), ActionType.CREATE_TASK, 0,
                "{\"title\":\"Enviar agradecimento ao cliente\",\"dueInDays\":1,\"priority\":\"HIGH\"}"));
        return wf;
    }

    private Workflow followUpParada(UUID companyId) {
        Workflow wf = Workflow.create(companyId, "Follow-up após oportunidade parada",
                "Cria uma tarefa de follow-up quando uma oportunidade fica sem atividade por 7+ dias.",
                TriggerEvent.OPPORTUNITY_STALE);
        wf.addCondition(WorkflowCondition.create(companyId, wf.getId(), "opportunity.daysWithoutActivity",
                ConditionOperator.GREATER_OR_EQUAL, "7", 0));
        wf.addAction(WorkflowAction.create(companyId, wf.getId(), ActionType.CREATE_TASK, 0,
                "{\"title\":\"Fazer follow-up da oportunidade parada\",\"dueInDays\":1,\"priority\":\"MEDIUM\"}"));
        return wf;
    }
}