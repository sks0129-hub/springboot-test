package com.example.certificateportal.suggestion;

import java.time.LocalDateTime;

public record SuggestionPost(
        long id,
        SuggestionCategory category,
        String title,
        String content,
        LocalDateTime createdAt,
        String authorId,
        String authorName
) {
}
