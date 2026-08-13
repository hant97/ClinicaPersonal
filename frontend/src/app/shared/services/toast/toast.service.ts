import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface ToastMessage {
  id: number;
  text: string;
  type: 'success' | 'error' | 'info';
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private toastsSubject = new BehaviorSubject<ToastMessage[]>([]);
  toasts$ = this.toastsSubject.asObservable();
  private idCounter = 0;
  private timers = new Map<number, ReturnType<typeof setTimeout>>();

  show(text: string, type: 'success' | 'error' | 'info' = 'success', duration = 4000): void {
    const id = ++this.idCounter;
    this.toastsSubject.next([...this.toastsSubject.getValue(), { id, text, type }]);
    const timer = setTimeout(() => this.dismiss(id), duration);
    this.timers.set(id, timer);
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
