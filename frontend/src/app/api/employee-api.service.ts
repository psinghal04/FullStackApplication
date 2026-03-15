import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { timeout } from 'rxjs/operators';
import {
  EmployeeCreateRequest,
  EmployeeContactUpdateRequest,
  EmployeeDetails,
  EmployeeSummary,
  EmployeeUpdateRequest,
  PageResponse,
  EmployeeSummaryV2,
  EmployeeDetailsV2,
  EmployeeCreateV2Request,
  EmployeeUpdateV2Request
} from './employee.models';

@Injectable({ providedIn: 'root' })
export class EmployeeApiService {
  private readonly http = inject(HttpClient);

  private readonly apiBaseUrl =
    (window as { __HR_APP_CONFIG__?: { apiBaseUrl?: string } }).__HR_APP_CONFIG__?.apiBaseUrl ??
    '/api/v1/employees';

  private readonly apiV2BaseUrl =
    (window as { __HR_APP_CONFIG__?: { apiV2BaseUrl?: string } }).__HR_APP_CONFIG__?.apiV2BaseUrl ??
    '/api/v2/employees';

  searchEmployees(criteria: { employeeId?: string | null; lastName?: string | null }, page: number, size: number): Observable<PageResponse<EmployeeSummary>> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));

    const employeeId = criteria.employeeId?.trim();
    const lastName = criteria.lastName?.trim();

    if (employeeId) {
      params = params.set('employeeId', employeeId);
    }

    if (lastName) {
      params = params.set('lastName', lastName);
    }

    return this.withAuthHeaders((headers) =>
      this.http.get<PageResponse<EmployeeSummary>>(`${this.apiBaseUrl}/search`, { headers, params })
    ).pipe(timeout(15000));
  }

  getEmployeeDetails(employeeId: string): Observable<EmployeeDetails> {
    return this.withAuthHeaders((headers) =>
      this.http.get<EmployeeDetails>(`${this.apiBaseUrl}/${encodeURIComponent(employeeId)}`, { headers })
    );
  }

  getMyEmployeeDetails(): Observable<EmployeeDetails> {
    return this.withAuthHeaders((headers) =>
      this.http.get<EmployeeDetails>(`${this.apiBaseUrl}/me`, { headers })
    );
  }

  createEmployee(payload: EmployeeCreateRequest): Observable<EmployeeSummary> {
    return this.withAuthHeaders((headers) =>
      this.http.post<EmployeeSummary>(this.apiBaseUrl, payload, { headers })
    );
  }

  updateEmployee(employeeId: string, payload: EmployeeUpdateRequest): Observable<EmployeeSummary> {
    return this.withAuthHeaders((headers) =>
      this.http.put<EmployeeSummary>(`${this.apiBaseUrl}/${encodeURIComponent(employeeId)}`, payload, { headers })
    );
  }

  patchEmployeeContact(employeeId: string, payload: EmployeeContactUpdateRequest): Observable<EmployeeSummary> {
    return this.withAuthHeaders((headers) =>
      this.http.patch<EmployeeSummary>(`${this.apiBaseUrl}/${encodeURIComponent(employeeId)}/contact`, payload, { headers })
    );
  }

  // V2 API Methods with Manager Support

  searchEmployeesV2(criteria: { employeeId?: string | null; lastName?: string | null }, page: number, size: number): Observable<PageResponse<EmployeeSummaryV2>> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));

    const employeeId = criteria.employeeId?.trim();
    const lastName = criteria.lastName?.trim();

    if (employeeId) {
      params = params.set('employeeId', employeeId);
    }

    if (lastName) {
      params = params.set('lastName', lastName);
    }

    return this.withAuthHeaders((headers) =>
      this.http.get<PageResponse<EmployeeSummaryV2>>(`${this.apiV2BaseUrl}/search`, { headers, params })
    ).pipe(timeout(15000));
  }

  getEmployeeDetailsV2(employeeId: string): Observable<EmployeeDetailsV2> {
    return this.withAuthHeaders((headers) =>
      this.http.get<EmployeeDetailsV2>(`${this.apiV2BaseUrl}/${encodeURIComponent(employeeId)}`, { headers })
    );
  }

  getMyEmployeeDetailsV2(): Observable<EmployeeDetailsV2> {
    return this.withAuthHeaders((headers) =>
      this.http.get<EmployeeDetailsV2>(`${this.apiV2BaseUrl}/me`, { headers })
    );
  }

  createEmployeeV2(payload: EmployeeCreateV2Request): Observable<EmployeeSummaryV2> {
    return this.withAuthHeaders((headers) =>
      this.http.post<EmployeeSummaryV2>(this.apiV2BaseUrl, payload, { headers })
    );
  }

  updateEmployeeV2(employeeId: string, payload: EmployeeUpdateV2Request): Observable<EmployeeSummaryV2> {
    return this.withAuthHeaders((headers) =>
      this.http.put<EmployeeSummaryV2>(`${this.apiV2BaseUrl}/${encodeURIComponent(employeeId)}`, payload, { headers })
    );
  }

  getSubordinates(managerEmployeeId: string): Observable<EmployeeSummaryV2[]> {
    return this.withAuthHeaders((headers) =>
      this.http.get<EmployeeSummaryV2[]>(`${this.apiV2BaseUrl}/${encodeURIComponent(managerEmployeeId)}/subordinates`, { headers })
    );
  }

  private withAuthHeaders<T>(operation: (headers: HttpHeaders) => Observable<T>): Observable<T> {
    // BFF pattern: authentication is via HttpOnly session cookie, no Bearer token needed.
    // Cookies are sent automatically by the browser on same-origin requests.
    return operation(new HttpHeaders());
  }
}
