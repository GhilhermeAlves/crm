package com.becommerce.crm.communication.telegram.application;

import com.becommerce.crm.communication.telegram.infrastructure.TelegramLinkJpaRepository;
import com.becommerce.crm.identity.application.port.output.PermissionRepository;
import com.becommerce.crm.identity.application.port.output.RoleRepository;
import com.becommerce.crm.identity.application.port.output.UserRoleRepository;
import com.becommerce.crm.identity.membership.application.port.output.MembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Resolve o usuário do CRM a partir do usuário do Telegram (mesma regra do LocalCurrentUserResolver). */
@Service
public class TelegramUserResolver {

    private final TelegramLinkJpaRepository links;
    private final PermissionRepository permissions;
    private final MembershipRepository memberships;
    private final UserRoleRepository userRoles;
    private final RoleRepository roles;

    public TelegramUserResolver(TelegramLinkJpaRepository links, PermissionRepository permissions,
                                MembershipRepository memberships, UserRoleRepository userRoles,
                                RoleRepository roles) {
        this.links = links;
        this.permissions = permissions;
        this.memberships = memberships;
        this.userRoles = userRoles;
        this.roles = roles;
    }

    public record LinkedUser(UUID userId, long chatId, UUID conversationId,
                             List<String> permissions, List<String> roles) {}

    @Transactional(readOnly = true)
    public Optional<LinkedUser> resolve(UUID companyId, long telegramUserId) {
        return TelegramConfigService.withTenant(companyId,
                () -> links.findByCompanyIdAndTelegramUserId(companyId, telegramUserId)
                        .filter(l -> memberships.existsActiveByUserIdAndCompanyId(l.getUserId(), companyId))
                        .map(l -> new LinkedUser(l.getUserId(), l.getChatId(), l.getAiConversationId(),
                                permissions.findEffectivePermissionNamesByUserIdAndCompanyId(l.getUserId(), companyId),
                                userRoles.findByUserIdAndCompanyId(l.getUserId(), companyId).stream()
                                        .map(ur -> roles.findById(ur.getRoleId()))
                                        .flatMap(Optional::stream)
                                        .map(r -> r.getName())
                                        .toList())));
    }
}
