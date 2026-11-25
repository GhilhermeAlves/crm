package com.becommerce.crm.masterdata.person;

import com.becommerce.crm.masterdata.person.PersonDtos.*;
import com.becommerce.crm.shared.security.config.CurrentCompanyId;
import com.becommerce.crm.shared.security.filter.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Cadastro central de pessoas (doc seção 9 e 22.2). */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/persons")
public class PersonController {

    static final String OWN = "Você só pode acessar o cadastro de pessoas da sua própria empresa.";

    private final PersonService service;
    private final PersonRoleService roles;

    public PersonController(PersonService service, PersonRoleService roles) {
        this.service = service;
        this.roles = roles;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('person:read')")
    public PageResult<PersonSummary> search(@CurrentCompanyId(OWN) UUID companyId,
                                            @RequestParam(required = false) String q,
                                            @RequestParam(required = false) String type,
                                            @RequestParam(required = false) String role,
                                            @RequestParam(required = false) Boolean active,
                                            @RequestParam(required = false) Boolean provisional,
                                            @RequestParam(required = false) String uf,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "50") int size,
                                            @AuthenticationPrincipal CurrentUser user) {
        return service.search(companyId, q, type, role, active, provisional, uf, page, size, user);
    }

    @GetMapping("/check")
    @PreAuthorize("hasAuthority('person:read')")
    public List<DuplicateMember> check(@CurrentCompanyId(OWN) UUID companyId,
                                       @RequestParam(required = false) String taxId,
                                       @RequestParam(required = false) String phone,
                                       @RequestParam(required = false) UUID excludeId) {
        return service.check(companyId, taxId, phone, excludeId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('person:read')")
    public PersonDetail get(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id,
                            @AuthenticationPrincipal CurrentUser user) {
        return service.get(companyId, id, user);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('person:create')")
    public ResponseEntity<PersonDetail> create(@CurrentCompanyId(OWN) UUID companyId, @RequestBody PersonRequest req,
                                               @AuthenticationPrincipal CurrentUser user) {
        if (req.roles() != null) {
            if (req.roles().contains("SUPPLIER")) PersonAccess.require(user, "supplier:manage", "Sem permissão para cadastrar fornecedores.");
            if (req.roles().contains("EMPLOYEE")) PersonAccess.require(user, "employee:manage", "Sem permissão para cadastrar funcionários.");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(companyId, req, user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('person:update')")
    public PersonDetail update(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id, @RequestBody PersonRequest req,
                               @AuthenticationPrincipal CurrentUser user) {
        return service.update(companyId, id, req, user);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('person:update')")
    public PersonDetail status(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id, @RequestBody StatusRequest req,
                               @AuthenticationPrincipal CurrentUser user) {
        return service.setStatus(companyId, id, req.active(), user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('person:delete')")
    public ResponseEntity<Void> delete(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id,
                                       @RequestBody(required = false) DeleteRequest req,
                                       @RequestParam(required = false) String reason,
                                       @AuthenticationPrincipal CurrentUser user) {
        service.softDelete(companyId, id, req != null && req.reason() != null ? req.reason() : reason, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('person:delete')")
    public PersonDetail restore(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id,
                                @AuthenticationPrincipal CurrentUser user) {
        return service.restore(companyId, id, user);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('person:read')")
    public List<HistoryEntry> history(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id) {
        return service.history(companyId, id);
    }

    // ---------------------------------------------------------------- endereços

    @PostMapping("/{id}/addresses")
    @PreAuthorize("hasAuthority('person:update')")
    public List<Address> addAddress(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id, @RequestBody AddressRequest req,
                                    @AuthenticationPrincipal CurrentUser user) {
        return service.addAddress(companyId, id, req, user.userId(), true);
    }

    @PutMapping("/{id}/addresses/{addressId}")
    @PreAuthorize("hasAuthority('person:update')")
    public List<Address> updateAddress(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id, @PathVariable UUID addressId,
                                       @RequestBody AddressRequest req, @AuthenticationPrincipal CurrentUser user) {
        return service.updateAddress(companyId, id, addressId, req, user.userId());
    }

    @DeleteMapping("/{id}/addresses/{addressId}")
    @PreAuthorize("hasAuthority('person:update')")
    public List<Address> deleteAddress(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id, @PathVariable UUID addressId,
                                       @AuthenticationPrincipal CurrentUser user) {
        return service.deleteAddress(companyId, id, addressId, user.userId());
    }

    // ---------------------------------------------------------------- contatos

    @PostMapping("/{id}/contact-channels")
    @PreAuthorize("hasAuthority('person:update')")
    public List<ContactChannel> addChannel(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id,
                                           @RequestBody ContactChannelRequest req, @AuthenticationPrincipal CurrentUser user) {
        return service.addChannel(companyId, id, req, user.userId(), true);
    }

    @PutMapping("/{id}/contact-channels/{channelId}")
    @PreAuthorize("hasAuthority('person:update')")
    public List<ContactChannel> updateChannel(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id,
                                              @PathVariable UUID channelId, @RequestBody ContactChannelRequest req,
                                              @AuthenticationPrincipal CurrentUser user) {
        return service.updateChannel(companyId, id, channelId, req, user.userId());
    }

    @DeleteMapping("/{id}/contact-channels/{channelId}")
    @PreAuthorize("hasAuthority('person:update')")
    public List<ContactChannel> deleteChannel(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id,
                                              @PathVariable UUID channelId, @AuthenticationPrincipal CurrentUser user) {
        return service.deleteChannel(companyId, id, channelId, user.userId());
    }

    // ---------------------------------------------------------------- documentos (person:sensitive)

    @PostMapping("/{id}/documents")
    @PreAuthorize("hasAuthority('person:sensitive')")
    public List<Document> addDocument(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id,
                                      @RequestBody DocumentRequest req, @AuthenticationPrincipal CurrentUser user) {
        return service.addDocument(companyId, id, req, user.userId());
    }

    @PutMapping("/{id}/documents/{docId}")
    @PreAuthorize("hasAuthority('person:sensitive')")
    public List<Document> updateDocument(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id, @PathVariable UUID docId,
                                         @RequestBody DocumentRequest req, @AuthenticationPrincipal CurrentUser user) {
        return service.updateDocument(companyId, id, docId, req, user.userId());
    }

    @DeleteMapping("/{id}/documents/{docId}")
    @PreAuthorize("hasAuthority('person:sensitive')")
    public List<Document> deleteDocument(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id, @PathVariable UUID docId,
                                         @AuthenticationPrincipal CurrentUser user) {
        return service.deleteDocument(companyId, id, docId, user.userId());
    }

    // ---------------------------------------------------------------- dados bancários (person:bank)

    @GetMapping("/{id}/bank-accounts")
    @PreAuthorize("hasAuthority('person:bank')")
    public List<BankAccount> bankAccounts(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id) {
        return service.bankAccounts(companyId, id);
    }

    @PostMapping("/{id}/bank-accounts")
    @PreAuthorize("hasAuthority('person:bank')")
    public List<BankAccount> addBankAccount(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id,
                                            @RequestBody BankAccountRequest req, @AuthenticationPrincipal CurrentUser user) {
        return service.saveBankAccount(companyId, id, null, req, user.userId());
    }

    @PutMapping("/{id}/bank-accounts/{accountId}")
    @PreAuthorize("hasAuthority('person:bank')")
    public List<BankAccount> updateBankAccount(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id,
                                               @PathVariable UUID accountId, @RequestBody BankAccountRequest req,
                                               @AuthenticationPrincipal CurrentUser user) {
        return service.saveBankAccount(companyId, id, accountId, req, user.userId());
    }

    @DeleteMapping("/{id}/bank-accounts/{accountId}")
    @PreAuthorize("hasAuthority('person:bank')")
    public List<BankAccount> deleteBankAccount(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id,
                                               @PathVariable UUID accountId, @AuthenticationPrincipal CurrentUser user) {
        return service.deleteBankAccount(companyId, id, accountId, user.userId());
    }

    // ---------------------------------------------------------------- relacionamentos

    @PostMapping("/{id}/relationships")
    @PreAuthorize("hasAuthority('person:update')")
    public List<Relationship> addRelationship(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id,
                                              @RequestBody RelationshipRequest req, @AuthenticationPrincipal CurrentUser user) {
        return service.addRelationship(companyId, id, req, user.userId());
    }

    @DeleteMapping("/{id}/relationships/{relId}")
    @PreAuthorize("hasAuthority('person:update')")
    public List<Relationship> deleteRelationship(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id,
                                                 @PathVariable UUID relId, @AuthenticationPrincipal CurrentUser user) {
        return service.deleteRelationship(companyId, id, relId, user.userId());
    }

    // ---------------------------------------------------------------- papéis

    @PutMapping("/{id}/customer")
    @PreAuthorize("hasAuthority('person:update')")
    public PersonDetail saveCustomer(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id,
                                     @RequestBody(required = false) CustomerRequest req, @AuthenticationPrincipal CurrentUser user) {
        if (roles.rolesOf(companyId, id).contains("CUSTOMER")) roles.updateCustomer(companyId, id, req, user);
        else roles.enableCustomer(companyId, id, req, user, true);
        return service.get(companyId, id, user);
    }

    @DeleteMapping("/{id}/customer")
    @PreAuthorize("hasAuthority('person:update')")
    public PersonDetail disableCustomer(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id,
                                        @AuthenticationPrincipal CurrentUser user) {
        roles.disableCustomer(companyId, id, user);
        return service.get(companyId, id, user);
    }

    @PutMapping("/{id}/supplier")
    @PreAuthorize("hasAuthority('supplier:manage')")
    public PersonDetail saveSupplier(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id,
                                     @RequestBody(required = false) SupplierRequest req, @AuthenticationPrincipal CurrentUser user) {
        if (roles.rolesOf(companyId, id).contains("SUPPLIER")) roles.updateSupplier(companyId, id, req, user);
        else roles.enableSupplier(companyId, id, req, user, true);
        return service.get(companyId, id, user);
    }

    @DeleteMapping("/{id}/supplier")
    @PreAuthorize("hasAuthority('supplier:manage')")
    public PersonDetail disableSupplier(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id,
                                        @AuthenticationPrincipal CurrentUser user) {
        roles.disableSupplier(companyId, id, user);
        return service.get(companyId, id, user);
    }

    @PutMapping("/{id}/employee")
    @PreAuthorize("hasAuthority('employee:manage')")
    public PersonDetail saveEmployee(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id,
                                     @RequestBody(required = false) EmployeeRequest req, @AuthenticationPrincipal CurrentUser user) {
        if (roles.rolesOf(companyId, id).contains("EMPLOYEE")) roles.updateEmployee(companyId, id, req, user);
        else roles.enableEmployee(companyId, id, req, user, true);
        return service.get(companyId, id, user);
    }

    @DeleteMapping("/{id}/employee")
    @PreAuthorize("hasAuthority('employee:manage')")
    public PersonDetail disableEmployee(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id,
                                        @AuthenticationPrincipal CurrentUser user) {
        roles.disableEmployee(companyId, id, user);
        return service.get(companyId, id, user);
    }

    @PostMapping("/{id}/employee/specialties")
    @PreAuthorize("hasAuthority('employee:manage')")
    public List<Specialty> addSpecialty(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id,
                                        @RequestBody SpecialtyRequest req, @AuthenticationPrincipal CurrentUser user) {
        return roles.addSpecialty(companyId, id, req, user);
    }

    @PutMapping("/{id}/employee/specialties/{specialtyId}")
    @PreAuthorize("hasAuthority('employee:manage')")
    public List<Specialty> updateSpecialty(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id,
                                           @PathVariable UUID specialtyId, @RequestBody SpecialtyRequest req,
                                           @AuthenticationPrincipal CurrentUser user) {
        return roles.updateSpecialty(companyId, id, specialtyId, req, user);
    }

    @DeleteMapping("/{id}/employee/specialties/{specialtyId}")
    @PreAuthorize("hasAuthority('employee:manage')")
    public List<Specialty> deleteSpecialty(@CurrentCompanyId(OWN) UUID companyId, @PathVariable UUID id,
                                           @PathVariable UUID specialtyId, @AuthenticationPrincipal CurrentUser user) {
        return roles.deleteSpecialty(companyId, id, specialtyId, user);
    }
}
