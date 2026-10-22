package com.becommerce.crm.sales.tag.application.port.out;

import com.becommerce.crm.sales.tag.domain.Tag;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository {

    Tag save(Tag tag);

    Optional<Tag> findById(UUID id);

    List<Tag> findByCompanyId(UUID companyId);

    Optional<Tag> findByCompanyIdAndName(UUID companyId, String name);

    void delete(Tag tag);
}
