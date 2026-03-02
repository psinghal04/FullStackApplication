import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { EmployeeApiService } from '../api/employee-api.service';
import { EmployeeDetails, EmployeeSummary } from '../api/employee.models';
import { EmployeeFormComponent } from '../components/employee-form.component';

@Component({
  selector: 'app-hr-employee-details-page',
  standalone: true,
  imports: [CommonModule, EmployeeFormComponent],
  template: `
    <section class="space-y-4">
      <h2 class="ui-section-title">HR Employee Detail</h2>
      <p class="ui-helper-text">Employee ID: {{ employeeId }}</p>

      @if (isLoading) {
        <div class="ui-card space-y-3">
          <div class="app-skeleton h-6 w-56 rounded"></div>
          <div class="app-skeleton h-4 w-64 rounded"></div>
        </div>
      }

      @if (loadError) {
        <p class="ui-error-text">{{ loadError }}</p>
      }

      @if (employeeId && employeeDetails) {
        <app-employee-form [employeeId]="employeeId" [employee]="employeeDetails" (saved)="onSaved($event)"></app-employee-form>
      }
    </section>
  `
})
export class HrEmployeeDetailsPageComponent {
  readonly employeeId: string | null;
  employeeDetails: EmployeeDetails | null = null;
  loadError: string | null = null;
  isLoading = true;

  private readonly api = inject(EmployeeApiService);

  constructor(route: ActivatedRoute) {
    this.employeeId = route.snapshot.paramMap.get('employeeId');

    if (this.employeeId) {
      this.api.getEmployeeDetails(this.employeeId).subscribe({
        next: (details) => {
          this.isLoading = false;
          this.employeeDetails = details;
        },
        error: (error: { error?: { message?: string } }) => {
          this.isLoading = false;
          this.loadError = error.error?.message ?? 'Failed to load employee details.';
        }
      });
      return;
    }

    this.isLoading = false;
  }

  onSaved(summary: EmployeeSummary): void {
    if (this.employeeDetails) {
      this.employeeDetails = {
        ...this.employeeDetails,
        employeeId: summary.employeeId,
        firstName: summary.firstName,
        lastName: summary.lastName,
        jobTitle: summary.jobTitle,
        emailAddress: summary.emailAddress,
        dateOfTermination: summary.dateOfTermination
      };
    }
  }
}

