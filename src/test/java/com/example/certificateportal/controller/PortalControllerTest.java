package com.example.certificateportal.controller;

import com.example.certificateportal.employee.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(PortalController.class)
@Import(EmployeeService.class)
class PortalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginPageIsDisplayed() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void validEmployeeLoginRedirectsToMainAndStoresEmployeeName() throws Exception {
        mockMvc.perform(post("/login").param("username", "yoojw").param("password", "1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main"))
                .andExpect(request().sessionAttribute("loginUserId", "yoojw"))
                .andExpect(request().sessionAttribute("loginUserName", "유지완"));
    }

    @Test
    void invalidLoginDisplaysError() throws Exception {
        mockMvc.perform(post("/login").param("username", "user").param("password", "wrong"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void protectedPageRedirectsWithoutLogin() throws Exception {
        mockMvc.perform(get("/main"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void menuPagesAreAccessibleAfterLogin() throws Exception {
        mockMvc.perform(get("/certificate/request").sessionAttr("loginUserId", "yoojw"))
                .andExpect(status().isOk()).andExpect(view().name("certificate-request"));
        mockMvc.perform(get("/certificate/issue").sessionAttr("loginUserId", "yoojw"))
                .andExpect(status().isOk()).andExpect(view().name("certificate-issue"));
        mockMvc.perform(get("/suggestions").sessionAttr("loginUserId", "yoojw"))
                .andExpect(status().isOk()).andExpect(view().name("suggestions"));
    }

    @Test
    void mainPageDisplaysEmployeeName() throws Exception {
        mockMvc.perform(get("/main")
                        .sessionAttr("loginUserId", "gdhong")
                        .sessionAttr("loginUserName", "홍길동"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("홍길동")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("님 환영합니다.")));
    }

    @Test
    void employeeCsvIsNotPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/employee.csv"))
                .andExpect(status().isNotFound());
    }

    @Test
    void logoutRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/logout").sessionAttr("loginUserId", "admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
