package com.example.hrapp.security;

import com.example.hrapp.employee.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.Set;

/**
 * Converts Keycloak JWTs into Spring Security authentication tokens.
 *
 * <p>This converter maps realm roles to {@code ROLE_*} authorities and builds a custom
 * {@link EmployeeJwtPrincipal} containing both normalized employee identity and raw claims for
 * downstream authorization and policy filters.</p>
 */
@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger log = LoggerFactory.getLogger(KeycloakJwtAuthenticationConverter.class);

    private final EmployeeRepository employeeRepository;

    public KeycloakJwtAuthenticationConverter(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    /**
     * Creates an authenticated principal + authorities from a validated JWT.
     */
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.addAll(extractRealmRoles(jwt));

        String employeeId = resolveEmployeeId(jwt);
        EmployeeJwtPrincipal principal = new EmployeeJwtPrincipal(employeeId, jwt.getSubject(), jwt.getClaims());
        return new UsernamePasswordAuthenticationToken(principal, jwt.getTokenValue(), authorities);
    }

    private String resolveEmployeeId(Jwt jwt) {
        // Preferred source: explicit employee_id claim.
        return Optional.ofNullable(readEmployeeIdClaim(jwt))
            .filter(Predicate.not(String::isBlank))
            // Compatibility fallback: derive from username prefix if it looks like EMP-xxxx.
            .or(() -> Optional.ofNullable(deriveEmployeeIdFromUsername(jwt.getClaimAsString("preferred_username"))))
            // Final fallback for legacy principals: map email to employee record.
            .or(() -> Optional.ofNullable(readEmployeeIdFromEmail(jwt))
                .filter(Predicate.not(String::isBlank)))
            .orElse(null);
    }

    private String readEmployeeIdClaim(Jwt jwt) {
        Object claim = jwt.getClaim("employee_id");
        if (claim instanceof String claimValue) {
            return claimValue.trim();
        }
        if (claim instanceof Collection<?> claimValues) {
            return claimValues.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .map(String::trim)
                .filter(Predicate.not(String::isBlank))
                .findFirst()
                .orElse(null);
        }
        return null;
    }

    private String readEmployeeIdFromEmail(Jwt jwt) {
        try {
            return Optional.ofNullable(jwt.getClaimAsString("email"))
                .map(String::trim)
                .filter(Predicate.not(String::isBlank))
                .flatMap(employeeRepository::findByEmailAddressIgnoreCase)
                .map(employee -> employee.getEmployeeId())
                .orElse(null);
        } catch (RuntimeException exception) {
            String subject = jwt.getSubject();
            Object rawEmail = jwt.getClaims().get("email");
            log.warn("Failed to resolve employeeId by email fallback for subject={} email={}", subject, rawEmail, exception);
            return null;
        }
    }

    private String deriveEmployeeIdFromUsername(String preferredUsername) {
        if (preferredUsername == null || preferredUsername.isBlank()) {
            return null;
        }

        int atSymbolIndex = preferredUsername.indexOf('@');
        String candidate = atSymbolIndex > 0
            ? preferredUsername.substring(0, atSymbolIndex)
            : preferredUsername;

        candidate = candidate.trim();
        if (candidate.isBlank() || !candidate.contains("-")) {
            return null;
        }

        if (!candidate.chars().allMatch(character -> Character.isLetterOrDigit(character) || character == '-')) {
            return null;
        }

        return candidate.toUpperCase();
    }

    private Collection<? extends GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Object realmAccessObj = jwt.getClaim("realm_access");
        if (!(realmAccessObj instanceof Map<?, ?> realmAccessMap)) {
            return Set.of();
        }

        Object rolesObj = realmAccessMap.get("roles");
        if (!(rolesObj instanceof Collection<?> roles)) {
            return Set.of();
        }

        return roles.stream()
            .filter(Objects::nonNull)
            .map(Object::toString)
            .map(this::toRoleAuthority)
            .map(SimpleGrantedAuthority::new)
            .toList();
    }

    private String toRoleAuthority(String role) {
        String roleFormat = role.startsWith("ROLE_") ? "prefixed" : "plain";
        return switch (roleFormat) {
            case "prefixed" -> role;
            case "plain" -> "ROLE_" + role;
            default -> throw new IllegalStateException("Unexpected role format: " + roleFormat);
        };
    }
}
