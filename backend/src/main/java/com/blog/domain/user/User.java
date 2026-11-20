package com.blog.domain.user;

/**
 * 管理员账号聚合根。密码以不可逆摘要保存，领域层不关心加密算法细节。
 */
public class User {

    private final Long id;
    private String username;
    private String passwordHash;
    private String nickname;
    private String avatar;
    private String email;

    public User(Long id, String username, String passwordHash, String nickname, String avatar, String email) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.avatar = avatar;
        this.email = email;
    }

    public static User newAccount(String username, String passwordHash, String nickname) {
        if (username == null || username.isBlank()) {
            throw com.blog.domain.common.DomainException.invalid("用户名不能为空");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw com.blog.domain.common.DomainException.invalid("密码不能为空");
        }
        return new User(null, username.trim(), passwordHash, nickname == null || nickname.isBlank() ? username : nickname, null, null);
    }

    public void changePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw com.blog.domain.common.DomainException.invalid("新密码不能为空");
        }
        this.passwordHash = newPasswordHash;
    }

    public void rename(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw com.blog.domain.common.DomainException.invalid("昵称不能为空");
        }
        this.nickname = nickname.trim();
    }

    public void updateProfile(String nickname, String avatar, String email) {
        rename(nickname);
        this.avatar = avatar;
        this.email = email;
    }

    public Long id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String nickname() {
        return nickname;
    }

    public String avatar() {
        return avatar;
    }

    public String email() {
        return email;
    }
}
