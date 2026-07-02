package com.example.certificateportal.trace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class TraceLogService {

    private static final Logger log = LoggerFactory.getLogger(TraceLogService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private final Path logDirectory;
    private final Clock clock;

    @Autowired
    public TraceLogService(
            @Value("${portal.trace-log.directory:src/main/resources/log}") String logDirectory
    ) {
        this(logDirectory, Clock.systemDefaultZone());
    }

    TraceLogService(String logDirectory, Clock clock) {
        this.logDirectory = Path.of(logDirectory).toAbsolutePath().normalize();
        this.clock = clock;
    }

    public synchronized void record(String userId, String action) {
        LocalDateTime timestamp = LocalDateTime.now(clock);
        String line = "%s / %s / %s%n".formatted(
                timestamp.format(TIMESTAMP_FORMAT), clean(userId), clean(action)
        );
        Path logFile = resolveLogFile(timestamp.toLocalDate());

        try {
            Files.createDirectories(logDirectory);
            Files.writeString(
                    logFile,
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            log.error("행위추적 로그를 저장하지 못했습니다: {}", logFile, exception);
        }
    }

    public synchronized List<TraceLogEntry> find(LocalDate date, int startHour, int endHour) {
        return find(date, startHour, endHour, "");
    }

    public synchronized List<TraceLogEntry> find(LocalDate date,
                                                 int startHour,
                                                 int endHour,
                                                 String userId) {
        Path logFile = resolveLogFile(date);
        if (Files.notExists(logFile)) {
            return List.of();
        }

        String exactUserId = userId == null ? "" : userId.strip();
        List<TraceLogEntry> entries = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(logFile, StandardCharsets.UTF_8)) {
                parse(line).filter(entry -> {
                    int hour = entry.timestamp().getHour();
                    boolean withinHours = hour >= startHour && hour <= endHour;
                    boolean userMatches = exactUserId.isEmpty() || entry.userId().equals(exactUserId);
                    return withinHours && userMatches;
                }).ifPresent(entries::add);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("행위추적 로그를 읽을 수 없습니다.", exception);
        }
        return List.copyOf(entries);
    }

    Path resolveLogFile(LocalDate date) {
        return logDirectory.resolve("traceout_" + date.format(FILE_DATE_FORMAT) + ".log");
    }

    private java.util.Optional<TraceLogEntry> parse(String line) {
        String[] values = line.split(" / ", 3);
        if (values.length != 3) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(new TraceLogEntry(
                    LocalDateTime.parse(values[0], TIMESTAMP_FORMAT), values[1], values[2]
            ));
        } catch (DateTimeParseException exception) {
            log.warn("형식이 올바르지 않은 행위추적 로그를 건너뜁니다: {}", line);
            return java.util.Optional.empty();
        }
    }

    private String clean(String value) {
        return value == null ? "unknown" : value.replace('\r', ' ').replace('\n', ' ').replace(" / ", "-");
    }
}
