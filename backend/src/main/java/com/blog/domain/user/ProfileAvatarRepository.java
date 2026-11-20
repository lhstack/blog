package com.blog.domain.user;

public interface ProfileAvatarRepository {
    void save(Long userId, String id, String mediaType, byte[] content);
    void clear(Long userId);
    Image find(String id);

    record Image(String mediaType, byte[] content) {}
}
