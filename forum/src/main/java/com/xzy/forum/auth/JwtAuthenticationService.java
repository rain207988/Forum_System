package com.xzy.forum.auth;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import com.xzy.forum.exception.ApplicationException;
import com.xzy.forum.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtAuthenticationService {

    private static final String CLAIM_TOKEN_VERSION = "tokenVersion";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtAuthenticationService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.getExpireHours(), ChronoUnit.HOURS);
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim(CLAIM_TOKEN_VERSION, buildTokenVersion(user))
                .signWith(secretKey)
                .compact();
    }

    public Long parseUserId(String token) {
        Claims claims = parseClaims(token);
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            throw unauthorized("无效的登录凭证");
        }
    }

    public void validateToken(String token, User user) {
        Claims claims = parseClaims(token);
        String tokenVersion = claims.get(CLAIM_TOKEN_VERSION, String.class);
        if (!buildTokenVersion(user).equals(tokenVersion)) {
            throw unauthorized("登录状态已失效，请重新登录");
        }
    }

    public Instant getExpiresAt(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    public String resolveToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        String prefix = jwtProperties.getPrefix();
        if (!authorizationHeader.startsWith(prefix)) {
            return null;
        }
        return authorizationHeader.substring(prefix.length()).trim();
    }

    public String getAuthorizationHeaderName() {
        return jwtProperties.getHeader();
    }

    public String getTokenPrefix() {
        return jwtProperties.getPrefix();
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw unauthorized("登录状态已失效，请重新登录");
        }
    }

    private String buildTokenVersion(User user) {
        return DigestUtils.sha256Hex(jwtProperties.getSecret() + ":" + user.getId() + ":" + user.getPassword());
    }

    private ApplicationException unauthorized(String message) {
        return new ApplicationException(AppResult.failed(ResultCode.FAILED_UNAUTHORIZED.getCode(), message));
    }
}
