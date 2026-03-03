package com.example.hrapp.employee;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EmployeeRepositoryIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("hr_test")
        .withUsername("hr_test")
        .withPassword("hr_test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    void findByLastNameContainingIgnoreCase_returnsMultiplePartialMatchesWithPagination() {
        employeeRepository.save(buildEmployee("EMP-100001", "Alice", "Smith", "alice.smith@example.com"));
        employeeRepository.save(buildEmployee("EMP-100002", "Bob", "Smithson", "bob.smithson@example.com"));
        employeeRepository.save(buildEmployee("EMP-100003", "Chris", "Anderson", "chris.anderson@example.com"));

        Page<Employee> result = employeeRepository.findByLastNameContainingIgnoreCase("smi", PageRequest.of(0, 25));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
            .extracting(Employee::getLastName)
            .containsExactlyInAnyOrder("Smith", "Smithson");
    }

    @Test
    void findByLastNameContainingIgnoreCase_returnsNoMatches() {
        employeeRepository.save(buildEmployee("EMP-200001", "Dana", "Clark", "dana.clark@example.com"));

        Page<Employee> result = employeeRepository.findByLastNameContainingIgnoreCase("zzz", PageRequest.of(0, 25));

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    private Employee buildEmployee(String employeeId, String firstName, String lastName, String emailAddress) {
        Employee employee = new Employee();
        employee.setId(UUID.randomUUID());
        employee.setEmployeeId(employeeId);
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setJobTitle("Engineer");
        employee.setDateOfBirth(LocalDate.of(1991, 1, 1));
        employee.setGender("Unknown");
        employee.setDateOfHire(LocalDate.of(2024, 1, 1));
        employee.setDateOfTermination(null);
        employee.setHomeAddress("{}");
        employee.setMailingAddress("{}");
        employee.setTelephoneNumber("+1-555-0100");
        employee.setEmailAddress(emailAddress);
        employee.setCreatedAt(OffsetDateTime.now());
        employee.setUpdatedAt(OffsetDateTime.now());
        return employee;
    }
}
