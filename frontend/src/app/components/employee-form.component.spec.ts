import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { throwError } from 'rxjs';
import { EmployeeApiService } from '../api/employee-api.service';
import { EmployeeFormComponent } from './employee-form.component';

describe('EmployeeFormComponent', () => {
  let fixture: ComponentFixture<EmployeeFormComponent>;
  let component: EmployeeFormComponent;
  let apiServiceSpy: jasmine.SpyObj<EmployeeApiService>;

  beforeEach(async () => {
    apiServiceSpy = jasmine.createSpyObj<EmployeeApiService>('EmployeeApiService', ['updateEmployee']);

    await TestBed.configureTestingModule({
      imports: [EmployeeFormComponent],
      providers: [provideNoopAnimations(), { provide: EmployeeApiService, useValue: apiServiceSpy }]
    }).compileComponents();

    fixture = TestBed.createComponent(EmployeeFormComponent);
    component = fixture.componentInstance;
    component.employeeId = 'EMP-0001';
    component.employee = {
      id: '1',
      employeeId: 'EMP-0001',
      firstName: 'John',
      lastName: 'Doe',
      jobTitle: 'Engineer',
      dateOfBirth: '1990-01-01',
      gender: 'Male',
      dateOfHire: '2024-01-01',
      dateOfTermination: null,
      homeAddress: 'home',
      mailingAddress: 'mail',
      telephoneNumber: '+1-555-0100',
      emailAddress: 'john.doe@example.com',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z'
    };
    fixture.detectChanges();
  });

  it('shows server-side validation message when PUT fails', () => {
    apiServiceSpy.updateEmployee.and.returnValue(
      throwError(() => ({
        error: {
          status: 400,
          message: 'emailAddress: emailAddress must be a valid email'
        }
      }))
    );

    component.submit();
    fixture.detectChanges();

    expect(apiServiceSpy.updateEmployee).toHaveBeenCalled();
    expect(component.serverErrorMessage).toContain('emailAddress');
  });
});
