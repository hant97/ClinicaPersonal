import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { ToastComponent } from './shared/components/toast/toast/toast.component';
import { ToastService } from './shared/services/toast/toast.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, ToastComponent],
  templateUrl: './app.component.html'
})
export class AppComponent {
  private readonly toastService = inject(ToastService);
  readonly hasToasts = toSignal(this.toastService.toasts$.pipe(map(messages => messages.length > 0)), {
    initialValue: false
  });
  title = 'frontend';
}
