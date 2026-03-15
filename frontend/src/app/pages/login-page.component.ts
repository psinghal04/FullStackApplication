import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  template: `
    <h2>Login</h2>
    @if (showAuthError) {
      <p>Sign-in failed. Please retry. If it keeps failing, contact support.</p>
    }
    <p>Use the button below to sign in with Keycloak.</p>
    <button type="button" (click)="login()">Sign In</button>
  `
})
export class LoginPageComponent implements OnInit {
  showAuthError = false;

  constructor(
    private readonly authService: AuthService,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.showAuthError = this.route.snapshot.queryParamMap.has('authError');
    if (!this.showAuthError) {
      this.authService.login();
    }
  }

  login(): void {
    this.authService.login(window.location.origin + '/employee/profile');
  }
}
