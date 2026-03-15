import { APP_INITIALIZER } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter, withPreloading } from '@angular/router';
import { AppComponent } from './app/app.component';
import { csrfInterceptor } from './app/api/csrf.interceptor';
import { appRoutes } from './app/app.routes';
import { AuthService } from './app/auth/auth.service';
import { SelectivePreloadingStrategy } from './app/routing/selective-preloading.strategy';

function initializeAuth(authService: AuthService): () => Promise<void> {
  return () => authService.init();
}

bootstrapApplication(AppComponent, {
  providers: [
    provideHttpClient(withInterceptors([csrfInterceptor])),
    provideRouter(appRoutes, withPreloading(SelectivePreloadingStrategy)),
    provideAnimations(),
    {
      provide: APP_INITIALIZER,
      useFactory: initializeAuth,
      deps: [AuthService],
      multi: true
    }
  ]
}).catch((error: unknown) => console.error(error));
