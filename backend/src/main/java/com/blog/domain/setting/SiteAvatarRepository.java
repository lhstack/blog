package com.blog.domain.setting;

public interface SiteAvatarRepository {
    String location();

    AvatarImage findImage(String location);

    void save(String location, AvatarImage image);

    record AvatarImage(String mediaType, byte[] content) {}
}
