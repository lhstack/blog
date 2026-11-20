package com.blog.domain.comment;

import com.blog.domain.common.DateRange;
import com.blog.domain.common.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CommentRepository {

    Comment save(Comment comment);

    Optional<Comment> findById(Long id);

    List<Comment> findApprovedByArticleId(Long articleId);

    Map<Long, Long> countApprovedByArticleIds(Collection<Long> articleIds);

    PageResult<Comment> search(Comment.Status status, Long articleId, DateRange dates, int page, int size);

    List<Comment> findRecentApproved(int limit);

    long countByStatus(Comment.Status status);

    void deleteById(Long id);
}
