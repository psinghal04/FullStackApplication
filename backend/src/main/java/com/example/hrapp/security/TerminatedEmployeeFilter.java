package com.example.hrapp.security;

import com.example.hrapp.employee.EmployeeRepository;
import com.example.hrapp.identity.KeycloakAdminClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

@Component
public class TerminatedEmployeeFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TerminatedEmployeeFilter.class);

    private final EmployeeRepository employeeRepository;
    private final KeycloakAdminClient keycloakAdminClient;
    private final ObjectMapper objectMapper;

    public TerminatedEmployeeFilter(
        EmployeeRepository employeeRepository,
        KeycloakAdminClient keycloakAdminClient,
        ObjectMapper objectMapper
    ) {
        this.employeeRepository = employeeRepository;
        this.keycloakAdminClient = keycloakAdminClient;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        Object principalObj = authentication.getPrincipal();
        if (!(principalObj instanceof EmployeeJwtPrincipal principal)) {
            filterChain.doFilter(request, response);
            return;
        }

        String employeeId = principal.getEmployee_id();
        if (employeeId == null || employeeId.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        LocalDate today = LocalDate.now();
        boolean terminated = employeeRepository.findByEmployeeId(employeeId)
            .map(employee -> employee.getDateOfTermination() != null && !employee.getDateOfTermination().isAfter(today))
            .orElse(false);

        if (terminated) {
            String email = principal.getClaims().get("email") instanceof String claimEmail
                ? claimEmail
                : null;

            if (email != null && !email.isBlank()) {
                keycloakAdminClient.setUserEnabledByEmail(email, false);
            }

            log.warn(
                "Denied request for terminated employeeId={} path={} method={}",
                employeeId,
                request.getRequestURI(),
                request.getMethod()
            );

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            Map<String, Object> payload = Map.of(
                "status", 403,
                "message", "Employee terminated",
                "reason", "terminated"
            );
            objectMapper.writeValue(response.getWriter(), payload);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
