package com.blog.infrastructure.config;

import com.blog.interfaces.rest.support.ApiDocGuardFilter;
import com.blog.interfaces.rest.support.SpaEntryNoCacheFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 接口文档出入口开关。
 * <p>
 * 一个布尔属性 {@code blog.api-doc.enabled} 同时控制三处：
 * 1. springdoc 是否注册 /v3/api-docs 与 swagger-ui；
 * 2. knife4j 是否注册 /doc.html；
 * 3. {@link ApiDocGuardFilter} 是否放行文档路径。
 * <p>
 * 默认 false（生产安全基线），开发环境由 {@code application-dev.yml} 打开。
 */
@Configuration
public class ApiDocConfig {

    /**
     * 注册文档路径守卫，顺序尽量靠前：鉴权拦截器之前就完成拦截，
     * 避免未登录用户从文档入口探测到后端结构。
     */
    @Bean
    public FilterRegistrationBean<ApiDocGuardFilter> apiDocGuardFilter(
            @Value("${blog.api-doc.enabled:false}") boolean apiDocEnabled) {
        FilterRegistrationBean<ApiDocGuardFilter> registration =
                new FilterRegistrationBean<>(new ApiDocGuardFilter(apiDocEnabled));
        registration.addUrlPatterns("/*");
        registration.setName("apiDocGuardFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }

    /**
     * 前端入口页不缓存：避免发版后浏览器继续使用旧 index.html 引用已删除的 chunk。
     * 放在静态资源处理之后执行（顺序靠后），此时 Content-Type 已确定。
     */
    @Bean
    public FilterRegistrationBean<SpaEntryNoCacheFilter> spaEntryNoCacheFilter() {
        FilterRegistrationBean<SpaEntryNoCacheFilter> registration =
                new FilterRegistrationBean<>(new SpaEntryNoCacheFilter());
        registration.addUrlPatterns("/*");
        registration.setName("spaEntryNoCacheFilter");
        registration.setOrder(Ordered.LOWEST_PRECEDENCE);
        return registration;
    }
}
