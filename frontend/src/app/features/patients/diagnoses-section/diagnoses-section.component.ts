import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DiagnosisService } from '../../../core/services/diagnosis.service';
import { Diagnosis } from '../../../core/models/diagnosis.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { LucideAngularModule, Stethoscope, Edit, Trash2 } from 'lucide-angular';

@Component({
  selector: 'app-diagnoses-section',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PaginationComponent, LucideAngularModule],
  templateUrl: './diagnoses-section.component.html',
})
export class DiagnosesSectionComponent implements OnInit {
  readonly Stethoscope = Stethoscope;
  readonly Edit = Edit;
  readonly Trash2 = Trash2;

  @Input() patientId!: number;

  diagnoses: Diagnosis[] = [];
  showForm = false;
  selected?: Diagnosis;
  form!: FormGroup;
  isSaving = false;
  loading = true;

  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  constructor(
    private fb: FormBuilder,
    private service: DiagnosisService,
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
        this.diagnoses = page.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.show('Error al cargar diagnósticos', 'error');
      }
    });
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.load();
  }

  openForm(diagnosis?: Diagnosis): void {
    this.selected = diagnosis;
    this.form = this.fb.group({
      category: [diagnosis?.category || ''],
      description: [diagnosis?.description || '', Validators.required],
      status: [diagnosis?.status || 'ACTIVO'],
      diagnosisDate: [diagnosis?.diagnosisDate || ''],
      notes: [diagnosis?.notes || '']
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
    const data: Diagnosis = { ...this.form.value, patientId: this.patientId };

    const request = this.selected?.id
      ? this.service.update(this.selected.id, data)
      : this.service.create(this.patientId, data);

    request.subscribe({
      next: () => {
        this.toast.show(this.selected?.id ? 'Diagnóstico actualizado' : 'Diagnóstico creado', 'success');
        this.isSaving = false;
        this.closeForm();
        this.load();
      },
      error: () => {
        this.isSaving = false;
        this.toast.show('Error al guardar el diagnóstico', 'error');
      }
    });
  }

  async delete(id: number): Promise<void> {
    const confirmed = await this.notification.confirm(
      'Eliminar Diagnóstico',
      '¿Está seguro de eliminar este diagnóstico?',
      'Sí, eliminar',
      'Cancelar'
    );
    if (confirmed) {
      this.service.delete(id).subscribe({
        next: () => {
          this.toast.show('Diagnóstico eliminado', 'success');
          this.load();
        },
        error: () => this.toast.show('Error al eliminar el diagnóstico', 'error')
      });
    }
  }

  statusLabel(status?: string): string {
    return status === 'RESUELTO' ? 'Resuelto' : 'Activo';
  }
}
