package com.example.hrapp.employee;

import com.example.hrapp.common.api.GlobalExceptionHandler;
import com.example.hrapp.employee.dto.EmployeeContactUpdateDTO;
import com.example.hrapp.employee.dto.EmployeeCreateDTO;
import com.example.hrapp.employee.dto.EmployeeDetailsDTO;
import com.example.hrapp.employee.dto.EmployeeSummaryDTO;
import com.example.hrapp.identity.KeycloakAdminClient;
import com.example.hrapp.identity.KeycloakAdminProperties;
import com.example.hrapp.security.EmployeeJwtPrincipal;
import com.example.hrapp.security.TerminatedEmployeeFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmployeeControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        employeeRepository = Mockito.mock(EmployeeRepository.class);

        EmployeeService service = new EmployeeService(null, null, null) {
            @Override
            public EmployeeSummaryDTO create(EmployeeCreateDTO request) {
                EmployeeSummaryDTO dto = new EmployeeSummaryDTO();
                dto.setEmployeeId("EMP-000123");
                dto.setFirstName(request.getFirstName());
                dto.setLastName(request.getLastName());
                dto.setJobTitle(request.getJobTitle());
                dto.setEmailAddress(request.getEmailAddress());
                return dto;
            }

            @Override
            public EmployeeDetailsDTO getDetailsByEmployeeId(String employeeId) {
                EmployeeDetailsDTO dto = new EmployeeDetailsDTO();
                dto.setEmployeeId(employeeId);
                dto.setFirstName("Jane");
                dto.setLastName("Smith");
                return dto;
            }

            @Override
            public EmployeeSummaryDTO patchContactByEmployeeId(String employeeId, EmployeeContactUpdateDTO request) {
                EmployeeSummaryDTO dto = new EmployeeSummaryDTO();
                dto.setEmployeeId(employeeId);
                dto.setEmailAddress(request.getEmailAddress());
                return dto;
            }
        };

        EmployeeController controller = new EmployeeController(service);
        KeycloakAdminProperties keycloakAdminProperties = new KeycloakAdminProperties();
        keycloakAdminProperties.setServerUrl("http://localhost");
        keycloakAdminProperties.setRealm("test-realm");
        keycloakAdminProperties.setClientId("test-client");
        keycloakAdminProperties.setClientSecret("test-secret");
        keycloakAdminProperties.setEmployeeDomain("example.com");

        KeycloakAdminClient keycloakAdminClient = new KeycloakAdminClient(keycloakAdminProperties) {
            @Override
            public void setUserEnabledByEmail(String emailAddress, boolean enabled) {
            }
        };
        TerminatedEmployeeFilter terminatedEmployeeFilter = new TerminatedEmployeeFilter(employeeRepository, keycloakAdminClient, objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilter(terminatedEmployeeFilter)
            .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createEndpoint_returns201() throws Exception {
        setAuthenticatedPrincipal("HR-ADMIN-0001", "ROLE_HR_ADMIN");
        Mockito.when(employeeRepository.findByEmployeeId("HR-ADMIN-0001")).thenReturn(Optional.empty());

        EmployeeCreateDTO request = new EmployeeCreateDTO();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setJobTitle("Engineer");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setGender("Male");
        request.setDateOfHire(LocalDate.of(2024, 1, 1));
        request.setHomeAddress("{}");
        request.setMailingAddress("{}");
        request.setTelephoneNumber("+1-555-0100");
        request.setEmailAddress("john.doe@example.com");

        mockMvc.perform(post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.employeeId").value("EMP-000123"));
    }

    @Test
    void getEndpoint_returns200() throws Exception {
        setAuthenticatedPrincipal("HR-ADMIN-0001", "ROLE_HR_ADMIN");
        Mockito.when(employeeRepository.findByEmployeeId("HR-ADMIN-0001")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/employees/EMP-000123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.employeeId").value("EMP-000123"));
    }

    @Test
    void patchContactEndpoint_returns200() throws Exception {
        setAuthenticatedPrincipal("EMP-000123", "ROLE_EMPLOYEE");
        Mockito.when(employeeRepository.findByEmployeeId("EMP-000123")).thenReturn(Optional.empty());

        EmployeeContactUpdateDTO request = new EmployeeContactUpdateDTO();
        request.setHomeAddress("new-home");
        request.setMailingAddress("new-mail");
        request.setTelephoneNumber("+1-555-1111");
        request.setEmailAddress("new.email@example.com");

        mockMvc.perform(patch("/api/v1/employees/EMP-000123/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.employeeId").value("EMP-000123"));
    }

    @Test
    void getEndpoint_returns403_whenTerminated() throws Exception {
        setAuthenticatedPrincipal("EMP-TERM-0001", "ROLE_EMPLOYEE");

        Employee terminated = new Employee();
        terminated.setEmployeeId("EMP-TERM-0001");
        terminated.setDateOfTermination(LocalDate.now());
        Mockito.when(employeeRepository.findByEmployeeId("EMP-TERM-0001")).thenReturn(Optional.of(terminated));

        String responseBody = mockMvc.perform(get("/api/v1/employees/EMP-TERM-0001"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.reason").value("terminated"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(responseBody).contains("Employee terminated");
    }

    private void setAuthenticatedPrincipal(String employeeId, String... roles) {
        EmployeeJwtPrincipal principal = new EmployeeJwtPrincipal(employeeId, "sub-" + employeeId, Map.of("employee_id", employeeId));
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
            .map(SimpleGrantedAuthority::new)
            .toList();

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, "token", authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
