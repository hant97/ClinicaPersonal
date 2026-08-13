import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { PsychologyEvaluationService } from '../../../core/services/psychology-evaluation.service';
import { PsychologyEvaluation } from '../../../core/models/psychology-evaluation.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { LucideAngularModule, Brain } from 'lucide-angular';

@Component({
  selector: 'app-psychology-evaluation-section',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PaginationComponent, LucideAngularModule],
  templateUrl: './psychology-evaluation-section.component.html',
})
export class PsychologyEvaluationSectionComponent implements OnInit {
  readonly Brain = Brain;

  @Input() patientId!: number;

  evaluations: PsychologyEvaluation[] = [];
  showForm = false;
  selected?: PsychologyEvaluation;
  form!: FormGroup;
  isSaving = false;
  loading = true;

  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  constructor(
    private fb: FormBuilder,
    private service: PsychologyEvaluationService,
    private toast: ToastService,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.service.getByPatientId(this.patientId, this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        this.currentPage = page.page.number;
        this.totalPages = page.page.totalPages;
        this.totalElements = page.page.totalElements;
        this.evaluations = page.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.show('Error al cargar evaluaciones', 'error');
      }
    });
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.load();
  }

  openForm(evaluation?: PsychologyEvaluation): void {
    this.selected = evaluation;
    this.form = this.fb.group({
      evaluationDate: [evaluation?.evaluationDate || this.today(), Validators.required],
      initialEvaluation: [evaluation?.initialEvaluation || ''],
      psychologicalHistory: [evaluation?.psychologicalHistory || ''],
      mentalExam: [evaluation?.mentalExam || ''],
      notes: [evaluation?.notes || '']
    });
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
    this.selected = undefined;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isSaving = true;
    const data: PsychologyEvaluation = { ...this.form.value, patientId: this.patientId };

    const request = this.selected?.id
      ? this.service.update(this.selected.id, data)
      : this.service.create(this.patientId, data);

    request.subscribe({
      next: () => {
        this.toast.show(this.selected?.id ? 'Evaluación actualizada' : 'Evaluación creada', 'success');
        this.isSaving = false;
        this.closeForm();
        this.load();
      },
      error: () => {
        this.isSaving = false;
        this.toast.show('Error al guardar la evaluación', 'error');
      }
    });
  }

  async delete(id: number): Promise<void> {
    const confirmed = await this.notification.confirm(
      'Eliminar Evaluación',
      '¿Está seguro de eliminar esta evaluación psicológica?',
      'Sí, eliminar',
      'Cancelar'
    );
    if (confirmed) {
      this.service.delete(id).subscribe({
        next: () => {
          this.toast.show('Evaluación eliminada', 'success');
          this.load();
        },
        error: () => this.toast.show('Error al eliminar la evaluación', 'error')
      });
    }
  }

  private today(): string {
    const now = new Date();
    const tzOffset = now.getTimezoneOffset() * 60000;
    return new Date(now.getTime() - tzOffset).toISOString().split('T')[0];
  }
}
