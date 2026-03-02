package com.example.hrapp.security;

import com.example.hrapp.employee.EmployeeRepository;
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
import java.util.Set;

@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final EmployeeRepository employeeRepository;

    public KeycloakJwtAuthenticationConverter(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.addAll(extractRealmRoles(jwt));

        String employeeId = resolveEmployeeId(jwt);
        EmployeeJwtPrincipal principal = new EmployeeJwtPrincipal(employeeId, jwt.getSubject(), jwt.getClaims());
        return new UsernamePasswordAuthenticationToken(principal, jwt.getTokenValue(), authorities);
    }

    private String resolveEmployeeId(Jwt jwt) {
        String employeeId = jwt.getClaimAsString("employee_id");
        if (employeeId != null && !employeeId.isBlank()) {
            return employeeId;
        }

        String preferredUsername = jwt.getClaimAsString("preferred_username");
        String usernameDerived = deriveEmployeeIdFromUsername(preferredUsername);
        if (usernameDerived != null) {
            return usernameDerived;
        }

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            return null;
        }

        return employeeRepository.findByEmailAddressIgnoreCase(email.trim())
            .map(employee -> employee.getEmployeeId())
            .orElse(null);
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

    @SuppressWarnings("unchecked")
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
        if (role.startsWith("ROLE_")) {
            return role;
        }
        return "ROLE_" + role;
    }
}
