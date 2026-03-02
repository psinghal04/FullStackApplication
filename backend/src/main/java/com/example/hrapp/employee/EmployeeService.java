package com.example.hrapp.employee;

import com.example.hrapp.common.exception.BadRequestException;
import com.example.hrapp.common.exception.ResourceNotFoundException;
import com.example.hrapp.employee.dto.EmployeeContactUpdateDTO;
import com.example.hrapp.employee.dto.EmployeeCreateDTO;
import com.example.hrapp.employee.dto.EmployeeDetailsDTO;
import com.example.hrapp.employee.dto.EmployeeSummaryDTO;
import com.example.hrapp.employee.dto.EmployeeUpdateDTO;
import com.example.hrapp.identity.KeycloakAdminClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final KeycloakAdminClient keycloakAdminClient;

    public EmployeeService(
        EmployeeRepository employeeRepository,
        EmployeeMapper employeeMapper,
        KeycloakAdminClient keycloakAdminClient
    ) {
        this.employeeRepository = employeeRepository;
        this.employeeMapper = employeeMapper;
        this.keycloakAdminClient = keycloakAdminClient;
    }

    @Transactional
    @CacheEvict(cacheNames = "employeeSearch", allEntries = true)
    public EmployeeSummaryDTO create(EmployeeCreateDTO request) {
        Employee employee = employeeMapper.toEntity(request);
        String generatedEmployeeId = generateEmployeeId();
        employee.setEmployeeId(generatedEmployeeId);

        Employee saved = employeeRepository.save(employee);

        keycloakAdminClient.upsertEmployeeUser(
            saved.getEmployeeId(),
            saved.getEmailAddress(),
            saved.getFirstName(),
            saved.getLastName()
        );
        keycloakAdminClient.setUserEnabledByEmail(saved.getEmailAddress(), !isTerminated(saved));
        return employeeMapper.toSummaryDTO(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "employeeDetails", key = "#employeeId")
    public EmployeeDetailsDTO getDetailsByEmployeeId(String employeeId) {
        Employee employee = findByEmployeeIdOrThrow(employeeId);
        return employeeMapper.toDetailsDTO(employee);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "employeeDetails", key = "#employeeId"),
        @CacheEvict(cacheNames = "employeeDetails", key = "#request.employeeId", condition = "#request != null && #request.employeeId != null"),
        @CacheEvict(cacheNames = "employeeSearch", allEntries = true)
    })
    public EmployeeSummaryDTO updateByEmployeeId(String employeeId, EmployeeUpdateDTO request) {
        Employee employee = findByEmployeeIdOrThrow(employeeId);
        ensureEmployeeIdIsUnique(request.getEmployeeId(), employee.getId());
        ensureEmailAddressIsUnchanged(request.getEmailAddress(), employee.getEmailAddress());

        employeeMapper.updateEntity(request, employee);
        Employee saved = employeeRepository.save(employee);
        keycloakAdminClient.setUserEnabledByEmail(saved.getEmailAddress(), !isTerminated(saved));
        return employeeMapper.toSummaryDTO(saved);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(cacheNames = "employeeDetails", key = "#employeeId"),
        @CacheEvict(cacheNames = "employeeSearch", allEntries = true)
    })
    public EmployeeSummaryDTO patchContactByEmployeeId(String employeeId, EmployeeContactUpdateDTO request) {
        Employee employee = findByEmployeeIdOrThrow(employeeId);
        ensureEmailAddressNotProvidedInContactPatch(request);
        employeeMapper.updateContactFields(request, employee);

        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toSummaryDTO(saved);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeSummaryDTO> searchByLastName(String partialLastName, Pageable pageable) {
        return employeeRepository.findByLastNameContainingIgnoreCase(partialLastName, pageable)
            .map(employeeMapper::toSummaryDTO);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeSummaryDTO> searchEmployees(String employeeId, String partialLastName, Pageable pageable) {
        String normalizedEmployeeId = normalize(employeeId);
        String normalizedLastName = normalize(partialLastName);

        if (normalizedEmployeeId == null && normalizedLastName == null) {
            throw new BadRequestException("Either employeeId or lastName must be provided");
        }

        if (normalizedEmployeeId != null) {
            return employeeRepository.findByEmployeeIdIgnoreCase(normalizedEmployeeId, pageable)
                .map(employeeMapper::toSummaryDTO);
        }

        return employeeRepository.findByLastNameContainingIgnoreCase(normalizedLastName, pageable)
            .map(employeeMapper::toSummaryDTO);
    }

    @Transactional(readOnly = true)
    public EmployeeSummaryDTO getSummaryByEmployeeId(String employeeId) {
        Employee employee = findByEmployeeIdOrThrow(employeeId);
        return employeeMapper.toSummaryDTO(employee);
    }

    private Employee findByEmployeeIdOrThrow(String employeeId) {
        return employeeRepository.findByEmployeeId(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found for employeeId: " + employeeId));
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
            throw new BadRequestException("emailAddress cannot be changed once created");
        }
    }

    private void ensureEmailAddressNotProvidedInContactPatch(EmployeeContactUpdateDTO request) {
        if (request.getEmailAddress() != null) {
            throw new BadRequestException("emailAddress cannot be changed once created");
        }
    }

    private String generateEmployeeId() {
        for (int attempts = 0; attempts < 20; attempts++) {
            int value = ThreadLocalRandom.current().nextInt(1, 1_000_000);
            String candidate = "EMP-" + String.format("%06d", value);
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
