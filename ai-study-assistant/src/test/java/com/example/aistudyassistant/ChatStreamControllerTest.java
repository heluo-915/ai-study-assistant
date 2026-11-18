package com.example.aistudyassistant;

import com.example.aistudyassistant.controller.ChatStreamController;
import com.example.aistudyassistant.security.JwtInterceptor;
import com.example.aistudyassistant.service.ChatStreamService;
import com.example.aistudyassistant.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

class ChatStreamControllerTest {
    // 验证登录、输入检查和流式事件输出。
    @Test
    void checksLoginInputAndStreamsEvents() throws Exception {
        ChatStreamService service = mock(ChatStreamService.class);
        JwtService jwt = mock(JwtService.class);
        when(jwt.parseUserId("valid")).thenReturn(5L);
        var mvc = MockMvcBuilders.standaloneSetup(new ChatStreamController(service))
                .addInterceptors(new JwtInterceptor(jwt)).build();
        mvc.perform(post("/chat/stream").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"hello\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/chat/stream").header("Authorization", "Bearer valid")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\" \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
        when(service.stream("hello")).thenReturn(Flux.just(
                ServerSentEvent.<String>builder("part").event("message").build(),
                ServerSentEvent.<String>builder("ok").event("done").build()));
        var result = mvc.perform(post("/chat/stream").header("Authorization", "Bearer valid")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"hello\"}"))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(result)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:message")))
                .andExpect(content().string(containsString("data:part")))
                .andExpect(content().string(containsString("event:done")));
    }
}
