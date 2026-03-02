import { Routes } from '@angular/router';
import { HrGuard } from '../auth/hr.guard';

export const hrRoutes: Routes = [
  {
    path: 'hr/employees',
    canActivate: [HrGuard],
    loadComponent: () =>
      import('../components/hr-employee-search.component').then((module) => module.HrEmployeeSearchComponent),
    data: { preload: true }
  },
  {
    path: 'hr/employees/new',
    canActivate: [HrGuard],
    loadComponent: () =>
      import('../pages/hr-employee-create-page.component').then((module) => module.HrEmployeeCreatePageComponent)
  },
  {
    path: 'hr/employees/:employeeId',
    canActivate: [HrGuard],
    loadComponent: () =>
      import('../pages/hr-employee-details-page.component').then((module) => module.HrEmployeeDetailsPageComponent)
  }
];