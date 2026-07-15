import { Component, EventEmitter, Input, Output, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MedicalRecordService } from '../../../core/services/medical-record.service';

@Component({
  selector: 'app-medical-record-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './medical-record-form.component.html',
  styleUrls: ['./medical-record-form.component.css']
})
export class MedicalRecordFormComponent implements OnInit {
  @Input() patientId!: number;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  recordForm!: FormGroup;
  isSubmitting = false;

  constructor(
    private fb: FormBuilder,
    private recordService: MedicalRecordService
  ) {}

  ngOnInit(): void {
    this.recordForm = this.fb.group({
      sessionDate: [new Date().toISOString().substring(0, 16), Validators.required],
      reasonForConsultation: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(500)]],
      evolutionNotes: ['', [Validators.required, Validators.minLength(10)]],
      presumptiveDiagnosis: ['', [Validators.maxLength(200)]],
      agreementsAndTasks: ['', [Validators.maxLength(500)]]
    });
  }

  onSubmit(): void {
    if (this.recordForm.invalid) {
      return;
    }

    this.isSubmitting = true;
    const newRecord = {
      ...this.recordForm.value,
      patientId: this.patientId
    };

    this.recordService.createRecord(newRecord).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.saved.emit();
      },
      error: (err) => {
        console.error('Error saving record', err);
        this.isSubmitting = false;
        alert('Ocurrió un error al guardar la sesión.');
      }
    });
  }

  onCancel(): void {
    this.cancelled.emit();
  }
}
