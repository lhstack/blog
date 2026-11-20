package com.blog.application.setting;

import com.blog.domain.common.DomainException;
import com.blog.domain.setting.SiteIdentityRepository;
import com.blog.domain.setting.SiteIdentityRepository.Identity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SiteIdentityApplicationService {
    public static final int NAME_LIMIT = 40;
    public static final int TITLE_LIMIT = 80;
    public static final int DESCRIPTION_LIMIT = 500;

    private final SiteIdentityRepository repository;
    private final Identity defaults;

    public SiteIdentityApplicationService(SiteIdentityRepository repository,
                                          @Value("${blog.site.name}") String name,
                                          @Value("${blog.site.title}") String title,
                                          @Value("${blog.site.description}") String description) {
        this.repository = repository;
        this.defaults = new Identity(name, title, description);
    }

    public IdentityView current() {
        Identity saved = repository.find();
        if (saved == null) saved = defaults;
        return new IdentityView(saved.name(), saved.title(), saved.description());
    }

    public IdentityView update(String name, String title, String description) {
        var identity = new Identity(value(name, defaults.name(), NAME_LIMIT, "站点名称"),
                value(title, defaults.title(), TITLE_LIMIT, "个人身份"),
                value(description, defaults.description(), DESCRIPTION_LIMIT, "站点简介"));
        repository.save(identity);
        return new IdentityView(identity.name(), identity.title(), identity.description());
    }

    private static String value(String input, String fallback, int limit, String label) {
        String value = input == null ? "" : input.strip();
        if (value.length() > limit) throw DomainException.invalid(label + "最多 " + limit + " 字");
        return value.isEmpty() ? fallback : value;
    }

    public record IdentityView(String name, String title, String description) {}
}
