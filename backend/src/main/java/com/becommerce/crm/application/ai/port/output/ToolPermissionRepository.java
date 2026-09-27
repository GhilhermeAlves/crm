package com.becommerce.crm.application.ai.port.output;

import com.becommerce.crm.domain.ai.ToolPermission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Permissão por papel para cada ferramenta (tabela {@code tool_permission}, V077).
 * O tenant é herdado da ferramenta via RLS.
 */
public interface ToolPermissionRepository {

    List<ToolPermission> findByToolId(UUID toolId);

    Optional<ToolPermission> findByToolAndRole(UUID toolId, String role);

    ToolPermission save(ToolPermission permission);

    void delete(UUID id);
}
