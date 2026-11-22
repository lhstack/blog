package com.blog.domain.setting;

public interface SiteAvatarRepository {
    String location();

    void save(String location);
}
