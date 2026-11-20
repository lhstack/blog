package com.blog.interfaces.rest.admin;

import com.blog.application.setting.TypewriterApplicationService;
import com.blog.application.setting.TypewriterApplicationService.TypewriterSettings;
import com.blog.domain.setting.TypewriterEntry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/settings/typewriter")
public class AdminTypewriterController {
    private final TypewriterApplicationService service;

    public AdminTypewriterController(TypewriterApplicationService service) { this.service = service; }

    @GetMapping
    public TypewriterSettings get() { return service.settings(); }

    @PutMapping
    public TypewriterSettings save(@Valid @RequestBody UpdateRequest request) {
        return service.save(request.entries().stream().map(EntryRequest::toEntry).toList());
    }

    record UpdateRequest(@NotNull @Size(max = TypewriterApplicationService.MAX_ENTRIES) List<@NotNull @Valid EntryRequest> entries) {}

    record EntryRequest(@NotBlank @Size(max = 200) String text,
                        @NotNull @Min(1) @Max(127) Integer daysMask,
                        @NotNull @Min(0) @Max(1439) Integer startMinute,
                        @NotNull @Min(0) @Max(1439) Integer endMinute) {
        TypewriterEntry toEntry() { return new TypewriterEntry(text, daysMask, startMinute, endMinute); }
    }
}
