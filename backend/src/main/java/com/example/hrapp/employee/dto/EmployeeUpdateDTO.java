package com.example.hrapp.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class EmployeeUpdateDTO {

    @NotBlank(message = "employeeId is required")
    @Size(max = 50, message = "employeeId must be at most 50 characters")
    private String employeeId;

    @NotBlank(message = "firstName is required")
    @Size(max = 100, message = "firstName must be at most 100 characters")
    private String firstName;

    @NotBlank(message = "lastName is required")
    @Size(max = 100, message = "lastName must be at most 100 characters")
    private String lastName;

    @NotBlank(message = "jobTitle is required")
    @Size(max = 150, message = "jobTitle must be at most 150 characters")
    private String jobTitle;

    @NotNull(message = "dateOfBirth is required")
    private LocalDate dateOfBirth;

    @NotBlank(message = "gender is required")
    @Size(max = 50, message = "gender must be at most 50 characters")
    private String gender;

    @NotNull(message = "dateOfHire is required")
    private LocalDate dateOfHire;

    private LocalDate dateOfTermination;

    @NotBlank(message = "homeAddress is required")
    private String homeAddress;

    @NotBlank(message = "mailingAddress is required")
    private String mailingAddress;

    @NotBlank(message = "telephoneNumber is required")
    @Size(max = 30, message = "telephoneNumber must be at most 30 characters")
    private String telephoneNumber;

    @NotBlank(message = "emailAddress is required")
    @Email(message = "emailAddress must be a valid email")
    @Size(max = 255, message = "emailAddress must be at most 255 characters")
    private String emailAddress;

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getDateOfHire() {
        return dateOfHire;
    }

    public void setDateOfHire(LocalDate dateOfHire) {
        this.dateOfHire = dateOfHire;
    }

    public LocalDate getDateOfTermination() {
        return dateOfTermination;
    }

    public void setDateOfTermination(LocalDate dateOfTermination) {
        this.dateOfTermination = dateOfTermination;
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
}
