package com.example.certificateportal.trace;

import java.time.LocalDateTime;

public record TraceLogEntry(LocalDateTime timestamp, String userId, String action) {
}
