package com.drrcp.victimregistration.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String token;
    private String tokenType;
    private long expiresInSeconds;
    private String username;
    private String role;
}