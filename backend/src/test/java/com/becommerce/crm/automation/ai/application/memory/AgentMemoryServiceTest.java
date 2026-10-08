package com.becommerce.crm.automation.ai.application.memory;

import com.becommerce.crm.automation.ai.domain.AgentMemory;
import com.becommerce.crm.automation.ai.domain.MemorySource;
import com.becommerce.crm.automation.ai.domain.MemoryType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentMemoryServiceTest {

    private final UUID companyA = UUID.randomUUID();
    private final UUID companyB = UUID.randomUUID();
    private final UUID agentA = UUID.randomUUID();
    private final UUID agentB = UUID.randomUUID();
    private final UUID contact1 = UUID.randomUUID();
    private final UUID contact2 = UUID.randomUUID();

    private final InMemoryAgentMemoryRepository repository = new InMemoryAgentMemoryRepository();
    private final AgentMemoryService service = new AgentMemoryService(repository,
            new RankedMemoryRetriever(repository), new MemoryWritePolicy());

    @Test
    void save_persistsRelevantMemory() {
        AgentMemoryService.SaveOutcome outcome = service.save(companyA, agentA, contact1, MemoryType.PREFERENCE,
                "Prefere atendimento pela manhã.", 4, MemorySource.AGENT, null);

        assertTrue(outcome.saved());
        assertEquals(1, repository.store.size());
        AgentMemory saved = outcome.memory();
        assertEquals(companyA, saved.getCompanyId());
        assertEquals(contact1, saved.getContactId());
        assertEquals(4, saved.getImportance());
    }

    @Test
    void save_rejectsGreetingsAndTransactionalData() {
        assertFalse(service.save(companyA, agentA, contact1, MemoryType.FACT, "Oi, bom dia!", null,
                MemorySource.AGENT, null).saved());
        assertFalse(service.save(companyA, agentA, contact1, MemoryType.CONTEXTUAL,
                "Consulta agendada para quinta às 15h", null, MemorySource.AGENT, null).saved());
        assertTrue(repository.store.isEmpty(), "nada deve ser gravado");
    }

    @Test
    void save_withoutContact_isRejected() {
        assertFalse(service.save(companyA, agentA, null, MemoryType.FACT, "Já fez avaliação odontológica",
                null, MemorySource.AGENT, null).saved());
    }

    @Test
    void save_deduplicatesEquivalentContent() {
        service.save(companyA, agentA, contact1, MemoryType.PREFERENCE, "Prefere atendimento pela manhã",
                2, MemorySource.AGENT, null);
        service.save(companyA, agentA, contact1, MemoryType.PREFERENCE, "  prefere ATENDIMENTO pela manha ",
                5, MemorySource.AGENT, null);

        assertEquals(1, repository.store.size());
        assertEquals(5, repository.store.values().iterator().next().getImportance());
    }

    @Test
    void retrieve_filtersByContactAndCompany() {
        service.save(companyA, agentA, contact1, MemoryType.PREFERENCE, "Prefere atendimento pela manhã",
                3, MemorySource.AGENT, null);
        service.save(companyA, agentA, contact2, MemoryType.PROFILE, "Interesse em clareamento dental",
                3, MemorySource.AGENT, null);
        service.save(companyB, agentB, contact1, MemoryType.FACT, "Memória de outra empresa qualquer",
                3, MemorySource.AGENT, null);

        List<AgentMemory> forContact1 = service.retrieveRelevant(new MemoryQuery(companyA, agentA, contact1, "oi", 5));

        assertEquals(1, forContact1.size());
        assertEquals("Prefere atendimento pela manhã", forContact1.get(0).getContent());
        assertTrue(service.retrieveRelevant(new MemoryQuery(companyB, agentB, contact2, "oi", 5)).isEmpty(),
                "empresa B não enxerga memórias do contato 2 da empresa A");
    }

    @Test
    void retrieve_ordersByImportanceExcludesExpiredAndRespectsLimit() {
        repository.save(AgentMemory.create(companyA, agentA, contact1, MemoryType.FACT, "Fato pouco importante",
                1, MemorySource.AGENT, null, Map.of(), null));
        repository.save(AgentMemory.create(companyA, agentA, contact1, MemoryType.FACT, "Fato muito importante",
                5, MemorySource.AGENT, null, Map.of(), null));
        repository.save(AgentMemory.create(companyA, agentA, contact1, MemoryType.CONTEXTUAL, "Pendência vencida",
                5, MemorySource.AGENT, null, Map.of(), LocalDateTime.now().minusDays(1)));

        List<AgentMemory> top = service.retrieveRelevant(new MemoryQuery(companyA, agentA, contact1, null, 1));

        assertEquals(1, top.size());
        assertEquals("Fato muito importante", top.get(0).getContent());
    }

    @Test
    void update_changesContentOnlyWithinCompany() {
        AgentMemory saved = service.save(companyA, agentA, contact1, MemoryType.PREFERENCE,
                "Prefere atendimento pela manhã", 3, MemorySource.AGENT, null).memory();

        assertTrue(service.update(companyB, saved.getId(), null, "invasão de outra empresa", null, null).isEmpty());
        assertEquals("Prefere atendimento pela manhã", repository.store.get(saved.getId()).getContent());

        service.update(companyA, saved.getId(), MemoryType.PREFERENCE, "Prefere atendimento à tarde", 4, null);
        assertEquals("Prefere atendimento à tarde", repository.store.get(saved.getId()).getContent());
        assertEquals(4, repository.store.get(saved.getId()).getImportance());
    }

    @Test
    void delete_removesOnlyWithinCompany() {
        AgentMemory saved = service.save(companyA, agentA, contact1, MemoryType.FACT,
                "Já realizou avaliação odontológica", 3, MemorySource.AGENT, null).memory();

        assertFalse(service.delete(companyB, saved.getId()));
        assertEquals(1, repository.store.size());

        assertTrue(service.delete(companyA, saved.getId()));
        assertTrue(repository.store.isEmpty());
    }

    @Test
    void manualMemoryFromTeam_skipsSelectionPolicyButKeepsDomainValidation() {
        assertTrue(service.save(companyA, agentA, contact1, MemoryType.CONTEXTUAL,
                "Retorno marcado 10/11 conforme combinado com a dentista", 3, MemorySource.USER, null).saved());
        assertFalse(service.save(companyA, agentA, contact1, null, "Sem tipo", 3, MemorySource.USER, null).saved());
    }
}
