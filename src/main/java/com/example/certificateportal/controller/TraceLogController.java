package com.example.certificateportal.controller;

import com.example.certificateportal.trace.TraceLogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

@Controller
public class TraceLogController {

    private static final String LOGIN_USER_ID = "loginUserId";
    private static final String SPECIAL_AUTH = "specialAuth";
    private final TraceLogService traceLogService;

    public TraceLogController(TraceLogService traceLogService) {
        this.traceLogService = traceLogService;
    }

    @GetMapping("/trace-logs")
    public String traceLogs(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int startHour,
            @RequestParam(defaultValue = "23") int endHour,
            @RequestParam(defaultValue = "") String userId,
            @RequestParam(defaultValue = "false") boolean refresh,
            HttpSession session,
            Model model
    ) {
        if (session.getAttribute(LOGIN_USER_ID) == null) {
            return "redirect:/login";
        }
        if (!Boolean.TRUE.equals(session.getAttribute(SPECIAL_AUTH))) {
            return "redirect:/main";
        }

        LocalDate selectedDate = date == null ? LocalDate.now() : date;
        int selectedStartHour = Math.max(0, Math.min(23, startHour));
        int selectedEndHour = Math.max(0, Math.min(23, endHour));
        String selectedUserId = userId.strip();
        if (selectedStartHour > selectedEndHour) {
            model.addAttribute("error", "시작 시간은 종료 시간보다 늦을 수 없습니다.");
        }

        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("startHour", selectedStartHour);
        model.addAttribute("endHour", selectedEndHour);
        model.addAttribute("userId", selectedUserId);
        model.addAttribute("hours", IntStream.range(0, 24).boxed().toList());
        model.addAttribute("refreshed", refresh);
        model.addAttribute(
                "entries",
                refresh && selectedStartHour <= selectedEndHour
                        ? traceLogService.find(
                                selectedDate, selectedStartHour, selectedEndHour, selectedUserId
                        )
                        : List.of()
        );
        return "trace-logs";
    }
}
