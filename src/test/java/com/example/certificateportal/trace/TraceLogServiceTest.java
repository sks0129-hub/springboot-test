package com.example.certificateportal.trace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class TraceLogServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void writesDailyFileWithRequiredFormatAndFiltersByHour() throws Exception {
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-02T05:35:12Z"),
                ZoneId.of("Asia/Seoul")
        );
        TraceLogService service = new TraceLogService(tempDirectory.toString(), clock);

        service.record("gdhong", "SUGGESTIONS");

        Path logFile = tempDirectory.resolve("traceout_2026-07-02.log");
        assertThat(logFile).exists();
        assertThat(Files.readString(logFile, StandardCharsets.UTF_8))
                .isEqualTo("2026-07-02 14:35:12 / gdhong / SUGGESTIONS" + System.lineSeparator());
        assertThat(service.find(LocalDate.of(2026, 7, 2), 14, 14))
                .containsExactly(new TraceLogEntry(
                        java.time.LocalDateTime.of(2026, 7, 2, 14, 35, 12),
                        "gdhong",
                        "SUGGESTIONS"
                ));
        assertThat(service.find(LocalDate.of(2026, 7, 2), 15, 23)).isEmpty();
    }

    @Test
    void filtersByExactUserIdAndTreatsBlankAsAllUsers() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-02T01:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        TraceLogService service = new TraceLogService(tempDirectory.toString(), clock);
        service.record("yoojw", "MAIN");
        service.record("gdhong", "SUGGESTIONS");

        LocalDate date = LocalDate.of(2026, 7, 2);
        assertThat(service.find(date, 0, 23, ""))
                .extracting(TraceLogEntry::userId)
                .containsExactly("yoojw", "gdhong");
        assertThat(service.find(date, 0, 23, "  yoojw  "))
                .extracting(TraceLogEntry::userId)
                .containsExactly("yoojw");
        assertThat(service.find(date, 0, 23, "yoo%" )).isEmpty();
        assertThat(service.find(date, 0, 23, "*" )).isEmpty();
    }
}
