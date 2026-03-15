import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { CsrfTokenStore } from './csrf-token.store';

export interface CurrentUser {
  username: string;
  roles: string[];
  employeeId: string | null;
  email: string | null;
}

interface BffUserResponse {
  employeeId: string | null;
  username: string;
  email: string | null;
  roles: string[];
}

interface CsrfTokenResponse {
  token: string;
  headerName: string;
  parameterName: string;
}

interface E2EMockAuthConfig {
  username: string;
  roles: string[];
  employeeId: string | null;
  email?: string | null;
}

interface WindowWithE2EMockAuth {
  __HR_E2E_AUTH__?: E2EMockAuthConfig;
  Cypress?: unknown;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly csrfStore = inject(CsrfTokenStore);

  private readonly authenticatedSignal = signal<boolean>(false);
  private readonly currentUserSignal = signal<CurrentUser | null>(null);

  readonly isAuthenticated = this.authenticatedSignal.asReadonly();
  readonly currentUser = this.currentUserSignal.asReadonly();

  private readonly e2eMockAuth: E2EMockAuthConfig | null = (() => {
    const appWindow = window as WindowWithE2EMockAuth;
    if (appWindow.__HR_E2E_AUTH__) {
      return appWindow.__HR_E2E_AUTH__;
    }
    if (appWindow.Cypress) {
      return {
        username: 'e2e-user',
        roles: ['HR_ADMIN', 'EMPLOYEE'],
        employeeId: 'EMP-900001',
        email: 'e2e-user@company.local'
      };
    }
    return null;
  })();

  async init(): Promise<void> {
    if (this.e2eMockAuth) {
      this.authenticatedSignal.set(true);
      this.currentUserSignal.set({
        username: this.e2eMockAuth.username,
        roles: this.e2eMockAuth.roles,
        employeeId: this.e2eMockAuth.employeeId,
        email: this.e2eMockAuth.email ?? null
      });
      return;
    }

    // Fetch CSRF token
    try {
      const csrfResponse = await firstValueFrom(
        this.http.get<CsrfTokenResponse>('/api/auth/csrf')
      );
      this.csrfStore.setToken(csrfResponse.token, csrfResponse.headerName);
    } catch (error) {
      console.error('Failed to fetch CSRF token:', error);
    }

    // Check if user is authenticated
    try {
      const userResponse = await firstValueFrom(
        this.http.get<BffUserResponse>('/api/auth/me')
      );
      this.authenticatedSignal.set(true);
      this.currentUserSignal.set({
        username: userResponse.username,
        roles: userResponse.roles,
        employeeId: userResponse.employeeId,
        email: userResponse.email
      });
    } catch {
      // Not authenticated - this is expected for unauthenticated users
      this.authenticatedSignal.set(false);
      this.currentUserSignal.set(null);
    }
  }

  login(redirectUri?: string): void {
    if (this.e2eMockAuth) {
      return;
    }
    // Navigate directly to OAuth2 authorization endpoint, bypassing /api/auth/login
    // to avoid redirect loop caused by Spring Security's loginPage configuration
    window.location.href = '/oauth2/authorization/keycloak';
  }

  async logout(): Promise<void> {
    if (this.e2eMockAuth) {
      this.authenticatedSignal.set(false);
      this.currentUserSignal.set(null);
      this.csrfStore.clearToken();
      return Promise.resolve();
    }

    try {
      // Send CSRF token BEFORE clearing it — the interceptor reads it from the store.
      const result = await firstValueFrom(
        this.http.post<{ logoutUrl: string }>('/api/auth/logout', {})
      );
      // Redirect to Keycloak's end_session endpoint to terminate the SSO session.
      // Without this, Keycloak auto-signs the user back in on the next page load.
      const keycloakLogoutUrl = result?.logoutUrl;
      if (keycloakLogoutUrl) {
        this.authenticatedSignal.set(false);
        this.currentUserSignal.set(null);
        this.csrfStore.clearToken();
        window.location.href = keycloakLogoutUrl;
        return;
      }
    } catch (error) {
      console.error('Logout failed:', error);
    }

    // Clear in-memory state after the server call (token needed for the POST above).
    this.authenticatedSignal.set(false);
    this.currentUserSignal.set(null);
    this.csrfStore.clearToken();

    // Redirect to login page
    window.location.href = '/login';
  }

  hasRole(role: string): boolean {
    return this.currentUserSignal()?.roles.includes(role) ?? false;
  }

  getEmployeeId(): string | null {
    return this.currentUserSignal()?.employeeId ?? null;
  }

  /**
   * @deprecated Access tokens are no longer exposed in BFF pattern.
   * Authentication is handled via HttpOnly session cookies.
   */
  async getAccessToken(): Promise<string | null> {
    console.warn('getAccessToken() is deprecated in BFF pattern - authentication uses session cookies');
    return null;
  }
}
