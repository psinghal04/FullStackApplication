package com.example.hrapp.employee.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Detailed employee response payload used by profile/details endpoints.
 */
public record EmployeeDetailsDTO(
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
    OffsetDateTime updatedAt
) {
}
