package com.becommerce.crm.application.company.service;

import com.becommerce.crm.application.company.dto.*;
import com.becommerce.crm.application.company.port.input.CompanyUseCase;
import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.application.company.port.output.CompanySettingsRepository;
import com.becommerce.crm.application.identity.port.output.EventPublisher;
import com.becommerce.crm.domain.company.*;
import com.becommerce.crm.domain.company.event.CompanyCreatedEvent;
import com.becommerce.crm.domain.company.event.CompanyDeletedEvent;
import com.becommerce.crm.domain.company.event.CompanyUpdatedEvent;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CompanyService implements CompanyUseCase {

    public static final int DEFAULT_MAX_USERS = 5;
    public static final int DEFAULT_MAX_STORAGE_MB = 1024;
    public static final int DEFAULT_MAX_CONTACTS = 500;

    private final CompanyRepository companyRepository;
    private final CompanySettingsRepository companySettingsRepository;
    private final EventPublisher eventPublisher;

    public CompanyService(CompanyRepository companyRepository,
                          CompanySettingsRepository companySettingsRepository,
                          EventPublisher eventPublisher) {
        this.companyRepository = companyRepository;
        this.companySettingsRepository = companySettingsRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(UUID id, UUID requesterCompanyId, boolean isSuperAdmin) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new CompanyNotFoundException(id));
        assertCompanyAccess(id, requesterCompanyId, isSuperAdmin);
        return mapToResponse(company);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanySummaryResponse> listCompanies(UUID requesterCompanyId, boolean isSuperAdmin) {
        if (isSuperAdmin) {
            return companyRepository.findAll()
                    .stream()
                    .map(this::mapToSummary)
                    .toList();
        }
        return companyRepository.findById(requesterCompanyId)
                .map(company -> List.of(mapToSummary(company)))
                .orElseGet(List::of);
    }

    @Override
    @Transactional
    public CompanyResponse createCompany(CreateCompanyRequest request) {
        if (companyRepository.existsByCnpj(request.cnpj())) {
            throw new CompanyAlreadyExistsException("CNPJ", request.cnpj());
        }
        if (companyRepository.existsByEmail(request.email())) {
            throw new CompanyAlreadyExistsException("email", request.email());
        }

        CompanyPlan plan = CompanyPlan.valueOf(request.plan().toUpperCase());
        int maxUsers = request.maxUsers() != null ? request.maxUsers() : DEFAULT_MAX_USERS;
        int maxStorageMb = request.maxStorageMb() != null ? request.maxStorageMb() : DEFAULT_MAX_STORAGE_MB;
        int maxContacts = request.maxContacts() != null ? request.maxContacts() : DEFAULT_MAX_CONTACTS;
        String addressCountry = request.addressCountry() != null ? request.addressCountry() : "Brasil";

        Company company = Company.create(
                request.legalName(),
                request.tradingName(),
                request.cnpj(),
                request.stateRegistration(),
                request.municipalRegistration(),
                request.email(),
                request.phone(),
                request.website(),
                request.addressZipCode(),
                request.addressStreet(),
                request.addressNumber(),
                request.addressComplement(),
                request.addressNeighborhood(),
                request.addressCity(),
                request.addressState(),
                addressCountry,
                plan,
                maxUsers,
                maxStorageMb,
                maxContacts,
                request.logoUrl(),
                request.notes()
        );

        Company saved = companyRepository.save(company);
        eventPublisher.publish(CompanyCreatedEvent.create(saved));
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CompanyResponse updateCompany(UUID id, UpdateCompanyRequest request,
                                         UUID requesterCompanyId, boolean isSuperAdmin) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new CompanyNotFoundException(id));
        assertCompanyAccess(id, requesterCompanyId, isSuperAdmin);

        String legalName = request.legalName() != null ? request.legalName() : company.getLegalName();
        String tradingName = request.tradingName() != null ? request.tradingName() : company.getTradingName();
        String email = request.email() != null ? request.email() : company.getEmail();
        String phone = request.phone() != null ? request.phone() : company.getPhone();
        String website = request.website() != null ? request.website() : company.getWebsite();
        String addressZipCode = request.addressZipCode() != null ? request.addressZipCode() : company.getAddressZipCode();
        String addressStreet = request.addressStreet() != null ? request.addressStreet() : company.getAddressStreet();
        String addressNumber = request.addressNumber() != null ? request.addressNumber() : company.getAddressNumber();
        String addressComplement = request.addressComplement() != null ? request.addressComplement() : company.getAddressComplement();
        String addressNeighborhood = request.addressNeighborhood() != null ? request.addressNeighborhood() : company.getAddressNeighborhood();
        String addressCity = request.addressCity() != null ? request.addressCity() : company.getAddressCity();
        String addressState = request.addressState() != null ? request.addressState() : company.getAddressState();
        String addressCountry = request.addressCountry() != null ? request.addressCountry() : company.getAddressCountry();
        CompanyPlan plan = request.plan() != null ? CompanyPlan.valueOf(request.plan().toUpperCase()) : company.getPlan();
        CompanyStatus status = request.status() != null ? CompanyStatus.valueOf(request.status().toUpperCase()) : company.getStatus();
        int maxUsers = request.maxUsers() != null ? request.maxUsers() : company.getMaxUsers();
        int maxStorageMb = request.maxStorageMb() != null ? request.maxStorageMb() : company.getMaxStorageMb();
        int maxContacts = request.maxContacts() != null ? request.maxContacts() : company.getMaxContacts();
        String logoUrl = request.logoUrl() != null ? request.logoUrl() : company.getLogoUrl();
        String notes = request.notes() != null ? request.notes() : company.getNotes();

        company.update(
                legalName, tradingName, email, phone, website,
                addressZipCode, addressStreet, addressNumber,
                addressComplement, addressNeighborhood,
                addressCity, addressState, addressCountry,
                plan, status, maxUsers, maxStorageMb, maxContacts,
                logoUrl, notes
        );

        Company saved = companyRepository.save(company);
        eventPublisher.publish(CompanyUpdatedEvent.create(saved));
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCompany(UUID id, UUID requesterCompanyId, boolean isSuperAdmin) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new CompanyNotFoundException(id));
        // Salvaguarda: nunca permitir excluir a empresa em que o usuário está
        // logado (evita órfãos/bloqueio de acesso como os usuários de memberships
        // perdidos por CASCADE). Edição continua permitida via updateCompany.
        if (requesterCompanyId != null && requesterCompanyId.equals(id)) {
            throw new CompanyDeletionForbiddenException(id);
        }
        assertCompanyAccess(id, requesterCompanyId, isSuperAdmin);
        companyRepository.deleteById(id);
        eventPublisher.publish(CompanyDeletedEvent.create(id, company.getLegalName(), company.getCnpj()));
    }

    @Override
    @Transactional(readOnly = true)
    public CompanySettingsResponse getCompanySettings(UUID companyId, UUID requesterCompanyId) {
        assertOwnCompanySettings(companyId, requesterCompanyId);
        return companySettingsRepository.findByCompanyId(companyId)
                .map(this::mapToSettingsResponse)
                .orElseGet(() -> new CompanySettingsResponse(
                        companyId.toString(),
                        CompanySettings.DEFAULT_TIMEZONE,
                        CompanySettings.DEFAULT_LOCALE,
                        CompanySettings.DEFAULT_CURRENCY,
                        null,
                        null,
                        null));
    }

    @Override
    @Transactional
    public CompanySettingsResponse updateCompanySettings(UUID companyId, UpdateCompanySettingsRequest request,
                                                         UUID requesterCompanyId) {
        assertOwnCompanySettings(companyId, requesterCompanyId);
        CompanySettings settings = companySettingsRepository.findByCompanyId(companyId)
                .orElseGet(() -> CompanySettings.create(
                        companyId,
                        CompanySettings.DEFAULT_TIMEZONE,
                        CompanySettings.DEFAULT_LOCALE,
                        CompanySettings.DEFAULT_CURRENCY,
                        null,
                        null));

        String timezone = request.timezone() != null ? request.timezone() : settings.getTimezone();
        String locale = request.locale() != null ? request.locale() : settings.getLocale();
        String currency = request.currency() != null ? request.currency() : settings.getCurrency();
        String businessHours = request.businessHours() != null ? request.businessHours() : settings.getBusinessHours();
        String notificationPreferences = request.notificationPreferences() != null
                ? request.notificationPreferences()
                : settings.getNotificationPreferences();

        settings.update(timezone, locale, currency, businessHours, notificationPreferences);

        CompanySettings saved = companySettingsRepository.save(settings);
        return mapToSettingsResponse(saved);
    }

    private void assertCompanyAccess(UUID companyId, UUID requesterCompanyId, boolean isSuperAdmin) {
        if (!isSuperAdmin && !companyId.equals(requesterCompanyId)) {
            throw new CrmAccessDeniedException("Acesso a esta empresa não permitido.");
        }
    }

    private void assertOwnCompanySettings(UUID companyId, UUID requesterCompanyId) {
        if (!companyId.equals(requesterCompanyId)) {
            throw new CrmAccessDeniedException("Settings disponíveis apenas para a própria empresa.");
        }
    }

    private CompanyResponse mapToResponse(Company company) {
        CompanyResponse.AddressResponse address = new CompanyResponse.AddressResponse(
                company.getAddressZipCode(),
                company.getAddressStreet(),
                company.getAddressNumber(),
                company.getAddressComplement(),
                company.getAddressNeighborhood(),
                company.getAddressCity(),
                company.getAddressState(),
                company.getAddressCountry()
        );

        return new CompanyResponse(
                company.getId().toString(),
                company.getLegalName(),
                company.getTradingName(),
                company.getCnpj(),
                company.getStateRegistration(),
                company.getMunicipalRegistration(),
                company.getEmail(),
                company.getPhone(),
                company.getWebsite(),
                address,
                company.getStatus().name().toLowerCase(),
                company.getPlan().name().toLowerCase(),
                company.getMaxUsers(),
                company.getMaxStorageMb(),
                company.getMaxContacts(),
                company.getLogoUrl(),
                company.getNotes(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }

    private CompanySummaryResponse mapToSummary(Company company) {
        return new CompanySummaryResponse(
                company.getId().toString(),
                company.getLegalName(),
                company.getTradingName(),
                company.getCnpj(),
                company.getEmail(),
                company.getPhone(),
                company.getStatus().name().toLowerCase(),
                company.getPlan().name().toLowerCase()
        );
    }

    private CompanySettingsResponse mapToSettingsResponse(CompanySettings settings) {
        return new CompanySettingsResponse(
                settings.getCompanyId().toString(),
                settings.getTimezone(),
                settings.getLocale(),
                settings.getCurrency(),
                settings.getBusinessHours(),
                settings.getNotificationPreferences(),
                settings.getUpdatedAt()
        );
    }
}
