import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DermatologicalEvaluationService } from '../../../core/services/dermatological-evaluation.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { DermatologicalEvaluation } from '../../../core/models/dermatological-evaluation.model';
import { CatalogItem } from '../../../core/models/catalog.model';
import { FocusTrapDirective } from '../../../shared/directives/focus-trap.directive';
import { ToastService } from '../../../shared/services/toast/toast.service';

@Component({
  selector: 'app-dermatological-evaluation-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FocusTrapDirective],
  templateUrl: './dermatological-evaluation-form.component.html',
})
export class DermatologicalEvaluationFormComponent implements OnInit {
  @Input() patientId!: number;
  @Input() evaluation?: DermatologicalEvaluation;
  @Output() close = new EventEmitter<boolean>();

  form!: FormGroup;
  isSaving = false;

  skinTypes: CatalogItem[] = [];
  lesionTypes: CatalogItem[] = [];
  bodyAreas: CatalogItem[] = [];
  dermProcedures: CatalogItem[] = [];

  constructor(
    private fb: FormBuilder,
    private evaluationService: DermatologicalEvaluationService,
    private catalogService: CatalogService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadCatalogs();
  }

  initForm(): void {
    const today = new Date();
    const tzOffset = today.getTimezoneOffset() * 60000;
    const localISO = new Date(today.getTime() - tzOffset).toISOString().split('T')[0];

    let evalDate = localISO;
    if (this.evaluation?.evaluationDate) {
      evalDate = this.evaluation.evaluationDate.includes('T') 
        ? this.evaluation.evaluationDate.split('T')[0] 
        : this.evaluation.evaluationDate;
    }

    let nextReviewDate = '';
    if (this.evaluation?.nextReviewDate) {
      nextReviewDate = this.evaluation.nextReviewDate.includes('T') 
        ? this.evaluation.nextReviewDate.split('T')[0] 
        : this.evaluation.nextReviewDate;
    }

    this.form = this.fb.group({
      evaluationDate: [evalDate, Validators.required],
      skinType: [this.evaluation?.skinType || ''],
      affectedArea: [this.evaluation?.affectedArea || ''],
      lesionType: [this.evaluation?.lesionType || ''],
      lesionSize: [this.evaluation?.lesionSize || ''],
      dermatologicalDiagnosis: [this.evaluation?.dermatologicalDiagnosis || '', Validators.required],
      treatmentIndicated: [this.evaluation?.treatmentIndicated || ''],
      procedurePerformed: [this.evaluation?.procedurePerformed || ''],
      evolutionNotes: [this.evaluation?.evolutionNotes || ''],
      nextReviewDate: [nextReviewDate]
    });
  }

  loadCatalogs(): void {
    this.catalogService.getActiveItemsByCatalogCode('SKIN_TYPE').subscribe(items => this.skinTypes = items);
    this.catalogService.getActiveItemsByCatalogCode('LESION_TYPE').subscribe(items => this.lesionTypes = items);
    this.catalogService.getActiveItemsByCatalogCode('BODY_AREA').subscribe(items => this.bodyAreas = items);
    this.catalogService.getActiveItemsByCatalogCode('DERM_PROCEDURE').subscribe(items => this.dermProcedures = items);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSaving = true;
    const data: DermatologicalEvaluation = {
      ...this.form.value,
      patientId: this.patientId
    };

    if (this.evaluation && this.evaluation.id) {
      this.evaluationService.update(this.evaluation.id, data).subscribe({
        next: () => {
          this.toastService.show('Evaluación actualizada', 'success');
          this.isSaving = false;
          this.close.emit(true);
        },
        error: () => {
          this.toastService.show('Error al actualizar evaluación', 'error');
          this.isSaving = false;
        }
      });
    } else {
      this.evaluationService.create(this.patientId, data).subscribe({
        next: () => {
          this.toastService.show('Evaluación creada', 'success');
          this.isSaving = false;
          this.close.emit(true);
        },
        error: () => {
          this.toastService.show('Error al crear evaluación', 'error');
          this.isSaving = false;
        }
      });
    }
  }

  cancel(): void {
    this.close.emit(false);
  }
}
