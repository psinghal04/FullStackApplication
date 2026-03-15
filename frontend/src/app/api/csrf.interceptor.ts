import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { CsrfTokenStore } from '../auth/csrf-token.store';

/**
 * HTTP interceptor that injects CSRF token into non-GET requests.
 * 
 * Spring Security's CSRF protection expects the token in a custom header
 * (default: X-XSRF-TOKEN) for state-changing operations.
 */
export const csrfInterceptor: HttpInterceptorFn = (req, next) => {
  const csrfStore = inject(CsrfTokenStore);
  const token = csrfStore.token();
  const headerName = csrfStore.headerName();

  // Only add CSRF token to state-changing methods (not GET/HEAD/OPTIONS)
  const requiresCsrf = !['GET', 'HEAD', 'OPTIONS'].includes(req.method);

  if (requiresCsrf && token) {
    req = req.clone({
      setHeaders: {
        [headerName]: token
      }
    });
  }

  return next(req);
};
