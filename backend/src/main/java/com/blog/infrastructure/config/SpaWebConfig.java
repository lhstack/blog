package com.blog.infrastructure.config;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * 单体部署时的前端托管配置。
 * <p>
 * 前端产物位于 {@code classpath:/static/}，由后端同一端口提供。由于 vue-router 使用
 * history 模式，直接访问或刷新 {@code /articles/1}、{@code /admin/dashboard} 时请求会先到后端，
 * 因此对「浏览器页面路由」回落到 {@code index.html}，交给前端路由处理。
 * <p>
 * 回落只针对前端路由，不做 {@code /**} 无差别兜底：
 * 带扩展名的路径视为静态资源，取不到就返回 404；
 * 归属后端的路径（{@code /api}、接口文档等）永不回落，避免把不存在的接口伪装成首页。
 * <p>
 * 缓存头由 {@link SpaEntryNoCacheFilter} 单独处理：入口页不缓存，其余保持默认。
 * 不在此处按路径拆分多个处理器 —— 多个 {@code ResourceHandlerRegistry} 项会互相遮蔽，
 * 导致 {@code /assets/**} 整体 404。
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    private static final String INDEX_PAGE = "index.html";

    private static final List<String> BACKEND_PREFIXES = List.of(
            "api",
            "media",
            "error",
            "actuator",
            "doc.html",
            "v3/api-docs",
            "v2/api-docs",
            "swagger-ui",
            "swagger-resources",
            "webjars",
            "knife4j"
    );

    private final WebProperties webProperties;

    public SpaWebConfig(WebProperties webProperties) {
        this.webProperties = webProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations(webProperties.getResources().getStaticLocations())
                .resourceChain(true)
                .addResolver(new SpaHistoryFallbackResolver());
    }

    private static final class SpaHistoryFallbackResolver extends PathResourceResolver {

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requested = super.getResource(resourcePath, location);
            if (requested != null) {
                return requested;
            }
            if (!isSpaRoute(resourcePath)) {
                return null;
            }
            return super.getResource(INDEX_PAGE, location);
        }

        private boolean isSpaRoute(String resourcePath) {
            if (resourcePath == null || resourcePath.isEmpty()) {
                return true;
            }
            String path = resourcePath.toLowerCase(Locale.ROOT);
            if (path.contains(".")) {
                return false;
            }
            for (String prefix : BACKEND_PREFIXES) {
                if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                    return false;
                }
            }
            return true;
        }
    }
}