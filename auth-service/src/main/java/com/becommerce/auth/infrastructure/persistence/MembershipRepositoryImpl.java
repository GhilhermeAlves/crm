package com.becommerce.auth.infrastructure.persistence;

import com.becommerce.auth.application.identity.port.output.MembershipRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class MembershipRepositoryImpl implements MembershipRepository {

    private final SpringDataMembershipRepository springData;

    public MembershipRepositoryImpl(SpringDataMembershipRepository springData) {
        this.springData = springData;
    }

    @Override
    public Optional<String> findMembershipRoleByUserIdAndCompanyId(UUID userId, UUID companyId) {
        return springData.findMembershipRoleByUserIdAndCompanyId(userId, companyId);
    }

    @Override
    public boolean existsActiveByUserIdAndCompanyId(UUID userId, UUID companyId) {
        return springData.existsActiveByUserIdAndCompanyId(userId, companyId);
    }
}
