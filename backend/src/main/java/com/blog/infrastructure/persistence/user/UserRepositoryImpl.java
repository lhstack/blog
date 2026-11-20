package com.blog.infrastructure.persistence.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.domain.user.User;
import com.blog.domain.user.UserRepository;
import com.blog.infrastructure.persistence.mapper.UserMapper;
import com.blog.infrastructure.persistence.po.UserPo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper mapper;

    public UserRepositoryImpl(UserMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        LambdaQueryWrapper<UserPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPo::getUsername, username).last("LIMIT 1");
        return Optional.ofNullable(mapper.selectOne(wrapper)).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public User save(User user) {
        UserPo po = toPo(user);
        if (po.getId() == null) {
            po.setCreatedAt(LocalDateTime.now());
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    private User toDomain(UserPo po) {
        return new User(po.getId(), po.getUsername(), po.getPasswordHash(),
                po.getNickname(), po.getAvatar(), po.getEmail());
    }

    private UserPo toPo(User user) {
        UserPo po = new UserPo();
        po.setId(user.id());
        po.setUsername(user.username());
        po.setPasswordHash(user.passwordHash());
        po.setNickname(user.nickname());
        po.setAvatar(user.avatar());
        po.setEmail(user.email());
        return po;
    }
}
