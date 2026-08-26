package com.becommerce.crm.application.identity.service;

import com.becommerce.crm.application.audit.service.TenantAuditRecorder;
import com.becommerce.crm.application.identity.dto.UserPermissionsResponse;
import com.becommerce.crm.application.identity.port.input.UserPermissionOverrideUseCase;
import com.becommerce.crm.application.identity.port.output.PermissionRepository;
import com.becommerce.crm.application.identity.port.output.UserPermissionOverrideRepository;
import com.becommerce.crm.application.identity.port.output.UserRepository;
import com.becommerce.crm.application.identity.port.output.UserRoleRepository;
import com.becommerce.crm.domain.audit.AuditAction;
import com.becommerce.crm.domain.audit.AuditModule;
import com.becommerce.crm.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Overrides individuais de permissão (Sprint 20 — Fase 2).
 * Política: efetiva = (perfis ∪ ALLOW) − DENY; INHERIT = ausência de linha.
 * A resolução efetiva é centralizada em
 * {@code PermissionRepository.findEffectivePermissionNamesByUserIdAndCompanyId}
 * (mesma regra no auth-service) — nunca duplicada nos controllers.
 */
@Service
public class UserPermissionOverrideService implements UserPermissionOverrideUseCase {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final com.becommerce.crm.application.identity.port.output.RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserPermissionOverrideRepository overrideRepository;
    private final TenantAuditRecorder auditor;

    public UserPermissionOverrideService(UserRepository userRepository,
                                         UserRoleRepository userRoleRepository,
                                         com.becommerce.crm.application.identity.port.output.RoleRepository roleRepository,
                                         PermissionRepository permissionRepository,
                                         UserPermissionOverrideRepository overrideRepository,
                                         TenantAuditRecorder auditor) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.overrideRepository = overrideRepository;
        this.auditor = auditor;
    }

    @Override
    @Transactional(readOnly = true)
    public UserPermissionsResponse listPermissions(UUID companyId, UUID userId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireSameCompany(companyId, userId);

            List<String> roles = userRoleRepository.findByUserIdAndCompanyId(userId, companyId)
                    .stream()
                    .map(userRole -> roleRepository.findById(userRole.getRoleId()))
                    .filter(java.util.Optional::isPresent)
                    .map(java.util.Optional::get)
                    .map(role -> role.getName())
                    .toList();
            List<String> effective =
                    permissionRepository.findEffectivePermissionNamesByUserIdAndCompanyId(
                            userId, companyId);
            List<UserPermissionsResponse.OverrideEntry> overrides =
                    overrideRepository.findByUser(userId, companyId).stream()
                            .map(entry -> new UserPermissionsResponse.OverrideEntry(
                                    entry.permissionName(), entry.effect()))
                            .toList();
            return new UserPermissionsResponse(userId, roles, effective, overrides);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void setOverride(UUID companyId, UUID userId, UUID permissionId, String effect) {
        String normalized = normalizeEffect(effect);
        try {
            TenantContext.setCompanyId(companyId);
            requireSameCompany(companyId, userId);
            permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new IllegalArgumentException("Permissão não encontrada."));

            overrideRepository.upsert(userId, companyId, permissionId, normalized);

            auditor.record(companyId, AuditAction.UPDATE, AuditModule.PERMISSIONS,
                    "UserPermission", userId.toString(),
                    "Override de permissão: effect=" + normalized,
                    null, java.util.Map.of("permissionId", permissionId.toString(),
                            "effect", normalized));
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional
    public void removeOverride(UUID companyId, UUID userId, UUID permissionId) {
        try {
            TenantContext.setCompanyId(companyId);
            requireSameCompany(companyId, userId);
            overrideRepository.deleteByUserIdAndPermissionId(userId, permissionId);

            auditor.record(companyId, AuditAction.DELETE, AuditModule.PERMISSIONS,
                    "UserPermission", userId.toString(),
                    "Override de permissão removido (INHERIT)",
                    null, java.util.Map.of("permissionId", permissionId.toString()));
        } finally {
            TenantContext.clear();
        }
    }

    private void requireSameCompany(UUID companyId, UUID userId) {
        userRepository.findById(userId)
                .filter(user -> companyId.equals(user.getCompanyId()))
                .orElseThrow(() -> new CrmAccessDeniedException(
                        "Usuário não pertence à sua empresa."));
    }

    private static String normalizeEffect(String effect) {
        if ("ALLOW".equalsIgnoreCase(effect) || "DENY".equalsIgnoreCase(effect)) {
            return effect.toUpperCase();
        }
        throw new IllegalArgumentException("effect deve ser ALLOW ou DENY.");
    }
}
