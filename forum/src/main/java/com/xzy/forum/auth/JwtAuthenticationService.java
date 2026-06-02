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
import java.util.Objects;

import static com.xzy.forum.utils.UUIDUtils.UUID_32;

@Service
public class JwtAuthenticationService {

    private static final String CLAIM_TOKEN_VERSION = "tokenVersion";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String CLAIM_REFRESH_SESSION_ID = "refreshSessionId";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtAuthenticationService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.getExpireHours(), ChronoUnit.HOURS);
        return buildJwt(user, expiresAt, TOKEN_TYPE_ACCESS, null);
    }

    public RefreshTokenPayload generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.getRefreshExpireDays(), ChronoUnit.DAYS);
        String sessionId = UUID_32();
        String token = buildJwt(user, expiresAt, TOKEN_TYPE_REFRESH, sessionId);
        return new RefreshTokenPayload(token, sessionId, expiresAt);
    }

    public RefreshTokenPayload rotateRefreshToken(User user, String currentRefreshToken) {
        validateRefreshToken(currentRefreshToken, user);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.getRefreshExpireDays(), ChronoUnit.DAYS);
        String sessionId = getRefreshSessionId(currentRefreshToken);
        String token = buildJwt(user, expiresAt, TOKEN_TYPE_REFRESH, sessionId);
        return new RefreshTokenPayload(token, sessionId, expiresAt);
    }

    public void validateRefreshToken(String token, User user) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, TOKEN_TYPE_REFRESH);
        String tokenVersion = claims.get(CLAIM_TOKEN_VERSION, String.class);
        if (!buildTokenVersion(user).equals(tokenVersion)) {
            throw unauthorized("登录状态已失效，请重新登录");
        }
        if (getRefreshSessionId(claims) == null) {
            throw unauthorized("刷新凭证无效，请重新登录");
        }
    }

    public String getRefreshSessionId(String refreshToken) {
        return getRefreshSessionId(parseClaims(refreshToken));
    }

    public String getRefreshSessionIdOrNull(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        try {
            return getRefreshSessionId(refreshToken);
        } catch (ApplicationException e) {
            return null;
        }
    }

    private String buildJwt(User user, Instant expiresAt, String tokenType, String refreshSessionId) {
        var builder = Jwts.builder()
                .id(UUID_32())
                .subject(String.valueOf(user.getId()))
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiresAt))
                .claim(CLAIM_TOKEN_VERSION, buildTokenVersion(user))
                .claim(CLAIM_TOKEN_TYPE, tokenType);
        if (refreshSessionId != null) {
            builder.claim(CLAIM_REFRESH_SESSION_ID, refreshSessionId);
        }
        return builder.signWith(secretKey).compact();
    }

    public Long parseUserId(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, TOKEN_TYPE_ACCESS);
        return parseSubjectAsUserId(claims);
    }

    public Long parseRefreshUserId(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, TOKEN_TYPE_REFRESH);
        return parseSubjectAsUserId(claims);
    }

    private Long parseSubjectAsUserId(Claims claims) {
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            throw unauthorized("无效的登录凭证");
        }
    }

    public void validateToken(String token, User user) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, TOKEN_TYPE_ACCESS);
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

    public AuthTokenPair issueTokenPair(User user) {
        String accessToken = generateToken(user);
        RefreshTokenPayload refreshToken = generateRefreshToken(user);
        return new AuthTokenPair(accessToken, getExpiresAt(accessToken), refreshToken.token(), refreshToken.sessionId(), refreshToken.expiresAt());
    }

    public AuthTokenPair refreshTokenPair(User user, String currentRefreshToken) {
        String accessToken = generateToken(user);
        RefreshTokenPayload refreshToken = rotateRefreshToken(user, currentRefreshToken);
        return new AuthTokenPair(accessToken, getExpiresAt(accessToken), refreshToken.token(), refreshToken.sessionId(), refreshToken.expiresAt());
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

    private void validateTokenType(Claims claims, String expectedType) {
        String actualType = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!Objects.equals(expectedType, actualType)) {
            throw unauthorized("登录状态已失效，请重新登录");
        }
    }

    private String getRefreshSessionId(Claims claims) {
        String sessionId = claims.get(CLAIM_REFRESH_SESSION_ID, String.class);
        return sessionId == null || sessionId.isBlank() ? null : sessionId;
    }

    private String buildTokenVersion(User user) {
        return DigestUtils.sha256Hex(jwtProperties.getSecret() + ":" + user.getId() + ":" + user.getPassword());
    }

    private ApplicationException unauthorized(String message) {
        return new ApplicationException(AppResult.failed(ResultCode.FAILED_UNAUTHORIZED.getCode(), message));
    }

    public record RefreshTokenPayload(String token, String sessionId, Instant expiresAt) {
    }

    public record AuthTokenPair(String accessToken, Instant accessExpiresAt, String refreshToken, String refreshSessionId, Instant refreshExpiresAt) {
    }
}
