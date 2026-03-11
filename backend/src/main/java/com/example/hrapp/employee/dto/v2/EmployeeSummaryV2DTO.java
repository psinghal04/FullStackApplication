package com.example.hrapp.employee.dto.v2;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Lightweight employee payload used in list/search responses (API v2).
 * Includes manager reference support.
 */
public record EmployeeSummaryV2DTO(
    UUID id,
    String employeeId,
    String firstName,
    String lastName,
    String jobTitle,
    String emailAddress,
    LocalDate dateOfHire,
    LocalDate dateOfTermination,
    ManagerReferenceDTO manager
) {
}
