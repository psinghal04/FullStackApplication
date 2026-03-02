import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import * as Sentry from '@sentry/angular';
import { Observable } from 'rxjs';

@Injectable()
export class CorrelationIdInterceptorExample implements HttpInterceptor {
  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    // Example only: emit correlation IDs only for backend API traffic.
    // This avoids attaching app correlation headers to Keycloak/OIDC endpoints.
    const isBackendApiCall = req.url.startsWith('/api/') || req.url.includes('/api/v1/');
    if (!isBackendApiCall) {
      return next.handle(req);
    }

    const correlationId = crypto.randomUUID();

    Sentry.addBreadcrumb({
      category: 'http',
      level: 'info',
      message: `request correlation id: ${correlationId}`
    });

    Sentry.setTag('last_correlation_id', correlationId);

    const nextReq = req.clone({
      setHeaders: {
        'X-Correlation-Id': correlationId
      }
    });

    return next.handle(nextReq);
  }
}
