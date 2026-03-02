package com.example.hrapp.employee;

import com.example.hrapp.employee.dto.EmployeeContactUpdateDTO;
import com.example.hrapp.employee.dto.EmployeeCreateDTO;
import com.example.hrapp.employee.dto.EmployeeDetailsDTO;
import com.example.hrapp.employee.dto.EmployeeSummaryDTO;
import com.example.hrapp.employee.dto.EmployeeUpdateDTO;
import com.example.hrapp.security.EmployeeJwtPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API surface for employee operations.
 *
 * <p>Authorization is primarily declarative via {@code @PreAuthorize}. Business rules such as
 * immutable email and search precedence are delegated to the service layer.</p>
 */
@RestController
@Validated
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * Creates a new employee and triggers identity provisioning.
     */
    @PostMapping
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<EmployeeSummaryDTO> createEmployee(
        @Valid @RequestBody EmployeeCreateDTO request
    ) {
        EmployeeSummaryDTO created = employeeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Returns an employee profile for HR admins or the owning employee.
     */
    @GetMapping("/{employeeId}")
    @PreAuthorize("hasRole('HR_ADMIN') or (hasRole('EMPLOYEE') and #employeeId == authentication.principal.employee_id)")
    public ResponseEntity<EmployeeDetailsDTO> getEmployeeByPathEmployeeId(
        @PathVariable String employeeId
    ) {
        return ResponseEntity.ok(employeeService.getDetailsByEmployeeId(employeeId));
    }

    /**
     * Convenience endpoint to resolve the caller's own profile.
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<EmployeeDetailsDTO> getMyEmployeeProfile(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof EmployeeJwtPrincipal employeePrincipal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String employeeId = employeePrincipal.employee_id();
        if (employeeId == null || employeeId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(employeeService.getDetailsByEmployeeId(employeeId));
    }

    /**
     * Full update endpoint for HR admins.
     */
    @PutMapping("/{employeeId}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<EmployeeSummaryDTO> updateEmployee(
        @PathVariable String employeeId,
        @Valid @RequestBody EmployeeUpdateDTO request
    ) {
        return ResponseEntity.ok(employeeService.updateByEmployeeId(employeeId, request));
    }

    /**
     * Partial contact update endpoint for HR admins and owning employees.
     */
    @PatchMapping("/{employeeId}/contact")
    @PreAuthorize("hasRole('HR_ADMIN') or (hasRole('EMPLOYEE') and #employeeId == authentication.principal.employee_id)")
    public ResponseEntity<EmployeeSummaryDTO> updateEmployeeContact(
        @PathVariable String employeeId,
        @Valid @RequestBody EmployeeContactUpdateDTO request
    ) {
        return ResponseEntity.ok(employeeService.patchContactByEmployeeId(employeeId, request));
    }

    /**
     * Searches employees by employeeId or lastName.
     *
     * <p>If both parameters are supplied, {@code employeeId} takes precedence.</p>
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<Page<EmployeeSummaryDTO>> searchEmployees(
        @RequestParam(value = "employeeId", required = false) String employeeId,
        @RequestParam(value = "lastName", required = false) String lastName,
        @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
        @RequestParam(value = "size", defaultValue = "25") @Min(1) @Max(100) int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "lastName", "firstName"));
        return ResponseEntity.ok(employeeService.searchEmployees(employeeId, lastName, pageable));
    }

    @GetMapping(params = "employeeId")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<EmployeeSummaryDTO> getByEmployeeId(
        @RequestParam("employeeId") @NotBlank String employeeId
    ) {
        return ResponseEntity.ok(employeeService.getSummaryByEmployeeId(employeeId));
    }
}
