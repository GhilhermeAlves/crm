package com.becommerce.crm.application.company.service;

import com.becommerce.crm.application.company.dto.*;
import com.becommerce.crm.application.company.port.input.CompanyUseCase;
import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.application.identity.port.output.EventPublisher;
import com.becommerce.crm.domain.company.*;
import com.becommerce.crm.domain.company.event.CompanyCreatedEvent;
import com.becommerce.crm.domain.company.event.CompanyDeletedEvent;
import com.becommerce.crm.domain.company.event.CompanyUpdatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CompanyService implements CompanyUseCase {

    private final CompanyRepository companyRepository;
    private final EventPublisher eventPublisher;

    public CompanyService(CompanyRepository companyRepository, EventPublisher eventPublisher) {
        this.companyRepository = companyRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(UUID id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new CompanyNotFoundException(id));
        return mapToResponse(company);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanySummaryResponse> listCompanies() {
        return companyRepository.findAll()
                .stream()
                .map(this::mapToSummary)
                .toList();
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
        int maxUsers = request.maxUsers() != null ? request.maxUsers() : 5;
        int maxStorageMb = request.maxStorageMb() != null ? request.maxStorageMb() : 1024;
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
                request.logoUrl(),
                request.notes()
        );

        Company saved = companyRepository.save(company);
        eventPublisher.publish(CompanyCreatedEvent.create(saved));
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CompanyResponse updateCompany(UUID id, UpdateCompanyRequest request) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new CompanyNotFoundException(id));

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
        String logoUrl = request.logoUrl() != null ? request.logoUrl() : company.getLogoUrl();
        String notes = request.notes() != null ? request.notes() : company.getNotes();

        company.update(
                legalName, tradingName, email, phone, website,
                addressZipCode, addressStreet, addressNumber,
                addressComplement, addressNeighborhood,
                addressCity, addressState, addressCountry,
                plan, status, maxUsers, maxStorageMb,
                logoUrl, notes
        );

        Company saved = companyRepository.save(company);
        eventPublisher.publish(CompanyUpdatedEvent.create(saved));
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCompany(UUID id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new CompanyNotFoundException(id));
        companyRepository.deleteById(id);
        eventPublisher.publish(CompanyDeletedEvent.create(id, company.getLegalName(), company.getCnpj()));
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
}
