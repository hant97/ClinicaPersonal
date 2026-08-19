import { Component, ElementRef, Input, OnChanges, OnDestroy, SimpleChanges, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PaymentSummary } from '../../../core/models/payment.model';
import {
  LucideAngularModule, Banknote, Wallet, Receipt, Coins, TrendingUp, TrendingDown
} from 'lucide-angular';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

const METHOD_CHART_COLORS = ['#10b981', '#3b82f6', '#f59e0b', '#8b5cf6', '#64748b', '#ef4444'];

@Component({
  selector: 'app-billing-summary',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './billing-summary.component.html',
  host: {
    class: 'block'
  }
})
export class BillingSummaryComponent implements OnChanges, OnDestroy {
  readonly Banknote = Banknote;
  readonly Wallet = Wallet;
  readonly Receipt = Receipt;
  readonly Coins = Coins;
  readonly TrendingUp = TrendingUp;
  readonly TrendingDown = TrendingDown;

  @Input() summary: PaymentSummary | null = null;
  @Input() paymentMethodMap: Map<string, string> = new Map();
  @Input() showCharts: boolean = false;

  @ViewChild('incomeCanvas') incomeCanvas?: ElementRef<HTMLCanvasElement>;
  @ViewChild('methodCanvas') methodCanvas?: ElementRef<HTMLCanvasElement>;

  private incomeChart: Chart | null = null;
  private methodChart: Chart | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['summary'] || changes['paymentMethodMap'] || changes['showCharts']) {
      if (this.showCharts) {
        setTimeout(() => this.renderCharts(), 50);
      }
    }
  }

  ngOnDestroy(): void {
    if (this.incomeChart) {
      this.incomeChart.destroy();
    }
    if (this.methodChart) {
      this.methodChart.destroy();
    }
  }

  getPaymentMethodText(method: string): string {
    return this.paymentMethodMap.get(method) || method;
  }

  getTopServiceBarWidth(total: number): number {
    if (!this.summary || this.summary.topServices.length === 0) return 0;
    const max = Math.max(...this.summary.topServices.map(s => s.total));
    return max > 0 ? Math.round((total / max) * 100) : 0;
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
            ticks: {
              font: { family: 'Inter', size: 10 },
              color: '#64748b',
              maxTicksLimit: 6,
              maxRotation: 45,
              minRotation: 0
            }
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
}
