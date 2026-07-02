package com.example.certificateportal.suggestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SuggestionServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void deletedIdsAreNotReusedAndNextIdSurvivesRestart() {
        Path storage = tempDirectory.resolve("suggestions.json");
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        SuggestionService service = new SuggestionService(storage.toString(), objectMapper);

        service.create(SuggestionCategory.FREE, "첫 글", "내용", "user1", "사용자1");
        service.create(SuggestionCategory.SUGGESTION, "둘째 글", "내용", "user1", "사용자1");
        SuggestionPost third = service.create(
                SuggestionCategory.FREE, "셋째 글", "내용", "user2", "사용자2"
        );
        service.delete(third.id());

        SuggestionPost fourth = service.create(
                SuggestionCategory.SUGGESTION, "넷째 글", "내용", "user2", "사용자2"
        );
        assertThat(fourth.id()).isEqualTo(4);

        SuggestionService restartedService = new SuggestionService(storage.toString(), objectMapper);
        SuggestionPost fifth = restartedService.create(
                SuggestionCategory.FREE, "다섯째 글", "내용", "user1", "사용자1"
        );

        assertThat(fifth.id()).isEqualTo(5);
        assertThat(restartedService.findAll()).extracting(SuggestionPost::id)
                .containsExactly(5L, 4L, 2L, 1L);
    }

    @Test
    void onlyAuthorOrAdministratorCanManagePost() {
        SuggestionService service = new SuggestionService(
                tempDirectory.resolve("authority.json").toString(),
                new ObjectMapper().findAndRegisterModules()
        );
        SuggestionPost post = service.create(
                SuggestionCategory.FREE, "권한 테스트", "내용", "writer", "작성자"
        );

        assertThat(service.canManage(post, "writer", false)).isTrue();
        assertThat(service.canManage(post, "other", false)).isFalse();
        assertThat(service.canManage(post, "admin", true)).isTrue();
    }
}
