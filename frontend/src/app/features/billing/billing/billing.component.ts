import { Component, OnInit } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { PaymentService } from '../../../core/services/payment.service';
import { Payment } from '../../../core/models/payment.model';
import { PaymentFormComponent } from '../payment-form/payment-form.component';
import { PatientService } from '../../../core/services/patient/patient.service';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { LucideAngularModule, Plus, Edit, Trash2, Download } from 'lucide-angular';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ExportService } from '../../../shared/services/export/export.service';
import { ToastService } from '../../../shared/services/toast/toast.service';

@Component({
  selector: 'app-billing',
  standalone: true,
  imports: [CommonModule, PaymentFormComponent, LucideAngularModule, PaginationComponent],
  templateUrl: './billing.component.html',
  styleUrl: './billing.component.css'
})
export class BillingComponent implements OnInit {
  readonly Plus = Plus;
  readonly Edit = Edit;
  readonly Trash2 = Trash2;
  readonly Download = Download;
  
  payments: Payment[] = [];
  showForm = false;
  selectedPayment: Payment | null = null;
  patientMap = new Map<number, string>();
  
  currentPage: number = 0;
  pageSize: number = 10;
  totalPages: number = 0;
  totalElements: number = 0;

  constructor(
    private paymentService: PaymentService,
    private patientService: PatientService,
    private notificationService: NotificationService,
    private toastService: ToastService,
    private exportService: ExportService
  ) {}

  ngOnInit(): void {
    // Preload patients to map IDs to names
    this.patientService.getAll(0, 1000).subscribe({
      next: (patientsPage) => {
        patientsPage.content.forEach(p => this.patientMap.set(p.id!, `${p.firstName} ${p.lastName}`));
        this.loadPayments();
      },
      error: (err) => console.error('Error fetching patients', err)
    });
  }

  loadPayments(): void {
    this.paymentService.getAll(this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        this.totalPages = page.page.totalPages;
        this.totalElements = page.page.totalElements;
        this.payments = page.content.map(pay => ({
          ...pay,
          patientName: this.patientMap.get(pay.patientId) || 'Paciente Desconocido'
        }));
      },
      error: (err) => console.error('Error fetching payments', err)
    });
  }
  
  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadPayments();
  }

  openForm(payment?: Payment): void {
    this.selectedPayment = payment || null;
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
  }

  onPaymentSaved(): void {
    this.showForm = false;
    this.selectedPayment = null;
    this.loadPayments();
  }

  deletePayment(id: number): void {
    this.notificationService.confirm(
      'Eliminar Cobro',
      '¿Está seguro de que desea eliminar este cobro?',
      'Eliminar',
      'Cancelar'
    ).then((confirmed: boolean) => {
      if (confirmed) {
        this.paymentService.delete(id).subscribe({
          next: () => {
            this.toastService.show('Cobro eliminado exitosamente', 'success');
            this.loadPayments();
          },
          error: (err) => {
            console.error('Error deleting payment', err);
            this.toastService.show('Error al eliminar el cobro', 'error');
          }
        });
      }
    });
  }

  getPaymentMethodText(method: string): string {
    switch (method) {
      case 'CASH': return 'Efectivo';
      case 'TRANSFER': return 'Transferencia';
      case 'CARD': return 'Tarjeta';
      default: return method;
    }
  }

  exportPayments(): void {
    const dataToExport = this.payments.map(pay => ({
      'Paciente': pay.patientName,
      'Fecha': (pay.paymentDate || '').replace('T', ' '),
      'Monto': pay.amount,
      'Método de Pago': this.getPaymentMethodText(pay.paymentMethod),
      'Motivo': pay.description || ''
    }));

    this.exportService.exportToExcel(dataToExport, 'Cobros_Facturacion');
  }
}
