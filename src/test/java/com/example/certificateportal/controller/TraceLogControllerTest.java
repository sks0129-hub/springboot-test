package com.example.certificateportal.controller;

import com.example.certificateportal.trace.TraceLogEntry;
import com.example.certificateportal.trace.TraceLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(TraceLogController.class)
class TraceLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TraceLogService traceLogService;

    @Test
    void userCannotAccessTraceLogPage() throws Exception {
        mockMvc.perform(get("/trace-logs")
                        .sessionAttr("loginUserId", "yoojw")
                        .sessionAttr("specialAuth", false))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main"));
    }

    @Test
    void administratorCanRefreshLogsForSelectedDateAndHours() throws Exception {
        LocalDate date = LocalDate.of(2026, 7, 2);
        given(traceLogService.find(date, 9, 17, "yoojw")).willReturn(List.of(
                new TraceLogEntry(LocalDateTime.of(2026, 7, 2, 10, 30), "yoojw", "SUGGESTIONS")
        ));

        mockMvc.perform(get("/trace-logs")
                        .sessionAttr("loginUserId", "admin")
                        .sessionAttr("loginUserName", "admin")
                        .sessionAttr("specialAuth", true)
                        .param("date", "2026-07-02")
                        .param("startHour", "9")
                        .param("endHour", "17")
                        .param("userId", "yoojw")
                        .param("refresh", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("trace-logs"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SUGGESTIONS")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("yoojw")));

        verify(traceLogService).find(date, 9, 17, "yoojw");
    }

    @Test
    void blankUserIdRequestsLogsForEveryUser() throws Exception {
        LocalDate date = LocalDate.of(2026, 7, 2);

        mockMvc.perform(get("/trace-logs")
                        .sessionAttr("loginUserId", "admin")
                        .sessionAttr("specialAuth", true)
                        .param("date", "2026-07-02")
                        .param("startHour", "0")
                        .param("endHour", "23")
                        .param("userId", "   ")
                        .param("refresh", "true"))
                .andExpect(status().isOk());

        verify(traceLogService).find(date, 0, 23, "");
    }

    @Test
    void invalidHourRangeDisplaysErrorWithoutReadingFile() throws Exception {
        mockMvc.perform(get("/trace-logs")
                        .sessionAttr("loginUserId", "admin")
                        .sessionAttr("specialAuth", true)
                        .param("date", "2026-07-02")
                        .param("startHour", "18")
                        .param("endHour", "9")
                        .param("refresh", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "시작 시간은 종료 시간보다 늦을 수 없습니다."
                )));
    }
}
