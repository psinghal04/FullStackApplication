package com.example.hrapp.employee.dto.v2;

import java.util.UUID;

/**
 * Lightweight reference to a manager, used to avoid circular dependencies in DTOs.
 */
public record ManagerReferenceDTO(
    UUID id,
    String employeeId,
    String firstName,
    String lastName,
    String jobTitle
) {
}
