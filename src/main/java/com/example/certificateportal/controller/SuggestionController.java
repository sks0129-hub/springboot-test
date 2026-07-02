package com.example.certificateportal.controller;

import com.example.certificateportal.suggestion.SuggestionCategory;
import com.example.certificateportal.suggestion.SuggestionForm;
import com.example.certificateportal.suggestion.SuggestionPost;
import com.example.certificateportal.suggestion.SuggestionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class SuggestionController {

    private static final String LOGIN_USER_ID = "loginUserId";
    private static final String LOGIN_USER_NAME = "loginUserName";
    private static final String SPECIAL_AUTH = "specialAuth";
    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping("/suggestions")
    public String list(HttpSession session, Model model) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        model.addAttribute("posts", suggestionService.findAll());
        return "suggestions";
    }

    @GetMapping("/suggestions/new")
    public String createForm(HttpSession session, Model model) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        SuggestionForm form = new SuggestionForm();
        form.setCategory(SuggestionCategory.FREE.name());
        prepareForm(model, form, "게시글 작성", "/suggestions");
        return "suggestion-form";
    }

    @PostMapping("/suggestions")
    public String create(@ModelAttribute SuggestionForm form, HttpSession session, Model model) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        FormValues values = validate(form, model);
        if (values == null) {
            prepareForm(model, form, "게시글 작성", "/suggestions");
            return "suggestion-form";
        }

        SuggestionPost post = suggestionService.create(
                values.category(), values.title(), values.content(),
                (String) session.getAttribute(LOGIN_USER_ID),
                (String) session.getAttribute(LOGIN_USER_NAME)
        );
        return "redirect:/suggestions/" + post.id();
    }

    @GetMapping("/suggestions/{id}")
    public String detail(@PathVariable long id, HttpSession session, Model model) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        SuggestionPost post = findPost(id);
        model.addAttribute("post", post);
        model.addAttribute("canManage", canManage(post, session));
        return "suggestion-detail";
    }

    @GetMapping("/suggestions/{id}/edit")
    public String editForm(@PathVariable long id, HttpSession session, Model model) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        SuggestionPost post = findPost(id);
        requireManagePermission(post, session);

        SuggestionForm form = new SuggestionForm();
        form.setCategory(post.category().name());
        form.setTitle(post.title());
        form.setContent(post.content());
        prepareForm(model, form, "게시글 수정", "/suggestions/" + id);
        return "suggestion-form";
    }

    @PostMapping("/suggestions/{id}")
    public String update(@PathVariable long id,
                         @ModelAttribute SuggestionForm form,
                         HttpSession session,
                         Model model) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        SuggestionPost post = findPost(id);
        requireManagePermission(post, session);
        FormValues values = validate(form, model);
        if (values == null) {
            prepareForm(model, form, "게시글 수정", "/suggestions/" + id);
            return "suggestion-form";
        }

        suggestionService.update(id, values.category(), values.title(), values.content());
        return "redirect:/suggestions/" + id;
    }

    @PostMapping("/suggestions/{id}/delete")
    public String delete(@PathVariable long id, HttpSession session) {
        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }
        SuggestionPost post = findPost(id);
        requireManagePermission(post, session);
        suggestionService.delete(id);
        return "redirect:/suggestions";
    }

    private void prepareForm(Model model, SuggestionForm form, String heading, String action) {
        model.addAttribute("form", form);
        model.addAttribute("categories", SuggestionCategory.values());
        model.addAttribute("heading", heading);
        model.addAttribute("formAction", action);
    }

    private FormValues validate(SuggestionForm form, Model model) {
        SuggestionCategory category;
        try {
            category = SuggestionCategory.valueOf(form.getCategory());
        } catch (IllegalArgumentException | NullPointerException exception) {
            model.addAttribute("error", "카테고리를 선택해 주세요.");
            return null;
        }

        String title = form.getTitle() == null ? "" : form.getTitle().strip();
        String content = form.getContent() == null ? "" : form.getContent().strip();
        if (title.isEmpty() || title.length() > 100) {
            model.addAttribute("error", "제목은 1자 이상 100자 이하로 입력해 주세요.");
            return null;
        }
        if (content.isEmpty() || content.length() > 5000) {
            model.addAttribute("error", "내용은 1자 이상 5,000자 이하로 입력해 주세요.");
            return null;
        }
        return new FormValues(category, title, content);
    }

    private SuggestionPost findPost(long id) {
        return suggestionService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "게시글을 찾을 수 없습니다."));
    }

    private boolean canManage(SuggestionPost post, HttpSession session) {
        return suggestionService.canManage(
                post,
                (String) session.getAttribute(LOGIN_USER_ID),
                Boolean.TRUE.equals(session.getAttribute(SPECIAL_AUTH))
        );
    }

    private void requireManagePermission(SuggestionPost post, HttpSession session) {
        if (!canManage(post, session)) {
            throw new ResponseStatusException(FORBIDDEN, "게시글을 수정하거나 삭제할 권한이 없습니다.");
        }
    }

    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute(LOGIN_USER_ID) != null;
    }

    private record FormValues(SuggestionCategory category, String title, String content) {
    }
}
