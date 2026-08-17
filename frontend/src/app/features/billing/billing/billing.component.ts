import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PaymentService, PaymentFilters } from '../../../core/services/payment.service';
import { Payment, PaymentSummary } from '../../../core/models/payment.model';
import { PaymentFormComponent } from '../payment-form/payment-form.component';
import { PaymentDetailComponent } from '../payment-detail/payment-detail.component';
import { PatientService } from '../../../core/services/patient/patient.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import {
  LucideAngularModule, Plus, Edit, Trash2, Download, Eye, Search,
  Banknote, Wallet, Receipt, Coins, TrendingUp, TrendingDown, FilterX, CalendarCheck
} from 'lucide-angular';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ExportService } from '../../../shared/services/export/export.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

const METHOD_CHART_COLORS = ['#10b981', '#3b82f6', '#f59e0b', '#8b5cf6', '#64748b', '#ef4444'];

@Component({
  selector: 'app-billing',
  standalone: true,
  imports: [CommonModule, FormsModule, PaymentFormComponent, PaymentDetailComponent, LucideAngularModule, PaginationComponent],
  templateUrl: './billing.component.html'
})
export class BillingComponent implements OnInit, OnDestroy {
  readonly Plus = Plus;
  readonly Edit = Edit;
  readonly Trash2 = Trash2;
  readonly Download = Download;
  readonly Eye = Eye;
  readonly Search = Search;
  readonly Banknote = Banknote;
  readonly Wallet = Wallet;
  readonly Receipt = Receipt;
  readonly Coins = Coins;
  readonly TrendingUp = TrendingUp;
  readonly TrendingDown = TrendingDown;
  readonly FilterX = FilterX;
  readonly CalendarCheck = CalendarCheck;

  @ViewChild('incomeCanvas') incomeCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('methodCanvas') methodCanvas?: ElementRef<HTMLCanvasElement>;

  payments: Payment[] = [];
  summary: PaymentSummary | null = null;
  showForm = false;
  selectedPayment: Payment | null = null;
  viewingPayment: Payment | null = null;
  initialPaymentData: { patientId?: number; appointmentId?: number; description?: string } | null = null;
  patientMap = new Map<number, string>();
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

  private incomeChart: Chart | null = null;
  private methodChart: Chart | null = null;
  private searchSubject = new Subject<string>();
  private searchSubscription?: Subscription;
  private destroy$ = new Subject<void>();

  constructor(
    private paymentService: PaymentService,
    private patientService: PatientService,
    private catalogService: CatalogService,
    private notificationService: NotificationService,
    private toastService: ToastService,
    private exportService: ExportService,
    private route: ActivatedRoute,
    private router: Router
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

    // Preload patients to map IDs to names
    this.patientService.getAll(0, 1000).subscribe({
      next: (patientsPage) => {
        patientsPage.content.forEach(p => this.patientMap.set(p.id!, `${p.firstName} ${p.lastName}`));
        this.loadPayments();
      },
      error: (err) => console.error('Error fetching patients', err)
    });

    // Prefill form when navigating from Agenda ("Registrar Cobro" de una cita)
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      if (params['newPayment'] === 'true' && params['patientId']) {
        this.initialPaymentData = {
          patientId: Number(params['patientId']),
          appointmentId: params['appointmentId'] ? Number(params['appointmentId']) : undefined,
          description: params['description'] || undefined
        };
        this.openForm();
        this.router.navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true });
      }
    });
  }

  ngOnDestroy(): void {
    if (this.searchSubscription) {
      this.searchSubscription.unsubscribe();
    }
    this.destroy$.next();
    this.destroy$.complete();
    if (this.incomeChart) {
      this.incomeChart.destroy();
    }
    if (this.methodChart) {
      this.methodChart.destroy();
    }
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
      paymentMethod: this.filterMethod || undefined
    };
  }

  get hasActiveFilters(): boolean {
    return !!(this.filterDateFrom || this.filterDateTo || this.filterMethod);
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.loadPayments();
  }

  clearFilters(): void {
    this.filterDateFrom = '';
    this.filterDateTo = '';
    this.filterMethod = '';
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
        this.payments = page.content.map(pay => ({
          ...pay,
          patientName: this.patientMap.get(pay.patientId) || 'Paciente Desconocido'
        }));
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

  loadSummary(): void {
    this.paymentService.getSummary().subscribe({
      next: (summary) => {
        this.summary = summary;
        setTimeout(() => this.renderCharts(), 50);
      },
      error: (err) => console.error('Error fetching payment summary', err)
    });
  }

  private renderCharts(): void {
    this.renderIncomeChart();
    this.renderMethodChart();
  }

  private renderIncomeChart(): void {
    if (!this.incomeCanvas?.nativeElement || !this.summary) return;

    if (this.incomeChart) {
      this.incomeChart.destroy();
    }

    const ctx = this.incomeCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    const gradient = ctx.createLinearGradient(0, 0, 0, 220);
    gradient.addColorStop(0, 'rgba(16, 185, 129, 0.15)');
    gradient.addColorStop(1, 'rgba(16, 185, 129, 0.00)');

    const labels = this.summary.dailyIncome.map(d => `${d.date.substring(8, 10)}/${d.date.substring(5, 7)}`);
    const dataPoints = this.summary.dailyIncome.map(d => d.total);

    this.incomeChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: 'Ingresos',
          data: dataPoints,
          borderColor: '#10b981',
          borderWidth: 2,
          backgroundColor: gradient,
          fill: true,
          tension: 0.35,
          pointRadius: 0,
          pointHoverRadius: 5,
          pointHoverBackgroundColor: '#10b981'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#0f172a',
            titleFont: { family: 'Inter', size: 12, weight: 'bold' },
            bodyFont: { family: 'Inter', size: 12 },
            padding: 10,
            cornerRadius: 8,
            callbacks: {
              label: (context) => `S/ ${Number(context.parsed.y).toFixed(2)}`
            }
          }
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: { font: { family: 'Inter', size: 10 }, color: '#64748b', maxTicksLimit: 10 }
          },
          y: {
            beginAtZero: true,
            grid: { color: 'rgba(226, 232, 240, 0.6)' },
            ticks: { font: { family: 'Inter', size: 11 }, color: '#64748b' }
          }
        }
      }
    });
  }

  private renderMethodChart(): void {
    if (!this.methodCanvas?.nativeElement || !this.summary) return;

    if (this.methodChart) {
      this.methodChart.destroy();
    }

    const ctx = this.methodCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    const breakdown = this.summary.methodBreakdown;
    const labels = breakdown.map(m => this.getPaymentMethodText(m.method));
    const dataPoints = breakdown.map(m => m.total);

    this.methodChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: dataPoints,
          backgroundColor: METHOD_CHART_COLORS.slice(0, breakdown.length),
          borderWidth: 0,
          hoverOffset: 4
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '68%',
        plugins: {
          legend: {
            position: 'bottom',
            labels: { font: { family: 'Inter', size: 11 }, color: '#475569', boxWidth: 10, boxHeight: 10, padding: 12 }
          },
          tooltip: {
            backgroundColor: '#0f172a',
            padding: 8,
            cornerRadius: 6,
            callbacks: {
              label: (context) => ` S/ ${Number(context.parsed).toFixed(2)}`
            }
          }
        }
      }
    });
  }

  getTopServiceBarWidth(total: number): number {
    if (!this.summary || this.summary.topServices.length === 0) return 0;
    const max = Math.max(...this.summary.topServices.map(s => s.total));
    return max > 0 ? Math.round((total / max) * 100) : 0;
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

  getPaymentMethodText(method: string): string {
    return this.paymentMethodMap.get(method) || method;
  }

  exportPayments(): void {
    const size = this.totalElements > 0 ? this.totalElements : this.pageSize;
    this.paymentService.getAll(0, size, this.currentFilters()).subscribe({
      next: (page) => {
        const dataToExport = page.content.map(pay => ({
          'Paciente': this.patientMap.get(pay.patientId) || 'Paciente Desconocido',
          'Fecha': (pay.paymentDate || '').replace('T', ' '),
          'Monto': pay.amount,
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
}
