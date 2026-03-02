import * as Sentry from '@sentry/angular';
import { BrowserTracing } from '@sentry/browser';

// Example only: adapt DSN/release/targets per environment.
Sentry.init({
  dsn: 'https://<public-key>@o0.ingest.sentry.io/<project-id>',
  environment: 'dev',
  release: 'hr-frontend@local',
  integrations: [new BrowserTracing()],
  tracesSampleRate: 0.2,
  tracePropagationTargets: [
    'localhost',
    /^\/api\//,
    /^https:\/\/api\.example\.com\//
  ]
});
