package com.becommerce.crm.masterdata.person;

import com.becommerce.crm.masterdata.company.application.service.CompanyModulesService;
import com.becommerce.crm.masterdata.company.domain.CompanySegment;
import com.becommerce.crm.masterdata.person.PersonDtos.*;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static com.becommerce.crm.masterdata.person.PersonService.*;

/**
 * Papéis da pessoa (doc 3.2, 4.7–4.9, 22.3–22.5): cliente (e paciente no segmento saúde),
 * fornecedor e funcionário, com cargos e especialidades.
 *
 * Paciente: no segmento saúde o cliente tem linha em contacts com is_patient = TRUE e os campos
 * de prontuário/convênio em contacts; o gatilho V147 mantém customers/patients. Assim as telas
 * clínicas atuais (que usam contacts) continuam funcionando.
 */
@Service
public class PersonRoleService {

    static final Set<String> SUPPLIER_TYPES = Set.of("LAB", "MATERIAL", "SERVICE", "OTHER");
    static final Set<String> CONTRACT_TYPES = Set.of("CLT", "PJ", "INTERN", "OTHER");
    static final Set<String> EMPLOYEE_STATUS = Set.of("ACTIVE", "ON_LEAVE", "TERMINATED");
    static final Set<String> SPECIALTIES = Set.of("GENERIC", "DENTAL", "TRICHOLOGY", "OCCUPATIONAL_SAFETY");

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;
    private final PersonHistoryService history;
    private final PersonFieldSettingsService fields;
    private final CompanyModulesService modules;
    private final PersonContactBridge bridge;

    public PersonRoleService(JdbcTemplate jdbc, NamedParameterJdbcTemplate named, PersonHistoryService history,
                             PersonFieldSettingsService fields, CompanyModulesService modules, PersonContactBridge bridge) {
        this.jdbc = jdbc;
        this.named = named;
        this.history = history;
        this.fields = fields;
        this.modules = modules;
        this.bridge = bridge;
    }

    // =================================================================== leitura

    public List<String> rolesOf(UUID companyId, UUID personId) {
        List<String> r = new ArrayList<>();
        if (exists("customers", companyId, personId)) r.add("CUSTOMER");
        if (exists("patients", companyId, personId)) r.add("PATIENT");
        if (exists("suppliers", companyId, personId)) r.add("SUPPLIER");
        if (exists("employees", companyId, personId)) r.add("EMPLOYEE");
        return r;
    }

    /** Contextos de obrigatoriedade dos papéis atuais (PATIENT conta como CUSTOMER). */
    public List<String> roleContexts(UUID companyId, UUID personId) {
        List<String> c = new ArrayList<>(rolesOf(companyId, personId));
        c.remove("PATIENT");
        return c;
    }

    public CustomerInfo customer(UUID companyId, UUID personId) {
        List<CustomerInfo> l = jdbc.query("SELECT c.*, p.record_number, p.health_plan, p.health_plan_number, p.referral_source, "
                        + "p.person_id AS patient_id FROM customers c LEFT JOIN patients p ON p.person_id = c.person_id "
                        + "WHERE c.company_id = ? AND c.person_id = ?",
                (rs, i) -> new CustomerInfo(rs.getString("customer_code"), rs.getBigDecimal("credit_limit"),
                        rs.getBoolean("blocked"), rs.getString("block_reason"), rs.getObject("customer_since", LocalDate.class),
                        rs.getBoolean("active"), rs.getString("notes"),
                        rs.getObject("patient_id") == null ? null : new PatientInfo(rs.getString("record_number"),
                                rs.getString("health_plan"), rs.getString("health_plan_number"), rs.getString("referral_source"))),
                companyId, personId);
        return l.isEmpty() ? null : l.get(0);
    }

    public SupplierInfo supplier(UUID companyId, UUID personId) {
        List<SupplierInfo> l = jdbc.query("SELECT * FROM suppliers WHERE company_id = ? AND person_id = ?",
                (rs, i) -> new SupplierInfo(rs.getString("supplier_code"), rs.getString("supplier_type"),
                        (Integer) rs.getObject("payment_terms_days", Integer.class), rs.getBoolean("blocked"),
                        rs.getString("block_reason"), rs.getBoolean("active"), rs.getString("notes")),
                companyId, personId);
        return l.isEmpty() ? null : l.get(0);
    }

    public EmployeeInfo employee(UUID companyId, UUID personId, boolean salaryVisible) {
        List<EmployeeInfo> l = jdbc.query("SELECT e.*, j.title, "
                        + "nullif(trim(coalesce(u.first_name,'') || ' ' || coalesce(u.last_name,'')), '') AS uname, u.email AS uemail "
                        + "FROM employees e LEFT JOIN job_titles j ON j.id = e.job_title_id LEFT JOIN users u ON u.id = e.user_id "
                        + "WHERE e.company_id = ? AND e.person_id = ?",
                (rs, i) -> new EmployeeInfo(rs.getString("registration_number"), rs.getObject("job_title_id", UUID.class),
                        rs.getString("title"), rs.getObject("hire_date", LocalDate.class),
                        rs.getObject("termination_date", LocalDate.class), rs.getString("contract_type"), rs.getString("status"),
                        salaryVisible ? rs.getBigDecimal("salary") : null, salaryVisible, rs.getObject("user_id", UUID.class),
                        rs.getString("uname") != null ? rs.getString("uname") : rs.getString("uemail"), rs.getString("notes"),
                        specialties(companyId, personId)),
                companyId, personId);
        return l.isEmpty() ? null : l.get(0);
    }

    public List<Specialty> specialties(UUID companyId, UUID personId) {
        return jdbc.query("SELECT * FROM employee_specialties WHERE company_id = ? AND employee_id = ? ORDER BY is_primary DESC, created_at",
                (rs, i) -> new Specialty(id(rs, "id"), rs.getString("specialty"), rs.getString("council"),
                        rs.getString("council_number"), rs.getString("council_uf"), rs.getBoolean("is_primary")),
                companyId, personId);
    }

    // =================================================================== cliente / paciente

    @Transactional
    public void enableCustomer(UUID companyId, UUID personId, CustomerRequest req, CurrentUser user, boolean validate) {
        requirePersonActive(companyId, personId);
        bridge.createContactFor(companyId, personId);
        boolean health = modules.segmentOf(companyId) != CompanySegment.GENERIC;
        boolean existed = exists("customers", companyId, personId);
        if (health) {
            PatientInfo pt = req == null ? null : req.patient();
            try {
                jdbc.update("UPDATE contacts SET is_patient = TRUE, patient_since = coalesce(patient_since, ?, CURRENT_DATE), "
                                + "record_number = coalesce(?, record_number), health_plan = coalesce(?, health_plan), "
                                + "health_plan_number = coalesce(?, health_plan_number), "
                                + "referral_source = coalesce(?, referral_source), updated_at = now() "
                                + "WHERE company_id = ? AND id = ?",
                        req == null ? null : req.customerSince(), pt == null ? null : blank(pt.recordNumber()),
                        pt == null ? null : blank(pt.healthPlan()), pt == null ? null : blank(pt.healthPlanNumber()),
                        pt == null ? null : blank(pt.referralSource()), companyId, personId);
            } catch (DuplicateKeyException e) {
                throw PersonException.conflict("RECORD_NUMBER_IN_USE", "Este nº de prontuário já está em outro cadastro.");
            }
        }
        if (!exists("customers", companyId, personId)) {
            jdbc.update("INSERT INTO customers (person_id, company_id, customer_code, created_by) VALUES (?, ?, ?, ?)",
                    personId, companyId, nextCode("customers", "customer_code", companyId), user.userId());
        }
        applyCustomer(companyId, personId, req, user.userId());
        if (!existed) history.record(companyId, personId, user.userId(), "ROLE_ENABLED", "customers", null,
                Map.of("role", health ? "CUSTOMER/PATIENT" : "CUSTOMER"), null);
        if (validate) fields.validate(companyId, personId, roleContexts(companyId, personId));
    }

    @Transactional
    public void updateCustomer(UUID companyId, UUID personId, CustomerRequest req, CurrentUser user) {
        if (!exists("customers", companyId, personId)) throw PersonException.notFound("Cliente");
        CustomerInfo before = customer(companyId, personId);
        if (req != null && req.patient() != null && exists("patients", companyId, personId)) {
            PatientInfo pt = req.patient();
            try {
                jdbc.update("UPDATE contacts SET record_number = ?, health_plan = ?, health_plan_number = ?, referral_source = ?, "
                                + "updated_at = now() WHERE company_id = ? AND id = ?",
                        blank(pt.recordNumber()), blank(pt.healthPlan()), blank(pt.healthPlanNumber()),
                        blank(pt.referralSource()), companyId, personId);
            } catch (DuplicateKeyException e) {
                throw PersonException.conflict("RECORD_NUMBER_IN_USE", "Este nº de prontuário já está em outro cadastro.");
            }
        }
        applyCustomer(companyId, personId, req, user.userId());
        history.record(companyId, personId, user.userId(), "UPDATE", "customers", customerMap(before),
                customerMap(customer(companyId, personId)), null);
        fields.validate(companyId, personId, roleContexts(companyId, personId));
    }

    @Transactional
    public void disableCustomer(UUID companyId, UUID personId, CurrentUser user) {
        if (!exists("customers", companyId, personId)) throw PersonException.notFound("Cliente");
        if (bridge.hasContact(companyId, personId)) {
            jdbc.update("UPDATE contacts SET is_patient = FALSE, updated_at = now() WHERE company_id = ? AND id = ?", companyId, personId);
        }
        jdbc.update("DELETE FROM patients WHERE company_id = ? AND person_id = ?", companyId, personId);
        jdbc.update("DELETE FROM customers WHERE company_id = ? AND person_id = ?", companyId, personId);
        history.record(companyId, personId, user.userId(), "ROLE_DISABLED", "customers", Map.of("role", "CUSTOMER"), null, null);
    }

    private void applyCustomer(UUID companyId, UUID personId, CustomerRequest req, UUID userId) {
        if (req == null) return;
        if (Boolean.TRUE.equals(req.blocked()) && blank(req.blockReason()) == null) {
            throw PersonException.badRequest("Informe o motivo do bloqueio.");
        }
        jdbc.update("UPDATE customers SET credit_limit = ?, blocked = coalesce(?, blocked), block_reason = ?, "
                        + "customer_since = coalesce(?, customer_since), active = coalesce(?, active), notes = ?, "
                        + "updated_by = ?, updated_at = now() WHERE company_id = ? AND person_id = ?",
                req.creditLimit(), req.blocked(), Boolean.TRUE.equals(req.blocked()) ? blank(req.blockReason()) : null,
                req.customerSince(), req.active(), blank(req.notes()), userId, companyId, personId);
    }

    private static Map<String, Object> customerMap(CustomerInfo c) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (c == null) return m;
        m.put("creditLimit", c.creditLimit());
        m.put("blocked", c.blocked());
        m.put("blockReason", c.blockReason());
        m.put("customerSince", c.customerSince() == null ? null : c.customerSince().toString());
        m.put("active", c.active());
        if (c.patient() != null) {
            m.put("recordNumber", c.patient().recordNumber());
            m.put("healthPlan", c.patient().healthPlan());
            m.put("healthPlanNumber", c.patient().healthPlanNumber());
            m.put("referralSource", c.patient().referralSource());
        }
        return m;
    }

    // =================================================================== fornecedor

    @Transactional
    public void enableSupplier(UUID companyId, UUID personId, SupplierRequest req, CurrentUser user, boolean validate) {
        requirePersonActive(companyId, personId);
        if (!exists("suppliers", companyId, personId)) {
            jdbc.update("INSERT INTO suppliers (person_id, company_id, supplier_code, created_by) VALUES (?, ?, ?, ?)",
                    personId, companyId, nextCode("suppliers", "supplier_code", companyId), user.userId());
            history.record(companyId, personId, user.userId(), "ROLE_ENABLED", "suppliers", null, Map.of("role", "SUPPLIER"), null);
        }
        applySupplier(companyId, personId, req, user.userId());
        if (validate) fields.validate(companyId, personId, roleContexts(companyId, personId));
    }

    @Transactional
    public void updateSupplier(UUID companyId, UUID personId, SupplierRequest req, CurrentUser user) {
        if (!exists("suppliers", companyId, personId)) throw PersonException.notFound("Fornecedor");
        SupplierInfo before = supplier(companyId, personId);
        applySupplier(companyId, personId, req, user.userId());
        SupplierInfo after = supplier(companyId, personId);
        history.record(companyId, personId, user.userId(), "UPDATE", "suppliers", supplierMap(before), supplierMap(after), null);
        fields.validate(companyId, personId, roleContexts(companyId, personId));
    }

    @Transactional
    public void disableSupplier(UUID companyId, UUID personId, CurrentUser user) {
        if (jdbc.update("DELETE FROM suppliers WHERE company_id = ? AND person_id = ?", companyId, personId) == 0) {
            throw PersonException.notFound("Fornecedor");
        }
        history.record(companyId, personId, user.userId(), "ROLE_DISABLED", "suppliers", Map.of("role", "SUPPLIER"), null, null);
    }

    private void applySupplier(UUID companyId, UUID personId, SupplierRequest req, UUID userId) {
        if (req == null) return;
        String type = req.supplierType() == null ? null : upper(req.supplierType());
        if (type != null && !SUPPLIER_TYPES.contains(type)) throw PersonException.badRequest("Tipo de fornecedor inválido.");
        if (req.paymentTermsDays() != null && req.paymentTermsDays() < 0) throw PersonException.badRequest("Prazo inválido.");
        if (Boolean.TRUE.equals(req.blocked()) && blank(req.blockReason()) == null) {
            throw PersonException.badRequest("Informe o motivo do bloqueio.");
        }
        jdbc.update("UPDATE suppliers SET supplier_type = coalesce(?, supplier_type), payment_terms_days = ?, "
                        + "blocked = coalesce(?, blocked), block_reason = ?, active = coalesce(?, active), notes = ?, "
                        + "updated_by = ?, updated_at = now() WHERE company_id = ? AND person_id = ?",
                type, req.paymentTermsDays(), req.blocked(), Boolean.TRUE.equals(req.blocked()) ? blank(req.blockReason()) : null,
                req.active(), blank(req.notes()), userId, companyId, personId);
    }

    private static Map<String, Object> supplierMap(SupplierInfo s) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (s == null) return m;
        m.put("supplierType", s.supplierType());
        m.put("paymentTermsDays", s.paymentTermsDays());
        m.put("blocked", s.blocked());
        m.put("blockReason", s.blockReason());
        m.put("active", s.active());
        m.put("notes", s.notes());
        return m;
    }

    // =================================================================== funcionário

    @Transactional
    public void enableEmployee(UUID companyId, UUID personId, EmployeeRequest req, CurrentUser user, boolean validate) {
        requirePersonActive(companyId, personId);
        if (!exists("employees", companyId, personId)) {
            jdbc.update("INSERT INTO employees (person_id, company_id, created_by) VALUES (?, ?, ?)", personId, companyId, user.userId());
            history.record(companyId, personId, user.userId(), "ROLE_ENABLED", "employees", null, Map.of("role", "EMPLOYEE"), null);
        }
        applyEmployee(companyId, personId, req, user);
        if (validate) fields.validate(companyId, personId, roleContexts(companyId, personId));
    }

    @Transactional
    public void updateEmployee(UUID companyId, UUID personId, EmployeeRequest req, CurrentUser user) {
        if (!exists("employees", companyId, personId)) throw PersonException.notFound("Funcionário");
        boolean salary = PersonAccess.has(user, "employee:salary");
        EmployeeInfo before = employee(companyId, personId, salary);
        applyEmployee(companyId, personId, req, user);
        history.record(companyId, personId, user.userId(), "UPDATE", "employees", employeeMap(before),
                employeeMap(employee(companyId, personId, salary)), null);
        fields.validate(companyId, personId, roleContexts(companyId, personId));
    }

    @Transactional
    public void disableEmployee(UUID companyId, UUID personId, CurrentUser user) {
        if (jdbc.update("DELETE FROM employees WHERE company_id = ? AND person_id = ?", companyId, personId) == 0) {
            throw PersonException.notFound("Funcionário");
        }
        history.record(companyId, personId, user.userId(), "ROLE_DISABLED", "employees", Map.of("role", "EMPLOYEE"), null, null);
    }

    private void applyEmployee(UUID companyId, UUID personId, EmployeeRequest req, CurrentUser user) {
        if (req == null) return;
        String contract = req.contractType() == null ? null : upper(req.contractType());
        if (contract != null && !CONTRACT_TYPES.contains(contract)) throw PersonException.badRequest("Tipo de contrato inválido.");
        String status = req.status() == null ? null : upper(req.status());
        if (status != null && !EMPLOYEE_STATUS.contains(status)) throw PersonException.badRequest("Situação inválida.");
        if (req.hireDate() != null && req.terminationDate() != null && req.terminationDate().isBefore(req.hireDate())) {
            throw PersonException.badRequest("O desligamento não pode ser antes da admissão.");
        }
        if (req.jobTitleId() != null && !Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM job_titles WHERE company_id = ? AND id = ?)", Boolean.class, companyId, req.jobTitleId()))) {
            throw PersonException.badRequest("Cargo não encontrado.");
        }
        if (req.userId() != null) {
            Boolean member = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM memberships WHERE user_id = ? AND company_id = ? "
                    + "AND status = 'ACTIVE')", Boolean.class, req.userId(), companyId);
            if (!Boolean.TRUE.equals(member)) throw PersonException.badRequest("O usuário não faz parte da equipe desta empresa.");
        }
        UUID previousTitle = jdbc.queryForObject("SELECT job_title_id FROM employees WHERE company_id = ? AND person_id = ?",
                UUID.class, companyId, personId);
        boolean salaryAllowed = PersonAccess.has(user, "employee:salary");
        try {
            jdbc.update("UPDATE employees SET registration_number = ?, job_title_id = ?, hire_date = ?, termination_date = ?, "
                            + "contract_type = ?, status = coalesce(?, status), "
                            + "salary = CASE WHEN ? THEN ? ELSE salary END, user_id = ?, notes = ?, updated_by = ?, updated_at = now() "
                            + "WHERE company_id = ? AND person_id = ?",
                    blank(req.registrationNumber()), req.jobTitleId(), req.hireDate(), req.terminationDate(), contract, status,
                    salaryAllowed, req.salary(), req.userId(), blank(req.notes()), user.userId(), companyId, personId);
        } catch (DuplicateKeyException e) {
            throw PersonException.conflict("EMPLOYEE_DUPLICATE", "Matrícula ou usuário já vinculado a outro funcionário.");
        }
        if (!Objects.equals(previousTitle, req.jobTitleId())) {
            jdbc.update("UPDATE employee_job_history SET end_date = CURRENT_DATE WHERE company_id = ? AND employee_id = ? "
                    + "AND end_date IS NULL", companyId, personId);
            if (req.jobTitleId() != null) {
                jdbc.update("INSERT INTO employee_job_history (company_id, employee_id, job_title_id, start_date) VALUES (?,?,?,?)",
                        companyId, personId, req.jobTitleId(), req.hireDate() != null && previousTitle == null
                                ? req.hireDate() : LocalDate.now());
            }
        }
        syncMembershipSpecialty(companyId, personId);
    }

    private static Map<String, Object> employeeMap(EmployeeInfo e) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (e == null) return m;
        m.put("registrationNumber", e.registrationNumber());
        m.put("jobTitle", e.jobTitle());
        m.put("hireDate", e.hireDate() == null ? null : e.hireDate().toString());
        m.put("terminationDate", e.terminationDate() == null ? null : e.terminationDate().toString());
        m.put("contractType", e.contractType());
        m.put("status", e.status());
        m.put("salary", e.salary());
        m.put("userId", e.userId() == null ? null : e.userId().toString());
        return m;
    }

    @Transactional
    public List<Specialty> addSpecialty(UUID companyId, UUID personId, SpecialtyRequest req, CurrentUser user) {
        if (!exists("employees", companyId, personId)) throw PersonException.notFound("Funcionário");
        String sp = upper(req.specialty());
        if (!SPECIALTIES.contains(sp)) throw PersonException.badRequest("Especialidade inválida.");
        boolean primary = Boolean.TRUE.equals(req.primary()) || specialties(companyId, personId).isEmpty();
        if (primary) jdbc.update("UPDATE employee_specialties SET is_primary = FALSE WHERE company_id = ? AND employee_id = ?",
                companyId, personId);
        try {
            jdbc.update("INSERT INTO employee_specialties (company_id, employee_id, specialty, council, council_number, council_uf, "
                            + "is_primary) VALUES (?,?,?,?,?,?,?)",
                    companyId, personId, sp, upperBlank(req.council()), blank(req.councilNumber()), upperBlank(req.councilUf()), primary);
        } catch (DuplicateKeyException e) {
            throw PersonException.conflict("SPECIALTY_EXISTS", "Esta especialidade já está cadastrada.");
        }
        history.record(companyId, personId, user.userId(), "UPDATE", "employee_specialties", null,
                Map.of("specialty", sp, "council", String.valueOf(req.council()), "councilNumber", String.valueOf(req.councilNumber())), null);
        syncMembershipSpecialty(companyId, personId);
        return specialties(companyId, personId);
    }

    @Transactional
    public List<Specialty> updateSpecialty(UUID companyId, UUID personId, UUID id, SpecialtyRequest req, CurrentUser user) {
        Boolean ok = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM employee_specialties WHERE company_id = ? AND employee_id = ? "
                + "AND id = ?)", Boolean.class, companyId, personId, id);
        if (!Boolean.TRUE.equals(ok)) throw PersonException.notFound("Especialidade");
        if (Boolean.TRUE.equals(req.primary())) {
            jdbc.update("UPDATE employee_specialties SET is_primary = FALSE WHERE company_id = ? AND employee_id = ?", companyId, personId);
        }
        jdbc.update("UPDATE employee_specialties SET council = ?, council_number = ?, council_uf = ?, "
                        + "is_primary = coalesce(?, is_primary) WHERE id = ?",
                upperBlank(req.council()), blank(req.councilNumber()), upperBlank(req.councilUf()), req.primary(), id);
        history.record(companyId, personId, user.userId(), "UPDATE", "employee_specialties", null,
                Map.of("council", String.valueOf(req.council()), "councilNumber", String.valueOf(req.councilNumber())), null);
        syncMembershipSpecialty(companyId, personId);
        return specialties(companyId, personId);
    }

    @Transactional
    public List<Specialty> deleteSpecialty(UUID companyId, UUID personId, UUID id, CurrentUser user) {
        if (jdbc.update("DELETE FROM employee_specialties WHERE company_id = ? AND employee_id = ? AND id = ?", companyId, personId, id) == 0) {
            throw PersonException.notFound("Especialidade");
        }
        jdbc.update("UPDATE employee_specialties SET is_primary = TRUE WHERE id = (SELECT id FROM employee_specialties WHERE "
                + "company_id = ? AND employee_id = ? ORDER BY created_at LIMIT 1) AND NOT EXISTS (SELECT 1 FROM employee_specialties "
                + "WHERE company_id = ? AND employee_id = ? AND is_primary)", companyId, personId, companyId, personId);
        history.record(companyId, personId, user.userId(), "UPDATE", "employee_specialties", Map.of("specialtyId", id.toString()), null, null);
        syncMembershipSpecialty(companyId, personId);
        return specialties(companyId, personId);
    }

    /**
     * Agenda e procedimentos ainda usam o usuário (memberships.specialty). Enquanto isso, a
     * especialidade principal do funcionário vinculado é copiada para o vínculo dele na empresa.
     */
    private void syncMembershipSpecialty(UUID companyId, UUID personId) {
        List<Map<String, Object>> e = jdbc.queryForList("SELECT e.user_id, (SELECT specialty FROM employee_specialties s "
                + "WHERE s.company_id = e.company_id AND s.employee_id = e.person_id ORDER BY s.is_primary DESC, s.created_at LIMIT 1) AS sp "
                + "FROM employees e WHERE e.company_id = ? AND e.person_id = ?", companyId, personId);
        if (e.isEmpty() || e.get(0).get("user_id") == null || e.get(0).get("sp") == null) return;
        jdbc.update("UPDATE memberships SET specialty = ?, updated_at = now() WHERE user_id = ? AND company_id = ? "
                + "AND status = 'ACTIVE' AND specialty IS DISTINCT FROM ?", e.get(0).get("sp"), e.get(0).get("user_id"), companyId,
                e.get(0).get("sp"));
    }

    // =================================================================== cargos

    @Transactional(readOnly = true)
    public List<JobTitle> jobTitles(UUID companyId) {
        return jdbc.query("SELECT * FROM job_titles WHERE company_id = ? ORDER BY active DESC, lower(title)",
                (rs, i) -> new JobTitle(id(rs, "id"), rs.getString("title"), rs.getBoolean("active")), companyId);
    }

    @Transactional
    public List<JobTitle> saveJobTitle(UUID companyId, UUID id, JobTitleRequest req) {
        String title = blank(req.title());
        if (title == null) throw PersonException.badRequest("Informe o nome do cargo.");
        try {
            if (id == null) {
                jdbc.update("INSERT INTO job_titles (company_id, title) VALUES (?, ?)", companyId, trunc(title, 80));
            } else if (jdbc.update("UPDATE job_titles SET title = ?, active = coalesce(?, active) WHERE company_id = ? AND id = ?",
                    trunc(title, 80), req.active(), companyId, id) == 0) {
                throw PersonException.notFound("Cargo");
            }
        } catch (DuplicateKeyException e) {
            throw PersonException.conflict("JOB_TITLE_EXISTS", "Já existe um cargo com este nome.");
        }
        return jobTitles(companyId);
    }

    // =================================================================== listas dos menus

    @Transactional(readOnly = true)
    public PageResult<CustomerRow> customers(UUID companyId, String q, Boolean active, Boolean blocked, String healthPlan,
                                             Boolean upcoming, Integer birthMonth, int page, int size, boolean sensitive) {
        MapSqlParameterSource ps = new MapSqlParameterSource("c", companyId);
        StringBuilder where = new StringBuilder(" FROM customers cu JOIN persons p ON p.id = cu.person_id "
                + "LEFT JOIN patients pt ON pt.person_id = cu.person_id WHERE cu.company_id = :c AND NOT p.deleted");
        PersonService.appendSearch(where, ps, q);
        if (q != null && !q.isBlank()) {
            where.insert(where.length() - 1, " OR lower(coalesce(pt.record_number, '')) LIKE :q OR cu.customer_code = :qraw");
            ps.addValue("qraw", q.trim());
        }
        if (active != null) {
            where.append(" AND cu.active = :a");
            ps.addValue("a", active);
        }
        if (blocked != null) {
            where.append(" AND cu.blocked = :b");
            ps.addValue("b", blocked);
        }
        if (healthPlan != null && !healthPlan.isBlank()) {
            where.append(" AND lower(pt.health_plan) LIKE :hp");
            ps.addValue("hp", "%" + healthPlan.trim().toLowerCase(Locale.ROOT) + "%");
        }
        if (Boolean.TRUE.equals(upcoming)) {
            where.append(" AND EXISTS (SELECT 1 FROM appointments ap WHERE ap.contact_id = cu.person_id AND ap.start_at >= now())");
        }
        if (birthMonth != null && birthMonth >= 1 && birthMonth <= 12) {
            where.append(" AND EXTRACT(MONTH FROM p.birth_or_foundation_date) = :bm");
            ps.addValue("bm", birthMonth);
        }
        long total = Optional.ofNullable(named.queryForObject("SELECT count(*)" + where, ps, Long.class)).orElse(0L);
        int sz = Math.max(1, Math.min(size, 200));
        ps.addValue("lim", sz).addValue("off", Math.max(0, page) * sz);
        List<CustomerRow> items = named.query("SELECT p.id, p.name, p.tax_id, p.birth_or_foundation_date, cu.customer_code, "
                        + "cu.customer_since, cu.active, cu.blocked, pt.person_id AS pid, pt.record_number, pt.health_plan, "
                        + "(SELECT value FROM person_contacts pc WHERE pc.person_id = p.id AND pc.active AND pc.contact_type = 'MOBILE' "
                        + " ORDER BY pc.is_primary DESC, pc.created_at LIMIT 1) AS mobile"
                        + where + " ORDER BY lower(p.name), p.id LIMIT :lim OFFSET :off", ps,
                (rs, i) -> new CustomerRow(id(rs, "id"), rs.getString("name"), showTax(rs.getString("tax_id"), sensitive),
                        rs.getString("mobile"), rs.getString("customer_code"), rs.getObject("customer_since", LocalDate.class),
                        rs.getBoolean("active"), rs.getBoolean("blocked"), rs.getObject("pid") != null,
                        rs.getString("record_number"), rs.getString("health_plan"),
                        rs.getObject("birth_or_foundation_date", LocalDate.class)));
        return new PageResult<>(items, total, Math.max(0, page), sz);
    }

    @Transactional(readOnly = true)
    public PageResult<SupplierRow> suppliers(UUID companyId, String q, String type, Boolean active, Boolean blocked, int page,
                                             int size, boolean sensitive) {
        MapSqlParameterSource ps = new MapSqlParameterSource("c", companyId);
        StringBuilder where = new StringBuilder(" FROM suppliers s JOIN persons p ON p.id = s.person_id "
                + "WHERE s.company_id = :c AND NOT p.deleted");
        PersonService.appendSearch(where, ps, q);
        if (type != null && SUPPLIER_TYPES.contains(type)) {
            where.append(" AND s.supplier_type = :t");
            ps.addValue("t", type);
        }
        if (active != null) {
            where.append(" AND s.active = :a");
            ps.addValue("a", active);
        }
        if (blocked != null) {
            where.append(" AND s.blocked = :b");
            ps.addValue("b", blocked);
        }
        long total = Optional.ofNullable(named.queryForObject("SELECT count(*)" + where, ps, Long.class)).orElse(0L);
        int sz = Math.max(1, Math.min(size, 200));
        ps.addValue("lim", sz).addValue("off", Math.max(0, page) * sz);
        List<SupplierRow> items = named.query("SELECT p.id, p.name, p.trade_name, p.tax_id, s.supplier_code, s.supplier_type, "
                        + "s.active, s.blocked, "
                        + "(SELECT value FROM person_contacts pc WHERE pc.person_id = p.id AND pc.active AND pc.contact_type IN ('PHONE','MOBILE') "
                        + " ORDER BY pc.is_primary DESC, pc.created_at LIMIT 1) AS phone, "
                        + "(SELECT value FROM person_contacts pc WHERE pc.person_id = p.id AND pc.active AND pc.contact_type = 'EMAIL' "
                        + " ORDER BY pc.is_primary DESC, pc.created_at LIMIT 1) AS email"
                        + where + " ORDER BY lower(p.name), p.id LIMIT :lim OFFSET :off", ps,
                (rs, i) -> new SupplierRow(id(rs, "id"), rs.getString("name"), rs.getString("trade_name"),
                        showTax(rs.getString("tax_id"), sensitive), rs.getString("supplier_code"), rs.getString("supplier_type"),
                        rs.getString("phone"), rs.getString("email"), rs.getBoolean("active"), rs.getBoolean("blocked")));
        return new PageResult<>(items, total, Math.max(0, page), sz);
    }

    @Transactional(readOnly = true)
    public PageResult<EmployeeRow> employees(UUID companyId, String q, UUID jobTitleId, String specialty, String status,
                                             Boolean withUser, Boolean professional, int page, int size) {
        MapSqlParameterSource ps = new MapSqlParameterSource("c", companyId);
        StringBuilder where = new StringBuilder(" FROM employees e JOIN persons p ON p.id = e.person_id "
                + "LEFT JOIN job_titles j ON j.id = e.job_title_id LEFT JOIN users u ON u.id = e.user_id "
                + "WHERE e.company_id = :c AND NOT p.deleted");
        PersonService.appendSearch(where, ps, q);
        if (jobTitleId != null) {
            where.append(" AND e.job_title_id = :j");
            ps.addValue("j", jobTitleId);
        }
        if (specialty != null && SPECIALTIES.contains(specialty)) {
            where.append(" AND EXISTS (SELECT 1 FROM employee_specialties s WHERE s.employee_id = e.person_id AND s.specialty = :sp)");
            ps.addValue("sp", specialty);
        }
        if (status != null && EMPLOYEE_STATUS.contains(status)) {
            where.append(" AND e.status = :st");
            ps.addValue("st", status);
        }
        if (withUser != null) where.append(withUser ? " AND e.user_id IS NOT NULL" : " AND e.user_id IS NULL");
        if (professional != null) {
            where.append(professional ? " AND EXISTS" : " AND NOT EXISTS")
                    .append(" (SELECT 1 FROM employee_specialties s WHERE s.employee_id = e.person_id)");
        }
        long total = Optional.ofNullable(named.queryForObject("SELECT count(*)" + where, ps, Long.class)).orElse(0L);
        int sz = Math.max(1, Math.min(size, 200));
        ps.addValue("lim", sz).addValue("off", Math.max(0, page) * sz);
        List<EmployeeRow> items = named.query("SELECT p.id, p.name, e.registration_number, j.title, e.user_id, e.hire_date, e.status, "
                        + "nullif(trim(coalesce(u.first_name,'') || ' ' || coalesce(u.last_name,'')), '') AS uname, u.email AS uemail, "
                        + "(SELECT string_agg(s.specialty, ',' ORDER BY s.is_primary DESC) FROM employee_specialties s "
                        + " WHERE s.employee_id = e.person_id) AS specs, "
                        + "(SELECT s.council || ' ' || s.council_number || coalesce('/' || s.council_uf, '') FROM employee_specialties s "
                        + " WHERE s.employee_id = e.person_id AND s.council_number IS NOT NULL ORDER BY s.is_primary DESC LIMIT 1) AS council"
                        + where + " ORDER BY lower(p.name), p.id LIMIT :lim OFFSET :off", ps,
                (rs, i) -> new EmployeeRow(id(rs, "id"), rs.getString("name"), rs.getString("registration_number"),
                        rs.getString("title"), rs.getString("specs") == null ? List.of() : List.of(rs.getString("specs").split(",")),
                        rs.getString("council"), rs.getObject("user_id", UUID.class),
                        rs.getString("uname") != null ? rs.getString("uname") : rs.getString("uemail"),
                        rs.getObject("hire_date", LocalDate.class), rs.getString("status")));
        return new PageResult<>(items, total, Math.max(0, page), sz);
    }

    @Transactional(readOnly = true)
    public List<UserOption> teamUsers(UUID companyId) {
        return jdbc.query("SELECT u.id, nullif(trim(coalesce(u.first_name,'') || ' ' || coalesce(u.last_name,'')), '') AS n, u.email "
                        + "FROM memberships m JOIN users u ON u.id = m.user_id WHERE m.company_id = ? AND m.status = 'ACTIVE' "
                        + "ORDER BY lower(coalesce(u.first_name, u.email))",
                (rs, i) -> new UserOption(id(rs, "id"), rs.getString("n") != null ? rs.getString("n") : rs.getString("email"),
                        rs.getString("email")), companyId);
    }

    // =================================================================== auxiliares

    private boolean exists(String table, UUID companyId, UUID personId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM " + table
                + " WHERE company_id = ? AND person_id = ?)", Boolean.class, companyId, personId));
    }

    private void requirePersonActive(UUID companyId, UUID personId) {
        List<Boolean> d = jdbc.queryForList("SELECT deleted FROM persons WHERE company_id = ? AND id = ?", Boolean.class,
                companyId, personId);
        if (d.isEmpty()) throw PersonException.notFound("Cadastro");
        if (Boolean.TRUE.equals(d.get(0))) throw PersonException.badRequest("Cadastro excluído: restaure antes de habilitar papéis.");
    }

    private String nextCode(String table, String column, UUID companyId) {
        Integer n = jdbc.queryForObject("SELECT coalesce(max(" + column + "::int), 0) + 1 FROM " + table
                + " WHERE company_id = ? AND " + column + " ~ '^[0-9]{1,9}$'", Integer.class, companyId);
        return String.valueOf(n == null ? 1 : n);
    }

    @SuppressWarnings("unused")
    private static BigDecimal nz(BigDecimal b) {
        return b == null ? BigDecimal.ZERO : b;
    }
}
