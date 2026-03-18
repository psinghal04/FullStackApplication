package com.example.hrapp.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Resolves the externally visible request origin when the application is behind a proxy.
 */
@Component
public class RequestOriginResolver {

    public String resolveBaseUrl(HttpServletRequest request) {
        String scheme = firstNonBlank(request.getHeader("X-Forwarded-Proto"), request.getScheme());
        String host = firstNonBlank(request.getHeader("X-Forwarded-Host"), request.getHeader("Host"));
        return scheme + "://" + host;
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }
}
