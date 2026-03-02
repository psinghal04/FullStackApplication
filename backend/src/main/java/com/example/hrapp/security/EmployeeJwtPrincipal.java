package com.example.hrapp.security;

import java.util.Map;

public class EmployeeJwtPrincipal {

    private final String employee_id;
    private final String subject;
    private final Map<String, Object> claims;

    public EmployeeJwtPrincipal(String employeeId, String subject, Map<String, Object> claims) {
        this.employee_id = employeeId;
        this.subject = subject;
        this.claims = claims;
    }

    public String getEmployee_id() {
        return employee_id;
    }

    public String getSubject() {
        return subject;
    }

    public Map<String, Object> getClaims() {
        return claims;
    }
}
