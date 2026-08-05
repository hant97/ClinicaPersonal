import { Component, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Payment } from '../../../core/models/payment.model';
import { LucideAngularModule, Printer, X } from 'lucide-angular';
import { ClinicSettingsService, ClinicSettings } from '../../../core/services/clinic-settings.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-payment-detail',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './payment-detail.component.html'
})
export class PaymentDetailComponent implements OnInit, OnDestroy {
  @Input() payment!: Payment;
  @Input() paymentMethodText: string = '';
  @Output() close = new EventEmitter<void>();

  readonly Printer = Printer;
  readonly X = X;

  clinicSettings: ClinicSettings | null = null;
  private settingsSubscription?: Subscription;

  constructor(private clinicSettingsService: ClinicSettingsService) {}

  ngOnInit(): void {
    this.settingsSubscription = this.clinicSettingsService.settings$.subscribe(
      settings => this.clinicSettings = settings
    );
    this.clinicSettingsService.loadSettings();
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

  printReceipt(): void {
    window.print();
  }

  onClose(): void {
    this.close.emit();
  }
}
