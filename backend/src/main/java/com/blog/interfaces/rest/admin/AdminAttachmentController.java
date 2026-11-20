package com.blog.interfaces.rest.admin;

import com.blog.application.attachment.AttachmentApplicationService;
import com.blog.application.attachment.AttachmentApplicationService.AttachmentView;
import com.blog.domain.common.DateRange;
import com.blog.domain.common.PageResult;
import com.blog.interfaces.rest.support.AttachmentResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/attachments")
public class AdminAttachmentController {
    private final AttachmentApplicationService service;

    public AdminAttachmentController(AttachmentApplicationService service) { this.service = service; }

    @GetMapping
    public PageResult<AttachmentView> list(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(required = false) String kind,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) LocalDate from,
                                         @RequestParam(required = false) LocalDate to) {
        return service.list(page, kind, keyword, new DateRange(from, to));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentView upload(@RequestPart("file") MultipartFile file) throws IOException {
        try (var input = file.getInputStream()) {
            return service.upload(file.getOriginalFilename(), file.getSize(), input);
        }
    }

    @PutMapping("/{id}")
    public AttachmentView rename(@PathVariable UUID id, @Valid @RequestBody RenameRequest request) {
        return service.rename(id, request.name());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }

    @GetMapping("/{id}/{kind}")
    public ResponseEntity<Resource> download(@PathVariable UUID id, @PathVariable String kind) {
        return AttachmentResponse.of(service.download(id, kind, true));
    }

    record RenameRequest(@NotBlank @Size(max = 255) String name) {}
}
