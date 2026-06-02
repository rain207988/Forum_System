package com.xzy.forum.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConditionalOnProperty(prefix = "forum.features", name = "swagger-enabled", havingValue = "true")
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        List<Server> servers = new ArrayList<>();
        Server server = new Server();
        server.setUrl("http://localhost:" + serverPort);
        server.setDescription("本地开发服务器");
        servers.add(server);

        return new OpenAPI()
                .info(new Info()
                        .title("论坛系统 API 文档")
                        .version("1.0.0")
                        .description("论坛系统 RESTful API 接口文档")
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(servers)
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .schemaRequirement("Bearer Authentication", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT Token 认证"));
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .pathsToMatch("/**")
                .build();
    }

    @Bean
    public GroupedOpenApi testApi() {
        return GroupedOpenApi.builder()
                .group("test")
                .pathsToMatch("/test/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("user")
                .pathsToMatch("/user/**", "/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi articleApi() {
        return GroupedOpenApi.builder()
                .group("article")
                .pathsToMatch("/article/**", "/reply/**", "/board/**", "/message/**")
                .build();
    }
}
