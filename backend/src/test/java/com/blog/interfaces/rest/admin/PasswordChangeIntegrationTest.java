package com.blog.interfaces.rest.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"blog.database.type=sqlite", "blog.admin.username=password-test",
        "blog.admin.password=password-before-change"})
@AutoConfigureMockMvc
class PasswordChangeIntegrationTest {
    private static final String PROFILE = "/api/admin/profile";
    private static final String TOKEN = "blog-token";
    private static final String OLD_PASSWORD = "password-before-change";
    private static final String NEW_PASSWORD = "password-after-change";

    @TempDir
    static Path dataDir;

    @DynamicPropertySource
    static void configureDataDir(DynamicPropertyRegistry registry) {
        registry.add("blog.data-dir", () -> dataDir.toString());
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void changingPasswordRevokesCurrentTokenAndOnlyTheNewPasswordWorks() throws Exception {
        String token = json.readTree(login(OLD_PASSWORD).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray()).path("token").asText();
        mvc.perform(get(PROFILE).header(TOKEN, token)).andExpect(status().isOk());

        mvc.perform(put(PROFILE + "/password").header(TOKEN, token).contentType(MediaType.APPLICATION_JSON)
                        .content(json.createObjectNode().put("oldPassword", OLD_PASSWORD)
                                .put("newPassword", NEW_PASSWORD).toString()))
                .andExpect(status().isOk());
        mvc.perform(get(PROFILE).header(TOKEN, token)).andExpect(status().isUnauthorized());
        login(OLD_PASSWORD).andExpect(status().isUnauthorized());
        login(NEW_PASSWORD).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }

    private ResultActions login(String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.createObjectNode().put("username", "password-test").put("password", password).toString()));
    }
}
