import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { EmployeeApiService } from '../api/employee-api.service';
import { HrEmployeeSearchComponent } from './hr-employee-search.component';

describe('HrEmployeeSearchComponent', () => {
  let fixture: ComponentFixture<HrEmployeeSearchComponent>;
  let component: HrEmployeeSearchComponent;
  let apiServiceSpy: jasmine.SpyObj<EmployeeApiService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    apiServiceSpy = jasmine.createSpyObj<EmployeeApiService>('EmployeeApiService', ['searchEmployees']);
    routerSpy = jasmine.createSpyObj<Router>('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [HrEmployeeSearchComponent],
      providers: [
        provideNoopAnimations(),
        { provide: EmployeeApiService, useValue: apiServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HrEmployeeSearchComponent);
    component = fixture.componentInstance;
  });

  it('searches employees and renders paginated results', () => {
    apiServiceSpy.searchEmployees.and.returnValue(
      of({
        content: [
          {
            id: '1',
            employeeId: 'EMP-0001',
            firstName: 'Alice',
            lastName: 'Smith',
            jobTitle: 'Engineer',
            emailAddress: 'alice@example.com',
            dateOfHire: '2024-01-01',
            dateOfTermination: null
          }
        ],
        pageable: { pageNumber: 0, pageSize: 25 },
        totalElements: 1,
        totalPages: 1,
        size: 25,
        number: 0
      })
    );

    component.lastNameControl.setValue('smith');
    component.search(0);
    fixture.detectChanges();

    expect(apiServiceSpy.searchEmployees).toHaveBeenCalledWith({ employeeId: '', lastName: 'smith' }, 0, 25);
    const rows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows.length).toBe(1);
    expect(rows[0].textContent).toContain('EMP-0001');
  });

  it('prefers employeeId when both employeeId and lastName are entered', () => {
    apiServiceSpy.searchEmployees.and.returnValue(
      of({
        content: [],
        pageable: { pageNumber: 0, pageSize: 25 },
        totalElements: 0,
        totalPages: 0,
        size: 25,
        number: 0
      })
    );

    component.employeeIdControl.setValue('emp-0001');
    component.lastNameControl.setValue('smith');
    component.search(0);

    expect(apiServiceSpy.searchEmployees).toHaveBeenCalledWith({ employeeId: 'emp-0001', lastName: 'smith' }, 0, 25);
  });

  it('shows validation error when both fields are empty', () => {
    component.employeeIdControl.setValue('');
    component.lastNameControl.setValue('');
    component.search(0);

    expect(apiServiceSpy.searchEmployees).not.toHaveBeenCalled();
    expect(component.errorMessage).toContain('either an Employee ID or a last name');
  });

  it('routes to details when row clicked', () => {
    component.openDetails({
      id: '1',
      employeeId: 'EMP-0001',
      firstName: 'Alice',
      lastName: 'Smith',
      jobTitle: 'Engineer',
      emailAddress: 'alice@example.com',
      dateOfHire: '2024-01-01',
      dateOfTermination: null
    });

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/hr/employees', 'EMP-0001']);
  });
});
