package com.becommerce.crm.communication.telegram.application;

import com.becommerce.crm.communication.telegram.domain.TelegramLink;
import com.becommerce.crm.communication.telegram.infrastructure.TelegramLinkJpaRepository;
import com.becommerce.crm.identity.application.port.output.PermissionRepository;
import com.becommerce.crm.identity.application.port.output.RoleRepository;
import com.becommerce.crm.identity.application.port.output.UserRoleRepository;
import com.becommerce.crm.identity.membership.application.port.output.MembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramUserResolverTest {

    static final UUID CO = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();

    @Mock TelegramLinkJpaRepository links;
    @Mock PermissionRepository permissions;
    @Mock MembershipRepository memberships;
    @Mock UserRoleRepository userRoles;
    @Mock RoleRepository roles;
    TelegramUserResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TelegramUserResolver(links, permissions, memberships, userRoles, roles);
    }

    @Test
    void unlinkedReturnsEmpty() {
        when(links.findByCompanyIdAndTelegramUserId(CO, 5L)).thenReturn(Optional.empty());
        assertTrue(resolver.resolve(CO, 5L).isEmpty());
    }

    @Test
    void inactiveMemberReturnsEmpty() {
        when(links.findByCompanyIdAndTelegramUserId(CO, 5L)).thenReturn(Optional.of(link()));
        when(memberships.existsActiveByUserIdAndCompanyId(USER, CO)).thenReturn(false);
        assertTrue(resolver.resolve(CO, 5L).isEmpty());
    }

    @Test
    void activeMemberGetsEffectivePermissions() {
        when(links.findByCompanyIdAndTelegramUserId(CO, 5L)).thenReturn(Optional.of(link()));
        when(memberships.existsActiveByUserIdAndCompanyId(USER, CO)).thenReturn(true);
        when(permissions.findEffectivePermissionNamesByUserIdAndCompanyId(USER, CO)).thenReturn(List.of("ai:chat"));
        when(userRoles.findByUserIdAndCompanyId(USER, CO)).thenReturn(List.of());
        var u = resolver.resolve(CO, 5L).orElseThrow();
        assertEquals(USER, u.userId());
        assertEquals(List.of("ai:chat"), u.permissions());
    }

    private TelegramLink link() {
        TelegramLink l = new TelegramLink();
        l.setCompanyId(CO);
        l.setUserId(USER);
        l.setTelegramUserId(5L);
        l.setChatId(5L);
        return l;
    }
}
