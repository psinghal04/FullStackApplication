package com.example.hrapp.employee;

import com.example.hrapp.employee.dto.EmployeeCreateDTO;
import com.example.hrapp.employee.dto.EmployeeDetailsDTO;
import com.example.hrapp.employee.dto.EmployeeContactUpdateDTO;
import com.example.hrapp.employee.dto.EmployeeSummaryDTO;
import com.example.hrapp.employee.dto.EmployeeUpdateDTO;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class EmployeeMapper {

    public Employee toEntity(EmployeeCreateDTO source) {
        Employee employee = new Employee();
        employee.setId(UUID.randomUUID());
        employee.setFirstName(source.getFirstName());
        employee.setLastName(source.getLastName());
        employee.setJobTitle(source.getJobTitle());
        employee.setDateOfBirth(source.getDateOfBirth());
        employee.setGender(source.getGender());
        employee.setDateOfHire(source.getDateOfHire());
        employee.setDateOfTermination(source.getDateOfTermination());
        employee.setHomeAddress(source.getHomeAddress());
        employee.setMailingAddress(source.getMailingAddress());
        employee.setTelephoneNumber(source.getTelephoneNumber());
        employee.setEmailAddress(source.getEmailAddress());

        OffsetDateTime now = OffsetDateTime.now();
        employee.setCreatedAt(now);
        employee.setUpdatedAt(now);
        return employee;
    }

    public void updateEntity(EmployeeUpdateDTO source, Employee target) {
        target.setEmployeeId(source.getEmployeeId());
        target.setFirstName(source.getFirstName());
        target.setLastName(source.getLastName());
        target.setJobTitle(source.getJobTitle());
        target.setDateOfBirth(source.getDateOfBirth());
        target.setGender(source.getGender());
        target.setDateOfHire(source.getDateOfHire());
        target.setDateOfTermination(source.getDateOfTermination());
        target.setHomeAddress(source.getHomeAddress());
        target.setMailingAddress(source.getMailingAddress());
        target.setTelephoneNumber(source.getTelephoneNumber());
        target.setUpdatedAt(OffsetDateTime.now());
    }

    public void updateContactFields(EmployeeContactUpdateDTO source, Employee target) {
        if (source.getHomeAddress() != null) {
            target.setHomeAddress(source.getHomeAddress());
        }
        if (source.getMailingAddress() != null) {
            target.setMailingAddress(source.getMailingAddress());
        }
        if (source.getTelephoneNumber() != null) {
            target.setTelephoneNumber(source.getTelephoneNumber());
        }
        target.setUpdatedAt(OffsetDateTime.now());
    }

    public EmployeeSummaryDTO toSummaryDTO(Employee source) {
        EmployeeSummaryDTO dto = new EmployeeSummaryDTO();
        dto.setId(source.getId());
        dto.setEmployeeId(source.getEmployeeId());
        dto.setFirstName(source.getFirstName());
        dto.setLastName(source.getLastName());
        dto.setJobTitle(source.getJobTitle());
        dto.setEmailAddress(source.getEmailAddress());
        dto.setDateOfHire(source.getDateOfHire());
        dto.setDateOfTermination(source.getDateOfTermination());
        return dto;
    }

    public EmployeeDetailsDTO toDetailsDTO(Employee source) {
        EmployeeDetailsDTO dto = new EmployeeDetailsDTO();
        dto.setId(source.getId());
        dto.setEmployeeId(source.getEmployeeId());
        dto.setFirstName(source.getFirstName());
        dto.setLastName(source.getLastName());
        dto.setJobTitle(source.getJobTitle());
        dto.setDateOfBirth(source.getDateOfBirth());
        dto.setGender(source.getGender());
        dto.setDateOfHire(source.getDateOfHire());
        dto.setDateOfTermination(source.getDateOfTermination());
        dto.setHomeAddress(source.getHomeAddress());
        dto.setMailingAddress(source.getMailingAddress());
        dto.setTelephoneNumber(source.getTelephoneNumber());
        dto.setEmailAddress(source.getEmailAddress());
        dto.setCreatedAt(source.getCreatedAt());
        dto.setUpdatedAt(source.getUpdatedAt());
        return dto;
    }
}
