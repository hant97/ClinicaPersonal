import { Component, Input, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { COMMON_STANDALONE_IMPORTS } from '../../../shared/common-standalone-imports';
import { TreatmentService } from '../../../core/services/treatment.service';
import { Treatment } from '../../../core/models/treatment.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { LucideAngularModule } from 'lucide-angular';
import {
  Pill, Edit, Trash2 } from '../../../shared/icons/lucide-icons';

@Component({
  selector: 'app-treatments-section',
  standalone: true,
  imports: [...COMMON_STANDALONE_IMPORTS, PaginationComponent, LucideAngularModule],
  templateUrl: './treatments-section.component.html',
})
export class TreatmentsSectionComponent implements OnInit {
  readonly Pill = Pill;
  readonly Edit = Edit;
  readonly Trash2 = Trash2;

  @Input() patientId!: number;

  treatments: Treatment[] = [];
  showForm = false;
  selected?: Treatment;
  form!: FormGroup;
  isSaving = false;
  loading = true;

  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  constructor(
    private fb: FormBuilder,
    private service: TreatmentService,
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
        this.treatments = page.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.show('Error al cargar tratamientos', 'error');
      }
    });
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.load();
  }

  openForm(treatment?: Treatment): void {
    this.selected = treatment;
    this.form = this.fb.group({
      name: [treatment?.name || '', Validators.required],
      dose: [treatment?.dose || ''],
      route: [treatment?.route || ''],
      frequency: [treatment?.frequency || ''],
      startDate: [treatment?.startDate || ''],
      endDate: [treatment?.endDate || ''],
      status: [treatment?.status || 'ACTIVO'],
      notes: [treatment?.notes || '']
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
    const data: Treatment = { ...this.form.value, patientId: this.patientId };

    const request = this.selected?.id
      ? this.service.update(this.selected.id, data)
      : this.service.create(this.patientId, data);

    request.subscribe({
      next: () => {
        this.toast.show(this.selected?.id ? 'Tratamiento actualizado' : 'Tratamiento creado', 'success');
        this.isSaving = false;
        this.closeForm();
        this.load();
      },
      error: () => {
        this.isSaving = false;
        this.toast.show('Error al guardar el tratamiento', 'error');
      }
    });
  }

  async delete(id: number): Promise<void> {
    const confirmed = await this.notification.confirm(
      'Eliminar Tratamiento',
      '¿Está seguro de eliminar este tratamiento?',
      'Sí, eliminar',
      'Cancelar'
    );
    if (confirmed) {
      this.service.delete(id).subscribe({
        next: () => {
          this.toast.show('Tratamiento eliminado', 'success');
          this.load();
        },
        error: () => this.toast.show('Error al eliminar el tratamiento', 'error')
      });
    }
  }

  statusLabel(status?: string): string {
    switch (status) {
      case 'COMPLETADO': return 'Completado';
      case 'SUSPENDIDO': return 'Suspendido';
      default: return 'Activo';
    }
  }
}
