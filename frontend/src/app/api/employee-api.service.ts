import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { from, Observable, throwError } from 'rxjs';
import { switchMap, timeout } from 'rxjs/operators';
import { AuthService } from '../auth/auth.service';
import {
  EmployeeCreateRequest,
  EmployeeContactUpdateRequest,
  EmployeeDetails,
  EmployeeSummary,
  EmployeeUpdateRequest,
  PageResponse
} from './employee.models';

@Injectable({ providedIn: 'root' })
export class EmployeeApiService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  private readonly apiBaseUrl =
    (window as { __HR_APP_CONFIG__?: { apiBaseUrl?: string } }).__HR_APP_CONFIG__?.apiBaseUrl ??
    '/api/v1/employees';

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

  private withAuthHeaders<T>(operation: (headers: HttpHeaders) => Observable<T>): Observable<T> {
    return from(this.authService.getAccessToken()).pipe(
      switchMap((token) => {
        if (!token) {
          return throwError(() => ({
            status: 401,
            error: {
              status: 401,
              message: 'Session expired. Please sign in again.'
            }
          }));
        }

        const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
        return operation(headers);
      })
    );
  }
}
