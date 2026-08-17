import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { LucideAngularModule, Plus, Receipt, Banknote, Wallet, CreditCard, Landmark, Smartphone } from 'lucide-angular';
import { PaymentService } from '../../../core/services/payment.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { Payment } from '../../../core/models/payment.model';

@Component({
  selector: 'app-patient-payments-section',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './patient-payments-section.component.html'
})
export class PatientPaymentsSectionComponent implements OnInit {
  readonly Plus = Plus;
  readonly Receipt = Receipt;
  readonly Banknote = Banknote;
  readonly Wallet = Wallet;
  readonly CreditCard = CreditCard;
  readonly Landmark = Landmark;
  readonly Smartphone = Smartphone;

  @Input() patientId!: number;
  @Output() paymentsCount = new EventEmitter<number>();

  payments: Payment[] = [];
  paymentsTotal = 0;
  paymentMethodMap = new Map<string, string>();

  constructor(
    private paymentService: PaymentService,
    private catalogService: CatalogService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.catalogService.getActiveItemsByCatalogCode('PAYMENT_METHOD').subscribe({
      next: (items) => items.forEach(item => this.paymentMethodMap.set(item.itemCode, item.itemName))
    });
    this.loadPayments();
  }

  loadPayments(): void {
    this.paymentService.getByPatientId(this.patientId, 0, 100).subscribe({
      next: (page) => {
        this.payments = page.content;
        this.paymentsTotal = page.content.reduce((sum, p) => sum + (p.amount || 0), 0);
        this.paymentsCount.emit(page.content.length);
      },
      error: (err) => console.error('Error fetching payments', err)
    });
  }

  getPaymentMethodText(method: string): string {
    return this.paymentMethodMap.get(method) || method;
  }

  getPaymentMethodVisual(method: string): { icon: any; classes: string } {
    const code = (method || '').toUpperCase();
    switch (code) {
      case 'EFECTIVO':
        return { icon: Banknote, classes: 'bg-emerald-50 text-emerald-700 border-emerald-200' };
      case 'TARJETA':
        return { icon: CreditCard, classes: 'bg-violet-50 text-violet-700 border-violet-200' };
      case 'TRANSFERENCIA':
        return { icon: Landmark, classes: 'bg-blue-50 text-blue-700 border-blue-200' };
      case 'YAPE':
      case 'PLIN':
        return { icon: Smartphone, classes: 'bg-amber-50 text-amber-700 border-amber-200' };
      default:
        return { icon: Wallet, classes: 'bg-slate-100 text-slate-700 border-line' };
    }
  }

  registerPayment(): void {
    this.router.navigate(['/billing'], {
      queryParams: { newPayment: 'true', patientId: this.patientId }
    });
  }
}
