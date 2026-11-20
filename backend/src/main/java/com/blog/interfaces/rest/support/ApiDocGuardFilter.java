package com.blog.interfaces.rest.support;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 接口文档访问守卫。
 * <p>
 * 关闭状态下拦截 Knife4j（/doc.html、/webjars/**）、springdoc（/v3/api-docs、/swagger-ui）
 * 以及历史遗留的 swagger2 路径。
 * <p>
 * 统一返回 404 而非 403，避免暴露"此处存在受保护资源"这一事实。
 * 本过滤器只做路径拦截；文档端点是否注册由 springdoc / knife4j 自身的开关控制，
 * 两层同时关闭才算真正收口。
 */
public class ApiDocGuardFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DocGuardPaths.LOG_NAME);

    private final boolean enabled;

    public ApiDocGuardFilter(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!enabled && DocGuardPaths.matches(request)) {
            log.debug("接口文档入口已关闭，拒绝访问: {} {}", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":404,\"message\":\"Not Found\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
