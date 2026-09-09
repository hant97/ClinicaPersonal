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
import { LucideAngularModule } from 'lucide-angular';
import {
  Plus, Edit, Trash2, Download, Eye, Search, FilterX, CalendarCheck, CalendarRange,
  LayoutGrid, List, Banknote, Wallet, CreditCard, Landmark, Smartphone, MoreHorizontal, BarChart3
} from '../../../shared/icons/lucide-icons';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ExportService } from '../../../shared/services/export/export.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { ViewPreferenceService } from '../../../shared/services/view-preference/view-preference.service';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { fetchAllPages } from '../../../core/utils/pagination.util';

type DatePreset = 'TODAY' | 'WEEK' | 'MONTH' | 'LAST_MONTH' | 'YEAR' | 'CUSTOM';

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
  readonly CalendarRange = CalendarRange;
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

  selectedPreset: DatePreset = 'MONTH';

  readonly datePresets: { code: DatePreset; label: string }[] = [
    { code: 'TODAY', label: 'Hoy' },
    { code: 'WEEK', label: 'Últimos 7 días' },
    { code: 'MONTH', label: 'Este mes' },
    { code: 'LAST_MONTH', label: 'Mes anterior' },
    { code: 'YEAR', label: 'Este año' },
    { code: 'CUSTOM', label: 'Personalizado' }
  ];

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
    this.setPresetDates('MONTH', false);

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
        this.loadSummary(this.filterDateFrom, this.filterDateTo);
      },
      error: (err) => {
        console.error('Error fetching payment methods', err);
        this.loadSummary(this.filterDateFrom, this.filterDateTo);
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
    return this.selectedPreset !== 'MONTH'
      || !!(this.searchTerm || this.filterMethod || this.filterStatus);
  }

  get dateRangeIncomplete(): boolean {
    return (!!this.filterDateFrom && !this.filterDateTo) || (!this.filterDateFrom && !!this.filterDateTo);
  }

  get dateRangeInvalid(): boolean {
    return !!(this.filterDateFrom && this.filterDateTo && this.filterDateFrom > this.filterDateTo);
  }

  get periodLabel(): string {
    switch (this.selectedPreset) {
      case 'TODAY': return 'de hoy';
      case 'WEEK': return 'de los últimos 7 días';
      case 'LAST_MONTH': return 'del mes anterior';
      case 'YEAR': return 'de este año';
      case 'CUSTOM': return 'del período seleccionado';
      default: return 'de este mes';
    }
  }

  get appliedPeriodLabel(): string {
    if (!this.filterDateFrom || !this.filterDateTo) return 'Selecciona ambas fechas';
    return `Resultados del ${this.formatIsoDate(this.filterDateFrom)} al ${this.formatIsoDate(this.filterDateTo)}`;
  }

  applyFilters(): void {
    if (this.dateRangeIncomplete || this.dateRangeInvalid) return;
    this.currentPage = 0;
    this.loadPayments();
  }

  applyDateRange(): void {
    this.selectedPreset = 'CUSTOM';
    if (this.dateRangeIncomplete || this.dateRangeInvalid) return;
    this.currentPage = 0;
    this.loadPayments();
    this.loadSummary(this.filterDateFrom, this.filterDateTo);
  }

  setPreset(preset: DatePreset): void {
    this.setPresetDates(preset, true);
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.searchSubject.next('');
    this.filterMethod = '';
    this.filterStatus = '';
    this.setPresetDates('MONTH', true);
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
    this.loadSummary(this.filterDateFrom || undefined, this.filterDateTo || undefined);
  }

  onPaymentSaved(): void {
    this.showForm = false;
    this.selectedPayment = null;
    this.initialPaymentData = null;
    this.loadPayments();
    this.loadSummary(this.filterDateFrom || undefined, this.filterDateTo || undefined);
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
            this.loadSummary(this.filterDateFrom || undefined, this.filterDateTo || undefined);
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
    fetchAllPages((page, size) => this.paymentService.getAll(page, size, this.currentFilters())).subscribe({
      next: (payments) => {
        const dataToExport = payments.map(pay => ({
          'Paciente': pay.patientName || 'Paciente Desconocido',
          'Fecha': (pay.paymentDate || '').replace('T', ' '),
          'Monto': pay.amount,
          'Abonado': pay.paidAmount ?? 0,
          'Saldo': pay.balanceAmount ?? pay.amount,
          'Estado': this.getStatusText(pay.status),
          'Método de Pago': this.getPaymentMethodText(pay.paymentMethod),
          'Motivo': pay.description || ''
        }));

        this.exportService.exportToCsv(dataToExport, 'Cobros_Facturacion');
        this.toastService.show(`${payments.length} cobros exportados`, 'success');
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

  private setPresetDates(preset: DatePreset, reload: boolean): void {
    this.selectedPreset = preset;
    if (preset === 'CUSTOM') return;

    const now = new Date();
    let from = new Date(now);
    let to = new Date(now);

    switch (preset) {
      case 'WEEK':
        from.setDate(from.getDate() - 6);
        break;
      case 'MONTH':
        from = new Date(now.getFullYear(), now.getMonth(), 1);
        break;
      case 'LAST_MONTH':
        from = new Date(now.getFullYear(), now.getMonth() - 1, 1);
        to = new Date(now.getFullYear(), now.getMonth(), 0);
        break;
      case 'YEAR':
        from = new Date(now.getFullYear(), 0, 1);
        break;
    }

    this.filterDateFrom = this.toIsoDate(from);
    this.filterDateTo = this.toIsoDate(to);

    if (reload) {
      this.currentPage = 0;
      this.loadPayments();
      this.loadSummary(this.filterDateFrom, this.filterDateTo);
    }
  }

  private toIsoDate(date: Date): string {
    const pad = (value: number) => value.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  }

  private formatIsoDate(value: string): string {
    const [year, month, day] = value.split('-');
    return `${day}/${month}/${year}`;
  }
}
