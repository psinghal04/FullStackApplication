package com.example.hrapp.employee.dto;

import java.time.LocalDate;
import java.util.UUID;

public class EmployeeSummaryDTO {

    private UUID id;
    private String employeeId;
    private String firstName;
    private String lastName;
    private String jobTitle;
    private String emailAddress;
    private LocalDate dateOfHire;
    private LocalDate dateOfTermination;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
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
}
