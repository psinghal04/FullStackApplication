import { Component } from '@angular/core';
import { ToastService } from './toast.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  template: `
    <div class="fixed right-4 top-4 z-[1000] grid gap-2">
      @for (toast of toastService.toasts(); track toast.id) {
        <div
          [class.bg-success]="toast.type === 'success'"
          [class.bg-danger]="toast.type === 'error'"
          class="min-w-[260px] rounded-md px-4 py-3 text-sm font-medium text-white shadow-card"
        >
          {{ toast.message }}
        </div>
      }
    </div>
  `
})
export class ToastContainerComponent {
  constructor(public toastService: ToastService) {}
}
