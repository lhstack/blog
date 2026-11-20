package com.blog.interfaces.rest.support;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void unusableSseResponseIsNotRewritten() throws Exception {
        MvcResult stream = mvc.perform(get("/stream").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(stream))
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(AsyncRequestNotUsableException.class))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(""));
    }

    @Test
    void unexpectedExceptionOnJsonEndpointStillReturnsApiResponse() throws Exception {
        mvc.perform(get("/json").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("服务器内部错误"));
    }

    @RestController
    static class TestController {

        @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        DeferredResult<Void> stream(HttpServletResponse response) {
            response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
            DeferredResult<Void> result = new DeferredResult<>();
            result.setErrorResult(new AsyncRequestNotUsableException("ServletOutputStream failed to flush",
                    new IOException("Broken pipe")));
            return result;
        }

        @GetMapping(value = "/json", produces = MediaType.APPLICATION_JSON_VALUE)
        void json() {
            throw new IllegalStateException("test failure");
        }
    }
}
