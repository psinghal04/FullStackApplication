import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { AuthService } from '../auth/auth.service';
import { ThemeService } from '../ui/theme.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule
  ],
  template: `
    <a href="#main-content" class="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded-md focus:bg-surface focus:px-3 focus:py-2 focus:shadow-card">Skip to main content</a>

    <mat-sidenav-container class="min-h-screen">
      <mat-sidenav [opened]="menuOpen()" mode="side" class="!w-64 border-r border-border bg-surface" (closedStart)="menuOpen.set(false)">
        <div class="p-4 text-lg font-semibold">HR App</div>
        <mat-nav-list>
          <a mat-list-item routerLink="/employee/profile" routerLinkActive="!bg-primary/15 !text-text">My Profile</a>
          @if (authService.hasRole('HR_ADMIN')) {
            <a mat-list-item routerLink="/hr/employees" routerLinkActive="!bg-primary/15 !text-text">Search Employees</a>
            <a mat-list-item routerLink="/hr/employees/new" routerLinkActive="!bg-primary/15 !text-text">Add Employee</a>
          }
        </mat-nav-list>
      </mat-sidenav>

      <mat-sidenav-content>
        <mat-toolbar class="!bg-surface !text-text border-b border-border !sticky top-0 z-20">
          <button mat-icon-button type="button" aria-label="Toggle navigation" (click)="toggleMenu()">
            <mat-icon>menu</mat-icon>
          </button>
          <span class="ml-2 font-semibold">HR Portal</span>
          <span class="mx-3 hidden text-sm text-muted md:inline" *ngIf="authService.currentUser() as user">{{ user.username }} ({{ user.roles.join(', ') }})</span>
          <span class="flex-1"></span>
          <button mat-icon-button type="button" (click)="themeService.toggleTheme()" aria-label="Toggle color theme">
            <mat-icon>{{ themeIcon() }}</mat-icon>
          </button>
          <button mat-stroked-button type="button" (click)="logout()">Logout</button>
        </mat-toolbar>

        <main id="main-content" class="ui-page">
          <router-outlet></router-outlet>
        </main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `
})
export class AppShellComponent {
  readonly authService = inject(AuthService);
  readonly themeService = inject(ThemeService);
  readonly menuOpen = signal<boolean>(true);
  readonly themeIcon = computed(() => (this.themeService.theme() === 'dark' ? 'light_mode' : 'dark_mode'));

  toggleMenu(): void {
    this.menuOpen.set(!this.menuOpen());
  }

  logout(): void {
    void this.authService.logout();
  }
}