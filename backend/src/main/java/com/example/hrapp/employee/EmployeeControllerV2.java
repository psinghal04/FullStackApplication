package com.example.hrapp.employee;

import com.example.hrapp.employee.dto.v2.EmployeeCreateV2DTO;
import com.example.hrapp.employee.dto.v2.EmployeeDetailsV2DTO;
import com.example.hrapp.employee.dto.v2.EmployeeSummaryV2DTO;
import com.example.hrapp.employee.dto.v2.EmployeeUpdateV2DTO;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API surface for employee operations (API v2).
 *
 * <p>Extends v1 functionality with manager relationship support. Includes endpoints
 * for assigning managers and retrieving subordinates.</p>
 */
@RestController
@Validated
@RequestMapping("/api/v2/employees")
public class EmployeeControllerV2 {

    private final EmployeeServiceV2 employeeServiceV2;

    public EmployeeControllerV2(EmployeeServiceV2 employeeServiceV2) {
        this.employeeServiceV2 = employeeServiceV2;
    }

    /**
     * Creates a new employee with optional manager assignment.
     */
    @PostMapping
    @PreAuthorize(EmployeeAuthorizationExpressions.HR_ADMIN_ONLY)
    public ResponseEntity<EmployeeSummaryV2DTO> createEmployee(
        @Valid @RequestBody EmployeeCreateV2DTO request
    ) {
        EmployeeSummaryV2DTO created = employeeServiceV2.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Returns an employee profile for HR admins or the owning employee (with manager info).
     */
    @GetMapping("/{employeeId}")
    @PreAuthorize(EmployeeAuthorizationExpressions.HR_ADMIN_OR_SELF)
    public ResponseEntity<EmployeeDetailsV2DTO> getEmployeeByPathEmployeeId(
        @PathVariable String employeeId
    ) {
        return ResponseEntity.ok(employeeServiceV2.getDetailsByEmployeeId(employeeId));
    }

    /**
     * Convenience endpoint to resolve the caller's own profile.
     */
    @GetMapping("/me")
    @PreAuthorize(EmployeeAuthorizationExpressions.EMPLOYEE_OR_HR_ADMIN)
    public ResponseEntity<EmployeeDetailsV2DTO> getMyEmployeeProfile(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        String employeeId = null;

        if (principal instanceof EmployeeJwtPrincipal jwtPrincipal) {
            employeeId = jwtPrincipal.employee_id();
        } else if (principal instanceof com.example.hrapp.auth.BffSessionPrincipal bffPrincipal) {
            employeeId = bffPrincipal.employeeId();
        }

        if (employeeId == null || employeeId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(employeeServiceV2.getDetailsByEmployeeId(employeeId));
    }

    /**
     * Full update endpoint for HR admins (including manager assignment).
     */
    @PutMapping("/{employeeId}")
    @PreAuthorize(EmployeeAuthorizationExpressions.HR_ADMIN_ONLY)
    public ResponseEntity<EmployeeSummaryV2DTO> updateEmployee(
        @PathVariable String employeeId,
        @Valid @RequestBody EmployeeUpdateV2DTO request
    ) {
        return ResponseEntity.ok(employeeServiceV2.updateByEmployeeId(employeeId, request));
    }

    /**
     * Searches employees by employeeId or lastName.
     *
     * <p>If both parameters are supplied, {@code employeeId} takes precedence.</p>
     */
    @GetMapping("/search")
    @PreAuthorize(EmployeeAuthorizationExpressions.HR_ADMIN_ONLY)
    public ResponseEntity<Page<EmployeeSummaryV2DTO>> searchEmployees(
        @RequestParam(value = "employeeId", required = false) String employeeId,
        @RequestParam(value = "lastName", required = false) String lastName,
        @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
        @RequestParam(value = "size", defaultValue = "25") @Min(1) @Max(100) int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "lastName", "firstName"));
        return ResponseEntity.ok(employeeServiceV2.searchEmployees(employeeId, lastName, pageable));
    }

    @GetMapping(params = "employeeId")
    @PreAuthorize(EmployeeAuthorizationExpressions.HR_ADMIN_ONLY)
    public ResponseEntity<EmployeeSummaryV2DTO> getByEmployeeId(
        @RequestParam("employeeId") @NotBlank String employeeId
    ) {
        return ResponseEntity.ok(employeeServiceV2.getSummaryByEmployeeId(employeeId));
    }

    /**
     * Returns all employees who report to the specified manager.
     * 
     * <p>New in v2: allows querying the organizational hierarchy.</p>
     * <p>HR admins can view any employee's subordinates. Employees can view their own subordinates.</p>
     */
    @GetMapping("/{employeeId}/subordinates")
    @PreAuthorize(EmployeeAuthorizationExpressions.HR_ADMIN_OR_SELF)
    public ResponseEntity<List<EmployeeSummaryV2DTO>> getSubordinates(
        @PathVariable String employeeId
    ) {
        return ResponseEntity.ok(employeeServiceV2.getSubordinates(employeeId));
    }
}
