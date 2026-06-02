package com.xzy.forum.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "forum.jwt")
public class JwtProperties {

    private String secret = "replace-this-with-env-secret-for-production";
    private long expireHours = 12;
    private String issuer = "forum-system";
    private String header = "Authorization";
    private String prefix = "Bearer ";
}
