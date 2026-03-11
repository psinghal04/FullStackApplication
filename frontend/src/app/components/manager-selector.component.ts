import { CommonModule } from '@angular/common';
import { Component, EventEmitter, inject, Input, Output } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EmployeeApiService } from '../api/employee-api.service';
import { EmployeeSummaryV2, ManagerReference } from '../api/employee.models';

@Component({
  selector: 'app-manager-selector',
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
    <div class="space-y-3">
      <div class="flex items-center justify-between">
        <label class="text-sm font-medium">Manager (Optional)</label>
        @if (selectedManager) {
          <button mat-button color="warn" type="button" (click)="clearSelection()">
            <span class="inline-flex items-center gap-1">
              <mat-icon>close</mat-icon>
              Clear
            </span>
          </button>
        }
      </div>

      @if (selectedManager) {
        <div class="rounded-md border border-success bg-success/5 p-3">
          <div class="flex items-center gap-2">
            <mat-icon class="text-success">check_circle</mat-icon>
            <div>
              <p class="font-medium">{{ selectedManager.firstName }} {{ selectedManager.lastName }}</p>
              <p class="text-sm text-muted">{{ selectedManager.employeeId }} - {{ selectedManager.jobTitle }}</p>
            </div>
          </div>
        </div>
      } @else {
        <div class="rounded-md border border-border bg-surface p-4">
          <p class="mb-3 text-sm text-muted">Search for an employee to assign as manager</p>

          <form (submit)="$event.preventDefault(); search()" class="mb-3 grid gap-2 md:grid-cols-[1fr_1fr_auto]">
            <mat-form-field appearance="fill" subscriptSizing="dynamic" class="min-w-0">
              <mat-label>Employee ID</mat-label>
              <input
                matInput
                [formControl]="employeeIdControl"
                type="text"
                placeholder="e.g. EMP-000123"
                (keydown.enter)="$event.preventDefault(); search()"
              />
            </mat-form-field>

            <mat-form-field appearance="fill" subscriptSizing="dynamic" class="min-w-0">
              <mat-label>Last name</mat-label>
              <input
                matInput
                [formControl]="lastNameControl"
                type="text"
                placeholder="Partial search"
                (keydown.enter)="$event.preventDefault(); search()"
              />
            </mat-form-field>

            <button mat-flat-button color="primary" type="button" class="h-14" (click)="search()" [disabled]="isSearching">
              <mat-icon>search</mat-icon>
            </button>
          </form>

          @if (searchError) {
            <p class="mb-2 text-sm text-error">{{ searchError }}</p>
          }

          @if (isSearching) {
            <div class="flex items-center gap-2 text-sm text-muted">
              <mat-spinner diameter="20"></mat-spinner>
              <span>Searching...</span>
            </div>
          }

          @if (!isSearching && searchResults.length > 0) {
            <div class="max-h-64 space-y-1 overflow-y-auto">
              @for (employee of searchResults; track employee.id) {
                <button
                  type="button"
                  (click)="selectManager(employee)"
                  class="w-full rounded-md border border-border bg-background p-3 text-left transition-colors hover:bg-surface"
                >
                  <p class="font-medium">{{ employee.firstName }} {{ employee.lastName }}</p>
                  <p class="text-sm text-muted">{{ employee.employeeId }} - {{ employee.jobTitle }}</p>
                </button>
              }
            </div>
          }

          @if (!isSearching && hasSearched && searchResults.length === 0) {
            <p class="text-sm text-muted">No employees found. Try a different search.</p>
          }
        </div>
      }
    </div>
  `
})
export class ManagerSelectorComponent {
  private readonly api = inject(EmployeeApiService);

  @Input() set manager(value: ManagerReference | null) {
    if (value) {
      this.selectedManager = value;
    }
  }

  @Output() managerChange = new EventEmitter<ManagerReference | null>();

  readonly employeeIdControl = new FormControl<string>('', { nonNullable: true });
  readonly lastNameControl = new FormControl<string>('', { nonNullable: true });

  selectedManager: ManagerReference | null = null;
  searchResults: EmployeeSummaryV2[] = [];
  isSearching = false;
  searchError: string | null = null;
  hasSearched = false;

  search(): void {
    const employeeId = this.employeeIdControl.value.trim();
    const lastName = this.lastNameControl.value.trim();

    if (!employeeId && !lastName) {
      this.searchError = 'Please enter an Employee ID or last name';
      return;
    }

    this.searchError = null;
    this.isSearching = true;
    this.hasSearched = true;

    this.api.searchEmployeesV2({ employeeId, lastName }, 0, 10).subscribe({
      next: (result) => {
        this.searchResults = result.content ?? [];
        this.isSearching = false;
      },
      error: (error: { error?: { message?: string } }) => {
        this.searchError = error.error?.message ?? 'Search failed. Please try again.';
        this.searchResults = [];
        this.isSearching = false;
      }
    });
  }

  selectManager(employee: EmployeeSummaryV2): void {
    this.selectedManager = {
      id: employee.id,
      employeeId: employee.employeeId,
      firstName: employee.firstName,
      lastName: employee.lastName,
      jobTitle: employee.jobTitle
    };
    this.managerChange.emit(this.selectedManager);
    this.searchResults = [];
    this.employeeIdControl.setValue('');
    this.lastNameControl.setValue('');
    this.hasSearched = false;
  }

  clearSelection(): void {
    this.selectedManager = null;
    this.managerChange.emit(null);
    this.searchResults = [];
    this.hasSearched = false;
  }
}
