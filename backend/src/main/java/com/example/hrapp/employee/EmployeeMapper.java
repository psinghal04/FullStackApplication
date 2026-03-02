package com.example.hrapp.employee;

import com.example.hrapp.employee.dto.EmployeeCreateDTO;
import com.example.hrapp.employee.dto.EmployeeDetailsDTO;
import com.example.hrapp.employee.dto.EmployeeContactUpdateDTO;
import com.example.hrapp.employee.dto.EmployeeSummaryDTO;
import com.example.hrapp.employee.dto.EmployeeUpdateDTO;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Maps between employee entities and API DTOs.
 *
 * <p>Timestamp ownership stays in the backend layer to ensure consistent audit fields regardless
 * of client payload contents.</p>
 */
@Component
public class EmployeeMapper {

    public Employee toEntity(EmployeeCreateDTO source) {
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
        return employee;
    }

    public void updateEntity(EmployeeUpdateDTO source, Employee target) {
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
    }

    public void updateContactFields(EmployeeContactUpdateDTO source, Employee target) {
        if (source.homeAddress() != null) {
            target.setHomeAddress(source.homeAddress());
        }
        if (source.mailingAddress() != null) {
            target.setMailingAddress(source.mailingAddress());
        }
        if (source.telephoneNumber() != null) {
            target.setTelephoneNumber(source.telephoneNumber());
        }
        target.setUpdatedAt(OffsetDateTime.now());
    }

    public EmployeeSummaryDTO toSummaryDTO(Employee source) {
        return new EmployeeSummaryDTO(
            source.getId(),
            source.getEmployeeId(),
            source.getFirstName(),
            source.getLastName(),
            source.getJobTitle(),
            source.getEmailAddress(),
            source.getDateOfHire(),
            source.getDateOfTermination()
        );
    }

    public EmployeeDetailsDTO toDetailsDTO(Employee source) {
        return new EmployeeDetailsDTO(
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
            source.getUpdatedAt()
        );
    }
}
