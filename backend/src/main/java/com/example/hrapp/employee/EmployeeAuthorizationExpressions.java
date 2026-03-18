package com.example.hrapp.employee;

public final class EmployeeAuthorizationExpressions {
    public static final String HR_ADMIN_ONLY = "hasRole('HR_ADMIN')";
    public static final String EMPLOYEE_OR_HR_ADMIN = "hasAnyRole('EMPLOYEE','HR_ADMIN')";
    public static final String HR_ADMIN_OR_SELF =
        "hasRole('HR_ADMIN') or (hasRole('EMPLOYEE') and #employeeId == authentication.principal.employee_id)";

    private EmployeeAuthorizationExpressions() {
    }
}
