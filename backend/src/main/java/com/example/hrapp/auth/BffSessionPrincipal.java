package com.example.hrapp.auth;

import com.example.hrapp.security.JwtClaimNames;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Custom principal for BFF session-based authentication.
 * Implements OAuth2User for compatibility with Spring Security.
 */
public class BffSessionPrincipal implements OAuth2User {
    private final BffSession session;
    private final List<GrantedAuthority> authorities;

    public BffSessionPrincipal(BffSession session) {
        this.session = session;
        this.authorities = session.roles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .map(authority -> (GrantedAuthority) authority)
            .toList();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of(
            JwtClaimNames.EMPLOYEE_ID, session.employeeId() != null ? session.employeeId() : "",
            "username", session.username(),
            JwtClaimNames.EMAIL, session.email() != null ? session.email() : "",
            JwtClaimNames.ROLES, session.roles()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return session.username();
    }

    public String employeeId() {
        return session.employeeId();
    }

    /**
     * Alias for {@link #employeeId()} using the underscore convention required by
     * {@code @PreAuthorize} SpEL expressions referencing
     * {@code authentication.principal.employee_id}.
     */
    public String employee_id() {
        return session.employeeId();
    }

    public String username() {
        return session.username();
    }

    public String email() {
        return session.email();
    }

    public List<String> roles() {
        return session.roles();
    }

    public BffSession getSession() {
        return session;
    }
}
