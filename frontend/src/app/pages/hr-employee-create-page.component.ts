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
import { ApiError, EmployeeCreateRequest } from '../api/employee.models';

@Component({
  selector: 'app-hr-employee-create-page',
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
      <h2 class="ui-section-title">Add Employee</h2>

      <form [formGroup]="form" (ngSubmit)="create()" class="ui-card grid gap-4">
        <div class="grid gap-3 md:grid-cols-2">
          <mat-form-field appearance="fill" subscriptSizing="dynamic">
            <mat-label>First name</mat-label>
            <input matInput type="text" formControlName="firstName" />
            @if (isInvalid('firstName')) { <mat-error>First name is required.</mat-error> }
          </mat-form-field>

          <mat-form-field appearance="fill" subscriptSizing="dynamic">
            <mat-label>Last name</mat-label>
            <input matInput type="text" formControlName="lastName" />
            @if (isInvalid('lastName')) { <mat-error>Last name is required.</mat-error> }
          </mat-form-field>

          <mat-form-field appearance="fill" subscriptSizing="dynamic">
            <mat-label>Job title</mat-label>
            <input matInput type="text" formControlName="jobTitle" />
            @if (isInvalid('jobTitle')) { <mat-error>Job title is required.</mat-error> }
          </mat-form-field>

          <mat-form-field appearance="fill" subscriptSizing="dynamic">
            <mat-label>Gender</mat-label>
            <input matInput type="text" formControlName="gender" />
            @if (isInvalid('gender')) { <mat-error>Gender is required.</mat-error> }
          </mat-form-field>

          <mat-form-field appearance="fill" subscriptSizing="dynamic">
            <mat-label>Date of birth</mat-label>
            <input matInput type="date" formControlName="dateOfBirth" />
            @if (isInvalid('dateOfBirth')) { <mat-error>Date of birth is required.</mat-error> }
          </mat-form-field>

          <mat-form-field appearance="fill" subscriptSizing="dynamic">
            <mat-label>Date of hire</mat-label>
            <input matInput type="date" formControlName="dateOfHire" />
            @if (isInvalid('dateOfHire')) { <mat-error>Date of hire is required.</mat-error> }
          </mat-form-field>

          <mat-form-field appearance="fill" subscriptSizing="dynamic">
            <mat-label>Date of termination (optional)</mat-label>
            <input matInput type="date" formControlName="dateOfTermination" />
          </mat-form-field>

          <mat-form-field appearance="fill" subscriptSizing="dynamic">
            <mat-label>Telephone number</mat-label>
            <input matInput type="text" formControlName="telephoneNumber" />
            @if (isInvalid('telephoneNumber')) { <mat-error>Telephone number is required.</mat-error> }
          </mat-form-field>

          <mat-form-field appearance="fill" subscriptSizing="dynamic" class="md:col-span-2">
            <mat-label>Home address</mat-label>
            <textarea matInput rows="2" formControlName="homeAddress"></textarea>
            @if (isInvalid('homeAddress')) { <mat-error>Home address is required.</mat-error> }
          </mat-form-field>

          <mat-form-field appearance="fill" subscriptSizing="dynamic" class="md:col-span-2">
            <mat-label>Mailing address</mat-label>
            <textarea matInput rows="2" formControlName="mailingAddress"></textarea>
            @if (isInvalid('mailingAddress')) { <mat-error>Mailing address is required.</mat-error> }
          </mat-form-field>

          <mat-form-field appearance="fill" subscriptSizing="dynamic" class="md:col-span-2">
            <mat-label>Email address</mat-label>
            <input matInput type="email" formControlName="emailAddress" />
            @if (isInvalid('emailAddress')) {
              <mat-error>Valid email address is required.</mat-error>
            }
          </mat-form-field>
        </div>

        @if (errorMessage) {
          <p class="ui-error-text">{{ errorMessage }}</p>
        }

        @if (successMessage) {
          <p class="text-sm text-success">{{ successMessage }}</p>
        }

        <div class="flex items-center gap-3">
          <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || submitting" class="px-5">
            <span class="inline-flex items-center gap-2">
              <mat-icon>{{ submitting ? 'sync' : 'person_add' }}</mat-icon>
              {{ submitting ? 'Creating Employee...' : 'Create Employee' }}
            </span>
          </button>
          @if (submitting) {
            <mat-spinner diameter="20"></mat-spinner>
          }
        </div>
      </form>
    </section>
  `
})
export class HrEmployeeCreatePageComponent {
  private readonly api = inject(EmployeeApiService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);

  readonly form = this.formBuilder.nonNullable.group({
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    jobTitle: ['', [Validators.required]],
    dateOfBirth: ['', [Validators.required]],
    gender: ['', [Validators.required]],
    dateOfHire: ['', [Validators.required]],
    dateOfTermination: [''],
    homeAddress: ['', [Validators.required]],
    mailingAddress: ['', [Validators.required]],
    telephoneNumber: ['', [Validators.required]],
    emailAddress: ['', [Validators.required, Validators.email]]
  });

  submitting = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  create(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.errorMessage = null;
    this.successMessage = null;

    const raw = this.form.getRawValue();
    const payload: EmployeeCreateRequest = {
      firstName: raw.firstName.trim(),
      lastName: raw.lastName.trim(),
      jobTitle: raw.jobTitle.trim(),
      dateOfBirth: raw.dateOfBirth,
      gender: raw.gender.trim(),
      dateOfHire: raw.dateOfHire,
      dateOfTermination: raw.dateOfTermination ? raw.dateOfTermination : null,
      homeAddress: raw.homeAddress.trim(),
      mailingAddress: raw.mailingAddress.trim(),
      telephoneNumber: raw.telephoneNumber.trim(),
      emailAddress: raw.emailAddress.trim()
    };

    this.api.createEmployee(payload).subscribe({
      next: (created) => {
        this.submitting = false;
        this.successMessage = `Employee created: ${created.employeeId}`;
        void this.router.navigate(['/hr/employees', created.employeeId]);
      },
      error: (error: { error?: ApiError }) => {
        this.submitting = false;
        this.errorMessage = error.error?.message ?? 'Failed to create employee.';
      }
    });
  }

  isInvalid(controlName: keyof EmployeeCreateRequest): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && (control.dirty || control.touched);
  }
}
