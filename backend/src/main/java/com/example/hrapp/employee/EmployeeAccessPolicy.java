package com.example.hrapp.employee;

import com.example.hrapp.common.exception.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Imperative access checks used by non-annotation-driven call paths.
 *
 * <p>Most endpoint authorization is declarative via Spring Security expressions; this policy
 * remains useful for explicit checks in service/utility flows when needed.</p>
 */
@Component
public class EmployeeAccessPolicy {

    public void requireHr(String roleHeader) {
        if (!isHr(roleHeader)) {
            throw new AccessDeniedException("HR role is required for this operation");
        }
    }

    public void requireHrOrSelf(String roleHeader, String requesterEmployeeId, String targetEmployeeId) {
        if (isHr(roleHeader)) {
            return;
        }

        if (requesterEmployeeId == null || requesterEmployeeId.isBlank()) {
            throw new AccessDeniedException("Requester identity is required for this operation");
        }

        if (!requesterEmployeeId.equalsIgnoreCase(targetEmployeeId)) {
            throw new AccessDeniedException("You can only access your own employee record");
        }
    }

    private boolean isHr(String roleHeader) {
        return roleHeader != null && roleHeader.equalsIgnoreCase("HR");
    }
}
