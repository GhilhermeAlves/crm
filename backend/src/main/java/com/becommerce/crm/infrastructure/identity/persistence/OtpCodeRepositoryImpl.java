package com.becommerce.crm.infrastructure.identity.persistence;

import com.becommerce.crm.domain.identity.OtpCode;
import com.becommerce.crm.domain.identity.repository.OtpCodeRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OtpCodeRepositoryImpl implements OtpCodeRepository {

    private final SpringDataOtpCodeRepository repository;
    private final OtpCodeMapper mapper;

    public OtpCodeRepositoryImpl(SpringDataOtpCodeRepository repository, OtpCodeMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public OtpCode save(OtpCode otpCode) {
        OtpCodeJpaEntity entity = mapper.toJpaEntity(otpCode);
        OtpCodeJpaEntity saved = repository.save(entity);
        return mapper.toDomainEntity(saved);
    }

    @Override
    public Optional<OtpCode> findLatestByPhone(String phoneE164) {
        return repository.findLatestByPhone(phoneE164, PageRequest.of(0, 1))
                .stream().findFirst().map(mapper::toDomainEntity);
    }

    @Override
    public Optional<OtpCode> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public void deleteExpired() {
        repository.deleteExpired(LocalDateTime.now());
    }
}