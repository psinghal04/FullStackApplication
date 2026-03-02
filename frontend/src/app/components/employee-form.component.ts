import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { EmployeeApiService } from '../api/employee-api.service';
import { ApiError, EmployeeDetails, EmployeeSummary, EmployeeUpdateRequest } from '../api/employee.models';

@Component({
  selector: 'app-employee-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule],
  template: `
    <form [formGroup]="form" (ngSubmit)="submit()" class="ui-card grid gap-4">
      <div class="grid gap-3 md:grid-cols-2">
        <mat-form-field appearance="fill" subscriptSizing="dynamic">
          <mat-label>Employee ID</mat-label>
          <input matInput type="text" formControlName="employeeId" />
          @if (isInvalid('employeeId')) { <mat-error>Employee ID is required.</mat-error> }
        </mat-form-field>

        <mat-form-field appearance="fill" subscriptSizing="dynamic">
          <mat-label>Job title</mat-label>
          <input matInput type="text" formControlName="jobTitle" />
          @if (isInvalid('jobTitle')) { <mat-error>Job title is required.</mat-error> }
        </mat-form-field>

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
          <mat-label>Date of birth</mat-label>
          <input matInput type="date" formControlName="dateOfBirth" />
          @if (isInvalid('dateOfBirth')) { <mat-error>Date of birth is required.</mat-error> }
        </mat-form-field>

        <mat-form-field appearance="fill" subscriptSizing="dynamic">
          <mat-label>Gender</mat-label>
          <input matInput type="text" formControlName="gender" />
          @if (isInvalid('gender')) { <mat-error>Gender is required.</mat-error> }
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

        <mat-form-field appearance="fill" subscriptSizing="dynamic">
          <mat-label>Telephone number</mat-label>
          <input matInput type="text" formControlName="telephoneNumber" />
          @if (isInvalid('telephoneNumber')) { <mat-error>Telephone number is required.</mat-error> }
        </mat-form-field>

        <mat-form-field appearance="fill" subscriptSizing="dynamic">
          <mat-label>Email address</mat-label>
          <input matInput type="email" [value]="initialEmailAddress" readonly disabled />
          <mat-hint>Email address is immutable after employee creation.</mat-hint>
        </mat-form-field>
      </div>

      @if (serverErrorMessage) {
        <p class="ui-error-text">{{ serverErrorMessage }}</p>
      }

      @if (successMessage) {
        <p class="text-sm text-success">{{ successMessage }}</p>
      }

      <div>
        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || submitting" class="px-5">
          <span class="inline-flex items-center gap-2">
            <mat-icon>{{ submitting ? 'sync' : 'save' }}</mat-icon>
            {{ submitting ? 'Saving Employee...' : 'Save Employee Changes' }}
          </span>
        </button>
      </div>
    </form>
  `
})
export class EmployeeFormComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly api = inject(EmployeeApiService);

  @Input({ required: true }) employeeId!: string;

  @Output() saved = new EventEmitter<EmployeeSummary>();

  @Input() set employee(value: EmployeeDetails | null) {
    if (!value) {
      return;
    }

    this.form.patchValue({
      employeeId: value.employeeId,
      firstName: value.firstName,
      lastName: value.lastName,
      jobTitle: value.jobTitle,
      dateOfBirth: value.dateOfBirth,
      gender: value.gender,
      dateOfHire: value.dateOfHire,
      dateOfTermination: value.dateOfTermination ?? '',
      homeAddress: value.homeAddress,
      mailingAddress: value.mailingAddress,
      telephoneNumber: value.telephoneNumber,
      emailAddress: value.emailAddress
    });

    this.initialEmailAddress = value.emailAddress;
  }

  initialEmailAddress = '';

  readonly form = this.formBuilder.nonNullable.group({
    employeeId: ['', [Validators.required]],
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
  serverErrorMessage: string | null = null;
  successMessage: string | null = null;

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.serverErrorMessage = null;
    this.successMessage = null;

    const raw = this.form.getRawValue();
    const payload: EmployeeUpdateRequest = {
      employeeId: raw.employeeId.trim(),
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

    this.api.updateEmployee(this.employeeId, payload).subscribe({
      next: (result) => {
        this.submitting = false;
        this.successMessage = 'Employee updated successfully.';
        this.saved.emit(result);
      },
      error: (error: { error?: ApiError }) => {
        this.submitting = false;
        this.serverErrorMessage = error.error?.message ?? 'Failed to update employee.';
      }
    });
  }

  isInvalid(controlName: keyof EmployeeUpdateRequest): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && (control.dirty || control.touched);
  }
}
