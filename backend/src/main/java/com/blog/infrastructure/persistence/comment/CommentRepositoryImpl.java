package com.blog.infrastructure.persistence.comment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.domain.comment.Comment;
import com.blog.domain.comment.CommentRepository;
import com.blog.domain.common.DateRange;
import com.blog.domain.common.PageResult;
import com.blog.infrastructure.persistence.mapper.CommentMapper;
import com.blog.infrastructure.persistence.po.CommentPo;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class CommentRepositoryImpl implements CommentRepository {

    private final CommentMapper mapper;

    public CommentRepositoryImpl(CommentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Comment save(Comment comment) {
        CommentPo po = toPo(comment);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<Comment> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<Comment> findApprovedByArticleId(Long articleId) {
        LambdaQueryWrapper<CommentPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentPo::getArticleId, articleId)
                .eq(CommentPo::getStatus, Comment.Status.APPROVED.name())
                .orderByAsc(CommentPo::getCreatedAt);
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public Map<Long, Long> countApprovedByArticleIds(Collection<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<CommentPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(CommentPo::getArticleId)
                .eq(CommentPo::getStatus, Comment.Status.APPROVED.name())
                .in(CommentPo::getArticleId, articleIds);
        Map<Long, Long> result = new HashMap<>();
        for (CommentPo po : mapper.selectList(wrapper)) {
            result.merge(po.getArticleId(), 1L, Long::sum);
        }
        return result;
    }

    @Override
    public PageResult<Comment> search(Comment.Status status, Long articleId, DateRange dates, int page, int size) {
        LambdaQueryWrapper<CommentPo> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(CommentPo::getStatus, status.name());
        }
        if (articleId != null) wrapper.eq(CommentPo::getArticleId, articleId);
        if (dates.from() != null) wrapper.ge(CommentPo::getCreatedAt, dates.from().toString());
        if (dates.until() != null) wrapper.lt(CommentPo::getCreatedAt, dates.until().toString());
        wrapper.orderByDesc(CommentPo::getCreatedAt).orderByDesc(CommentPo::getId);
        IPage<CommentPo> result = mapper.selectPage(Page.of(page, size), wrapper);
        return PageResult.of(result.getRecords().stream().map(this::toDomain).toList(),
                result.getTotal(), page, size);
    }

    @Override
    public List<Comment> findRecentApproved(int limit) {
        LambdaQueryWrapper<CommentPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentPo::getStatus, Comment.Status.APPROVED.name())
                .orderByDesc(CommentPo::getCreatedAt)
                .last("LIMIT " + Math.max(limit, 1));
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public long countByStatus(Comment.Status status) {
        LambdaQueryWrapper<CommentPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentPo::getStatus, status.name());
        return mapper.selectCount(wrapper);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private Comment toDomain(CommentPo po) {
        return new Comment(po.getId(), po.getArticleId(), po.getAuthor(), po.getEmail(),
                po.getWebsite(), po.getContent(), po.getIp(),
                Comment.Status.of(po.getStatus()), po.getCreatedAt());
    }

    private CommentPo toPo(Comment comment) {
        CommentPo po = new CommentPo();
        po.setId(comment.id());
        po.setArticleId(comment.articleId());
        po.setAuthor(comment.author());
        po.setEmail(comment.email());
        po.setWebsite(comment.website());
        po.setContent(comment.content());
        po.setIp(comment.ip());
        po.setStatus(comment.status().name());
        po.setCreatedAt(comment.createdAt());
        return po;
    }
}
