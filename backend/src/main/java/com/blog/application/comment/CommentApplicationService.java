package com.blog.application.comment;

import com.blog.application.comment.CommentViews.CommentThread;
import com.blog.application.comment.CommentViews.CommentView;
import com.blog.domain.article.Article;
import com.blog.domain.article.ArticleRepository;
import com.blog.domain.comment.Comment;
import com.blog.domain.comment.CommentRepository;
import com.blog.domain.common.DomainException;
import com.blog.domain.common.DateRange;
import com.blog.domain.common.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentApplicationService {

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;

    public CommentApplicationService(CommentRepository commentRepository, ArticleRepository articleRepository) {
        this.commentRepository = commentRepository;
        this.articleRepository = articleRepository;
    }

    @Transactional
    public CommentView submit(Long articleId, String author, String email, String website, String content, String ip) {
        Article article = articleRepository.findById(articleId)
                .filter(Article::isPublished)
                .orElseThrow(() -> DomainException.notFound("文章不存在或尚未发布"));
        return CommentView.from(commentRepository.save(
                Comment.submit(article.id(), author, email, website, content, ip)));
    }

    @Transactional(readOnly = true)
    public CommentThread listApproved(Long articleId) {
        List<Comment> comments = commentRepository.findApprovedByArticleId(articleId);
        return new CommentThread(comments.stream().map(CommentView::from).toList(), comments.size());
    }

    @Transactional(readOnly = true)
    public List<CommentView> recentApproved(int limit) {
        return commentRepository.findRecentApproved(limit).stream().map(CommentView::from).toList();
    }

    @Transactional(readOnly = true)
    public PageResult<CommentView> search(String status, Long articleId, DateRange dates, int page, int size) {
        Comment.Status parsed = status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)
                ? null
                : Comment.Status.of(status);
        PageResult<Comment> result = commentRepository.search(parsed, articleId, dates,
                Math.max(page, 1), Math.min(Math.max(size, 1), 100));
        var titles = articleRepository.findByIds(result.items().stream().map(Comment::articleId).distinct().toList())
                .stream().collect(Collectors.toMap(Article::id, Article::title));
        return PageResult.of(result.items().stream().map(comment -> CommentView.from(comment, titles.get(comment.articleId()))).toList(),
                result.total(), result.page(), result.size());
    }

    @Transactional
    public CommentView moderate(Long id, String action) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> DomainException.notFound("评论不存在: " + id));
        switch (action == null ? "" : action.trim().toLowerCase()) {
            case "approve" -> comment.approve();
            case "reject" -> comment.reject();
            default -> throw DomainException.invalid("未知的审核动作: " + action);
        }
        return CommentView.from(commentRepository.save(comment));
    }

    @Transactional
    public void delete(Long id) {
        commentRepository.findById(id)
                .orElseThrow(() -> DomainException.notFound("评论不存在: " + id));
        commentRepository.deleteById(id);
    }
}
