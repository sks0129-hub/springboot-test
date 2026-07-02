package com.example.certificateportal.trace;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class TraceLogInterceptor implements HandlerInterceptor {

    private static final String LOGIN_USER_ID = "loginUserId";
    private static final String SPECIAL_AUTH = "specialAuth";
    private final TraceLogService traceLogService;

    public TraceLogInterceptor(TraceLogService traceLogService) {
        this.traceLogService = traceLogService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        Object userId = recordableUserId(request);
        String action = resolveAction(request);
        if (userId != null && action != null) {
            traceLogService.record(userId.toString(), action);
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) {
        if (!"POST".equals(request.getMethod()) || !"/login".equals(request.getRequestURI())) {
            return;
        }
        if (modelAndView == null || !"redirect:/main".equals(modelAndView.getViewName())) {
            return;
        }
        Object userId = recordableUserId(request);
        if (userId != null) {
            traceLogService.record(userId.toString(), "LOGIN_SUCCESS");
        }
    }

    private Object recordableUserId(HttpServletRequest request) {
        if (request.getSession(false) == null) {
            return null;
        }
        if (Boolean.TRUE.equals(request.getSession(false).getAttribute(SPECIAL_AUTH))) {
            return null;
        }
        return request.getSession(false).getAttribute(LOGIN_USER_ID);
    }

    private String resolveAction(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI().substring(request.getContextPath().length());

        if ("GET".equals(method) && "/main".equals(path)) return "MAIN";
        if ("GET".equals(method) && "/certificate/request".equals(path)) return "CERTIFICATE_REQUEST";
        if ("GET".equals(method) && "/certificate/issue".equals(path)) return "CERTIFICATE_ISSUE";
        if ("GET".equals(method) && "/certificate/requests".equals(path)) return "CERTIFICATE_REQUESTS";
        if ("GET".equals(method) && "/suggestions".equals(path)) return "SUGGESTIONS";
        if ("GET".equals(method) && "/suggestions/new".equals(path)) return "SUGGESTION_CREATE_FORM";
        if ("POST".equals(method) && "/suggestions".equals(path)) return "SUGGESTION_CREATE";
        if ("GET".equals(method) && path.matches("/suggestions/\\d+")) return "SUGGESTION_DETAIL";
        if ("GET".equals(method) && path.matches("/suggestions/\\d+/edit")) return "SUGGESTION_EDIT_FORM";
        if ("POST".equals(method) && path.matches("/suggestions/\\d+")) return "SUGGESTION_UPDATE";
        if ("POST".equals(method) && path.matches("/suggestions/\\d+/delete")) return "SUGGESTION_DELETE";
        if ("GET".equals(method) && "/trace-logs".equals(path)) {
            return "true".equals(request.getParameter("refresh")) ? "TRACE_LOG_REFRESH" : "TRACE_LOGS";
        }
        if ("POST".equals(method) && "/logout".equals(path)) return "LOGOUT";
        return null;
    }
}
