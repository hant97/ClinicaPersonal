import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MedicalRecordService } from '../../../core/services/medical-record.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { MedicalRecord } from '../../../core/models/medical-record.model';

@Component({
  selector: 'app-medical-record-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './medical-record-form.component.html',
  styleUrls: ['./medical-record-form.component.css']
})
export class MedicalRecordFormComponent implements OnInit {
  @Input() patientId!: number;
  @Input() record?: MedicalRecord;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  recordForm!: FormGroup;
  isSaving = false;

  constructor(
    private fb: FormBuilder,
    private medicalRecordService: MedicalRecordService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.recordForm = this.fb.group({
      diagnosis: [this.record?.diagnosis || ''],
      currentMedication: [this.record?.currentMedication || ''],
      treatmentPlan: [this.record?.treatmentPlan || ''],
      treatmentStatus: [this.record?.treatmentStatus || 'Activo', [Validators.required]]
    });
  }

  onSubmit(): void {
    if (this.recordForm.valid) {
      this.isSaving = true;
      const data: MedicalRecord = {
        ...this.recordForm.value,
        patientId: this.patientId
      };

      const request = this.record?.id 
        ? this.medicalRecordService.updateRecord(this.record.id, data)
        : this.medicalRecordService.createRecord(data);

      request.subscribe({
        next: () => {
          this.toastService.show(this.record?.id ? 'Registro actualizado' : 'Registro creado', 'success');
          this.saved.emit();
        },
        error: () => {
          this.toastService.show('Error al guardar el registro', 'error');
          this.isSaving = false;
        }
      });
    }
  }

  cancel(): void {
    this.cancelled.emit();
  }
}
