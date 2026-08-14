import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AllergyService } from '../../../core/services/allergy.service';
import { Allergy } from '../../../core/models/allergy.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { LucideAngularModule, AlertTriangle, Edit, Trash2 } from 'lucide-angular';

@Component({
  selector: 'app-allergies-section',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PaginationComponent, LucideAngularModule],
  templateUrl: './allergies-section.component.html',
})
export class AllergiesSectionComponent implements OnInit {
  readonly AlertTriangle = AlertTriangle;
  readonly Edit = Edit;
  readonly Trash2 = Trash2;

  @Input() patientId!: number;

  allergies: Allergy[] = [];
  showForm = false;
  selected?: Allergy;
  form!: FormGroup;
  isSaving = false;
  loading = true;

  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  constructor(
    private fb: FormBuilder,
    private service: AllergyService,
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
        this.allergies = page.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.show('Error al cargar alergias', 'error');
      }
    });
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.load();
  }

  openForm(allergy?: Allergy): void {
    this.selected = allergy;
    this.form = this.fb.group({
      allergen: [allergy?.allergen || '', Validators.required],
      type: [allergy?.type || ''],
      severity: [allergy?.severity || ''],
      reaction: [allergy?.reaction || ''],
      active: [allergy ? allergy.active : true],
      notes: [allergy?.notes || '']
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
    const data: Allergy = { ...this.form.value, patientId: this.patientId };

    const request = this.selected?.id
      ? this.service.update(this.selected.id, data)
      : this.service.create(this.patientId, data);

    request.subscribe({
      next: () => {
        this.toast.show(this.selected?.id ? 'Alergia actualizada' : 'Alergia creada', 'success');
        this.isSaving = false;
        this.closeForm();
        this.load();
      },
      error: () => {
        this.isSaving = false;
        this.toast.show('Error al guardar la alergia', 'error');
      }
    });
  }

  async delete(id: number): Promise<void> {
    const confirmed = await this.notification.confirm(
      'Eliminar Alergia',
      '¿Está seguro de eliminar esta alergia?',
      'Sí, eliminar',
      'Cancelar'
    );
    if (confirmed) {
      this.service.delete(id).subscribe({
        next: () => {
          this.toast.show('Alergia eliminada', 'success');
          this.load();
        },
        error: () => this.toast.show('Error al eliminar la alergia', 'error')
      });
    }
  }
}
