package com.xzy.forum.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "forum.rate-limit")
public class ForumRateLimitProperties {

    private boolean enabled = true;
    private LimitRule login = new LimitRule(10, 60);
    private LimitRule register = new LimitRule(5, 3600);
    private LimitRule messageSend = new LimitRule(20, 60);
    private LimitRule messageReply = new LimitRule(30, 60);

    @Getter
    @Setter
    public static class LimitRule {
        private int maxRequests;
        private int windowSeconds;

        public LimitRule() {
        }

        public LimitRule(int maxRequests, int windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
        }
    }
}
