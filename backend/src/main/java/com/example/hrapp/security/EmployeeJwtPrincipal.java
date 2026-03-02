package com.example.hrapp.security;

import java.util.Map;

/**
 * Lightweight authenticated principal used across controller authorization expressions
 * and policy filters.
 */
public record EmployeeJwtPrincipal(String employee_id, String subject, Map<String, Object> claims) {
}
