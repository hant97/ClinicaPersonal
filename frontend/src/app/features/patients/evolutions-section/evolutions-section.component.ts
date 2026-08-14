import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { EvolutionService } from '../../../core/services/evolution.service';
import { Evolution } from '../../../core/models/evolution.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { LucideAngularModule, Calendar, Edit, Trash2 } from 'lucide-angular';

@Component({
  selector: 'app-evolutions-section',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PaginationComponent, LucideAngularModule],
  templateUrl: './evolutions-section.component.html',
})
export class EvolutionsSectionComponent implements OnInit {
  readonly Calendar = Calendar;
  readonly Edit = Edit;
  readonly Trash2 = Trash2;

  @Input() patientId!: number;

  evolutions: Evolution[] = [];
  showForm = false;
  selected?: Evolution;
  form!: FormGroup;
  isSaving = false;
  loading = true;

  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  constructor(
    private fb: FormBuilder,
    private service: EvolutionService,
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
        this.evolutions = page.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.show('Error al cargar controles', 'error');
      }
    });
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.load();
  }

  openForm(evolution?: Evolution): void {
    this.selected = evolution;
    this.form = this.fb.group({
      controlDate: [evolution?.controlDate || this.today(), Validators.required],
      clinicalNotes: [evolution?.clinicalNotes || ''],
      nextControlDate: [evolution?.nextControlDate || '']
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
    const data: Evolution = { ...this.form.value, patientId: this.patientId };

    const request = this.selected?.id
      ? this.service.update(this.selected.id, data)
      : this.service.create(this.patientId, data);

    request.subscribe({
      next: () => {
        this.toast.show(this.selected?.id ? 'Control actualizado' : 'Control creado', 'success');
        this.isSaving = false;
        this.closeForm();
        this.load();
      },
      error: () => {
        this.isSaving = false;
        this.toast.show('Error al guardar el control', 'error');
      }
    });
  }

  async delete(id: number): Promise<void> {
    const confirmed = await this.notification.confirm(
      'Eliminar Control',
      '¿Está seguro de eliminar este control?',
      'Sí, eliminar',
      'Cancelar'
    );
    if (confirmed) {
      this.service.delete(id).subscribe({
        next: () => {
          this.toast.show('Control eliminado', 'success');
          this.load();
        },
        error: () => this.toast.show('Error al eliminar el control', 'error')
      });
    }
  }

  private today(): string {
    const now = new Date();
    const tzOffset = now.getTimezoneOffset() * 60000;
    return new Date(now.getTime() - tzOffset).toISOString().split('T')[0];
  }
}
