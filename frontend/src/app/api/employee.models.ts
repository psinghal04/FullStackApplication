export interface EmployeeSummary {
  id: string;
  employeeId: string;
  firstName: string;
  lastName: string;
  jobTitle: string;
  emailAddress: string;
  dateOfHire: string;
  dateOfTermination: string | null;
}

export interface EmployeeDetails {
  id: string;
  employeeId: string;
  firstName: string;
  lastName: string;
  jobTitle: string;
  dateOfBirth: string;
  gender: string;
  dateOfHire: string;
  dateOfTermination: string | null;
  homeAddress: string;
  mailingAddress: string;
  telephoneNumber: string;
  emailAddress: string;
  createdAt: string;
  updatedAt: string;
}

export interface EmployeeUpdateRequest {
  employeeId: string;
  firstName: string;
  lastName: string;
  jobTitle: string;
  dateOfBirth: string;
  gender: string;
  dateOfHire: string;
  dateOfTermination: string | null;
  homeAddress: string;
  mailingAddress: string;
  telephoneNumber: string;
  emailAddress: string;
}

export interface EmployeeCreateRequest {
  firstName: string;
  lastName: string;
  jobTitle: string;
  dateOfBirth: string;
  gender: string;
  dateOfHire: string;
  dateOfTermination: string | null;
  homeAddress: string;
  mailingAddress: string;
  telephoneNumber: string;
  emailAddress: string;
}

export interface EmployeeContactUpdateRequest {
  homeAddress: string;
  mailingAddress: string;
  telephoneNumber: string;
}

export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface ApiError {
  status: number;
  message: string;
  timestamp?: string;
  reason?: string;
}

// V2 Models with Manager Support

export interface ManagerReference {
  id: string;
  employeeId: string;
  firstName: string;
  lastName: string;
  jobTitle: string;
}

export interface EmployeeSummaryV2 {
  id: string;
  employeeId: string;
  firstName: string;
  lastName: string;
  jobTitle: string;
  emailAddress: string;
  dateOfHire: string;
  dateOfTermination: string | null;
  manager: ManagerReference | null;
}

export interface EmployeeDetailsV2 {
  id: string;
  employeeId: string;
  firstName: string;
  lastName: string;
  jobTitle: string;
  dateOfBirth: string;
  gender: string;
  dateOfHire: string;
  dateOfTermination: string | null;
  homeAddress: string;
  mailingAddress: string;
  telephoneNumber: string;
  emailAddress: string;
  createdAt: string;
  updatedAt: string;
  manager: ManagerReference | null;
}

export interface EmployeeCreateV2Request {
  firstName: string;
  lastName: string;
  jobTitle: string;
  dateOfBirth: string;
  gender: string;
  dateOfHire: string;
  dateOfTermination: string | null;
  homeAddress: string;
  mailingAddress: string;
  telephoneNumber: string;
  emailAddress: string;
  managerId: string | null;
}

export interface EmployeeUpdateV2Request {
  employeeId: string;
  firstName: string;
  lastName: string;
  jobTitle: string;
  dateOfBirth: string;
  gender: string;
  dateOfHire: string;
  dateOfTermination: string | null;
  homeAddress: string;
  mailingAddress: string;
  telephoneNumber: string;
  emailAddress: string;
  managerId: string | null;
}
