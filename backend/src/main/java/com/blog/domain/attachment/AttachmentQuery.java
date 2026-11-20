package com.blog.domain.attachment;

import com.blog.domain.common.DateRange;

public record AttachmentQuery(String kind, String keyword, DateRange dates) {}
