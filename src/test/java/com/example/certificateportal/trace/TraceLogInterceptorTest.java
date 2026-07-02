package com.example.certificateportal.trace;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class TraceLogInterceptorTest {

    private final TraceLogService traceLogService = mock(TraceLogService.class);
    private final TraceLogInterceptor interceptor = new TraceLogInterceptor(traceLogService);

    @Test
    void recordsLoggedInUsersMenuAndActionInEnglish() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/suggestions/7/delete");
        request.getSession().setAttribute("loginUserId", "yoojw");

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        verify(traceLogService).record("yoojw", "SUGGESTION_DELETE");
    }

    @Test
    void doesNotRecordAnonymousRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/main");

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        verifyNoInteractions(traceLogService);
    }

    @Test
    void doesNotRecordAdministratorAction() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/trace-logs");
        request.setParameter("refresh", "true");
        request.getSession().setAttribute("loginUserId", "admin");
        request.getSession().setAttribute("specialAuth", true);

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        verifyNoInteractions(traceLogService);
    }

    @Test
    void recordsSuccessfulLoginAfterSessionIsCreated() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.getSession().setAttribute("loginUserId", "gdhong");
        request.getSession().setAttribute("specialAuth", false);

        interceptor.postHandle(
                request,
                new MockHttpServletResponse(),
                new Object(),
                new ModelAndView("redirect:/main")
        );

        verify(traceLogService).record("gdhong", "LOGIN_SUCCESS");
    }

    @Test
    void doesNotRecordAdministratorLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.getSession().setAttribute("loginUserId", "admin");
        request.getSession().setAttribute("specialAuth", true);

        interceptor.postHandle(
                request,
                new MockHttpServletResponse(),
                new Object(),
                new ModelAndView("redirect:/main")
        );

        verifyNoInteractions(traceLogService);
    }
}
