import { Component, Input, OnInit, AfterViewInit, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AssessmentService } from '../../../core/services/assessment.service';
import { Assessment } from '../../../core/models/assessment.model';
import { AssessmentFormComponent } from '../assessment-form/assessment-form.component';
import { Chart, ChartConfiguration, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-assessment-list',
  standalone: true,
  imports: [CommonModule, AssessmentFormComponent],
  templateUrl: './assessment-list.component.html',
  styleUrl: './assessment-list.component.css'
})
export class AssessmentListComponent implements OnInit, OnDestroy {
  @Input() patientId!: number;
  @ViewChild('chartCanvas') chartCanvas!: ElementRef<HTMLCanvasElement>;
  
  assessments: Assessment[] = [];
  showForm: boolean = false;
  chart: Chart | null = null;

  constructor(private assessmentService: AssessmentService) {}

  ngOnInit() {
    this.loadAssessments();
  }

  ngOnDestroy() {
    if (this.chart) {
      this.chart.destroy();
    }
  }

  loadAssessments() {
    this.assessmentService.getAssessmentsByPatient(this.patientId).subscribe(data => {
      // Sort assessments by date ascending for the chart
      this.assessments = data.sort((a, b) => {
        const dateA = a.assessmentDate ? new Date(a.assessmentDate).getTime() : 0;
        const dateB = b.assessmentDate ? new Date(b.assessmentDate).getTime() : 0;
        return dateA - dateB;
      });
      this.renderChart();
    });
  }

  renderChart() {
    setTimeout(() => {
      if (this.chart) {
        this.chart.destroy();
      }
      
      if (this.assessments.length === 0 || !this.chartCanvas) return;

      // Group by test name
      const groupedData: { [testName: string]: Assessment[] } = {};
    const datesSet = new Set<string>();

    this.assessments.forEach(a => {
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
        backgroundColor: color + '33', // 20% opacity
        tension: 0.3,
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
          },
          title: {
            display: true,
            text: 'Evolución de Evaluaciones Psicométricas'
          }
        },
        scales: {
          y: {
            beginAtZero: true
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
      '#4299e1', // blue
      '#48bb78', // green
      '#ed8936', // orange
      '#9f7aea', // purple
      '#f56565', // red
      '#38b2ac', // teal
    ];
    return colors[index % colors.length];
  }

  openForm() {
    this.showForm = true;
  }

  closeForm(refresh: boolean) {
    this.showForm = false;
    if (refresh) {
      this.loadAssessments();
    }
  }
}
