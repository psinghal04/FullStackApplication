import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EmployeeApiService } from '../api/employee-api.service';
import { EmployeeSummary, PageResponse } from '../api/employee.models';

@Component({
  selector: 'app-hr-employee-search',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule
  ],
  template: `
    <section class="ui-card">
      <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h2 class="ui-section-title">HR Employee Search</h2>
      </div>

      <form (submit)="$event.preventDefault(); search(0)" class="mb-4 grid gap-3 md:grid-cols-[1fr_1fr_auto]">
        <mat-form-field appearance="fill" subscriptSizing="dynamic">
          <mat-label>Employee ID</mat-label>
          <input
            matInput
            [formControl]="employeeIdControl"
            type="text"
            placeholder="Exact match (e.g. EMP-000123)"
            (keydown.enter)="$event.preventDefault(); search(0)"
          />
          <mat-icon matSuffix>badge</mat-icon>
        </mat-form-field>

        <mat-form-field appearance="fill" subscriptSizing="dynamic">
          <mat-label>Last name</mat-label>
          <input
            matInput
            [formControl]="lastNameControl"
            type="text"
            placeholder="Search by last name (partial)"
            (keydown.enter)="$event.preventDefault(); search(0)"
          />
          <mat-icon matSuffix>search</mat-icon>
        </mat-form-field>
        <button mat-flat-button color="primary" type="button" class="h-14 px-6" (click)="search(0)">
          <span class="inline-flex items-center gap-2">
            <mat-icon>search</mat-icon>
            Search Employees
          </span>
        </button>
      </form>

      <p *ngIf="errorMessage" class="ui-error-text mb-3">{{ errorMessage }}</p>

      @if (isSearching) {
        <div class="mb-4 flex items-center gap-2 text-sm text-muted">
          <mat-spinner diameter="20"></mat-spinner>
          <span>Searching...</span>
        </div>
      }

      @if (isSearching && !pageResult) {
        <div class="space-y-2">
          @for (_ of [1, 2, 3, 4, 5]; track $index) {
            <div class="app-skeleton h-12 rounded-md"></div>
          }
        </div>
      }

      <ng-container *ngIf="pageResult as result">
        <p *ngIf="result.content.length === 0" class="ui-helper-text">
          No employees found for "{{ lastSearchLabel }}".
        </p>

        <div class="overflow-x-auto rounded-md border border-border">
          <table class="app-table">
            <thead class="bg-surface">
              <tr>
                <th>Employee ID</th>
                <th>First Name</th>
                <th>Last Name</th>
                <th>Job Title</th>
                <th>Email</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              <tr
                *ngFor="let employee of result.content; trackBy: trackByEmployee"
                (click)="openDetails(employee)"
                class="cursor-pointer transition-opacity hover:opacity-85"
              >
                <td>{{ employee.employeeId }}</td>
                <td>{{ employee.firstName }}</td>
                <td>{{ employee.lastName }}</td>
                <td>{{ employee.jobTitle }}</td>
                <td>{{ employee.emailAddress }}</td>
                <td>{{ statusLabel(employee) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="mt-4 flex flex-wrap items-center gap-2 text-sm">
          <button mat-stroked-button type="button" (click)="previousPage()" [disabled]="page <= 0">
            <span class="inline-flex items-center gap-1">
              <mat-icon>chevron_left</mat-icon>
              Previous
            </span>
          </button>
          <span class="ui-helper-text">Page {{ page + 1 }} / {{ pageResult.totalPages || 1 }}</span>
          <button mat-stroked-button type="button" (click)="nextPage()" [disabled]="(page + 1) >= pageResult.totalPages">
            <span class="inline-flex items-center gap-1">
              Next
              <mat-icon>chevron_right</mat-icon>
            </span>
          </button>
          <span class="ui-helper-text">Total: {{ pageResult.totalElements }}</span>
        </div>
      </ng-container>
    </section>
  `
})
export class HrEmployeeSearchComponent {
  private readonly api = inject(EmployeeApiService);
  private readonly router = inject(Router);

  readonly employeeIdControl = new FormControl<string>('', { nonNullable: true });

  readonly lastNameControl = new FormControl<string>('', {
    nonNullable: true
  });

  page = 0;
  readonly size = 25;
  pageResult: PageResponse<EmployeeSummary> | null = null;
  errorMessage: string | null = null;
  lastSearchLabel = '';
  isSearching = false;

  search(page: number): void {
    const employeeId = this.employeeIdControl.value.trim();
    const lastName = this.lastNameControl.value.trim();

    if (!employeeId && !lastName) {
      this.errorMessage = 'Please enter either an Employee ID or a last name to search.';
      return;
    }

    this.errorMessage = null;
    this.lastSearchLabel = employeeId || lastName;
    this.page = page;
    this.isSearching = true;

    this.api.searchEmployees({ employeeId, lastName }, this.page, this.size).subscribe({
      next: (result) => {
        this.pageResult = {
          ...result,
          content: result.content ?? []
        };
        this.isSearching = false;
      },
      error: (error: { error?: { message?: string } }) => {
        this.errorMessage = error.error?.message ?? 'Unable to search employees. Please try again.';
        this.pageResult = null;
        this.isSearching = false;
      }
    });
  }

  previousPage(): void {
    if (this.page > 0) {
      this.search(this.page - 1);
    }
  }

  nextPage(): void {
    if (this.pageResult && this.page + 1 < this.pageResult.totalPages) {
      this.search(this.page + 1);
    }
  }

  openDetails(employee: EmployeeSummary): void {
    void this.router.navigate(['/hr/employees', employee.employeeId]);
  }

  statusLabel(employee: EmployeeSummary): string {
    return employee.dateOfTermination ? 'Terminated' : 'Active';
  }

  trackByEmployee(index: number, employee: EmployeeSummary): string {
    return employee.employeeId || employee.id || String(index);
  }
}
