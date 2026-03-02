import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error';

export interface ToastMessage {
  id: number;
  type: ToastType;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly toastSignal = signal<ToastMessage[]>([]);
  private sequence = 0;

  readonly toasts = this.toastSignal.asReadonly();

  success(message: string): void {
    this.push('success', message);
  }

  error(message: string): void {
    this.push('error', message);
  }

  dismiss(id: number): void {
    this.toastSignal.update((current) => current.filter((toast) => toast.id !== id));
  }

  private push(type: ToastType, message: string): void {
    const id = ++this.sequence;
    const toast: ToastMessage = { id, type, message };
    this.toastSignal.update((current) => [...current, toast]);

    setTimeout(() => this.dismiss(id), 3500);
  }
}
