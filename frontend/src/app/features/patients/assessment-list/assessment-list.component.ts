import { Component, Input, OnInit, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AssessmentService } from '../../../core/services/assessment.service';
import { Assessment, InterpretationBand, interpretScore, bandColorClasses } from '../../../core/models/assessment.model';
import { AssessmentFormComponent } from '../assessment-form/assessment-form.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import {
  LucideAngularModule,
  Brain,
  TrendingDown,
  TrendingUp,
  Minus,
  Activity,
  Filter,
  Sparkles,
  Plus,
  CheckCircle2,
  Calendar
} from 'lucide-angular';
import { Chart, ChartConfiguration, registerables } from 'chart.js';

Chart.register(...registerables);

export interface EvolutionSummary {
  testName: string;
  count: number;
  initialScore: number;
  initialDate: string;
  latestScore: number;
  latestDate: string;
  delta: number;
  percentChange: number;
  trend: 'improvement' | 'worsened' | 'stable';
  trendLabel: string;
  latestInterpretation?: InterpretationBand | null;
}

@Component({
  selector: 'app-assessment-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    AssessmentFormComponent,
    PaginationComponent,
    LucideAngularModule
  ],
  templateUrl: './assessment-list.component.html',
})
export class AssessmentListComponent implements OnInit, OnDestroy {
  readonly Brain = Brain;
  readonly TrendingDown = TrendingDown;
  readonly TrendingUp = TrendingUp;
  readonly Minus = Minus;
  readonly Activity = Activity;
  readonly Filter = Filter;
  readonly Sparkles = Sparkles;
  readonly Plus = Plus;
  readonly CheckCircle2 = CheckCircle2;
  readonly Calendar = Calendar;

  @Input() patientId!: number;
  @ViewChild('chartCanvas') chartCanvas!: ElementRef<HTMLCanvasElement>;

  // Table paginated state
  tableAssessments: Assessment[] = [];
  currentPage: number = 0;
  pageSize: number = 10;
  totalPages: number = 0;
  totalElements: number = 0;

  // Evolution complete history
  allAssessments: Assessment[] = [];
  availableTestNames: string[] = [];
  selectedTestFilter: string = 'ALL';

  // Evolution computed metrics
  summaries: EvolutionSummary[] = [];
  activeSummary: EvolutionSummary | null = null;

  showForm: boolean = false;
  chart: any = null;
  isLoading: boolean = true;

  constructor(private assessmentService: AssessmentService) {}

  ngOnInit() {
    this.loadAllData();
  }

  ngOnDestroy() {
    if (this.chart) {
      this.chart.destroy();
    }
  }

  loadAllData() {
    this.isLoading = true;
    this.loadTableAssessments();
    this.loadEvolutionData();
  }

  loadTableAssessments() {
    this.assessmentService.getAssessmentsByPatient(this.patientId, this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        this.totalPages = page.page.totalPages;
        this.totalElements = page.page.totalElements;
        this.tableAssessments = page.content;
      },
      error: (err) => {
        console.error('Error fetching assessments page', err);
      }
    });
  }

  loadEvolutionData() {
    this.assessmentService.getPatientEvolution(this.patientId).subscribe({
      next: (data) => {
        this.allAssessments = data || [];
        this.isLoading = false;

        // Extract unique test names
        const namesSet = new Set<string>();
        this.allAssessments.forEach(a => {
          if (a.testName) namesSet.add(a.testName);
        });
        this.availableTestNames = Array.from(namesSet);

        this.computeEvolutionSummaries();
        this.renderChart();
      },
      error: (err) => {
        console.error('Error fetching evolution data', err);
        this.isLoading = false;
      }
    });
  }

  onFilterChange() {
    this.updateActiveSummary();
    this.renderChart();
  }

  private computeEvolutionSummaries() {
    this.summaries = [];
    const grouped: { [name: string]: Assessment[] } = {};

    this.allAssessments.forEach(a => {
      const name = a.testName || 'Test';
      if (!grouped[name]) grouped[name] = [];
      grouped[name].push(a);
    });

    Object.keys(grouped).forEach(name => {
      const items = grouped[name];
      if (items.length === 0) return;

      const first = items[0];
      const last = items[items.length - 1];
      const delta = last.totalScore - first.totalScore;
      const percent = first.totalScore !== 0
        ? Math.round(((last.totalScore - first.totalScore) / first.totalScore) * 100)
        : 0;

      let trend: 'improvement' | 'worsened' | 'stable' = 'stable';
      let trendLabel = 'Sin cambio significativo';

      if (delta < 0) {
        trend = 'improvement';
        trendLabel = `Reducción sintomática (${Math.abs(delta)} pts / ${Math.abs(percent)}%)`;
      } else if (delta > 0) {
        trend = 'worsened';
        trendLabel = `Incremento de puntaje (+${delta} pts / +${percent}%)`;
      }

      this.summaries.push({
        testName: name,
        count: items.length,
        initialScore: first.totalScore,
        initialDate: first.assessmentDate || '',
        latestScore: last.totalScore,
        latestDate: last.assessmentDate || '',
        delta,
        percentChange: percent,
        trend,
        trendLabel,
        latestInterpretation: this.interpretationOf(last)
      });
    });

    this.updateActiveSummary();
  }

  private updateActiveSummary() {
    if (this.selectedTestFilter === 'ALL') {
      this.activeSummary = this.summaries.length > 0 ? this.summaries[0] : null;
    } else {
      this.activeSummary = this.summaries.find(s => s.testName === this.selectedTestFilter) || null;
    }
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadTableAssessments();
  }

  renderChart() {
    setTimeout(() => {
      if (this.chart) {
        this.chart.destroy();
      }

      if (this.allAssessments.length === 0 || !this.chartCanvas) return;

      const filtered = this.selectedTestFilter === 'ALL'
        ? this.allAssessments
        : this.allAssessments.filter(a => a.testName === this.selectedTestFilter);

      if (filtered.length === 0) return;

      // Group by test name
      const groupedData: { [testName: string]: Assessment[] } = {};
      const datesSet = new Set<string>();

      filtered.forEach(a => {
        const testName = a.testName || 'Test Desconocido';
        if (!groupedData[testName]) {
          groupedData[testName] = [];
        }
        groupedData[testName].push(a);

        if (a.assessmentDate) {
          const d = new Date(a.assessmentDate);
          datesSet.add(d.toLocaleDateString());
        }
      });

      const labels = Array.from(datesSet).sort((a, b) => {
        const partsA = a.split('/');
        const partsB = b.split('/');
        if (partsA.length === 3 && partsB.length === 3) {
          return new Date(+partsA[2], +partsA[1] - 1, +partsA[0]).getTime() - new Date(+partsB[2], +partsB[1] - 1, +partsB[0]).getTime();
        }
        return 0;
      });

      const datasets = Object.keys(groupedData).map((testName, i) => {
        const color = this.getColor(i);
        const dataPoints = labels.map(label => {
          const assessment = groupedData[testName].find(a => {
            if (!a.assessmentDate) return false;
            return new Date(a.assessmentDate).toLocaleDateString() === label;
          });
          return assessment ? assessment.totalScore : null;
        });

        return {
          label: testName,
          data: dataPoints,
          borderColor: color,
          backgroundColor: color + '22', // 13% opacity
          pointBackgroundColor: color,
          pointBorderColor: '#ffffff',
          pointBorderWidth: 2,
          pointRadius: 5,
          pointHoverRadius: 7,
          tension: 0.35,
          fill: true,
          spanGaps: true
        };
      });

      const config: ChartConfiguration = {
        type: 'line',
        data: {
          labels: labels,
          datasets: datasets
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              position: 'top',
              labels: {
                boxWidth: 12,
                usePointStyle: true,
                font: {
                  size: 11,
                  weight: 'bold'
                }
              }
            },
            tooltip: {
              callbacks: {
                label: (context) => {
                  const testName = context.dataset.label || '';
                  const score = context.parsed.y;
                  return ` ${testName}: ${score} pts`;
                }
              }
            }
          },
          scales: {
            y: {
              beginAtZero: true,
              grid: {
                color: 'rgba(226, 232, 240, 0.6)'
              },
              ticks: {
                font: {
                  size: 11
                }
              }
            },
            x: {
              grid: {
                display: false
              },
              ticks: {
                font: {
                  size: 10
                }
              }
            }
          }
        }
      };

      const ctx = this.chartCanvas.nativeElement.getContext('2d');
      if (ctx) {
        this.chart = new Chart(ctx, config);
      }
    }, 0);
  }

  getColor(index: number): string {
    const colors = [
      '#7c3aed', // purple-600
      '#0284c7', // sky-600
      '#059669', // emerald-600
      '#d97706', // amber-600
      '#dc2626', // red-600
      '#0d9488', // teal-600
    ];
    return colors[index % colors.length];
  }

  interpretationOf(assessment: Assessment): InterpretationBand | null {
    return interpretScore(assessment.interpretationJson, assessment.totalScore);
  }

  bandClasses(assessment: Assessment): { badge: string; dot: string } {
    return bandColorClasses(this.interpretationOf(assessment)?.color);
  }

  openForm() {
    this.showForm = true;
  }

  closeForm(refresh: boolean) {
    this.showForm = false;
    if (refresh) {
      this.loadAllData();
    }
  }
}

