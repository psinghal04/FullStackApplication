import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { EmployeeApiService } from '../api/employee-api.service';
import { EmployeeDetailsV2, EmployeeSummaryV2 } from '../api/employee.models';
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

      @if (subordinates.length > 0) {
        <section class="ui-card mb-4 space-y-3">
          <h3 class="text-lg font-semibold">Direct Reports ({{ subordinates.length }})</h3>
          <div class="space-y-2">
            @for (report of subordinates; track report.id) {
              <div class="rounded-md border border-border bg-surface p-3">
                <p><strong>{{ report.firstName }} {{ report.lastName }}</strong></p>
                <p class="text-sm text-muted">{{ report.employeeId }} - {{ report.jobTitle }}</p>
              </div>
            }
          </div>
        </section>
      }
    </section>
  `
})
export class HrEmployeeDetailsPageComponent {
  readonly employeeId: string | null;
  employeeDetails: EmployeeDetailsV2 | null = null;
  subordinates: EmployeeSummaryV2[] = [];
  loadError: string | null = null;
  isLoading = true;

  private readonly api = inject(EmployeeApiService);

  constructor(route: ActivatedRoute) {
    this.employeeId = route.snapshot.paramMap.get('employeeId');

    if (this.employeeId) {
      this.api.getEmployeeDetailsV2(this.employeeId).subscribe({
        next: (details) => {
          this.isLoading = false;
          this.employeeDetails = details;
          
          // Load subordinates
          this.api.getSubordinates(details.employeeId).subscribe({
            next: (subs) => {
              this.subordinates = subs;
            },
            error: () => {
              // Silently fail if subordinates can't be loaded
              this.subordinates = [];
            }
          });
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

  onSaved(summary: EmployeeSummaryV2): void {
    if (this.employeeDetails) {
      this.employeeDetails = {
        ...this.employeeDetails,
        employeeId: summary.employeeId,
        firstName: summary.firstName,
        lastName: summary.lastName,
        jobTitle: summary.jobTitle,
        emailAddress: summary.emailAddress,
        dateOfHire: summary.dateOfHire,
        dateOfTermination: summary.dateOfTermination,
        manager: summary.manager
      };
    }
  }
}

