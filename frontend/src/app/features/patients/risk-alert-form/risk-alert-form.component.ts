import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';

import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RiskAlertService } from '../../../core/services/risk-alert.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { CatalogItem } from '../../../core/models/catalog.model';
import { forkJoin } from 'rxjs';
import { FocusTrapDirective } from '../../../shared/directives/focus-trap.directive';

@Component({
  selector: 'app-risk-alert-form',
  standalone: true,
  imports: [ReactiveFormsModule, FocusTrapDirective],
  templateUrl: './risk-alert-form.component.html',
})
export class RiskAlertFormComponent implements OnInit {
  @Input() patientId!: number;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  alertForm: FormGroup;
  isSaving = false;
  
  alertTypes: CatalogItem[] = [];
  alertLevels: CatalogItem[] = [];

  constructor(
    private fb: FormBuilder,
    private riskAlertService: RiskAlertService,
    private catalogService: CatalogService,
    private specialtyService: SpecialtyService,
    private toastService: ToastService
  ) {
    this.alertForm = this.fb.group({
      type: ['', Validators.required],
      level: ['', Validators.required],
      description: ['', [Validators.required, Validators.maxLength(1000)]]
    });
  }

  ngOnInit(): void {
    this.loadCatalogs();
  }

  loadCatalogs(): void {
    const catalogCode = this.specialtyService.isPsychology() ? 'RISK_ALERT_TYPE' : 'RISK_ALERT_TYPE_DERM';
    forkJoin({
      types: this.catalogService.getActiveItemsByCatalogCode(catalogCode),
      levels: this.catalogService.getActiveItemsByCatalogCode('RISK_ALERT_LEVEL')
    }).subscribe({
      next: (results) => {
        this.alertTypes = results.types;
        this.alertLevels = results.levels;
      },
      error: () => this.toastService.show('Error al cargar catálogos', 'error')
    });
  }

  onSubmit(): void {
    if (this.alertForm.valid) {
      this.isSaving = true;
      const alertData = {
        ...this.alertForm.value,
        patientId: this.patientId,
        active: true
      };

      this.riskAlertService.createAlert(this.patientId, alertData).subscribe({
        next: () => {
          this.toastService.show('Alerta guardada exitosamente', 'success');
          this.saved.emit();
        },
        error: () => {
          this.toastService.show('Error al guardar la alerta', 'error');
          this.isSaving = false;
        }
      });
    }
  }

  cancel(): void {
    this.cancelled.emit();
  }
}
