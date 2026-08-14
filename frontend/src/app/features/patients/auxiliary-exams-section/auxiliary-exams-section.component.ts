import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { AuxiliaryExamService } from '../../../core/services/auxiliary-exam.service';
import { AuxiliaryExam } from '../../../core/models/auxiliary-exam.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { LucideAngularModule, Microscope, Edit, Trash2 } from 'lucide-angular';

@Component({
  selector: 'app-auxiliary-exams-section',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PaginationComponent, LucideAngularModule],
  templateUrl: './auxiliary-exams-section.component.html',
})
export class AuxiliaryExamsSectionComponent implements OnInit {
  readonly Microscope = Microscope;
  readonly Edit = Edit;
  readonly Trash2 = Trash2;

  @Input() patientId!: number;

  exams: AuxiliaryExam[] = [];
  showForm = false;
  selected?: AuxiliaryExam;
  form!: FormGroup;
  isSaving = false;
  loading = true;

  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  constructor(
    private fb: FormBuilder,
    private service: AuxiliaryExamService,
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
        this.exams = page.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.show('Error al cargar exámenes auxiliares', 'error');
      }
    });
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.load();
  }

  openForm(exam?: AuxiliaryExam): void {
    this.selected = exam;
    this.form = this.fb.group({
      examType: [exam?.examType || ''],
      description: [exam?.description || ''],
      result: [exam?.result || ''],
      examDate: [exam?.examDate || ''],
      notes: [exam?.notes || '']
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
    const data: AuxiliaryExam = { ...this.form.value, patientId: this.patientId };

    const request = this.selected?.id
      ? this.service.update(this.selected.id, data)
      : this.service.create(this.patientId, data);

    request.subscribe({
      next: () => {
        this.toast.show(this.selected?.id ? 'Examen actualizado' : 'Examen creado', 'success');
        this.isSaving = false;
        this.closeForm();
        this.load();
      },
      error: () => {
        this.isSaving = false;
        this.toast.show('Error al guardar el examen', 'error');
      }
    });
  }

  async delete(id: number): Promise<void> {
    const confirmed = await this.notification.confirm(
      'Eliminar Examen',
      '¿Está seguro de eliminar este examen auxiliar?',
      'Sí, eliminar',
      'Cancelar'
    );
    if (confirmed) {
      this.service.delete(id).subscribe({
        next: () => {
          this.toast.show('Examen eliminado', 'success');
          this.load();
        },
        error: () => this.toast.show('Error al eliminar el examen', 'error')
      });
    }
  }
}
