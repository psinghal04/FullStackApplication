import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const HrGuard: CanActivateFn = async (_route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    await authService.login(window.location.origin + state.url);
    return false;
  }

  if (!authService.hasRole('HR_ADMIN')) {
    return router.parseUrl('/employee/profile');
  }

  return true;
};
