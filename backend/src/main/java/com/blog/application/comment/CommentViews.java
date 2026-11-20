package com.blog.application.comment;

import com.blog.domain.comment.Comment;

import java.time.LocalDateTime;
import java.util.List;

public final class CommentViews {

    private CommentViews() {
    }

    public record CommentView(
            Long id,
            Long articleId,
            String author,
            String website,
            String content,
            String avatarSeed,
            String status,
            LocalDateTime createdAt,
            String articleTitle) {

        public static CommentView from(Comment comment) { return from(comment, null); }

        public static CommentView from(Comment comment, String articleTitle) {
            return new CommentView(
                    comment.id(),
                    comment.articleId(),
                    comment.author(),
                    comment.website(),
                    comment.content(),
                    comment.author(),
                    comment.status().name(),
                    comment.createdAt(), articleTitle);
        }
    }

    public record CommentThread(List<CommentView> items, long total) {
    }
}
