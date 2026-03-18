package com.example.hrapp.employee;

import com.example.hrapp.common.exception.BadRequestException;
import com.example.hrapp.common.exception.ResourceNotFoundException;
import com.example.hrapp.employee.dto.v2.EmployeeCreateV2DTO;
import com.example.hrapp.employee.dto.v2.EmployeeDetailsV2DTO;
import com.example.hrapp.employee.dto.v2.EmployeeSummaryV2DTO;
import com.example.hrapp.employee.dto.v2.EmployeeUpdateV2DTO;
import com.example.hrapp.identity.KeycloakAdminClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Application service for employee operations in API v2.
 * 
 * <p>Extends v1 functionality with manager relationship support. Handles manager
 * assignment validation and subordinate queries.</p>
 */
@Service
public class EmployeeServiceV2 {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapperV2 employeeMapperV2;
    private final KeycloakAdminClient keycloakAdminClient;

    public EmployeeServiceV2(
        EmployeeRepository employeeRepository,
        EmployeeMapperV2 employeeMapperV2,
        KeycloakAdminClient keycloakAdminClient
    ) {
        this.employeeRepository = employeeRepository;
        this.employeeMapperV2 = employeeMapperV2;
        this.keycloakAdminClient = keycloakAdminClient;
    }

    /**
     * Creates a new employee with optional manager assignment.
     */
    @Transactional
    @CacheEvict(cacheNames = "employeeSearch", allEntries = true)
    public EmployeeSummaryV2DTO create(EmployeeCreateV2DTO request) {
        Employee employee = employeeMapperV2.toEntity(request);
        String generatedEmployeeId = generateEmployeeId();
        employee.setEmployeeId(generatedEmployeeId);

        // Assign manager if provided
        if (request.managerId() != null) {
            Employee manager = findByIdOrThrow(request.managerId());
            employee.setManager(manager);
        }

        Employee saved = employeeRepository.save(employee);

        keycloakAdminClient.upsertEmployeeUser(
            saved.getEmployeeId(),
            saved.getEmailAddress(),
            saved.getFirstName(),
            saved.getLastName()
        );
        keycloakAdminClient.setUserEnabledByEmail(saved.getEmailAddress(), !isTerminated(saved));
        
        // Reload with manager to ensure it's available for DTO mapping
        Employee reloaded = findByEmployeeIdWithManagerOrThrow(saved.getEmployeeId());
        return employeeMapperV2.toSummaryDTO(reloaded);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "employeeDetailsV2", key = "#employeeId")
    public EmployeeDetailsV2DTO getDetailsByEmployeeId(String employeeId) {
        Employee employee = findByEmployeeIdWithManagerOrThrow(employeeId);
        return employeeMapperV2.toDetailsDTO(employee);
    }

    /**
     * Fully updates an employee including manager assignment.
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "employeeDetailsV2", key = "#employeeId"),
        @CacheEvict(cacheNames = "employeeDetailsV2", key = "#request.employeeId", condition = "#request != null && #request.employeeId != null"),
        @CacheEvict(cacheNames = "employeeSearch", allEntries = true)
    })
    public EmployeeSummaryV2DTO updateByEmployeeId(String employeeId, EmployeeUpdateV2DTO request) {
        Employee employee = findByEmployeeIdOrThrow(employeeId);
        ensureEmployeeIdIsUnique(request.employeeId(), employee.getId());
        ensureEmailAddressIsUnchanged(request.emailAddress(), employee.getEmailAddress());

        employeeMapperV2.updateEntity(request, employee);
        
        // Update manager assignment
        if (request.managerId() != null) {
            // Prevent self-management
            if (request.managerId().equals(employee.getId())) {
                throw new BadRequestException("An employee cannot be their own manager");
            }
            Employee manager = findByIdOrThrow(request.managerId());
            employee.setManager(manager);
        } else {
            employee.setManager(null);
        }

        Employee saved = employeeRepository.save(employee);
        keycloakAdminClient.setUserEnabledByEmail(saved.getEmailAddress(), !isTerminated(saved));
        
        // Reload with manager to ensure it's available for DTO mapping
        Employee reloaded = findByEmployeeIdWithManagerOrThrow(saved.getEmployeeId());
        return employeeMapperV2.toSummaryDTO(reloaded);
    }

    /**
     * Returns all employees who report to the specified manager.
     */
    @Transactional(readOnly = true)
    public List<EmployeeSummaryV2DTO> getSubordinates(String managerEmployeeId) {
        Employee manager = employeeRepository.findByEmployeeIdWithSubordinates(managerEmployeeId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found for employeeId: " + managerEmployeeId));
        return manager.getSubordinates().stream()
            .map(employeeMapperV2::toSummaryDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<EmployeeSummaryV2DTO> searchEmployees(String employeeId, String partialLastName, Pageable pageable) {
        String normalizedEmployeeId = normalize(employeeId);
        String normalizedLastName = normalize(partialLastName);

        if (normalizedEmployeeId == null && normalizedLastName == null) {
            throw new BadRequestException(EmployeeConstants.EMPLOYEE_SEARCH_CRITERIA_REQUIRED_MESSAGE);
        }

        List<Employee> employees;
        if (normalizedEmployeeId != null) {
            employees = employeeRepository.findByEmployeeIdIgnoreCaseWithManager(normalizedEmployeeId);
        } else {
            employees = employeeRepository.findByLastNameContainingIgnoreCaseWithManager(normalizedLastName);
        }

        // Sort by lastName, firstName to match original behavior
        employees.sort(Comparator.comparing(Employee::getLastName)
            .thenComparing(Employee::getFirstName));

        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), employees.size());
        List<Employee> pageContent = employees.subList(start, end);

        List<EmployeeSummaryV2DTO> dtos = pageContent.stream()
            .map(employeeMapperV2::toSummaryDTO)
            .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, employees.size());
    }

    @Transactional(readOnly = true)
    public EmployeeSummaryV2DTO getSummaryByEmployeeId(String employeeId) {
        Employee employee = findByEmployeeIdWithManagerOrThrow(employeeId);
        return employeeMapperV2.toSummaryDTO(employee);
    }

    private Employee findByEmployeeIdWithManagerOrThrow(String employeeId) {
        return employeeRepository.findByEmployeeIdWithManager(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found for employeeId: " + employeeId));
    }

    private Employee findByEmployeeIdOrThrow(String employeeId) {
        return employeeRepository.findByEmployeeId(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found for employeeId: " + employeeId));
    }

    private Employee findByIdOrThrow(UUID id) {
        return employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found for id: " + id));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void ensureEmployeeIdIsUnique(String newEmployeeId, UUID currentEmployeeDbId) {
        employeeRepository.findByEmployeeId(newEmployeeId)
            .ifPresent(existing -> {
                boolean sameRecord = currentEmployeeDbId != null && currentEmployeeDbId.equals(existing.getId());
                if (!sameRecord) {
                    throw new BadRequestException("employeeId already exists: " + newEmployeeId);
                }
            });
    }

    private void ensureEmailAddressIsUnchanged(String requestedEmailAddress, String currentEmailAddress) {
        if (!requestedEmailAddress.equalsIgnoreCase(currentEmailAddress)) {
            throw new BadRequestException(EmployeeConstants.EMAIL_ADDRESS_IMMUTABLE_MESSAGE);
        }
    }

    private String generateEmployeeId() {
        for (int attempts = 0; attempts < 20; attempts++) {
            int value = ThreadLocalRandom.current().nextInt(1, 1_000_000);
            String candidate = EmployeeConstants.EMPLOYEE_ID_PREFIX + String.format("%06d", value);
            if (!employeeRepository.existsByEmployeeId(candidate)) {
                return candidate;
            }
        }

        throw new BadRequestException("Unable to generate a unique employeeId");
    }

    private boolean isTerminated(Employee employee) {
        if (employee.getDateOfTermination() == null) {
            return false;
        }

        return !employee.getDateOfTermination().isAfter(LocalDate.now());
    }
}
