package com.xzy.forum.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "forum.features")
public class ForumFeatureProperties {

    private boolean swaggerEnabled = true;
    private boolean testApiEnabled = true;
}
