import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, ToastMessage } from '../../../services/toast/toast.service';
import { Subscription } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';
import {
  CheckCircle2,
  AlertCircle,
  AlertTriangle,
  Info,
  X
} from '../../../icons/lucide-icons';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './toast.component.html',
})
export class ToastComponent implements OnDestroy {
  readonly CheckCircle2 = CheckCircle2;
  readonly AlertCircle = AlertCircle;
  readonly AlertTriangle = AlertTriangle;
  readonly Info = Info;
  readonly X = X;

  messages: ToastMessage[] = [];
  private subscription: Subscription;

  constructor(private toastService: ToastService) {
    this.subscription = this.toastService.toasts$.subscribe(msgs => this.messages = msgs);
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  dismiss(id: number): void {
    this.toastService.dismiss(id);
  }
}
