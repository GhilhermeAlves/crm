package com.becommerce.crm.application.identity.port.output;

import java.util.List;
import java.util.UUID;

public interface JwtProvider {
    String generateAccessToken(UUID userId, UUID companyId, List<String> roles, List<String> permissions);
    String generateRefreshToken(UUID userId, String family);
    boolean validateToken(String token);
    UUID extractUserId(String token);
    UUID extractCompanyId(String token);
    List<String> extractRoles(String token);
    List<String> extractPermissions(String token);
}
