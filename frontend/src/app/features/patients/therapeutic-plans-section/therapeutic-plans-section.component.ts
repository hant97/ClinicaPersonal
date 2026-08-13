import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TherapeuticPlanService } from '../../../core/services/therapeutic-plan.service';
import { TherapeuticPlan } from '../../../core/models/therapeutic-plan.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { LucideAngularModule, ClipboardList } from 'lucide-angular';

@Component({
  selector: 'app-therapeutic-plans-section',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PaginationComponent, LucideAngularModule],
  templateUrl: './therapeutic-plans-section.component.html',
})
export class TherapeuticPlansSectionComponent implements OnInit {
  readonly ClipboardList = ClipboardList;

  @Input() patientId!: number;

  plans: TherapeuticPlan[] = [];
  showForm = false;
  selected?: TherapeuticPlan;
  form!: FormGroup;
  isSaving = false;
  loading = true;

  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  constructor(
    private fb: FormBuilder,
    private service: TherapeuticPlanService,
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
        this.plans = page.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.show('Error al cargar planes terapéuticos', 'error');
      }
    });
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.load();
  }

  openForm(plan?: TherapeuticPlan): void {
    this.selected = plan;
    this.form = this.fb.group({
      objectives: [plan?.objectives || ''],
      interventions: [plan?.interventions || ''],
      startDate: [plan?.startDate || ''],
      endDate: [plan?.endDate || ''],
      status: [plan?.status || 'ACTIVO'],
      notes: [plan?.notes || '']
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
    const data: TherapeuticPlan = { ...this.form.value, patientId: this.patientId };

    const request = this.selected?.id
      ? this.service.update(this.selected.id, data)
      : this.service.create(this.patientId, data);

    request.subscribe({
      next: () => {
        this.toast.show(this.selected?.id ? 'Plan actualizado' : 'Plan creado', 'success');
        this.isSaving = false;
        this.closeForm();
        this.load();
      },
      error: () => {
        this.isSaving = false;
        this.toast.show('Error al guardar el plan', 'error');
      }
    });
  }

  async delete(id: number): Promise<void> {
    const confirmed = await this.notification.confirm(
      'Eliminar Plan Terapéutico',
      '¿Está seguro de eliminar este plan terapéutico?',
      'Sí, eliminar',
      'Cancelar'
    );
    if (confirmed) {
      this.service.delete(id).subscribe({
        next: () => {
          this.toast.show('Plan eliminado', 'success');
          this.load();
        },
        error: () => this.toast.show('Error al eliminar el plan', 'error')
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
