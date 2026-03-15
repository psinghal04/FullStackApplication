package com.example.hrapp.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filter that validates BFF session cookies and sets up authentication.
 * Runs after bearer token authentication as a fallback.
 */
@Component
public class BffSessionAuthenticationFilter extends OncePerRequestFilter {
    private static final String SESSION_COOKIE_NAME = "BFF_SESSION_ID";

    private final BffSessionService sessionService;

    public BffSessionAuthenticationFilter(BffSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        // Only process if not already authenticated
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract session ID from cookie
        Optional<String> sessionId = extractSessionCookie(request);
        
        if (sessionId.isPresent()) {
            Optional<BffSession> session = sessionService.validateSession(sessionId.get());
            
            if (session.isPresent()) {
                // Create principal and authentication token
                BffSessionPrincipal principal = new BffSessionPrincipal(session.get());
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                    );
                
                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> extractSessionCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        for (Cookie cookie : cookies) {
            if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                return Optional.of(cookie.getValue());
            }
        }

        return Optional.empty();
    }
}
