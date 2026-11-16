package com.example.aistudyassistant;

import com.example.aistudyassistant.controller.ChatConversationStreamController;
import com.example.aistudyassistant.security.JwtInterceptor;
import com.example.aistudyassistant.service.ChatConversationStreamService;
import com.example.aistudyassistant.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

class ChatConversationStreamControllerTest {
    @Test
    void checksLoginInputOwnershipAndStreams() throws Exception {
        ChatConversationStreamService service = mock(ChatConversationStreamService.class);
        JwtService jwt = mock(JwtService.class);
        when(jwt.parseUserId("valid")).thenReturn(5L);
        var mvc = MockMvcBuilders.standaloneSetup(new ChatConversationStreamController(service))
                .addInterceptors(new JwtInterceptor(jwt)).build();
        String path = "/chat/sessions/8/messages/stream";
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"hello\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(path).header("Authorization", "Bearer valid").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\" \"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
        mvc.perform(post(path).header("Authorization", "Bearer valid").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"" + "中".repeat(21846) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
        when(service.prepare(8L, 5L, "hello")).thenReturn(Optional.empty());
        mvc.perform(post(path).header("Authorization", "Bearer valid").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"hello\"}")).andExpect(status().isNotFound());
        when(service.prepare(8L, 5L, "hello")).thenReturn(Optional.of(Flux.just(
                ServerSentEvent.<String>builder("part").event("message").build(),
                ServerSentEvent.<String>builder("ok").event("done").build())));
        var result = mvc.perform(post(path).header("Authorization", "Bearer valid").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"hello\"}")).andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(result)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:message")))
                .andExpect(content().string(containsString("event:done")));
    }
}
