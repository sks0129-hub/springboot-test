package com.example.certificateportal.controller;

import com.example.certificateportal.suggestion.SuggestionCategory;
import com.example.certificateportal.suggestion.SuggestionPost;
import com.example.certificateportal.suggestion.SuggestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(SuggestionController.class)
class SuggestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SuggestionService suggestionService;

    @Test
    void boardIsAvailableToBothUserAndAdministrator() throws Exception {
        given(suggestionService.findAll()).willReturn(List.of(postOwnedBy("writer")));

        mockMvc.perform(get("/suggestions")
                        .sessionAttr("loginUserId", "user")
                        .sessionAttr("specialAuth", false))
                .andExpect(status().isOk())
                .andExpect(view().name("suggestions"));

        mockMvc.perform(get("/suggestions")
                        .sessionAttr("loginUserId", "admin")
                        .sessionAttr("specialAuth", true))
                .andExpect(status().isOk())
                .andExpect(view().name("suggestions"));
    }

    @Test
    void unauthenticatedUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/suggestions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void loggedInUserCanCreatePostWithAllowedCategory() throws Exception {
        SuggestionPost created = postOwnedBy("user1");
        given(suggestionService.create(
                eq(SuggestionCategory.SUGGESTION), eq("개선 요청"), eq("내용입니다."),
                eq("user1"), eq("사용자1")
        )).willReturn(created);

        mockMvc.perform(post("/suggestions")
                        .sessionAttr("loginUserId", "user1")
                        .sessionAttr("loginUserName", "사용자1")
                        .sessionAttr("specialAuth", false)
                        .param("category", "SUGGESTION")
                        .param("title", "개선 요청")
                        .param("content", "내용입니다."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/suggestions/1"));
    }

    @Test
    void invalidCategoryIsRejected() throws Exception {
        mockMvc.perform(post("/suggestions")
                        .sessionAttr("loginUserId", "user1")
                        .sessionAttr("loginUserName", "사용자1")
                        .param("category", "OTHER")
                        .param("title", "제목")
                        .param("content", "내용"))
                .andExpect(status().isOk())
                .andExpect(view().name("suggestion-form"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void authorAndAdministratorCanEditPost() throws Exception {
        SuggestionPost post = postOwnedBy("writer");
        given(suggestionService.findById(1)).willReturn(Optional.of(post));
        given(suggestionService.canManage(post, "writer", false)).willReturn(true);
        given(suggestionService.canManage(post, "admin", true)).willReturn(true);

        mockMvc.perform(get("/suggestions/1/edit")
                        .sessionAttr("loginUserId", "writer")
                        .sessionAttr("specialAuth", false))
                .andExpect(status().isOk())
                .andExpect(view().name("suggestion-form"));

        mockMvc.perform(get("/suggestions/1/edit")
                        .sessionAttr("loginUserId", "admin")
                        .sessionAttr("specialAuth", true))
                .andExpect(status().isOk())
                .andExpect(view().name("suggestion-form"));
    }

    @Test
    void anotherUserCannotEditOrDeletePost() throws Exception {
        SuggestionPost post = postOwnedBy("writer");
        given(suggestionService.findById(1)).willReturn(Optional.of(post));
        given(suggestionService.canManage(post, "other", false)).willReturn(false);

        mockMvc.perform(get("/suggestions/1/edit")
                        .sessionAttr("loginUserId", "other")
                        .sessionAttr("specialAuth", false))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/suggestions/1/delete")
                        .sessionAttr("loginUserId", "other")
                        .sessionAttr("specialAuth", false))
                .andExpect(status().isForbidden());
    }

    @Test
    void authorCanDeletePost() throws Exception {
        SuggestionPost post = postOwnedBy("writer");
        given(suggestionService.findById(1)).willReturn(Optional.of(post));
        given(suggestionService.canManage(post, "writer", false)).willReturn(true);

        mockMvc.perform(post("/suggestions/1/delete")
                        .sessionAttr("loginUserId", "writer")
                        .sessionAttr("specialAuth", false))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/suggestions"));

        verify(suggestionService).delete(1);
    }

    private SuggestionPost postOwnedBy(String authorId) {
        return new SuggestionPost(
                1, SuggestionCategory.FREE, "제목", "내용",
                LocalDateTime.of(2026, 7, 2, 12, 0), authorId, "작성자"
        );
    }
}
