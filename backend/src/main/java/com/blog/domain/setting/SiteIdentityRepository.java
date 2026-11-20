package com.blog.domain.setting;

public interface SiteIdentityRepository {
    Identity find();
    void save(Identity identity);

    record Identity(String name, String title, String description) {}
}
