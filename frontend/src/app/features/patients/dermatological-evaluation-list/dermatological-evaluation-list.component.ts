import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DermatologicalEvaluationService } from '../../../core/services/dermatological-evaluation.service';
import { DermatologicalEvaluation } from '../../../core/models/dermatological-evaluation.model';
import { DermatologicalEvaluationFormComponent } from '../dermatological-evaluation-form/dermatological-evaluation-form.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { LucideAngularModule, Activity, Stethoscope, Edit, Trash2 } from 'lucide-angular';

@Component({
  selector: 'app-dermatological-evaluation-list',
  standalone: true,
  imports: [CommonModule, DermatologicalEvaluationFormComponent, PaginationComponent, LucideAngularModule],
  templateUrl: './dermatological-evaluation-list.component.html',
})
export class DermatologicalEvaluationListComponent implements OnInit {
  readonly Activity = Activity;
  readonly Stethoscope = Stethoscope;
  readonly Edit = Edit;
  readonly Trash2 = Trash2;

  @Input() patientId!: number;
  
  evaluations: DermatologicalEvaluation[] = [];
  showForm: boolean = false;
  selectedEvaluation?: DermatologicalEvaluation;
  
  currentPage: number = 0;
  pageSize: number = 10;
  totalPages: number = 0;
  totalElements: number = 0;

  constructor(
    private evaluationService: DermatologicalEvaluationService,
    private toastService: ToastService,
    private notificationService: NotificationService
  ) {}

  ngOnInit() {
    this.loadEvaluations();
  }

  loadEvaluations() {
    this.evaluationService.getByPatientId(this.patientId, this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        this.currentPage = page.page.number;
        this.totalPages = page.page.totalPages;
        this.totalElements = page.page.totalElements;
        this.evaluations = page.content;
      },
      error: (err) => {
        console.error('Error fetching evaluations', err);
        this.toastService.show('Error al cargar evaluaciones', 'error');
      }
    });
  }
  
  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadEvaluations();
  }

  openForm(evaluation?: DermatologicalEvaluation) {
    this.selectedEvaluation = evaluation;
    this.showForm = true;
  }

  closeForm(refresh: boolean) {
    this.showForm = false;
    this.selectedEvaluation = undefined;
    if (refresh) {
      this.loadEvaluations();
    }
  }

  async deleteEvaluation(id: number) {
    const confirmed = await this.notificationService.confirm(
      'Eliminar Evaluación',
      '¿Está seguro de eliminar esta evaluación dermatológica?',
      'Sí, eliminar',
      'Cancelar'
    );
    if (confirmed) {
      this.evaluationService.delete(id).subscribe({
        next: () => {
          this.toastService.show('Evaluación eliminada exitosamente', 'success');
          this.loadEvaluations();
        },
        error: (err) => {
          console.error('Error al eliminar evaluación', err);
          this.toastService.show('Error al eliminar evaluación', 'error');
        }
      });
    }
  }
}
