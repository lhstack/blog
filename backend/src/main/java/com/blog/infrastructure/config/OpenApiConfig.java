package com.blog.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 元信息。仅当 {@code blog.api-doc.enabled=true} 时注册，
 * 生产环境不会产生任何文档相关 Bean。
 */
@Configuration
@ConditionalOnProperty(name = "blog.api-doc.enabled", havingValue = "true")
public class OpenApiConfig {

    @Bean
    public OpenAPI blogOpenApi(@Value("${blog.site.name}") String siteName,
                               @Value("${blog.site.description}") String description) {
        return new OpenAPI()
                .info(new Info()
                        .title(siteName + " · 个人博客接口文档")
                        .description(description)
                        .version("1.0.0")
                        .contact(new Contact().name("站长").email("hello@yunjian.dev"))
                        .license(new License().name("Apache 2.0")))
                .addSecurityItem(new SecurityRequirement().addList("blog-token"))
                .schemaRequirement("blog-token", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("blog-token")
                        .description("Sa-Token 登录凭证，登录接口返回"));
    }
}
