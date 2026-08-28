import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AttentionService } from '../../../core/services/attention.service';
import { ExportService } from '../../../shared/services/export/export.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { ProfessionalProductivity } from '../../../core/models/attention.model';
import { LucideAngularModule } from 'lucide-angular';
import {
  BarChart3,
  CalendarRange,
  Download,
  Users,
  Wallet,
  CheckCircle2
} from '../../../shared/icons/lucide-icons';

type Preset = 'MONTH' | 'LAST_MONTH' | 'YEAR' | 'CUSTOM';

@Component({
  selector: 'app-productivity-report',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './productivity-report.component.html'
})
export class ProductivityReportComponent implements OnInit {
  readonly BarChart3 = BarChart3;
  readonly CalendarRange = CalendarRange;
  readonly Download = Download;
  readonly Users = Users;
  readonly Wallet = Wallet;
  readonly CheckCircle2 = CheckCircle2;

  rows: ProfessionalProductivity[] = [];
  loading = true;
  loadError = false;

  dateFrom = '';
  dateTo = '';
  selectedPreset: Preset = 'MONTH';

  constructor(
    private attentionService: AttentionService,
    private exportService: ExportService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.setPreset('MONTH');
  }

  get totals() {
    return this.rows.reduce(
      (acc, r) => ({
        totalAttentions: acc.totalAttentions + r.totalAttentions,
        billedAmount: acc.billedAmount + r.billedAmount,
        collectedAmount: acc.collectedAmount + r.collectedAmount
      }),
      { totalAttentions: 0, billedAmount: 0, collectedAmount: 0 }
    );
  }

  setPreset(preset: Preset): void {
    this.selectedPreset = preset;
    const now = new Date();
    const pad = (n: number) => n.toString().padStart(2, '0');
    const format = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

    if (preset === 'MONTH') {
      this.dateFrom = format(new Date(now.getFullYear(), now.getMonth(), 1));
      this.dateTo = format(now);
    } else if (preset === 'LAST_MONTH') {
      this.dateFrom = format(new Date(now.getFullYear(), now.getMonth() - 1, 1));
      this.dateTo = format(new Date(now.getFullYear(), now.getMonth(), 0));
    } else if (preset === 'YEAR') {
      this.dateFrom = format(new Date(now.getFullYear(), 0, 1));
      this.dateTo = format(now);
    }

    if (preset !== 'CUSTOM') {
      this.loadReport();
    }
  }

  onCustomDateChange(): void {
    this.selectedPreset = 'CUSTOM';
    if (this.dateFrom && this.dateTo) {
      this.loadReport();
    }
  }

  loadReport(): void {
    this.loading = true;
    this.loadError = false;
    this.attentionService.getProductivityReport(this.dateFrom || undefined, this.dateTo || undefined).subscribe({
      next: (rows) => {
        this.rows = rows;
        this.loading = false;
      },
      error: () => {
        this.rows = [];
        this.loading = false;
        this.loadError = true;
        this.toastService.show('Error al cargar el reporte de productividad', 'error');
      }
    });
  }

  exportReport(): void {
    if (this.rows.length === 0) {
      this.toastService.show('No hay datos para exportar en el período seleccionado', 'info');
      return;
    }
    const dataToExport = this.rows.map(r => ({
      'Profesional': r.professionalName,
      'Atenciones Totales': r.totalAttentions,
      'Atendidas': r.attendedAttentions,
      'Canceladas': r.cancelledAttentions,
      'Cobradas': r.paidAttentions,
      'Tasa de Finalización (%)': r.completionRate,
      'Facturado (S/)': r.billedAmount.toFixed(2),
      'Cobrado (S/)': r.collectedAmount.toFixed(2)
    }));
    this.exportService.exportToCsv(dataToExport, 'Reporte_Productividad_Profesionales');
    this.toastService.show(`${this.rows.length} profesionales exportados`, 'success');
  }
}
