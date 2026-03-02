import { InjectionToken } from '@angular/core';
import Keycloak from 'keycloak-js';

export const KEYCLOAK_INSTANCE = new InjectionToken<Keycloak>('KEYCLOAK_INSTANCE');
