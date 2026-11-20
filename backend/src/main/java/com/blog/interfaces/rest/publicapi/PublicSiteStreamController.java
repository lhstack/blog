package com.blog.interfaces.rest.publicapi;

import com.blog.application.site.SiteApplicationService;
import com.blog.application.site.SiteViews.DashboardView;
import com.blog.application.site.SiteViews.SiteInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reactor 暴露面：公开读接口的 Mono 包装 + SSE 实时心跳。
 * <p>
 * Spring MVC 支持以 Flux/Mono 作为返回值：SSE（text/event-stream）走流式写出，
 * 普通 JSON 走异步 Servlet 请求。阻塞式的应用层调用被显式调度到
 * {@link Schedulers#boundedElastic()}，避免占用事件循环。
 */
@Tag(name = "前台-实时", description = "Reactor 驱动的统计与 SSE 实时流")
@RestController
@RequestMapping("/api/public/site")
public class PublicSiteStreamController {

    private final SiteApplicationService siteService;
    private final AtomicLong connectionSeq = new AtomicLong();

    public PublicSiteStreamController(SiteApplicationService siteService) {
        this.siteService = siteService;
    }

    @Operation(summary = "站点统计（Mono 异步）")
    @GetMapping("/stats")
    public Mono<SiteInfo> stats() {
        return Mono.fromCallable(siteService::siteInfo)
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "首页统计（Mono 异步）")
    @GetMapping("/dashboard")
    public Mono<DashboardView> dashboard() {
        return Mono.fromCallable(siteService::dashboard)
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * SSE 实时心跳：每 8 秒推送一次站点快照，客户端断线由 EventSource 自动重连。
     * 流本身无累积状态，每次 tick 独立查询，天然支持多连接。
     */
    @Operation(summary = "站点统计 SSE 流", description = "text/event-stream，每 8 秒推送一次")
    @GetMapping(value = "/stats/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<StatsTick> statsStream() {
        long streamId = connectionSeq.incrementAndGet();
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(8))
                .onBackpressureDrop()
                .flatMap(tick -> Mono.fromCallable(() -> {
                            SiteInfo info = siteService.siteInfo();
                            return new StatsTick(streamId, LocalDateTime.now(),
                                    info.articleCount(), info.viewCount(), info.commentCount());
                        })
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    public record StatsTick(long streamId, LocalDateTime at, long articles, long views, long comments) {
    }
}
