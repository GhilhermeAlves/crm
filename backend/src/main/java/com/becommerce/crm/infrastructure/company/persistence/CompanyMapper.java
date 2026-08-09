package com.becommerce.crm.infrastructure.company.persistence;

import com.becommerce.crm.domain.company.Company;
import com.becommerce.crm.domain.company.CompanyPlan;
import com.becommerce.crm.domain.company.CompanyStatus;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public CompanyJpaEntity toJpaEntity(Company company) {
        CompanyJpaEntity entity = new CompanyJpaEntity();
        entity.setId(company.getId());
        entity.setLegalName(company.getLegalName());
        entity.setTradingName(company.getTradingName());
        entity.setCnpj(company.getCnpj());
        entity.setStateRegistration(company.getStateRegistration());
        entity.setMunicipalRegistration(company.getMunicipalRegistration());
        entity.setEmail(company.getEmail());
        entity.setPhone(company.getPhone());
        entity.setWebsite(company.getWebsite());
        entity.setAddressZipCode(company.getAddressZipCode());
        entity.setAddressStreet(company.getAddressStreet());
        entity.setAddressNumber(company.getAddressNumber());
        entity.setAddressComplement(company.getAddressComplement());
        entity.setAddressNeighborhood(company.getAddressNeighborhood());
        entity.setAddressCity(company.getAddressCity());
        entity.setAddressState(company.getAddressState());
        entity.setAddressCountry(company.getAddressCountry());
        entity.setPlan(company.getPlan().name());
        entity.setStatus(company.getStatus().name());
        entity.setMaxUsers(company.getMaxUsers());
        entity.setMaxStorageMb(company.getMaxStorageMb());
        entity.setMaxContacts(company.getMaxContacts());
        entity.setLogoUrl(company.getLogoUrl());
        entity.setNotes(company.getNotes());
        entity.setCreatedAt(company.getCreatedAt());
        entity.setUpdatedAt(company.getUpdatedAt());
        return entity;
    }

    public Company toDomainEntity(CompanyJpaEntity entity) {
        return Company.reconstitute(
                entity.getId(),
                entity.getLegalName(),
                entity.getTradingName(),
                entity.getCnpj(),
                entity.getStateRegistration(),
                entity.getMunicipalRegistration(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getWebsite(),
                entity.getAddressZipCode(),
                entity.getAddressStreet(),
                entity.getAddressNumber(),
                entity.getAddressComplement(),
                entity.getAddressNeighborhood(),
                entity.getAddressCity(),
                entity.getAddressState(),
                entity.getAddressCountry(),
                CompanyPlan.valueOf(entity.getPlan()),
                CompanyStatus.valueOf(entity.getStatus()),
                entity.getMaxUsers(),
                entity.getMaxStorageMb(),
                entity.getMaxContacts(),
                entity.getLogoUrl(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
