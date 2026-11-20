package com.blog.infrastructure.persistence.setting;

import com.blog.domain.setting.TypewriterEntry;
import com.blog.domain.setting.TypewriterRepository;
import com.blog.infrastructure.persistence.mapper.TypewriterMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TypewriterRepositoryImpl implements TypewriterRepository {
    private final TypewriterMapper mapper;
    private final ObjectMapper json;

    public TypewriterRepositoryImpl(TypewriterMapper mapper, ObjectMapper json) {
        this.mapper = mapper;
        this.json = json;
    }

    @Override
    public Optional<List<TypewriterEntry>> find() {
        String value = mapper.find();
        if (value == null) return Optional.empty();
        try {
            return Optional.of(json.readValue(value, new TypeReference<List<TypewriterEntry>>() {}));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("无法读取首页打字机配置", ex);
        }
    }

    @Override
    public void save(List<TypewriterEntry> entries) {
        try {
            mapper.save(json.writeValueAsString(entries));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("无法保存首页打字机配置", ex);
        }
    }
}
