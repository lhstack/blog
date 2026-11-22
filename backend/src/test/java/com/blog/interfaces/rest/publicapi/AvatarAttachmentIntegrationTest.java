package com.blog.interfaces.rest.publicapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"blog.admin.username=avatar-test", "blog.admin.password=avatar-test-password"})
@AutoConfigureMockMvc
class AvatarAttachmentIntegrationTest {
    private static final String ATTACHMENTS = "/api/admin/attachments";
    private static final String SITE = "/api/admin/settings/site/avatar";
    private static final String PROFILE = "/api/admin/profile";
    private static final String TOKEN = "blog-token";

    @TempDir
    static Path dataDir;

    @DynamicPropertySource
    static void configureDataDir(DynamicPropertyRegistry registry) {
        registry.add("blog.data-dir", () -> dataDir.toString());
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void uploadsShareTheAttachmentLibraryAndOnlyActiveAvatarUrlsCanServeThem() throws Exception {
        String token = field(mvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("""
                        {"username":"avatar-test","password":"avatar-test-password"}
                        """)), "token");
        byte[] image = png();
        mvc.perform(multipart(SITE).file(picture("invalid.png", new byte[]{1})).header(TOKEN, token))
                .andExpect(status().isBadRequest());
        mvc.perform(get(ATTACHMENTS).header(TOKEN, token)).andExpect(jsonPath("$.total").value(0));

        String site = upload(SITE, "site.png", image, token);
        String profile = upload(PROFILE + "/avatar", "profile.png", image, token);
        String siteFilename = filename(site);
        String profileFilename = filename(profile);
        assertThat(site).matches("/media/site-avatar/site-[0-9a-f]{16}\\.png");
        assertThat(profile).matches("/media/profile-avatar/profile-[0-9a-f]{16}\\.png");
        assertThat(dataDir.resolve("attachments/avatars").resolve(siteFilename)).hasBinaryContent(image);
        assertThat(dataDir.resolve("attachments/avatars").resolve(profileFilename)).hasBinaryContent(image);
        var entries = json.readTree(mvc.perform(get(ATTACHMENTS).header(TOKEN, token).param("kind", "image"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[*].name", hasItems("site.png", "profile.png")))
                .andReturn().getResponse().getContentAsByteArray()).path("items");
        String siteId = attachmentId(entries, siteFilename);
        String profileId = attachmentId(entries, profileFilename);

        mvc.perform(get(site)).andExpect(status().isOk()).andExpect(content().bytes(image))
                .andExpect(header().string("Cache-Control", containsString("max-age=300")))
                .andExpect(header().string("Cache-Control", containsString("public")));
        mvc.perform(get(profile)).andExpect(status().isOk()).andExpect(content().bytes(image))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get(ATTACHMENTS + "/files/" + profileFilename)).andExpect(status().isUnauthorized());
        mvc.perform(get(ATTACHMENTS + "/files/" + profileFilename).header(TOKEN, token))
                .andExpect(status().isOk()).andExpect(content().bytes(image));
        mvc.perform(get("/media/" + siteFilename)).andExpect(status().isNotFound());
        mvc.perform(get("/media/site-avatar/" + profileFilename)).andExpect(status().isNotFound());
        mvc.perform(get("/media/profile-avatar/" + siteFilename)).andExpect(status().isNotFound());
        mvc.perform(get("/api/public/site/avatar/" + siteId)).andExpect(status().isNotFound());

        mvc.perform(delete(ATTACHMENTS + "/" + siteId).header(TOKEN, token)).andExpect(status().isConflict());
        mvc.perform(delete(ATTACHMENTS + "/" + profileId).header(TOKEN, token)).andExpect(status().isConflict());

        String replacement = upload(SITE, "replacement.png", image, token);
        assertThat(replacement).isNotEqualTo(site);
        mvc.perform(get(site)).andExpect(status().isNotFound());
        mvc.perform(get("/media/" + filename(replacement))).andExpect(status().isNotFound());
        mvc.perform(get("/media/" + siteFilename)).andExpect(status().isNotFound());
        mvc.perform(get(ATTACHMENTS).header(TOKEN, token)).andExpect(jsonPath("$.total").value(3));
        mvc.perform(put(PROFILE).header(TOKEN, token).contentType("application/json")
                .content("""
                        {"nickname":"管理员","avatar":"https://example.com/avatar.png"}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.avatar").value("https://example.com/avatar.png"));
        mvc.perform(get(profile)).andExpect(status().isNotFound());
        mvc.perform(delete(ATTACHMENTS + "/" + siteId).header(TOKEN, token)).andExpect(status().isNoContent());
        mvc.perform(delete(ATTACHMENTS + "/" + profileId).header(TOKEN, token)).andExpect(status().isNoContent());
        assertThat(dataDir.resolve("attachments/avatars").resolve(siteFilename)).doesNotExist();
        assertThat(dataDir.resolve("attachments/avatars").resolve(profileFilename)).doesNotExist();
        checkArticleAttachment(image, token);
    }

    private String upload(String path, String filename, byte[] bytes, String token) throws Exception {
        return field(mvc.perform(multipart(path).file(picture(filename, bytes)).header(TOKEN, token)), "avatar");
    }

    private String field(ResultActions result, String key) throws Exception {
        return json.readTree(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray())
                .path(key).asText();
    }

    private static MockMultipartFile picture(String name, byte[] bytes) {
        return new MockMultipartFile("file", name, "image/png", bytes);
    }

    private void checkArticleAttachment(byte[] image, String token) throws Exception {
        var attachment = json.readTree(mvc.perform(multipart(ATTACHMENTS).file(picture("cover.png", image)).header(TOKEN, token))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsByteArray());
        String id = attachment.path("id").asText();
        String url = attachment.path("url").asText();
        assertThat(url).matches("/media/cover-[0-9a-f]{16}\\.png");
        assertThat(jdbc.queryForObject("SELECT file_path FROM blog_attachment WHERE id = ?", String.class, id))
                .isEqualTo("attachments/public/" + filename(url));
        assertThat(dataDir.resolve("attachments/public").resolve(filename(url))).hasBinaryContent(image);
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(image));
        mvc.perform(get(ATTACHMENTS + "/files/" + filename(url)).header(TOKEN, token))
                .andExpect(status().isOk()).andExpect(content().bytes(image));
        mvc.perform(put(ATTACHMENTS + "/" + id).header(TOKEN, token).contentType("application/json")
                        .content("{\"name\":\"renamed.png\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.url").value(url));
        String article = "/api/admin/articles/" + field(mvc.perform(post("/api/admin/articles").header(TOKEN, token)
                .contentType("application/json").content(json.createObjectNode().put("title", "Attachment paths")
                        .put("cover", url).put("content", "![cover](" + url + ")").toString())), "id");
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().bytes(image));
        mvc.perform(delete(ATTACHMENTS + "/" + id).header(TOKEN, token)).andExpect(status().isConflict());
        mvc.perform(post(article + "/publish").header(TOKEN, token)).andExpect(status().isOk());
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(image));
        mvc.perform(get("/api/public/attachments/" + id + "/image")).andExpect(status().isNotFound());
        mvc.perform(post(article + "/withdraw").header(TOKEN, token)).andExpect(status().isOk());
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().bytes(image));
        mvc.perform(delete(article).header(TOKEN, token)).andExpect(status().isOk());
        mvc.perform(delete(ATTACHMENTS + "/" + id).header(TOKEN, token)).andExpect(status().isNoContent());
        mvc.perform(get(url)).andExpect(status().isNotFound());
    }

    private static String filename(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    private static String attachmentId(com.fasterxml.jackson.databind.JsonNode entries, String filename) {
        for (var entry : entries) {
            if (filename.equals(entry.path("filename").asText())) return entry.path("id").asText();
        }
        throw new AssertionError("Attachment is missing from the library: " + filename);
    }

    private static byte[] png() throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return bytes.toByteArray();
    }
}
