package com.example.aistudyassistant;
import com.example.aistudyassistant.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudyAssistantToolsTest {
    @Test void toolsUseOnlyAuthenticatedServerContext() {
        var search=mock(NoteChunkSearchService.class);
        var wrong=mock(WrongQuizService.class);
        var stats=mock(QuizStatisticsService.class);
        var tools=new StudyAssistantTools(search, wrong, stats);
        var context=new ToolContext(Map.of("userId", 42L));
        tools.searchNotes("Java", context);
        tools.recentWrongQuestions(context);
        tools.quizStatistics(context);
        verify(search).search(42L, "Java");
        verify(wrong).findWrongQuestions(42L, false);
        verify(stats).statistics(42L);
    }
    @Test void missingIdentityIsRejectedBeforeAccessingData() {
        var search=mock(NoteChunkSearchService.class);
        var wrong=mock(WrongQuizService.class);
        var stats=mock(QuizStatisticsService.class);
        var tools=new StudyAssistantTools(search, wrong, stats);
        var context=new ToolContext(Map.of());
        assertThrows(IllegalStateException.class, () -> tools.searchNotes("Java", context));
        assertThrows(IllegalStateException.class, () -> tools.recentWrongQuestions(context));
        assertThrows(IllegalStateException.class, () -> tools.quizStatistics(context));
        verifyNoInteractions(search, wrong, stats);
    }
}
