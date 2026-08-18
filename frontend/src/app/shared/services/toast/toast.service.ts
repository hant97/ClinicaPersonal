import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface ToastMessage {
  id: number;
  text: string;
  type: 'success' | 'error' | 'info' | 'warning';
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private toastsSubject = new BehaviorSubject<ToastMessage[]>([]);
  toasts$ = this.toastsSubject.asObservable();
  private idCounter = 0;
  private timers = new Map<number, ReturnType<typeof setTimeout>>();

  show(text: string, type: 'success' | 'error' | 'info' | 'warning' = 'success', duration = 4000): void {
    const id = ++this.idCounter;
    this.toastsSubject.next([...this.toastsSubject.getValue(), { id, text, type }]);
    const timer = setTimeout(() => this.dismiss(id), duration);
    this.timers.set(id, timer);
  }

  success(text: string, duration = 4000): void {
    this.show(text, 'success', duration);
  }

  error(text: string, duration = 5000): void {
    this.show(text, 'error', duration);
  }

  info(text: string, duration = 4000): void {
    this.show(text, 'info', duration);
  }

  warning(text: string, duration = 4500): void {
    this.show(text, 'warning', duration);
  }

  dismiss(id: number): void {
    const timer = this.timers.get(id);
    if (timer) {
      clearTimeout(timer);
      this.timers.delete(id);
    }
    this.toastsSubject.next(this.toastsSubject.getValue().filter(t => t.id !== id));
  }
}
