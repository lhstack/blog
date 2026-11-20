package com.blog.application.setting;

import com.blog.domain.common.DomainException;
import com.blog.domain.setting.SitePresentationRepository;
import com.blog.domain.setting.TypewriterEntry;
import com.blog.domain.setting.TypewriterRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class TypewriterApplicationService {
    public static final int MAX_ENTRIES = 30;
    private final TypewriterRepository repository;
    private final SitePresentationRepository legacy;
    private final ZoneId zone;

    public TypewriterApplicationService(TypewriterRepository repository, SitePresentationRepository legacy,
                                        @Value("${blog.site.typewriter-zone:Asia/Shanghai}") String zone) {
        this.repository = repository;
        this.legacy = legacy;
        this.zone = ZoneId.of(zone);
    }

    public TypewriterSettings settings() {
        return new TypewriterSettings(zone.getId(), entries());
    }

    public TypewriterSettings save(List<TypewriterEntry> entries) {
        if (entries == null || entries.size() > MAX_ENTRIES || entries.stream().anyMatch(entry -> entry == null)) {
            throw DomainException.invalid("最多配置 30 条文案");
        }
        var snapshot = List.copyOf(entries);
        repository.save(snapshot);
        return new TypewriterSettings(zone.getId(), snapshot);
    }

    public ActiveText active() {
        var configured = entries();
        var now = ZonedDateTime.now(zone);
        var texts = configured.stream().filter(entry -> entry.activeAt(now.toLocalDateTime())).map(TypewriterEntry::text).toList();
        int refreshAfterMs = 60_000 - now.getSecond() * 1000 - now.getNano() / 1_000_000;
        return new ActiveText(texts, refreshAfterMs);
    }

    private List<TypewriterEntry> entries() {
        return repository.find().orElseGet(() -> {
            String text = legacy.typewriterText();
            return text == null || text.isBlank() ? List.of() : List.of(new TypewriterEntry(text, 127, 0, 0));
        });
    }

    public record TypewriterSettings(String zone, List<TypewriterEntry> entries) {}
    public record ActiveText(List<String> texts, int refreshAfterMs) {}
}
