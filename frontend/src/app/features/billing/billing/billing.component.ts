import { Component, OnInit } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { PaymentService } from '../../../core/services/payment.service';
import { Payment } from '../../../core/models/payment.model';
import { PaymentFormComponent } from '../payment-form/payment-form.component';
import { PatientService } from '../../../core/services/patient/patient.service';

@Component({
  selector: 'app-billing',
  standalone: true,
  imports: [CommonModule, PaymentFormComponent],
  templateUrl: './billing.component.html',
  styleUrl: './billing.component.css'
})
export class BillingComponent implements OnInit {
  payments: Payment[] = [];
  showForm = false;
  patientMap = new Map<number, string>();

  constructor(
    private paymentService: PaymentService,
    private patientService: PatientService
  ) {}

  ngOnInit(): void {
    // Preload patients to map IDs to names
    this.patientService.getAll().subscribe({
      next: (patients) => {
        patients.forEach(p => this.patientMap.set(p.id!, `${p.firstName} ${p.lastName}`));
        this.loadPayments();
      },
      error: (err) => console.error('Error fetching patients', err)
    });
  }

  loadPayments(): void {
    this.paymentService.getAll().subscribe({
      next: (data) => {
        this.payments = data.map(pay => ({
          ...pay,
          patientName: this.patientMap.get(pay.patientId) || 'Paciente Desconocido'
        }));
      },
      error: (err) => console.error('Error fetching payments', err)
    });
  }

  openForm(): void {
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
  }

  onPaymentSaved(): void {
    this.showForm = false;
    this.loadPayments();
  }

  getPaymentMethodText(method: string): string {
    switch (method) {
      case 'CASH': return 'Efectivo';
      case 'TRANSFER': return 'Transferencia';
      case 'CARD': return 'Tarjeta';
      default: return method;
    }
  }
}
