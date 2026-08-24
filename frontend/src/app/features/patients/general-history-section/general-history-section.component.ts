import { Component, Input, OnInit } from '@angular/core';

import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { GeneralHistoryService } from '../../../core/services/general-history.service';
import { GeneralHistory } from '../../../core/models/general-history.model';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { LucideAngularModule, FileText } from 'lucide-angular';

@Component({
  selector: 'app-general-history-section',
  standalone: true,
  imports: [ReactiveFormsModule, LucideAngularModule],
  templateUrl: './general-history-section.component.html',
})
export class GeneralHistorySectionComponent implements OnInit {
  readonly FileText = FileText;

  @Input() patientId!: number;

  history: GeneralHistory | null = null;
  form!: FormGroup;
  editing = false;
  loading = true;
  isSaving = false;

  constructor(
    private fb: FormBuilder,
    private service: GeneralHistoryService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.service.get(this.patientId).subscribe({
      next: (data) => {
        this.history = data.id ? data : null;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.show('Error al cargar antecedentes generales', 'error');
      }
    });
  }

  startEdit(): void {
    this.form = this.fb.group({
      pathologicalHistory: [this.history?.pathologicalHistory || ''],
      surgicalHistory: [this.history?.surgicalHistory || ''],
      familyHistory: [this.history?.familyHistory || ''],
      habits: [this.history?.habits || ''],
      notes: [this.history?.notes || '']
    });
    this.editing = true;
  }

  cancel(): void {
    this.editing = false;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isSaving = true;
    const data: GeneralHistory = { ...this.form.value, patientId: this.patientId };
    this.service.upsert(this.patientId, data).subscribe({
      next: (saved) => {
        this.history = saved;
        this.editing = false;
        this.isSaving = false;
        this.toast.show('Antecedentes generales guardados', 'success');
      },
      error: () => {
        this.isSaving = false;
        this.toast.show('Error al guardar antecedentes', 'error');
      }
    });
  }

  hasContent(): boolean {
    return !!(
      this.history?.pathologicalHistory ||
      this.history?.surgicalHistory ||
      this.history?.familyHistory ||
      this.history?.habits ||
      this.history?.notes
    );
  }
}
