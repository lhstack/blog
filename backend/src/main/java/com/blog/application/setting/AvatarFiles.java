package com.blog.application.setting;

import com.blog.application.attachment.AttachmentApplicationService;
import com.blog.application.attachment.AttachmentApplicationService.Download;
import com.blog.domain.common.DomainException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

@Component
public class AvatarFiles {
    private final AttachmentApplicationService attachments;

    public AvatarFiles(AttachmentApplicationService attachments) {
        this.attachments = attachments;
    }

    public String upload(String filename, byte[] content) {
        SiteAvatarApplicationService.validateImage(content);
        return attachments.uploadAvatar(filename, content.length, new ByteArrayInputStream(content));
    }

    public Download image(String filename, boolean referenced) {
        if (!referenced) throw DomainException.notFound("头像文件不存在");
        var file = attachments.download(filename, true);
        if (!file.attachment().kind().equals("image")) throw DomainException.notFound("头像文件不存在");
        return file;
    }
}
