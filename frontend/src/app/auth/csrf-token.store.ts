import { Injectable, signal } from '@angular/core';

/**
 * Store for managing CSRF token state.
 * The token is fetched from /auth/csrf and injected into requests via the CSRF interceptor.
 */
@Injectable({
  providedIn: 'root'
})
export class CsrfTokenStore {
  private readonly tokenSignal = signal<string | null>(null);
  private readonly headerNameSignal = signal<string>('X-XSRF-TOKEN');

  /**
   * Get the current CSRF token value (readonly signal)
   */
  readonly token = this.tokenSignal.asReadonly();

  /**
   * Get the CSRF header name (readonly signal)
   */
  readonly headerName = this.headerNameSignal.asReadonly();

  /**
   * Set the CSRF token and  header name from backend response
   */
  setToken(token: string, headerName: string = 'X-XSRF-TOKEN'): void {
    this.tokenSignal.set(token);
    this.headerNameSignal.set(headerName);
  }

  /**
   * Clear the stored CSRF token
   */
  clearToken(): void {
    this.tokenSignal.set(null);
  }
}
