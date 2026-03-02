import { Routes } from '@angular/router';
import { AppShellComponent } from './layout/app-shell.component';
import { LoginPageComponent } from './pages/login-page.component';
import { TerminatedPageComponent } from './pages/terminated-page.component';

export const appRoutes: Routes = [
  {
    path: 'login',
    component: LoginPageComponent
  },
  {
    path: 'terminated',
    component: TerminatedPageComponent
  },
  {
    path: '',
    component: AppShellComponent,
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'employee/profile'
      },
      {
        path: '',
        loadChildren: () => import('./routing/employee.routes').then((module) => module.employeeRoutes)
      },
      {
        path: '',
        loadChildren: () => import('./routing/hr.routes').then((module) => module.hrRoutes)
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
