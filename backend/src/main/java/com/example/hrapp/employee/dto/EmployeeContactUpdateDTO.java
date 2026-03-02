package com.example.hrapp.employee.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class EmployeeContactUpdateDTO {

    private String homeAddress;

    private String mailingAddress;

    @Size(max = 30, message = "telephoneNumber must be at most 30 characters")
    private String telephoneNumber;

    @Email(message = "emailAddress must be a valid email")
    @Size(max = 255, message = "emailAddress must be at most 255 characters")
    private String emailAddress;

    @AssertTrue(message = "at least one contact field must be provided")
    public boolean isAnyFieldPresent() {
        return homeAddress != null || mailingAddress != null || telephoneNumber != null || emailAddress != null;
    }

    @AssertTrue(message = "provided contact fields must not be blank")
    public boolean isNoProvidedFieldBlank() {
        return isNullOrNotBlank(homeAddress)
            && isNullOrNotBlank(mailingAddress)
            && isNullOrNotBlank(telephoneNumber)
            && isNullOrNotBlank(emailAddress);
    }

    public String getHomeAddress() {
        return homeAddress;
    }

    public void setHomeAddress(String homeAddress) {
        this.homeAddress = homeAddress;
    }

    public String getMailingAddress() {
        return mailingAddress;
    }

    public void setMailingAddress(String mailingAddress) {
        this.mailingAddress = mailingAddress;
    }

    public String getTelephoneNumber() {
        return telephoneNumber;
    }

    public void setTelephoneNumber(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    private boolean isNullOrNotBlank(String value) {
        return value == null || !value.isBlank();
    }
}
