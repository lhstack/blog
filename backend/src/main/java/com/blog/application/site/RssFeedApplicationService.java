package com.blog.application.site;

import com.blog.domain.article.ArticleFeedEntry;
import com.blog.domain.article.ArticleRepository;
import com.blog.application.setting.SiteIdentityApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RssFeedApplicationService {

    private static final int FEED_LIMIT = 20;

    private final ArticleRepository articleRepository;
    private final SiteIdentityApplicationService identityService;

    public RssFeedApplicationService(ArticleRepository articleRepository, SiteIdentityApplicationService identityService) {
        this.articleRepository = articleRepository;
        this.identityService = identityService;
    }

    @Transactional(readOnly = true)
    public FeedView latest() {
        var site = identityService.current();
        return new FeedView(site.name(), site.description(), articleRepository.findLatestPublishedEntries(FEED_LIMIT));
    }

    public record FeedView(String title, String description, List<ArticleFeedEntry> entries) {
    }
}
