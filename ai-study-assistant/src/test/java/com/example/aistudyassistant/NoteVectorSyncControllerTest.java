package com.example.aistudyassistant;
import com.example.aistudyassistant.controller.NoteVectorSyncController;
import com.example.aistudyassistant.mapper.NoteVectorSyncTaskMapper;
import com.example.aistudyassistant.security.JwtInterceptor;
import com.example.aistudyassistant.service.JwtService;
import com.example.aistudyassistant.service.NoteVectorSyncTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NoteVectorSyncControllerTest {
    @Test void endpointsRequireTokenAndScopeRequestsToAuthenticatedUser() throws Exception {
        var tasks=mock(NoteVectorSyncTaskService.class); var mapper=mock(NoteVectorSyncTaskMapper.class);
        var jwt=mock(JwtService.class); when(jwt.parseUserId("valid")).thenReturn(7L);
        var mvc=MockMvcBuilders.standaloneSetup(new NoteVectorSyncController(tasks,mapper))
                .addInterceptors(new JwtInterceptor(jwt)).build();
        mvc.perform(post("/notes/12/vector-sync")).andExpect(status().isUnauthorized());
        verifyNoInteractions(tasks,mapper);
        when(tasks.schedule(7L,12L,NoteVectorSyncTaskService.Action.UPSERT)).thenReturn(false,true);
        mvc.perform(post("/notes/12/vector-sync").header("Authorization","Bearer valid"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/notes/12/vector-sync").header("Authorization","Bearer valid"))
                .andExpect(status().isAccepted());
        when(tasks.find(7L,12L)).thenReturn(Optional.empty());
        mvc.perform(get("/notes/12/vector-sync").header("Authorization","Bearer valid"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/notes/12/vector-sync/retry").header("Authorization","Bearer valid"))
                .andExpect(status().isNotFound());
        verify(mapper).retry(7L,12L);
    }
}
