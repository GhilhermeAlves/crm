package com.becommerce.auth.application.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginResponse(
    @JsonProperty("access_token")
    String accessToken,

    @JsonProperty("token_type")
    String tokenType,

    @JsonProperty("expires_in")
    Integer expiresIn
) {
    public static LoginResponse from(String accessToken, String tokenType, Integer expiresIn) {
        return new LoginResponse(accessToken, tokenType, expiresIn);
    }
}
