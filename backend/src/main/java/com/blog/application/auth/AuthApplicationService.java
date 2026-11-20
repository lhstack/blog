package com.blog.application.auth;

import com.blog.application.auth.AuthViews.ProfileView;
import com.blog.application.setting.SiteAvatarApplicationService;
import com.blog.domain.user.ProfileAvatarRepository;
import com.blog.domain.user.ProfileAvatarRepository.Image;
import com.blog.domain.common.DomainException;
import com.blog.domain.user.PasswordHasher;
import com.blog.domain.user.User;
import com.blog.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * 账号用例：校验凭证、修改资料。
 * <p>
 * Token 签发由接口层（Sa-Token）负责，应用层不感知鉴权框架。
 */
@Service
public class AuthApplicationService {

    private final UserRepository userRepository;
    public static final String AVATAR_PATH = "/api/public/profile-avatar/";
    private final PasswordHasher passwordHasher;
    private final ProfileAvatarRepository avatars;

    public AuthApplicationService(UserRepository userRepository, PasswordHasher passwordHasher, ProfileAvatarRepository avatars) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.avatars = avatars;
    }

    @Transactional(readOnly = true)
    public Optional<ProfileView> authenticate(String username, String rawPassword) {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw DomainException.invalid("用户名或密码不能为空");
        }
        return userRepository.findByUsername(username.trim())
                .filter(user -> passwordHasher.matches(rawPassword, user.passwordHash()))
                .map(AuthApplicationService::toProfile);
    }

    @Transactional(readOnly = true)
    public ProfileView profile(Long userId) {
        return toProfile(requireUser(userId));
    }

    @Transactional
    public ProfileView updateProfile(Long userId, String nickname, String avatar, String email) {
        User user = requireUser(userId);
        String value = avatar == null || avatar.isBlank() ? null : avatar.strip();
        if (!java.util.Objects.equals(user.avatar(), value)) {
            if (value != null && !SiteAvatarApplicationService.isWebUrl(value)) {
                throw DomainException.invalid("请输入有效的 HTTP(S) 图片 URL");
            }
            avatars.clear(userId);
        }
        user.updateProfile(nickname, value, email);
        return toProfile(userRepository.save(user));
    }

    @Transactional
    public ProfileView uploadAvatar(Long userId, byte[] bytes) {
        var picture = SiteAvatarApplicationService.validateImage(bytes);
        var id = UUID.randomUUID();
        User user = requireUser(userId);
        avatars.save(userId, id.toString(), picture.mediaType(), picture.content());
        user.updateProfile(user.nickname(), AVATAR_PATH + id, user.email());
        return toProfile(userRepository.save(user));
    }

    public Image avatar(UUID id) {
        Image image = avatars.find(id.toString());
        if (image == null) throw DomainException.notFound("头像文件不存在");
        return image;
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = requireUser(userId);
        if (!passwordHasher.matches(oldPassword, user.passwordHash())) {
            throw DomainException.unauthorized("原密码不正确");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw DomainException.invalid("新密码长度不能少于 6 位");
        }
        user.changePassword(passwordHasher.hash(newPassword));
        userRepository.save(user);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> DomainException.notFound("账号不存在: " + userId));
    }

    private static ProfileView toProfile(User user) {
        return new ProfileView(user.id(), user.username(), user.nickname(), user.avatar(), user.email());
    }
}
