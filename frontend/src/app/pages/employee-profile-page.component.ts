import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';
import { EmployeeApiService } from '../api/employee-api.service';
import { ApiError, EmployeeContactUpdateRequest, EmployeeDetails } from '../api/employee.models';
import { AuthService } from '../auth/auth.service';
import { ToastService } from '../ui/toast.service';

@Component({
  selector: 'app-employee-profile-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  template: `
    <section class="space-y-4">
      <h2 class="ui-section-title">Employee Profile</h2>

      @if (isLoading) {
        <div class="ui-card space-y-3">
          <div class="app-skeleton h-6 w-48 rounded"></div>
          <div class="app-skeleton h-4 w-64 rounded"></div>
          <div class="app-skeleton h-4 w-56 rounded"></div>
        </div>
      }

      @if (loadError) {
        <p class="ui-error-text">{{ loadError }}</p>
      }

      @if (employee) {
        <section class="ui-card mb-4 space-y-1">
          <p><strong>Employee ID:</strong> {{ employee.employeeId }}</p>
          <p><strong>Name:</strong> {{ employee.firstName }} {{ employee.lastName }}</p>
          <p><strong>Job Title:</strong> {{ employee.jobTitle }}</p>
        </section>

        <form [formGroup]="contactForm" (ngSubmit)="save()" class="ui-card grid gap-4">
          <div class="grid gap-3 md:grid-cols-2">
            <mat-form-field appearance="fill" subscriptSizing="dynamic" class="md:col-span-2">
              <mat-label>Home address</mat-label>
              <textarea matInput rows="2" formControlName="homeAddress"></textarea>
              @if (isInvalid('homeAddress')) {
                <mat-error>Home address is required.</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="fill" subscriptSizing="dynamic" class="md:col-span-2">
              <mat-label>Mailing address</mat-label>
              <textarea matInput rows="2" formControlName="mailingAddress"></textarea>
              @if (isInvalid('mailingAddress')) {
                <mat-error>Mailing address is required.</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="fill" subscriptSizing="dynamic">
              <mat-label>Telephone number</mat-label>
              <input matInput type="text" formControlName="telephoneNumber" />
              @if (isInvalid('telephoneNumber')) {
                <mat-error>Telephone number is required.</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="fill" subscriptSizing="dynamic">
              <mat-label>Email address</mat-label>
              <input matInput type="email" [value]="employee.emailAddress" readonly disabled />
              <mat-hint>Email address is set at creation time and cannot be changed.</mat-hint>
            </mat-form-field>
          </div>

          <div class="flex items-center gap-3">
            <button mat-flat-button color="primary" type="submit" [disabled]="contactForm.invalid || saving">
              <span class="inline-flex items-center gap-2">
                <mat-icon>{{ saving ? 'sync' : 'save' }}</mat-icon>
                {{ saving ? 'Saving Contact Info...' : 'Save Contact Information' }}
              </span>
            </button>
            @if (saving) {
              <mat-spinner diameter="20"></mat-spinner>
            }
          </div>
        </form>
      }
    </section>
  `
})
export class EmployeeProfilePageComponent {
  private readonly authService = inject(AuthService);
  private readonly api = inject(EmployeeApiService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);

  employee: EmployeeDetails | null = null;
  loadError: string | null = null;
  saving = false;
  isLoading = true;

  readonly contactForm = this.formBuilder.nonNullable.group({
    homeAddress: ['', [Validators.required]],
    mailingAddress: ['', [Validators.required]],
    telephoneNumber: ['', [Validators.required]]
  });

  constructor() {
    const employeeId = this.authService.getEmployeeId();
    const employeeRequest = employeeId
      ? this.api.getEmployeeDetails(employeeId)
      : this.api.getMyEmployeeDetails();

    employeeRequest.subscribe({
      next: (details) => {
        this.employee = details;
        this.isLoading = false;
        this.contactForm.patchValue({
          homeAddress: details.homeAddress,
          mailingAddress: details.mailingAddress,
          telephoneNumber: details.telephoneNumber
        });
      },
      error: (error: { status?: number; error?: ApiError }) => {
        this.isLoading = false;
        if (this.isTerminatedError(error.error)) {
          this.redirectTerminated();
          return;
        }

        if (this.isUnauthorizedError(error)) {
          this.loadError = 'Session expired. Please sign in again.';
          return;
        }

        this.loadError = error.error?.message ?? 'Unable to load profile.';
      }
    });
  }

  save(): void {
    if (!this.employee) {
      return;
    }
    if (this.contactForm.invalid) {
      this.contactForm.markAllAsTouched();
      return;
    }

    this.saving = true;

    const raw = this.contactForm.getRawValue();
    const payload: EmployeeContactUpdateRequest = {
      homeAddress: raw.homeAddress.trim(),
      mailingAddress: raw.mailingAddress.trim(),
      telephoneNumber: raw.telephoneNumber.trim()
    };

    this.api.patchEmployeeContact(this.employee.employeeId, payload).subscribe({
      next: () => {
        this.saving = false;
        this.toastService.success('Contact information updated successfully.');
      },
      error: (error: { error?: ApiError }) => {
        this.saving = false;

        if (this.isTerminatedError(error.error)) {
          this.redirectTerminated();
          return;
        }

        this.toastService.error(error.error?.message ?? 'Failed to update contact information.');
      }
    });
  }

  isInvalid(controlName: keyof EmployeeContactUpdateRequest): boolean {
    const control = this.contactForm.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  private isTerminatedError(error: ApiError | undefined): boolean {
    return error?.status === 403 && error?.reason === 'terminated';
  }

  private isUnauthorizedError(error: { status?: number; error?: ApiError } | undefined): boolean {
    return error?.status === 401 || error?.error?.status === 401;
  }

  private redirectTerminated(): void {
    this.toastService.error('Access denied: employment has been terminated.');
    void this.router.navigate(['/terminated']);
  }
}
