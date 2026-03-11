package com.example.hrapp.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for employee records.
 */
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByEmployeeId(String employeeId);

    Optional<Employee> findByEmailAddressIgnoreCase(String emailAddress);

    boolean existsByEmployeeId(String employeeId);

    List<Employee> findByLastNameContainingIgnoreCase(String partial);

    Page<Employee> findByLastNameContainingIgnoreCase(String partial, Pageable pageable);

    Page<Employee> findByEmployeeIdIgnoreCase(String employeeId, Pageable pageable);

    /**
     * Fetches an employee by employeeId with manager relationship eagerly loaded.
     * Used by V2 API to include manager information in response.
     */
    @Query("SELECT e FROM Employee e LEFT JOIN FETCH e.manager WHERE e.employeeId = :employeeId")
    Optional<Employee> findByEmployeeIdWithManager(@Param("employeeId") String employeeId);

    /**
     * Fetches an employee by employeeId with subordinates relationship eagerly loaded.
     * Also fetches the manager of each subordinate for proper DTO mapping.
     * Used by V2 API to retrieve direct reports.
     */
    @Query("SELECT DISTINCT e FROM Employee e " +
           "LEFT JOIN FETCH e.subordinates s " +
           "LEFT JOIN FETCH s.manager " +
           "WHERE e.employeeId = :employeeId")
    Optional<Employee> findByEmployeeIdWithSubordinates(@Param("employeeId") String employeeId);

    /**
     * Searches employees by employeeId with manager relationship eagerly loaded.
     * Used by V2 API search to include manager information.
     */
    @Query("SELECT DISTINCT e FROM Employee e LEFT JOIN FETCH e.manager WHERE LOWER(e.employeeId) = LOWER(:employeeId)")
    List<Employee> findByEmployeeIdIgnoreCaseWithManager(@Param("employeeId") String employeeId);

    /**
     * Searches employees by partial lastName with manager relationship eagerly loaded.
     * Used by V2 API search to include manager information.
     */
    @Query("SELECT DISTINCT e FROM Employee e LEFT JOIN FETCH e.manager WHERE LOWER(e.lastName) LIKE LOWER(CONCAT('%', :partial, '%'))")
    List<Employee> findByLastNameContainingIgnoreCaseWithManager(@Param("partial") String partial);
}
