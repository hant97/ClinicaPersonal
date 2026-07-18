import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Payment } from '../../../core/models/payment.model';
import { LucideAngularModule, Printer, X } from 'lucide-angular';

@Component({
  selector: 'app-payment-detail',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './payment-detail.component.html',
  styleUrl: './payment-detail.component.css'
})
export class PaymentDetailComponent {
  @Input() payment!: Payment;
  @Input() paymentMethodText: string = '';
  @Output() close = new EventEmitter<void>();

  readonly Printer = Printer;
  readonly X = X;

  printReceipt(): void {
    window.print();
  }

  onClose(): void {
    this.close.emit();
  }
}
