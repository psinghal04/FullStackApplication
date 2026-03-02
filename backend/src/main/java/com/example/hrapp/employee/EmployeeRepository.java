package com.example.hrapp.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByEmployeeId(String employeeId);

    Optional<Employee> findByEmailAddressIgnoreCase(String emailAddress);

    boolean existsByEmployeeId(String employeeId);

    List<Employee> findByLastNameContainingIgnoreCase(String partial);

    Page<Employee> findByLastNameContainingIgnoreCase(String partial, Pageable pageable);

    Page<Employee> findByEmployeeIdIgnoreCase(String employeeId, Pageable pageable);
}
