package com.example.aistudyassistant;
import com.example.aistudyassistant.dto.WrongQuizResponse;
import com.example.aistudyassistant.mapper.StudyTaskMapper;
import com.example.aistudyassistant.service.StudyTaskService;
import com.example.aistudyassistant.service.WrongQuizService;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudyTaskServiceTest {
    @Test void generationSelectsOnlyCurrentUsersUnresolvedWrongQuestions() {
        var mapper=mock(StudyTaskMapper.class); var wrong=mock(WrongQuizService.class);
        var service=new StudyTaskService(mapper, wrong); var date=LocalDate.of(2026,11,16);
        when(wrong.findWrongQuestions(7L, false)).thenReturn(List.of(new WrongQuizResponse(4L, 5L, "题目", List.of(), false)));
        service.generate(7L, date);
        verify(mapper).add(7L, 4L, date);
        verify(mapper).findByDate(7L, date);
    }
    @Test void updateScopesTaskToUser() {
        var mapper=mock(StudyTaskMapper.class); var service=new StudyTaskService(mapper, mock(WrongQuizService.class));
        when(mapper.setCompleted(8L, 99L, true)).thenReturn(0);
        assertFalse(service.setCompleted(8L, 99L, true));
        verify(mapper).setCompleted(8L, 99L, true);
    }
}
