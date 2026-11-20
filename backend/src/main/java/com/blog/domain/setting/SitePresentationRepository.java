package com.blog.domain.setting;

import java.util.List;

public interface SitePresentationRepository {
    // Reads the migration fallback until the new playlist is saved for the first time.
    String typewriterText();

    String aboutMarkdown();

    void saveAboutMarkdown(String markdown);

    List<MusicTrack> tracks();

    boolean addTrack(MusicTrack track, int limit);

    boolean reorderTracks(List<String> ids);

    boolean renameTrack(String id, String name);

    boolean hasTrack(String id);

    boolean removeTrack(String id);
}
