package com.blog.domain.common;

import java.time.LocalDate;

/** Inclusive calendar dates; persistence uses a half-open interval. */
public record DateRange(LocalDate from, LocalDate to) {
    public DateRange {
        if ((from != null && (from.getYear() < 1 || from.getYear() > 9998))
                || (to != null && (to.getYear() < 1 || to.getYear() > 9998))
                || (from != null && to != null && from.isAfter(to))) {
            throw DomainException.invalid("日期范围不正确");
        }
    }

    public LocalDate until() { return to == null ? null : to.plusDays(1); }
}
