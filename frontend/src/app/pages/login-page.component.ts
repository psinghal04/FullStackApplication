import { Component, OnInit } from '@angular/core';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  template: `
    <h2>Login</h2>
    <p>Redirecting to Keycloak sign-in...</p>
  `
})
export class LoginPageComponent implements OnInit {
  constructor(private readonly authService: AuthService) {}

  ngOnInit(): void {
    void this.login();
  }

  login(): Promise<void> {
    return this.authService.login(window.location.origin + '/employee/profile');
  }
}
