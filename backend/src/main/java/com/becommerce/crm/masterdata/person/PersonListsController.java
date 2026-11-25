package com.becommerce.crm.masterdata.person;

import com.becommerce.crm.masterdata.person.PersonDtos.*;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Listas dos itens do menu Pessoas, cargos, duplicados e obrigatoriedade de campos (doc 22.3–22.8, 18). */
@RestController
@RequestMapping("/api/v1/companies/{companyId}")
public class PersonListsController {

    private static final String OWN = PersonController.OWN;

    private final PersonRoleService roles;
    private final PersonMergeService merge;
    private final PersonFieldSettingsService settings;

    public PersonListsController(PersonRoleService roles, PersonMergeService merge, PersonFieldSettingsService settings) {
        this.roles = roles;
        this.merge = merge;
        this.settings = settings;
    }

    @GetMapping("/customers")
    @PreAuthorize("hasAuthority('person:read')")
    public PageResult<CustomerRow> customers(@CurrentCompanyId(OWN) UUID companyId,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(required = false) Boolean active,
                                             @RequestParam(required = false) Boolean blocked,
                                             @RequestParam(required = false) String healthPlan,
                                             @RequestParam(required = false) Boolean upcoming,
                                             @RequestParam(required = false) Integer birthMonth,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "50") int size,
                                             @AuthenticationPrincipal CurrentUser user) {
        return roles.customers(companyId, q, active, blocked, healthPlan, upcoming, birthMonth, page, size,
                PersonAccess.has(user, "person:sensitive"));
    }

    @GetMapping("/suppliers")
    @PreAuthorize("hasAuthority('person:read')")
    public PageResult<SupplierRow> suppliers(@CurrentCompanyId(OWN) UUID companyId,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(required = false) String type,
                                             @RequestParam(required = false) Boolean active,
                                             @RequestParam(required = false) Boolean blocked,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "50") int size,
                                             @AuthenticationPrincipal CurrentUser user) {
        return roles.suppliers(companyId, q, type, active, blocked, page, size, PersonAccess.has(user, "person:sensitive"));
    }

    @GetMapping("/employees")
    @PreAuthorize("hasAuthority('person:read')")
    public PageResult<EmployeeRow> employees(@CurrentCompanyId(OWN) UUID companyId,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(required = false) UUID jobTitleId,
                                             @RequestParam(required = false) String specialty,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) Boolean withUser,
                                             @RequestParam(required = false) Boolean professional,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "50") int size) {
        return roles.employees(companyId, q, jobTitleId, specialty, status, withUser, professional, page, size);
    }

    @GetMapping("/employees/team-users")
    @PreAuthorize("hasAuthority('employee:manage')")
    public List<UserOption> teamUsers(@CurrentCompanyId(OWN) UUID companyId) {
        return roles.teamUsers(companyId);
    }

    @GetMapping("/job-titles")
    @PreAuthorize("hasAuthority('person:read')")
    public List<JobTitle> jobTitles(@CurrentCompanyId(OWN) UUID companyId) {
        return roles.jobTitles(companyId);
    }

    @PostMapping("/job-titles")
    @PreAuthorize("hasAuthority('employee:manage')")
    public List<JobTitle> createJobTitle(@CurrentCompanyId(OWN) UUID companyId, @RequestBody JobTitleRequest req) {
        return roles.saveJobTitle(companyId, null, req);
    }

    @PutMapping("/job-titles/{id}")
    @PreAuthorize("hasAuthority('employee:manage')")
    public List<JobTitle> updateJobTitle(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id,
                                         @RequestBody JobTitleRequest req) {
        return roles.saveJobTitle(companyId, id, req);
    }

    // ---------------------------------------------------------------- duplicados

    @GetMapping("/persons-duplicates")
    @PreAuthorize("hasAuthority('person:merge')")
    public List<DuplicateGroup> duplicates(@CurrentCompanyId(OWN) UUID companyId) {
        return merge.duplicates(companyId);
    }

    @PostMapping("/persons-duplicates/merge")
    @PreAuthorize("hasAuthority('person:merge')")
    public MergeResult merge(@CurrentCompanyId(OWN) UUID companyId, @RequestBody MergeRequest req,
                             @AuthenticationPrincipal CurrentUser user) {
        return merge.merge(companyId, req.keepId(), req.removeId(), req.reason(), user);
    }

    @PostMapping("/persons-duplicates/issues/{issueId}/dismiss")
    @PreAuthorize("hasAuthority('person:merge')")
    public ResponseEntity<Void> dismiss(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID issueId,
                                        @AuthenticationPrincipal CurrentUser user) {
        merge.dismissIssue(companyId, issueId, user);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- obrigatoriedade (seção 18)

    @GetMapping("/settings/person-fields")
    @PreAuthorize("hasAuthority('person:read')")
    public FieldSettings fieldSettings(@CurrentCompanyId(OWN) UUID companyId) {
        return settings.get(companyId);
    }

    @PutMapping("/settings/person-fields")
    @PreAuthorize("hasAuthority('settings:update')")
    public FieldSettings updateFieldSettings(@CurrentCompanyId(OWN) UUID companyId, @RequestBody FieldSettingsUpdate req,
                                             @AuthenticationPrincipal CurrentUser user) {
        return settings.update(companyId, req.changes() == null ? List.of() : req.changes(), user.userId());
    }

    @PostMapping("/settings/person-fields/reset")
    @PreAuthorize("hasAuthority('settings:update')")
    public FieldSettings resetFieldSettings(@CurrentCompanyId(OWN) UUID companyId) {
        return settings.resetToDefault(companyId);
    }
}
