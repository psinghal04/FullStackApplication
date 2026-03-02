import { Routes } from '@angular/router';
import { EmployeeGuard } from '../auth/employee.guard';

export const employeeRoutes: Routes = [
  {
    path: 'employee/profile',
    canActivate: [EmployeeGuard],
    loadComponent: () =>
      import('../pages/employee-profile-page.component').then((module) => module.EmployeeProfilePageComponent),
    data: { preload: true }
  }
];