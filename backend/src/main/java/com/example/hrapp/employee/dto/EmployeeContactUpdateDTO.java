package com.example.hrapp.employee.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Partial-update payload for mutable contact fields.
 */
public record EmployeeContactUpdateDTO(
    String homeAddress,
    String mailingAddress,
    @Size(max = 30, message = "telephoneNumber must be at most 30 characters")
    String telephoneNumber,
    @Email(message = "emailAddress must be a valid email")
    @Size(max = 255, message = "emailAddress must be at most 255 characters")
    String emailAddress
) {

    /**
     * Ensures PATCH requests are not empty.
     */
    @AssertTrue(message = "at least one contact field must be provided")
    public boolean isAnyFieldPresent() {
        return homeAddress != null || mailingAddress != null || telephoneNumber != null || emailAddress != null;
    }

    /**
     * Rejects blank values for fields that are explicitly supplied.
     */
    @AssertTrue(message = "provided contact fields must not be blank")
    public boolean isNoProvidedFieldBlank() {
        return isNullOrNotBlank(homeAddress)
            && isNullOrNotBlank(mailingAddress)
            && isNullOrNotBlank(telephoneNumber)
            && isNullOrNotBlank(emailAddress);
    }

    private boolean isNullOrNotBlank(String value) {
        return value == null || !value.isBlank();
    }
}
