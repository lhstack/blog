package com.blog.interfaces.rest.admin;

import com.blog.application.setting.SitePresentationApplicationService;
import com.blog.application.setting.SitePresentationApplicationService.AboutView;
import com.blog.application.setting.SitePresentationApplicationService.PresentationView;
import com.blog.application.setting.SitePresentationApplicationService.TrackView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/settings/site/presentation")
public class AdminSitePresentationController {
    private final SitePresentationApplicationService service;

    public AdminSitePresentationController(SitePresentationApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public PresentationView current() {
        return service.current();
    }

    @GetMapping("/about")
    public AboutView about() {
        return service.about();
    }

    @PutMapping("/about")
    public AboutView updateAbout(@Valid @RequestBody AboutRequest request) {
        return service.updateAbout(request.markdown());
    }

    @PostMapping(path = "/music", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public TrackView uploadMusic(@RequestPart("file") MultipartFile file,
                                 @RequestParam(required = false) String name) throws IOException {
        try (InputStream source = file.getInputStream()) {
            return service.addTrack(file.getOriginalFilename(), name, file.getSize(), source);
        }
    }

    @PutMapping("/music/{id}/name")
    public TrackView renameMusic(@PathVariable UUID id, @Valid @RequestBody TrackNameRequest request) {
        return service.renameTrack(id, request.name());
    }

    @PutMapping("/music/order")
    public List<TrackView> reorderMusic(@Valid @RequestBody TrackOrderRequest request) {
        return service.reorderTracks(request.ids());
    }

    @DeleteMapping("/music/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMusic(@PathVariable UUID id) {
        service.removeTrack(id);
    }

    record AboutRequest(
            @NotNull(message = "请提供关于页正文")
            @Size(max = SitePresentationApplicationService.ABOUT_LIMIT, message = "关于页正文不能超过 50000 个字符")
            String markdown) {}

    record TrackNameRequest(@NotBlank String name) {}

    record TrackOrderRequest(
            @NotNull @Size(max = SitePresentationApplicationService.TRACK_LIMIT)
            List<@NotNull UUID> ids) {}
}
