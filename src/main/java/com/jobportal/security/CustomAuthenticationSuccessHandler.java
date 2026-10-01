package com.jobportal.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // Return the user to the page they were trying to reach (e.g. a job they wanted to apply for),
        // as long as it is a page their role is allowed to see.
        String next = request.getParameter("next");
        if (next != null && next.startsWith("/") && !next.startsWith("//") && !next.startsWith("/\\") && !next.startsWith("/api/")
                && isAllowedFor(next, authentication)) {
            response.sendRedirect(request.getContextPath() + next);
            return;
        }

        SavedRequest saved = requestCache.getRequest(request, response);
        if (saved != null) {
            requestCache.removeRequest(request, response);
            String target = saved.getRedirectUrl();
            if (target != null && !target.contains("/api/") && isAllowedFor(target, authentication)) {
                response.sendRedirect(target);
                return;
            }
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_ADMIN".equals(role)) {
                response.sendRedirect(request.getContextPath() + "/admin/dashboard");
                return;
            } else if ("ROLE_RECRUITER".equals(role)) {
                response.sendRedirect(request.getContextPath() + "/recruiter/dashboard");
                return;
            } else if ("ROLE_JOB_SEEKER".equals(role)) {
                response.sendRedirect(request.getContextPath() + "/seeker/dashboard");
                return;
            }
        }
        response.sendRedirect(request.getContextPath() + "/");
    }

    private boolean isAllowedFor(String url, Authentication authentication) {
        String required = null;
        if (url.contains("/seeker/")) required = "ROLE_JOB_SEEKER";
        else if (url.contains("/recruiter/")) required = "ROLE_RECRUITER";
        else if (url.contains("/admin/")) required = "ROLE_ADMIN";
        if (required == null) return true;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (required.equals(authority.getAuthority())) return true;
        }
        return false;
    }
}
