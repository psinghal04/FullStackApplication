package com.example.hrapp.employee.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Lightweight employee payload used in list/search responses.
 */
public record EmployeeSummaryDTO(
    UUID id,
    String employeeId,
    String firstName,
    String lastName,
    String jobTitle,
    String emailAddress,
    LocalDate dateOfHire,
    LocalDate dateOfTermination
) {
}
