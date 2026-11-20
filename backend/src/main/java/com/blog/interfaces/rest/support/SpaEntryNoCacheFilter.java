package com.blog.interfaces.rest.support;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 前端入口页缓存策略。
 * <p>
 * {@code index.html} 是唯一不带内容 hash 的前端产物，它通过文件名引用带 hash 的 chunk。
 * 一旦被浏览器缓存，重新构建后页面会一直请求已不存在的旧 chunk —— 表现为
 * 「后端已更新、页面看不到新功能」。因此对 HTML 入口强制不缓存。
 * <p>
 * 实现方式：在链路执行前先按请求路径判定，对「可能返回 index.html 的请求」预置 no-store 头。
 * 不能在链路返回后再设置 —— {@code ResourceHttpRequestHandler} 会覆盖缓存头，
 * 后置设置会被它写入的值顶掉。
 * <p>
 * 只覆盖 HTML 入口候选路径，{@code /assets/**} 等带扩展名的真实静态资源不受影响。
 */
public class SpaEntryNoCacheFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isHtmlEntryCandidate(request)) {
            response.setHeader("Cache-Control", "no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 与 SPA 回落规则保持一致：无扩展名的页面路由，以及根路径与 index.html 本身。
     */
    private boolean isHtmlEntryCandidate(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        String path = uri.substring(request.getContextPath() == null ? 0 : request.getContextPath().length());
        if (path.isEmpty() || "/".equals(path) || "/index.html".equals(path)) {
            return true;
        }
        if (path.startsWith("/api/") || path.startsWith("/error")) {
            return false;
        }
        // 带扩展名的请求命中真实静态资源，交给默认策略
        String lastSegment = path.substring(path.lastIndexOf('/') + 1);
        return !lastSegment.contains(".");
    }
}