package com.example.hrapp.security;

import com.example.hrapp.employee.Employee;
import com.example.hrapp.employee.EmployeeRepository;
import com.example.hrapp.identity.KeycloakAdminClient;
import com.example.hrapp.identity.KeycloakAdminProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerminatedEmployeeFilterTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_blocksTerminatedEmployee_with403AndReasonPayload() throws ServletException, IOException {
        RecordingKeycloakAdminClient keycloakAdminClient = new RecordingKeycloakAdminClient();
        TerminatedEmployeeFilter filter = new TerminatedEmployeeFilter(employeeRepository, keycloakAdminClient, new ObjectMapper());

        String employeeId = "EMP-000777";
        Employee terminatedEmployee = new Employee();
        terminatedEmployee.setEmployeeId(employeeId);
        terminatedEmployee.setDateOfTermination(LocalDate.now());

        when(employeeRepository.findByEmployeeId(eq(employeeId))).thenReturn(Optional.of(terminatedEmployee));

        EmployeeJwtPrincipal principal = new EmployeeJwtPrincipal(
            employeeId,
            "sub-1",
            Map.of("employee_id", employeeId, "email", "john.doe@example.com")
        );
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(principal, "token", java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/employees/EMP-000777");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString())
            .contains("\"status\":403")
            .contains("\"message\":\"Employee terminated\"")
            .contains("\"reason\":\"terminated\"");
        assertThat(keycloakAdminClient.lastEmail).isEqualTo("john.doe@example.com");
        assertThat(keycloakAdminClient.lastEnabled).isFalse();
    }

    @Test
    void doFilter_allowsActiveEmployeeRequest() throws ServletException, IOException {
        RecordingKeycloakAdminClient keycloakAdminClient = new RecordingKeycloakAdminClient();
        TerminatedEmployeeFilter filter = new TerminatedEmployeeFilter(employeeRepository, keycloakAdminClient, new ObjectMapper());

        String employeeId = "EMP-000123";
        Employee activeEmployee = new Employee();
        activeEmployee.setEmployeeId(employeeId);
        activeEmployee.setDateOfTermination(null);

        when(employeeRepository.findByEmployeeId(eq(employeeId))).thenReturn(Optional.of(activeEmployee));

        EmployeeJwtPrincipal principal = new EmployeeJwtPrincipal(employeeId, "sub-2", Map.of("employee_id", employeeId));
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(principal, "token", java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/employees/EMP-000123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(keycloakAdminClient.lastEmail).isNull();
    }

    private static final class RecordingKeycloakAdminClient extends KeycloakAdminClient {
        private String lastEmail;
        private Boolean lastEnabled;

        private RecordingKeycloakAdminClient() {
            super(defaultProperties());
        }

        @Override
        public void setUserEnabledByEmail(String emailAddress, boolean enabled) {
            this.lastEmail = emailAddress;
            this.lastEnabled = enabled;
        }

        private static KeycloakAdminProperties defaultProperties() {
            return new KeycloakAdminProperties(
                "http://localhost",
                "test-realm",
                "test-client",
                "test-secret",
                "EMPLOYEE",
                "example.com",
                null,
                false
            );
        }
    }
}
