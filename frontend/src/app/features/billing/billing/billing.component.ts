import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PaymentService, PaymentFilters } from '../../../core/services/payment.service';
import { Payment, PaymentSummary } from '../../../core/models/payment.model';
import { PaymentFormComponent } from '../payment-form/payment-form.component';
import { PaymentDetailComponent } from '../payment-detail/payment-detail.component';
import { BillingSummaryComponent } from '../billing-summary/billing-summary.component';
import { CatalogService } from '../../../core/services/catalog.service';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import {
  LucideAngularModule, Plus, Edit, Trash2, Download, Eye, Search, FilterX, CalendarCheck,
  LayoutGrid, List, Banknote, Wallet, CreditCard, Landmark, Smartphone, MoreHorizontal, BarChart3
} from 'lucide-angular';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ExportService } from '../../../shared/services/export/export.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { ViewPreferenceService } from '../../../shared/services/view-preference/view-preference.service';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-billing',
  standalone: true,
  imports: [CommonModule, FormsModule, PaymentFormComponent, PaymentDetailComponent, BillingSummaryComponent, LucideAngularModule, PaginationComponent],
  templateUrl: './billing.component.html'
})
export class BillingComponent implements OnInit, OnDestroy {
  readonly Plus = Plus;
  readonly Edit = Edit;
  readonly Trash2 = Trash2;
  readonly Download = Download;
  readonly Eye = Eye;
  readonly Search = Search;
  readonly FilterX = FilterX;
  readonly CalendarCheck = CalendarCheck;
  readonly LayoutGrid = LayoutGrid;
  readonly List = List;
  readonly Banknote = Banknote;
  readonly Wallet = Wallet;
  readonly CreditCard = CreditCard;
  readonly Landmark = Landmark;
  readonly Smartphone = Smartphone;
  readonly MoreHorizontal = MoreHorizontal;
  readonly BarChart3 = BarChart3;

  payments: Payment[] = [];
  viewMode: 'table' | 'cards' = 'table';
  summary: PaymentSummary | null = null;
  showForm = false;
  showCharts = false;
  selectedPayment: Payment | null = null;
  viewingPayment: Payment | null = null;
  openMenuPaymentId: number | null = null;
  initialPaymentData: { patientId?: number; appointmentId?: number; clinicalServiceId?: number; description?: string } | null = null;
  paymentMethodMap = new Map<string, string>();
  paymentMethodOptions: { code: string; name: string }[] = [];

  currentPage: number = 0;
  pageSize: number = 10;
  totalPages: number = 0;
  totalElements: number = 0;
  isLoading = false;
  loadError = false;

  searchTerm: string = '';
  filterDateFrom: string = '';
  filterDateTo: string = '';
  filterMethod: string = '';
  filterStatus: string = '';

  summaryDateFrom: string = '';
  summaryDateTo: string = '';

  readonly statusOptions: { code: string; name: string }[] = [
    { code: 'PENDIENTE', name: 'Pendiente' },
    { code: 'PARCIAL', name: 'Parcial' },
    { code: 'PAGADO', name: 'Pagado' }
  ];

  private searchSubject = new Subject<string>();
  private searchSubscription?: Subscription;
  private destroy$ = new Subject<void>();

  constructor(
    private paymentService: PaymentService,
    private catalogService: CatalogService,
    private notificationService: NotificationService,
    private toastService: ToastService,
    private exportService: ExportService,
    private viewPreferenceService: ViewPreferenceService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.viewMode = this.viewPreferenceService.getViewMode<'table' | 'cards'>('billing_view_mode', 'table', 'cards');

    // Debounce search
    this.searchSubscription = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(term => {
      this.searchTerm = term;
      this.currentPage = 0;
      this.loadPayments();
    });

    // Load payment methods from catalog, then the summary (so charts use translated names)
    this.catalogService.getActiveItemsByCatalogCode('PAYMENT_METHOD').subscribe({
      next: (items) => {
        items.forEach(item => this.paymentMethodMap.set(item.itemCode, item.itemName));
        this.paymentMethodOptions = items.map(item => ({ code: item.itemCode, name: item.itemName }));
        this.loadSummary();
      },
      error: (err) => {
        console.error('Error fetching payment methods', err);
        this.loadSummary();
      }
    });

    // Prefill form when navigating from Agenda or Atenciones ("Registrar Cobro")
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      if ((params['newPayment'] === 'true' || params['attentionId']) && params['patientId']) {
        this.initialPaymentData = {
          patientId: Number(params['patientId']),
          appointmentId: params['appointmentId'] ? Number(params['appointmentId']) : undefined,
          clinicalServiceId: params['clinicalServiceId'] ? Number(params['clinicalServiceId']) : undefined,
          description: params['description'] || undefined
        };
        this.openForm();
        this.router.navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true });
      } else if (params['paymentId']) {
        this.paymentService.getById(Number(params['paymentId'])).subscribe({
          next: (payment: Payment) => this.viewPayment(payment),
          error: () => {}
        });
        this.router.navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true });
      }
    });

    this.loadPayments();
  }

  ngOnDestroy(): void {
    if (this.searchSubscription) {
      this.searchSubscription.unsubscribe();
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSearch(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchSubject.next(target.value);
  }

  private currentFilters(): PaymentFilters {
    return {
      searchTerm: this.searchTerm || undefined,
      dateFrom: this.filterDateFrom || undefined,
      dateTo: this.filterDateTo || undefined,
      paymentMethod: this.filterMethod || undefined,
      status: this.filterStatus || undefined
    };
  }

  get hasActiveFilters(): boolean {
    return !!(this.filterDateFrom || this.filterDateTo || this.filterMethod || this.filterStatus);
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.loadPayments();
  }

  clearFilters(): void {
    this.filterDateFrom = '';
    this.filterDateTo = '';
    this.filterMethod = '';
    this.filterStatus = '';
    this.currentPage = 0;
    this.loadPayments();
  }

  loadPayments(): void {
    this.isLoading = true;
    this.loadError = false;
    this.paymentService.getAll(this.currentPage, this.pageSize, this.currentFilters()).subscribe({
      next: (page) => {
        this.totalPages = page.page.totalPages;
        this.totalElements = page.page.totalElements;
        this.payments = page.content;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.loadError = true;
        console.error('Error fetching payments', err);
        this.toastService.show('Error al cargar los cobros', 'error');
      }
    });
  }

  loadSummary(dateFrom?: string, dateTo?: string): void {
    this.paymentService.getSummary(dateFrom, dateTo).subscribe({
      next: (summary) => this.summary = summary,
      error: (err) => console.error('Error fetching payment summary', err)
    });
  }

  onSummaryRangeChange(range: { dateFrom: string; dateTo: string }): void {
    this.summaryDateFrom = range.dateFrom;
    this.summaryDateTo = range.dateTo;
    this.loadSummary(range.dateFrom || undefined, range.dateTo || undefined);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadPayments();
  }

  openForm(payment?: Payment): void {
    this.selectedPayment = payment || null;
    if (payment) {
      this.initialPaymentData = null;
    }
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
    this.initialPaymentData = null;
  }

  viewPayment(payment: Payment): void {
    this.viewingPayment = payment;
  }

  closeView(): void {
    this.viewingPayment = null;
  }

  onPaymentChanged(): void {
    this.loadPayments();
    this.loadSummary(this.summaryDateFrom || undefined, this.summaryDateTo || undefined);
  }

  onPaymentSaved(): void {
    this.showForm = false;
    this.selectedPayment = null;
    this.initialPaymentData = null;
    this.loadPayments();
    this.loadSummary();
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
            this.loadSummary();
          },
          error: (err) => {
            console.error('Error deleting payment', err);
            this.toastService.show('Error al eliminar el cobro', 'error');
          }
        });
      }
    });
  }

  setViewMode(mode: 'table' | 'cards'): void {
    this.viewMode = mode;
    this.viewPreferenceService.setViewMode('billing_view_mode', mode);
  }

  toggleCharts(): void {
    this.showCharts = !this.showCharts;
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.openMenuPaymentId = null;
  }

  toggleMenu(paymentId?: number, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    if (!paymentId) return;
    this.openMenuPaymentId = this.openMenuPaymentId === paymentId ? null : paymentId;
  }

  closeMenu(): void {
    this.openMenuPaymentId = null;
  }

  formatPaymentDate(dateStr?: string): string {
    if (!dateStr) return '—';
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return dateStr;
    const months = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'set', 'oct', 'nov', 'dic'];
    const day = date.getDate();
    const month = months[date.getMonth()];
    let hours = date.getHours();
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const ampm = hours >= 12 ? 'p.m.' : 'a.m.';
    hours = hours % 12;
    hours = hours ? hours : 12;
    return `${day} ${month}, ${hours}:${minutes} ${ampm}`;
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

  exportPayments(): void {
    const size = this.totalElements > 0 ? this.totalElements : this.pageSize;
    this.paymentService.getAll(0, size, this.currentFilters()).subscribe({
      next: (page) => {
        const dataToExport = page.content.map(pay => ({
          'Paciente': pay.patientName || 'Paciente Desconocido',
          'Fecha': (pay.paymentDate || '').replace('T', ' '),
          'Monto': pay.amount,
          'Abonado': pay.paidAmount ?? 0,
          'Saldo': pay.balanceAmount ?? pay.amount,
          'Estado': this.getStatusText(pay.status),
          'Método de Pago': this.getPaymentMethodText(pay.paymentMethod),
          'Motivo': pay.description || ''
        }));

        this.exportService.exportToExcel(dataToExport, 'Cobros_Facturacion');
        this.toastService.show(`${page.content.length} cobros exportados`, 'success');
      },
      error: (err) => {
        console.error('Error exporting payments', err);
        this.toastService.show('Error al exportar los cobros', 'error');
      }
    });
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
      case 'PENDIENTE':
        return { classes: 'bg-amber-50 text-amber-700 border-amber-200' };
      case 'PARCIAL':
        return { classes: 'bg-sky-50 text-sky-700 border-sky-200' };
      case 'PAGADO':
        return { classes: 'bg-emerald-50 text-emerald-700 border-emerald-200' };
      default:
        return { classes: 'bg-slate-100 text-slate-600 border-line' };
    }
  }
}
