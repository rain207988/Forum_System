package com.xzy.forum.dto;

import com.xzy.forum.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class AuthResponse {
    private User user;
    private String token;
    private String tokenType;
    private Instant expiresAt;
    private String refreshToken;
    private Instant refreshExpiresAt;
}
