import { inject, Injectable, signal } from '@angular/core';
import Keycloak, { KeycloakInitOptions } from 'keycloak-js';
import { KEYCLOAK_INSTANCE } from './keycloak.tokens';

export interface CurrentUser {
  username: string;
  roles: string[];
  employeeId: string | null;
}

interface TokenClaims {
  preferred_username?: string;
  realm_access?: {
    roles?: string[];
  };
  employee_id?: string;
}

interface E2EMockAuthConfig {
  username: string;
  roles: string[];
  employeeId: string | null;
  accessToken?: string;
}

interface WindowWithE2EMockAuth {
  __HR_E2E_AUTH__?: E2EMockAuthConfig;
  Cypress?: unknown;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly keycloak: Keycloak = inject(KEYCLOAK_INSTANCE);

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
        accessToken: 'e2e-mock-token'
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
        employeeId: this.e2eMockAuth.employeeId
      });
      return;
    }

    const initOptions: KeycloakInitOptions = {
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false
    };

    const isAuthenticated = await this.keycloak.init(initOptions);
    this.authenticatedSignal.set(isAuthenticated);
    this.setCurrentUserFromToken();

    if (isAuthenticated) {
      await this.keycloak.updateToken(30).catch(() => {
        this.authenticatedSignal.set(false);
        this.currentUserSignal.set(null);
      });
    }
  }

  login(redirectUri?: string): Promise<void> {
    if (this.e2eMockAuth) {
      return Promise.resolve();
    }
    return this.keycloak.login({ redirectUri }) as Promise<void>;
  }

  logout(): Promise<void> {
    this.authenticatedSignal.set(false);
    this.currentUserSignal.set(null);

    if (this.e2eMockAuth) {
      return Promise.resolve();
    }

    return this.keycloak.logout({ redirectUri: window.location.origin + '/login' }) as Promise<void>;
  }

  hasRole(role: string): boolean {
    return this.currentUserSignal()?.roles.includes(role) ?? false;
  }

  getEmployeeId(): string | null {
    return this.currentUserSignal()?.employeeId ?? null;
  }

  async getAccessToken(): Promise<string | null> {
    if (this.e2eMockAuth) {
      return this.e2eMockAuth.accessToken ?? 'e2e-mock-token';
    }

    if (!this.authenticatedSignal()) {
      return null;
    }

    const currentToken = this.keycloak.token ?? null;
    if (!currentToken) {
      return null;
    }

    try {
      await this.keycloak.updateToken(30);
    } catch {
      return currentToken;
    }

    return this.keycloak.token ?? currentToken;
  }

  private setCurrentUserFromToken(): void {
    const claims = (this.keycloak.tokenParsed ?? {}) as TokenClaims;
    const roles = claims.realm_access?.roles ?? [];

    if (!this.authenticatedSignal()) {
      this.currentUserSignal.set(null);
      return;
    }

    this.currentUserSignal.set({
      username: claims.preferred_username ?? 'unknown',
      roles,
      employeeId: this.extractEmployeeId(claims)
    });
  }

  private extractEmployeeId(claims: TokenClaims): string | null {
    if (claims.employee_id && claims.employee_id.trim().length > 0) {
      return claims.employee_id;
    }

    const preferredUsername = claims.preferred_username?.trim();
    if (!preferredUsername) {
      return null;
    }

    const atIndex = preferredUsername.indexOf('@');
    const candidate = (atIndex > 0 ? preferredUsername.slice(0, atIndex) : preferredUsername).trim();

    if (!candidate || !candidate.includes('-')) {
      return null;
    }

    if (!/^[a-zA-Z0-9-]+$/.test(candidate)) {
      return null;
    }

    return candidate.toUpperCase();
  }
}
