package com.blog.infrastructure.persistence.category;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.domain.category.Category;
import com.blog.domain.category.CategoryRepository;
import com.blog.infrastructure.persistence.mapper.CategoryMapper;
import com.blog.infrastructure.persistence.po.CategoryPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryMapper mapper;

    public CategoryRepositoryImpl(CategoryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<Category> findAll() {
        LambdaQueryWrapper<CategoryPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(CategoryPo::getSortOrder).orderByAsc(CategoryPo::getId);
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Category> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<Category> findBySlug(String slug) {
        LambdaQueryWrapper<CategoryPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CategoryPo::getSlug, slug).last("LIMIT 1");
        return Optional.ofNullable(mapper.selectOne(wrapper)).map(this::toDomain);
    }

    @Override
    public Category save(Category category) {
        CategoryPo po = toPo(category);
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

    @Override
    public boolean existsByName(String name, Long excludeId) {
        LambdaQueryWrapper<CategoryPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CategoryPo::getName, name == null ? null : name.trim());
        if (excludeId != null) {
            wrapper.ne(CategoryPo::getId, excludeId);
        }
        return mapper.selectCount(wrapper) > 0;
    }

    private Category toDomain(CategoryPo po) {
        return new Category(po.getId(), po.getName(), po.getSlug(), po.getDescription(),
                po.getSortOrder() == null ? 0 : po.getSortOrder());
    }

    private CategoryPo toPo(Category category) {
        CategoryPo po = new CategoryPo();
        po.setId(category.id());
        po.setName(category.name());
        po.setSlug(category.slug());
        po.setDescription(category.description());
        po.setSortOrder(category.sortOrder());
        return po;
    }
}
