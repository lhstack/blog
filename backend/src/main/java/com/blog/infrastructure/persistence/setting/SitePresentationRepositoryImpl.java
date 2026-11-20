package com.blog.infrastructure.persistence.setting;

import com.blog.domain.setting.MusicTrack;
import com.blog.domain.setting.SitePresentationRepository;
import com.blog.infrastructure.persistence.mapper.SitePresentationMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SitePresentationRepositoryImpl implements SitePresentationRepository {
    private final SitePresentationMapper mapper;

    public SitePresentationRepositoryImpl(SitePresentationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String typewriterText() {
        return mapper.typewriterText();
    }

    @Override
    public String aboutMarkdown() {
        return mapper.aboutMarkdown();
    }

    @Override
    public void saveAboutMarkdown(String markdown) {
        mapper.saveAboutMarkdown(markdown);
    }

    @Override
    public List<MusicTrack> tracks() {
        return mapper.tracks();
    }

    @Override
    public boolean addTrack(MusicTrack track, int limit) {
        return mapper.addTrack(track, limit) == 1;
    }

    @Override
    public boolean reorderTracks(List<String> ids) {
        return ids.isEmpty() ? mapper.tracks().isEmpty() : mapper.reorderTracks(ids, ids.size()) == ids.size();
    }

    @Override
    public boolean renameTrack(String id, String name) {
        return mapper.renameTrack(id, name) == 1;
    }

    @Override
    public boolean hasTrack(String id) {
        return mapper.hasTrack(id);
    }

    @Override
    public boolean removeTrack(String id) {
        return mapper.removeTrack(id) == 1;
    }
}
