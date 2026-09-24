import { Component, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Payment, PaymentTransaction } from '../../../core/models/payment.model';
import { LucideAngularModule } from 'lucide-angular';
import {
  Printer, X, Plus, Trash2 } from '../../../shared/icons/lucide-icons';
import { ClinicSettingsService, ClinicSettings } from '../../../core/services/clinic-settings.service';
import { PaymentService } from '../../../core/services/payment.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { CatalogItem } from '../../../core/models/catalog.model';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { Subscription } from 'rxjs';
import { FocusTrapDirective } from '../../../shared/directives/focus-trap.directive';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-payment-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, FocusTrapDirective],
  templateUrl: './payment-detail.component.html'
})
export class PaymentDetailComponent implements OnInit, OnDestroy {
  @Input() payment!: Payment;
  @Input() paymentMethodText: string = '';
  @Output() close = new EventEmitter<void>();
  @Output() changed = new EventEmitter<void>();

  readonly Printer = Printer;
  readonly X = X;
  readonly Plus = Plus;
  readonly Trash2 = Trash2;

  clinicSettings: ClinicSettings | null = null;
  paymentMethods: CatalogItem[] = [];

  newTransactionAmount: number | null = null;
  newTransactionDate: string = '';
  newTransactionMethod: string = '';
  newTransactionNotes: string = '';
  isSubmitting = false;

  private settingsSubscription?: Subscription;

  constructor(
    private clinicSettingsService: ClinicSettingsService,
    private paymentService: PaymentService,
    private catalogService: CatalogService,
    private notificationService: NotificationService,
    private toastService: ToastService,
    private authService: AuthService
  ) {}

  /** Eliminar abonos está reservado a administradores (el backend también lo exige). */
  get canDelete(): boolean {
    return this.authService.isClinicAdmin();
  }

  ngOnInit(): void {
    this.settingsSubscription = this.clinicSettingsService.settings$.subscribe(
      settings => this.clinicSettings = settings
    );
    this.clinicSettingsService.loadSettings();

    this.catalogService.getActiveItemsByCatalogCode('PAYMENT_METHOD').subscribe({
      next: (items) => this.paymentMethods = items
    });

    this.newTransactionDate = this.toLocalDateTimeInput(new Date());
    this.newTransactionMethod = this.payment?.paymentMethod || '';
  }

  ngOnDestroy(): void {
    if (this.settingsSubscription) {
      this.settingsSubscription.unsubscribe();
    }
  }

  getLogoUrl(path: string | undefined): string {
    if (!path) return '';
    return this.clinicSettingsService.getLogoUrl(path);
  }

  get paidAmount(): number {
    return this.payment.paidAmount ?? 0;
  }

  get balanceAmount(): number {
    return this.payment.balanceAmount ?? (this.payment.amount - this.paidAmount);
  }

  get canAddTransaction(): boolean {
    return this.payment.status !== 'PAGADO';
  }

  getStatusText(status?: string): string {
    switch (status) {
      case 'PENDIENTE': return 'Pendiente';
      case 'PARCIAL': return 'Parcial';
      case 'PAGADO': return 'Pagado';
      default: return status || '—';
    }
  }

  getStatusVisual(status?: string): { classes: string } {
    switch (status) {
      case 'PENDIENTE': return { classes: 'bg-amber-50 text-amber-700 border-amber-200' };
      case 'PARCIAL': return { classes: 'bg-sky-50 text-sky-700 border-sky-200' };
      case 'PAGADO': return { classes: 'bg-emerald-50 text-emerald-700 border-emerald-200' };
      default: return { classes: 'bg-slate-100 text-slate-600 border-line' };
    }
  }

  getPaymentMethodText(method?: string): string {
    const found = this.paymentMethods.find(m => m.itemCode === method);
    return found?.itemName || method || '—';
  }

  addTransaction(): void {
    const amount = Number(this.newTransactionAmount || 0);
    if (amount <= 0) {
      this.toastService.show('Ingrese un monto de abono mayor a 0.', 'error');
      return;
    }
    if (amount > this.balanceAmount) {
      this.toastService.show('El abono no puede exceder el saldo pendiente.', 'error');
      return;
    }

    this.isSubmitting = true;
    const transaction: PaymentTransaction = {
      amount,
      transactionDate: this.newTransactionDate || undefined,
      paymentMethod: this.newTransactionMethod || undefined,
      notes: this.newTransactionNotes || undefined
    };

    this.paymentService.addTransaction(this.payment.id!, transaction).subscribe({
      next: (updated) => {
        this.payment = updated;
        this.isSubmitting = false;
        this.newTransactionAmount = null;
        this.newTransactionNotes = '';
        this.toastService.show('Abono registrado exitosamente', 'success');
        this.changed.emit();
      },
      error: (err) => {
        this.isSubmitting = false;
        console.error('Error adding transaction', err);
        this.toastService.show('Error al registrar el abono', 'error');
      }
    });
  }

  deleteTransaction(transaction: PaymentTransaction): void {
    this.notificationService.confirm(
      'Eliminar Abono',
      '¿Está seguro de que desea eliminar este abono?',
      'Eliminar',
      'Cancelar'
    ).then((confirmed: boolean) => {
      if (!confirmed) return;
      this.paymentService.deleteTransaction(this.payment.id!, transaction.id!).subscribe({
        next: () => {
          this.payment.transactions = (this.payment.transactions || []).filter(t => t.id !== transaction.id);
          this.payment.paidAmount = this.payment.transactions.reduce((sum, t) => sum + (t.amount || 0), 0);
          this.payment.balanceAmount = Math.max(0, this.payment.amount - this.payment.paidAmount);
          this.recalculateStatus();
          this.toastService.show('Abono eliminado exitosamente', 'success');
          this.changed.emit();
        },
        error: (err) => {
          console.error('Error deleting transaction', err);
          this.toastService.show('Error al eliminar el abono', 'error');
        }
      });
    });
  }

  private recalculateStatus(): void {
    const paid = this.paidAmount;
    if (paid <= 0) {
      this.payment.status = 'PENDIENTE';
    } else if (paid < this.payment.amount) {
      this.payment.status = 'PARCIAL';
    } else {
      this.payment.status = 'PAGADO';
    }
  }

  private toLocalDateTimeInput(date: Date): string {
    const tzOffset = date.getTimezoneOffset() * 60000;
    return new Date(date.getTime() - tzOffset).toISOString().substring(0, 16);
  }

  printReceipt(): void {
    window.print();
  }

  onClose(): void {
    this.close.emit();
  }
}
