import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { AppShellComponent } from './app-shell.component';
import { AuthService } from '../auth/auth.service';
import { ThemeService } from '../ui/theme.service';

describe('AppShellComponent', () => {
  let fixture: ComponentFixture<AppShellComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let themeServiceSpy: jasmine.SpyObj<ThemeService>;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['hasRole', 'logout'], {
      currentUser: signal({ username: 'alice', roles: ['EMPLOYEE'], employeeId: 'EMP-0001' })
    });

    themeServiceSpy = jasmine.createSpyObj<ThemeService>('ThemeService', ['toggleTheme'], {
      theme: signal<'light' | 'dark'>('light')
    });

    authServiceSpy.hasRole.and.callFake((role: string) => role === 'EMPLOYEE');
    authServiceSpy.logout.and.returnValue(Promise.resolve());

    await TestBed.configureTestingModule({
      imports: [AppShellComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ThemeService, useValue: themeServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AppShellComponent);
    fixture.detectChanges();
  });

  it('hides HR links for EMPLOYEE-only users', () => {
    const rendered = fixture.nativeElement.textContent;
    expect(rendered).toContain('My Profile');
    expect(rendered).not.toContain('Search Employees');
    expect(rendered).not.toContain('Add Employee');
  });

  it('calls logout from the toolbar button', () => {
    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    const logoutButton = buttons.find((button) => button.textContent?.includes('Logout'));

    expect(logoutButton).toBeTruthy();
    (logoutButton as HTMLButtonElement).click();

    expect(authServiceSpy.logout).toHaveBeenCalled();
  });
});
