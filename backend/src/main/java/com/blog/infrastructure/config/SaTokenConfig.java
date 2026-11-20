package com.blog.infrastructure.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 鉴权与 CORS 配置。
 * <p>
 * 鉴权收敛点：只有 {@code /api/admin/**} 需要登录，其余公开。
 * 接口文档路径在生产环境已被 {@link com.blog.interfaces.rest.support.ApiDocGuardFilter}
 * 拦截，这里再叠加一层：即便文档开关被误开，未登录也无法查看。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> SaRouter
                        .match("/api/admin/**")
                        .notMatch("/api/auth/**")
                        .check(route -> StpUtil.checkLogin())))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/public/**",
                        "/api/auth/login",
                        "/error",
                        "/favicon.ico");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("blog-token")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
