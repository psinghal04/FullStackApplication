package com.example.hrapp.employee;

import com.example.hrapp.employee.dto.v2.EmployeeCreateV2DTO;
import com.example.hrapp.employee.dto.v2.EmployeeDetailsV2DTO;
import com.example.hrapp.employee.dto.v2.EmployeeSummaryV2DTO;
import com.example.hrapp.employee.dto.v2.EmployeeUpdateV2DTO;
import com.example.hrapp.employee.dto.v2.ManagerReferenceDTO;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Maps between employee entities and API v2 DTOs.
 * 
 * <p>Handles manager relationships by converting manager entities to reference DTOs
 * to avoid circular dependencies. Manager assignment is done via ID reference.</p>
 */
@Component
public class EmployeeMapperV2 {

    public Employee toEntity(EmployeeCreateV2DTO source) {
        Employee employee = new Employee();
        employee.setId(UUID.randomUUID());
        employee.setFirstName(source.firstName());
        employee.setLastName(source.lastName());
        employee.setJobTitle(source.jobTitle());
        employee.setDateOfBirth(source.dateOfBirth());
        employee.setGender(source.gender());
        employee.setDateOfHire(source.dateOfHire());
        employee.setDateOfTermination(source.dateOfTermination());
        employee.setHomeAddress(source.homeAddress());
        employee.setMailingAddress(source.mailingAddress());
        employee.setTelephoneNumber(source.telephoneNumber());
        employee.setEmailAddress(source.emailAddress());

        OffsetDateTime now = OffsetDateTime.now();
        employee.setCreatedAt(now);
        employee.setUpdatedAt(now);
        
        // Note: manager is set separately in service layer after validation
        return employee;
    }

    public void updateEntity(EmployeeUpdateV2DTO source, Employee target) {
        target.setEmployeeId(source.employeeId());
        target.setFirstName(source.firstName());
        target.setLastName(source.lastName());
        target.setJobTitle(source.jobTitle());
        target.setDateOfBirth(source.dateOfBirth());
        target.setGender(source.gender());
        target.setDateOfHire(source.dateOfHire());
        target.setDateOfTermination(source.dateOfTermination());
        target.setHomeAddress(source.homeAddress());
        target.setMailingAddress(source.mailingAddress());
        target.setTelephoneNumber(source.telephoneNumber());
        target.setUpdatedAt(OffsetDateTime.now());
        
        // Note: manager is set separately in service layer after validation
    }

    public EmployeeSummaryV2DTO toSummaryDTO(Employee source) {
        ManagerReferenceDTO managerRef = null;
        if (source.getManager() != null) {
            managerRef = toManagerReferenceDTO(source.getManager());
        }
        
        return new EmployeeSummaryV2DTO(
            source.getId(),
            source.getEmployeeId(),
            source.getFirstName(),
            source.getLastName(),
            source.getJobTitle(),
            source.getEmailAddress(),
            source.getDateOfHire(),
            source.getDateOfTermination(),
            managerRef
        );
    }

    public EmployeeDetailsV2DTO toDetailsDTO(Employee source) {
        ManagerReferenceDTO managerRef = null;
        if (source.getManager() != null) {
            managerRef = toManagerReferenceDTO(source.getManager());
        }
        
        return new EmployeeDetailsV2DTO(
            source.getId(),
            source.getEmployeeId(),
            source.getFirstName(),
            source.getLastName(),
            source.getJobTitle(),
            source.getDateOfBirth(),
            source.getGender(),
            source.getDateOfHire(),
            source.getDateOfTermination(),
            source.getHomeAddress(),
            source.getMailingAddress(),
            source.getTelephoneNumber(),
            source.getEmailAddress(),
            source.getCreatedAt(),
            source.getUpdatedAt(),
            managerRef
        );
    }

    /**
     * Converts an Employee entity to a lightweight manager reference.
     * Used to avoid circular dependencies when including manager information.
     */
    public ManagerReferenceDTO toManagerReferenceDTO(Employee manager) {
        return new ManagerReferenceDTO(
            manager.getId(),
            manager.getEmployeeId(),
            manager.getFirstName(),
            manager.getLastName(),
            manager.getJobTitle()
        );
    }
}
