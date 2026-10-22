package com.becommerce.crm.sales.tag.infrastructure.persistence;

import com.becommerce.crm.sales.tag.application.port.out.TagRepository;
import com.becommerce.crm.sales.tag.domain.Tag;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TagRepositoryImpl implements TagRepository {

    private final TagJpaRepository jpa;

    public TagRepositoryImpl(TagJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Tag save(Tag tag) {
        return toDomain(jpa.save(toEntity(tag)));
    }

    @Override
    public Optional<Tag> findById(UUID id) {
        return jpa.findById(id).map(TagRepositoryImpl::toDomain);
    }

    @Override
    public List<Tag> findByCompanyId(UUID companyId) {
        return jpa.findByCompanyIdOrderByNameAsc(companyId).stream().map(TagRepositoryImpl::toDomain).toList();
    }

    @Override
    public Optional<Tag> findByCompanyIdAndName(UUID companyId, String name) {
        return jpa.findByCompanyIdAndName(companyId, name).map(TagRepositoryImpl::toDomain);
    }

    @Override
    public void delete(Tag tag) {
        jpa.deleteById(tag.getId());
    }

    private static TagJpaEntity toEntity(Tag tag) {
        TagJpaEntity entity = new TagJpaEntity();
        entity.setId(tag.getId());
        entity.setCompanyId(tag.getCompanyId());
        entity.setName(tag.getName());
        entity.setColor(tag.getColor());
        entity.setCreatedAt(tag.getCreatedAt());
        entity.setUpdatedAt(tag.getUpdatedAt());
        return entity;
    }

    private static Tag toDomain(TagJpaEntity entity) {
        return Tag.reconstitute(entity.getId(), entity.getCompanyId(), entity.getName(), entity.getColor(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
