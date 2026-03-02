package com.example.hrapp.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request payload for creating an employee.
 */
public record EmployeeCreateDTO(
    @NotBlank(message = "firstName is required")
    @Size(max = 100, message = "firstName must be at most 100 characters")
    String firstName,
    @NotBlank(message = "lastName is required")
    @Size(max = 100, message = "lastName must be at most 100 characters")
    String lastName,
    @NotBlank(message = "jobTitle is required")
    @Size(max = 150, message = "jobTitle must be at most 150 characters")
    String jobTitle,
    @NotNull(message = "dateOfBirth is required")
    LocalDate dateOfBirth,
    @NotBlank(message = "gender is required")
    @Pattern(regexp = "^(Male|Female|Unspecified)$", message = "gender must be one of: Male, Female, Unspecified")
    String gender,
    @NotNull(message = "dateOfHire is required")
    LocalDate dateOfHire,
    LocalDate dateOfTermination,
    @NotBlank(message = "homeAddress is required")
    String homeAddress,
    @NotBlank(message = "mailingAddress is required")
    String mailingAddress,
    @NotBlank(message = "telephoneNumber is required")
    @Size(max = 30, message = "telephoneNumber must be at most 30 characters")
    String telephoneNumber,
    @NotBlank(message = "emailAddress is required")
    @Email(message = "emailAddress must be a valid email")
    @Size(max = 255, message = "emailAddress must be at most 255 characters")
    String emailAddress
) {
}
