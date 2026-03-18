package com.example.hrapp.auth;

public final class AuthConstants {
    public static final String SESSION_COOKIE_NAME = "BFF_SESSION_ID";
    public static final String LOGIN_REDIRECT_PATH = "/oauth2/authorization/keycloak";
    public static final String EMPLOYEE_PROFILE_PATH = "/employee/profile";

    private AuthConstants() {
    }
}
