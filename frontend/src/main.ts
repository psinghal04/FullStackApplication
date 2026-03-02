import { APP_INITIALIZER } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter, withPreloading } from '@angular/router';
import Keycloak from 'keycloak-js';
import { AppComponent } from './app/app.component';
import { appRoutes } from './app/app.routes';
import { AuthService } from './app/auth/auth.service';
import { KEYCLOAK_INSTANCE } from './app/auth/keycloak.tokens';
import { SelectivePreloadingStrategy } from './app/routing/selective-preloading.strategy';

const keycloak = new Keycloak({
  url: (window as { __HR_APP_CONFIG__?: { keycloakUrl?: string } }).__HR_APP_CONFIG__?.keycloakUrl ?? 'http://localhost:8080',
  realm: (window as { __HR_APP_CONFIG__?: { keycloakRealm?: string } }).__HR_APP_CONFIG__?.keycloakRealm ?? 'hr',
  clientId: (window as { __HR_APP_CONFIG__?: { keycloakClientId?: string } }).__HR_APP_CONFIG__?.keycloakClientId ?? 'hr-frontend'
});

function initializeAuth(authService: AuthService): () => Promise<void> {
  return () => authService.init();
}

bootstrapApplication(AppComponent, {
  providers: [
    provideHttpClient(),
    provideRouter(appRoutes, withPreloading(SelectivePreloadingStrategy)),
    provideAnimations(),
    { provide: KEYCLOAK_INSTANCE, useValue: keycloak },
    {
      provide: APP_INITIALIZER,
      useFactory: initializeAuth,
      deps: [AuthService],
      multi: true
    }
  ]
}).catch((error: unknown) => console.error(error));
