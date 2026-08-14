import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MedicationService } from '../../../core/services/medication.service';
import { Medication } from '../../../core/models/medication.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { LucideAngularModule, Pill, Edit, Trash2 } from 'lucide-angular';

@Component({
  selector: 'app-medications-section',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PaginationComponent, LucideAngularModule],
  templateUrl: './medications-section.component.html',
})
export class MedicationsSectionComponent implements OnInit {
  readonly Pill = Pill;
  readonly Edit = Edit;
  readonly Trash2 = Trash2;

  @Input() patientId!: number;

  medications: Medication[] = [];
  showForm = false;
  selected?: Medication;
  form!: FormGroup;
  isSaving = false;
  loading = true;

  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  constructor(
    private fb: FormBuilder,
    private service: MedicationService,
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
        this.medications = page.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.show('Error al cargar medicamentos', 'error');
      }
    });
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.load();
  }

  openForm(medication?: Medication): void {
    this.selected = medication;
    this.form = this.fb.group({
      name: [medication?.name || '', Validators.required],
      dose: [medication?.dose || ''],
      frequency: [medication?.frequency || ''],
      startDate: [medication?.startDate || ''],
      endDate: [medication?.endDate || ''],
      active: [medication ? medication.active : true],
      notes: [medication?.notes || '']
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
    const data: Medication = { ...this.form.value, patientId: this.patientId };

    const request = this.selected?.id
      ? this.service.update(this.selected.id, data)
      : this.service.create(this.patientId, data);

    request.subscribe({
      next: () => {
        this.toast.show(this.selected?.id ? 'Medicamento actualizado' : 'Medicamento creado', 'success');
        this.isSaving = false;
        this.closeForm();
        this.load();
      },
      error: () => {
        this.isSaving = false;
        this.toast.show('Error al guardar el medicamento', 'error');
      }
    });
  }

  async delete(id: number): Promise<void> {
    const confirmed = await this.notification.confirm(
      'Eliminar Medicamento',
      '¿Está seguro de eliminar este medicamento?',
      'Sí, eliminar',
      'Cancelar'
    );
    if (confirmed) {
      this.service.delete(id).subscribe({
        next: () => {
          this.toast.show('Medicamento eliminado', 'success');
          this.load();
        },
        error: () => this.toast.show('Error al eliminar el medicamento', 'error')
      });
    }
  }
}
