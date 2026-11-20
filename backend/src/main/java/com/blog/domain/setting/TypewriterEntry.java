package com.blog.domain.setting;

import com.blog.domain.common.DomainException;
import java.time.LocalDateTime;

public record TypewriterEntry(String text, int daysMask, int startMinute, int endMinute) {
    public TypewriterEntry {
        text = text == null ? "" : text.strip();
        if (text.isEmpty() || text.length() > 200 || daysMask < 1 || daysMask > 127
                || startMinute < 0 || startMinute >= 1440 || endMinute < 0 || endMinute >= 1440) {
            throw DomainException.invalid("文案须为 1–200 字，且必须设置有效的星期和时段");
        }
    }

    public boolean activeAt(LocalDateTime now) {
        int minute = now.getHour() * 60 + now.getMinute();
        if (startMinute == endMinute) return onDay(now);
        if (startMinute < endMinute) return onDay(now) && minute >= startMinute && minute < endMinute;
        return (minute >= startMinute && onDay(now)) || (minute < endMinute && onDay(now.minusDays(1)));
    }

    private boolean onDay(LocalDateTime date) {
        return (daysMask & (1 << (date.getDayOfWeek().getValue() - 1))) != 0;
    }
}
