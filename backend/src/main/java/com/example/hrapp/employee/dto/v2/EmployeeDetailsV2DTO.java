package com.example.hrapp.employee.dto.v2;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Detailed employee response payload used by profile/details endpoints (API v2).
 * Includes manager reference support.
 */
public record EmployeeDetailsV2DTO(
    UUID id,
    String employeeId,
    String firstName,
    String lastName,
    String jobTitle,
    LocalDate dateOfBirth,
    String gender,
    LocalDate dateOfHire,
    LocalDate dateOfTermination,
    String homeAddress,
    String mailingAddress,
    String telephoneNumber,
    String emailAddress,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    ManagerReferenceDTO manager
) {
}
