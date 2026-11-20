package com.blog.infrastructure.persistence.tag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.domain.tag.Tag;
import com.blog.domain.tag.TagRepository;
import com.blog.infrastructure.persistence.mapper.TagMapper;
import com.blog.infrastructure.persistence.po.TagPo;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class TagRepositoryImpl implements TagRepository {

    private final TagMapper mapper;

    public TagRepositoryImpl(TagMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<Tag> findAll() {
        LambdaQueryWrapper<TagPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(TagPo::getId);
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Tag> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mapper.selectBatchIds(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Tag> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<Tag> findByName(String name) {
        LambdaQueryWrapper<TagPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TagPo::getName, name == null ? null : name.trim()).last("LIMIT 1");
        return Optional.ofNullable(mapper.selectOne(wrapper)).map(this::toDomain);
    }

    @Override
    public Tag save(Tag tag) {
        TagPo po = toPo(tag);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private Tag toDomain(TagPo po) {
        return new Tag(po.getId(), po.getName(), po.getColor());
    }

    private TagPo toPo(Tag tag) {
        TagPo po = new TagPo();
        po.setId(tag.id());
        po.setName(tag.name());
        po.setColor(tag.color());
        return po;
    }
}
