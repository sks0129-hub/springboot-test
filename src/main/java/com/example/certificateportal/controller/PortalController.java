package com.example.certificateportal.controller;

import com.example.certificateportal.employee.Employee;
import com.example.certificateportal.employee.EmployeeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class PortalController {

    private static final String LOGIN_USER_ID = "loginUserId";
    private static final String LOGIN_USER_NAME = "loginUserName";
    private static final String LOGIN_USER = "loginUser";
    private final EmployeeService employeeService;

    public PortalController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping({"/", "/login"})
    public String loginPage(HttpSession session) {
        return isLoggedIn(session) ? "redirect:/main" : "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        var authenticatedEmployee = employeeService.authenticate(username, password);
        if (authenticatedEmployee.isPresent()) {
            Employee employee = authenticatedEmployee.get();
            session.setAttribute(LOGIN_USER_ID, employee.userId());
            session.setAttribute(LOGIN_USER_NAME, employee.name());
            session.setAttribute(LOGIN_USER, employee.name());
            return "redirect:/main";
        }

        model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
        return "login";
    }

    @GetMapping("/main")
    public String main(HttpSession session) {
        return protectedPage(session, "main");
    }

    @GetMapping("/employee.csv")
    public String blockEmployeeCsv() {
        throw new ResponseStatusException(NOT_FOUND);
    }

    @GetMapping("/certificate/request")
    public String certificateRequest(HttpSession session) {
        return protectedPage(session, "certificate-request");
    }

    @GetMapping("/certificate/issue")
    public String certificateIssue(HttpSession session) {
        return protectedPage(session, "certificate-issue");
    }

    @GetMapping("/suggestions")
    public String suggestions(HttpSession session) {
        return protectedPage(session, "suggestions");
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    private String protectedPage(HttpSession session, String viewName) {
        return isLoggedIn(session) ? viewName : "redirect:/login";
    }

    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute(LOGIN_USER_ID) != null;
    }
}
