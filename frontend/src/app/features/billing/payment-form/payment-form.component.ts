import { Component, EventEmitter, Output, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { PaymentService } from '../../../core/services/payment.service';
import { PatientService } from '../../../core/services/patient/patient.service';
import { Patient } from '../../../core/models/patient.model';

@Component({
  selector: 'app-payment-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './payment-form.component.html',
  styleUrls: ['./payment-form.component.css']
})
export class PaymentFormComponent implements OnInit {
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  paymentForm!: FormGroup;
  isSubmitting = false;
  patients: Patient[] = [];

  constructor(
    private fb: FormBuilder,
    private paymentService: PaymentService,
    private patientService: PatientService
  ) {}

  ngOnInit(): void {
    this.paymentForm = this.fb.group({
      patientId: ['', Validators.required],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      paymentDate: [new Date().toISOString().substring(0, 16), Validators.required],
      paymentMethod: ['CASH', Validators.required],
      description: ['']
    });

    this.patientService.getAll().subscribe({
      next: (data) => this.patients = data,
      error: (err) => console.error('Error fetching patients', err)
    });
  }

  onSubmit(): void {
    if (this.paymentForm.invalid) {
      alert('Por favor, complete todos los campos obligatorios correctamente.');
      return;
    }

    this.isSubmitting = true;
    const formValue = this.paymentForm.value;
    const newPayment = {
      ...formValue,
      patientId: Number(formValue.patientId),
      amount: Number(formValue.amount)
    };
    
    this.paymentService.create(newPayment).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.saved.emit();
      },
      error: (err) => {
        console.error('Error saving payment', err);
        this.isSubmitting = false;
        alert('Ocurrió un error al guardar el cobro.');
      }
    });
  }

  onCancel(): void {
    this.cancelled.emit();
  }
}
