package com.example.hrapp.employee;

import com.example.hrapp.common.exception.BadRequestException;
import com.example.hrapp.employee.dto.EmployeeContactUpdateDTO;
import com.example.hrapp.employee.dto.EmployeeUpdateDTO;
import com.example.hrapp.employee.dto.EmployeeSummaryDTO;
import com.example.hrapp.identity.KeycloakAdminProperties;
import com.example.hrapp.identity.KeycloakAdminClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceSearchTest {

    @Mock
    private EmployeeRepository employeeRepository;

    private final EmployeeMapper employeeMapper = new EmployeeMapper();

    private EmployeeService employeeService;
    private RecordingKeycloakAdminClient keycloakAdminClient;

    @BeforeEach
    void setUp() {
        KeycloakAdminProperties properties = new KeycloakAdminProperties(
            "http://localhost:8080",
            "hr",
            "dummy-client",
            "dummy-secret",
            "EMPLOYEE",
            "company.local",
            "ChangeMe123!",
            true
        );

        keycloakAdminClient = new RecordingKeycloakAdminClient(properties);
        employeeService = new EmployeeService(employeeRepository, employeeMapper, keycloakAdminClient);
    }

    @Test
    void searchByLastName_returnsPaginatedMultipleMatches_forPartialCaseInsensitiveInput() {
        PageRequest pageable = PageRequest.of(0, 25);
        Employee employeeOne = employee("EMP-000101", "John", "Smith");
        Employee employeeTwo = employee("EMP-000102", "Jane", "Smithson");
        Page<Employee> repositoryResult = new PageImpl<>(List.of(employeeOne, employeeTwo), pageable, 2);

        when(employeeRepository.findByLastNameContainingIgnoreCase(eq("smith"), eq(pageable)))
            .thenReturn(repositoryResult);

        Page<EmployeeSummaryDTO> result = employeeService.searchByLastName("smith", pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(25);
        assertThat(result.getContent())
            .extracting(EmployeeSummaryDTO::lastName)
            .containsExactly("Smith", "Smithson");
    }

    @Test
    void searchByLastName_returnsEmptyPage_whenNoMatchesFound() {
        PageRequest pageable = PageRequest.of(0, 25);
        Page<Employee> repositoryResult = new PageImpl<>(List.of(), pageable, 0);

        when(employeeRepository.findByLastNameContainingIgnoreCase(eq("unknown"), eq(pageable)))
            .thenReturn(repositoryResult);

        Page<EmployeeSummaryDTO> result = employeeService.searchByLastName("unknown", pageable);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(25);
    }

    @Test
    void searchEmployees_prefersEmployeeId_whenBothEmployeeIdAndLastNameProvided() {
        PageRequest pageable = PageRequest.of(0, 25);
        Employee matchedByEmployeeId = employee("EMP-000201", "Case", "Insensitive");
        Page<Employee> repositoryResult = new PageImpl<>(List.of(matchedByEmployeeId), pageable, 1);

        when(employeeRepository.findByEmployeeIdIgnoreCase(eq("emp-000201"), eq(pageable)))
            .thenReturn(repositoryResult);

        Page<EmployeeSummaryDTO> result = employeeService.searchEmployees("emp-000201", "Smith", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).extracting(EmployeeSummaryDTO::employeeId).containsExactly("EMP-000201");
        verify(employeeRepository, never()).findByLastNameContainingIgnoreCase(any(String.class), eq(pageable));
    }

    @Test
    void searchEmployees_usesLastName_whenEmployeeIdNotProvided() {
        PageRequest pageable = PageRequest.of(0, 25);
        Employee employeeOne = employee("EMP-000301", "John", "Smith");
        Page<Employee> repositoryResult = new PageImpl<>(List.of(employeeOne), pageable, 1);

        when(employeeRepository.findByLastNameContainingIgnoreCase(eq("smith"), eq(pageable)))
            .thenReturn(repositoryResult);

        Page<EmployeeSummaryDTO> result = employeeService.searchEmployees(null, "smith", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).extracting(EmployeeSummaryDTO::lastName).containsExactly("Smith");
        verify(employeeRepository, never()).findByEmployeeIdIgnoreCase(any(String.class), eq(pageable));
    }

    @Test
    void searchEmployees_throwsBadRequest_whenNeitherEmployeeIdNorLastNameProvided() {
        PageRequest pageable = PageRequest.of(0, 25);

        assertThatThrownBy(() -> employeeService.searchEmployees("   ", "   ", pageable))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Either employeeId or lastName must be provided");

        verify(employeeRepository, never()).findByEmployeeIdIgnoreCase(any(String.class), eq(pageable));
        verify(employeeRepository, never()).findByLastNameContainingIgnoreCase(any(String.class), eq(pageable));
    }

    @Test
    void updateByEmployeeId_throwsBadRequest_whenEmailAddressChanges() {
        Employee existingEmployee = employee("EMP-000201", "John", "Doe");
        when(employeeRepository.findByEmployeeId(eq("EMP-000201"))).thenReturn(java.util.Optional.of(existingEmployee));

        EmployeeUpdateDTO request = new EmployeeUpdateDTO(
            "EMP-000201",
            "John",
            "Doe",
            "Engineer",
            LocalDate.of(1990, 1, 1),
            "Male",
            LocalDate.of(2024, 1, 1),
            null,
            "{}",
            "{}",
            "+1-555-0100",
            "john.changed@example.com"
        );

        assertThatThrownBy(() -> employeeService.updateByEmployeeId("EMP-000201", request))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("emailAddress cannot be changed once created");

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void patchContactByEmployeeId_throwsBadRequest_whenEmailAddressProvided() {
        Employee existingEmployee = employee("EMP-000202", "Jane", "Doe");
        when(employeeRepository.findByEmployeeId("EMP-000202")).thenReturn(java.util.Optional.of(existingEmployee));

        EmployeeContactUpdateDTO request = new EmployeeContactUpdateDTO(
            null,
            null,
            null,
            "jane.changed@example.com"
        );

        assertThatThrownBy(() -> employeeService.patchContactByEmployeeId("EMP-000202", request))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("emailAddress cannot be changed once created");

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void updateByEmployeeId_disablesKeycloakUser_whenDateOfTerminationIsToday() {
        Employee existingEmployee = employee("EMP-000401", "John", "Doe");
        existingEmployee.setDateOfTermination(null);
        when(employeeRepository.findByEmployeeId("EMP-000401")).thenReturn(java.util.Optional.of(existingEmployee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeUpdateDTO request = buildUpdateRequest(existingEmployee, LocalDate.now());

        employeeService.updateByEmployeeId("EMP-000401", request);

        assertThat(keycloakAdminClient.enableSyncCalls)
            .containsExactly(new EnableSyncCall(existingEmployee.getEmailAddress(), false));
    }

    @Test
    void updateByEmployeeId_enablesKeycloakUser_whenDateOfTerminationRemoved() {
        Employee existingEmployee = employee("EMP-000402", "Jane", "Doe");
        existingEmployee.setDateOfTermination(LocalDate.now().minusDays(2));
        when(employeeRepository.findByEmployeeId("EMP-000402")).thenReturn(java.util.Optional.of(existingEmployee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeUpdateDTO request = buildUpdateRequest(existingEmployee, null);

        employeeService.updateByEmployeeId("EMP-000402", request);

        assertThat(keycloakAdminClient.enableSyncCalls)
            .containsExactly(new EnableSyncCall(existingEmployee.getEmailAddress(), true));
    }

    private EmployeeUpdateDTO buildUpdateRequest(Employee existingEmployee, LocalDate dateOfTermination) {
        return new EmployeeUpdateDTO(
            existingEmployee.getEmployeeId(),
            existingEmployee.getFirstName(),
            existingEmployee.getLastName(),
            existingEmployee.getJobTitle(),
            existingEmployee.getDateOfBirth(),
            existingEmployee.getGender(),
            existingEmployee.getDateOfHire(),
            dateOfTermination,
            existingEmployee.getHomeAddress(),
            existingEmployee.getMailingAddress(),
            existingEmployee.getTelephoneNumber(),
            existingEmployee.getEmailAddress()
        );
    }

    private static final class RecordingKeycloakAdminClient extends KeycloakAdminClient {
        private final List<EnableSyncCall> enableSyncCalls = new ArrayList<>();

        private RecordingKeycloakAdminClient(KeycloakAdminProperties properties) {
            super(properties);
        }

        @Override
        public void upsertEmployeeUser(String employeeId, String emailAddress, String firstName, String lastName) {
        }

        @Override
        public void setUserEnabledByEmail(String emailAddress, boolean enabled) {
            enableSyncCalls.add(new EnableSyncCall(emailAddress, enabled));
        }
    }

    private record EnableSyncCall(String emailAddress, boolean enabled) {
    }

    private Employee employee(String employeeId, String firstName, String lastName) {
        Employee employee = new Employee();
        employee.setId(UUID.randomUUID());
        employee.setEmployeeId(employeeId);
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setJobTitle("Engineer");
        employee.setDateOfBirth(LocalDate.of(1990, 1, 1));
        employee.setGender("Unknown");
        employee.setDateOfHire(LocalDate.of(2024, 1, 1));
        employee.setHomeAddress("{}");
        employee.setMailingAddress("{}");
        employee.setTelephoneNumber("+1-555-0100");
        employee.setEmailAddress(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com");
        employee.setCreatedAt(OffsetDateTime.now());
        employee.setUpdatedAt(OffsetDateTime.now());
        return employee;
    }
}
