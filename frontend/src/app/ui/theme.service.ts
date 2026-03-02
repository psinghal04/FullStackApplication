import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';

export type AppTheme = 'light' | 'dark';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly storageKey = 'hr-app-theme';
  private readonly themeSignal = signal<AppTheme>('dark');

  readonly theme = this.themeSignal.asReadonly();

  constructor() {
    this.initialize();
  }

  toggleTheme(): void {
    this.setTheme(this.themeSignal() === 'dark' ? 'light' : 'dark');
  }

  setTheme(theme: AppTheme): void {
    this.themeSignal.set(theme);
    this.document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem(this.storageKey, theme);
  }

  private initialize(): void {
    const fromStorage = localStorage.getItem(this.storageKey);
    const theme: AppTheme = fromStorage === 'light' ? 'light' : 'dark';
    this.themeSignal.set(theme);
    this.document.documentElement.setAttribute('data-theme', theme);
  }
}