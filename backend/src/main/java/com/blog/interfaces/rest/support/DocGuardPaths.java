package com.blog.interfaces.rest.support;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 接口文档相关路径清单与匹配规则。
 * <p>
 * 按「路径段」匹配而不是朴素的 startsWith，避免 /doc.htmlx 之类的误判，
 * 同时覆盖 Knife4j、springdoc、swagger2 三套文档入口。
 */
public final class DocGuardPaths {

    static final String LOG_NAME = "ApiDocGuard";

    public static final List<String> PROTECTED_PREFIXES = List.of(
            "/doc.html",
            "/v3/api-docs",
            "/v2/api-docs",
            "/swagger-ui",
            "/swagger-resources",
            "/webjars",
            "/knife4j"
    );

    private DocGuardPaths() {
    }

    public static boolean matches(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        for (String prefix : PROTECTED_PREFIXES) {
            if (uri.equals(prefix) || uri.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }
}
