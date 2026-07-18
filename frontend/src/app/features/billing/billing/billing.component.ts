import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { PaymentService } from '../../../core/services/payment.service';
import { Payment } from '../../../core/models/payment.model';
import { PaymentFormComponent } from '../payment-form/payment-form.component';
import { PaymentDetailComponent } from '../payment-detail/payment-detail.component';
import { PatientService } from '../../../core/services/patient/patient.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { LucideAngularModule, Plus, Edit, Trash2, Download, Eye, Search } from 'lucide-angular';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ExportService } from '../../../shared/services/export/export.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

@Component({
  selector: 'app-billing',
  standalone: true,
  imports: [CommonModule, PaymentFormComponent, PaymentDetailComponent, LucideAngularModule, PaginationComponent],
  templateUrl: './billing.component.html',
  styleUrl: './billing.component.css'
})
export class BillingComponent implements OnInit, OnDestroy {
  readonly Plus = Plus;
  readonly Edit = Edit;
  readonly Trash2 = Trash2;
  readonly Download = Download;
  readonly Eye = Eye;
  readonly Search = Search;
  
  payments: Payment[] = [];
  showForm = false;
  selectedPayment: Payment | null = null;
  viewingPayment: Payment | null = null;
  patientMap = new Map<number, string>();
  paymentMethodMap = new Map<string, string>();
  
  currentPage: number = 0;
  pageSize: number = 10;
  totalPages: number = 0;
  totalElements: number = 0;

  searchTerm: string = '';
  private searchSubject = new Subject<string>();
  private searchSubscription?: Subscription;

  constructor(
    private paymentService: PaymentService,
    private patientService: PatientService,
    private catalogService: CatalogService,
    private notificationService: NotificationService,
    private toastService: ToastService,
    private exportService: ExportService
  ) {}

  ngOnInit(): void {
    // Debounce search
    this.searchSubscription = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(term => {
      this.searchTerm = term;
      this.currentPage = 0;
      this.loadPayments();
    });

    // Load payment methods from catalog
    this.catalogService.getActiveItemsByCatalogCode('PAYMENT_METHOD').subscribe({
      next: (items) => {
        items.forEach(item => this.paymentMethodMap.set(item.itemCode, item.itemName));
      },
      error: (err) => console.error('Error fetching payment methods', err)
    });

    // Preload patients to map IDs to names
    this.patientService.getAll(0, 1000).subscribe({
      next: (patientsPage) => {
        patientsPage.content.forEach(p => this.patientMap.set(p.id!, `${p.firstName} ${p.lastName}`));
        this.loadPayments();
      },
      error: (err) => console.error('Error fetching patients', err)
    });
  }

  ngOnDestroy(): void {
    if (this.searchSubscription) {
      this.searchSubscription.unsubscribe();
    }
  }

  onSearch(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchSubject.next(target.value);
  }

  loadPayments(): void {
    this.paymentService.getAll(this.currentPage, this.pageSize, this.searchTerm).subscribe({
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

  viewPayment(payment: Payment): void {
    this.viewingPayment = payment;
  }

  closeView(): void {
    this.viewingPayment = null;
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
    return this.paymentMethodMap.get(method) || method;
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
