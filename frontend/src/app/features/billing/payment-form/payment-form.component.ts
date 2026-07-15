import { Component, EventEmitter, Output, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { PaymentService } from '../../../core/services/payment.service';
import { PatientService } from '../../../core/services/patient/patient.service';
import { Patient } from '../../../core/models/patient.model';
import { Payment } from '../../../core/models/payment.model';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { PatientAutocompleteComponent } from '../../../shared/components/patient-autocomplete/patient-autocomplete.component';

@Component({
  selector: 'app-payment-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PatientAutocompleteComponent],
  templateUrl: './payment-form.component.html',
  styleUrls: ['./payment-form.component.css']
})
export class PaymentFormComponent implements OnInit {
  @Input() payment: Payment | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  paymentForm!: FormGroup;
  isSubmitting = false;

  constructor(
    private fb: FormBuilder,
    private paymentService: PaymentService,
    private patientService: PatientService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    const now = new Date();
    const tzOffset = now.getTimezoneOffset() * 60000;
    const localISO = new Date(now.getTime() - tzOffset).toISOString().substring(0, 16);

    this.paymentForm = this.fb.group({
      patientId: [{ value: this.payment?.patientId || '', disabled: !!this.payment }, Validators.required],
      amount: [this.payment?.amount || '', [Validators.required, Validators.min(0.01), Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
      paymentDate: [this.payment ? this.payment.paymentDate.substring(0, 16) : localISO, Validators.required],
      paymentMethod: [this.payment?.paymentMethod || 'CASH', Validators.required],
      description: [this.payment?.description || '', [Validators.maxLength(255)]]
    });
  }

  onSubmit(): void {
    if (this.paymentForm.invalid) {
      this.toastService.show('Por favor, complete todos los campos obligatorios correctamente.', 'error');
      return;
    }

    this.isSubmitting = true;
    const formValue = this.paymentForm.getRawValue();
    const newPayment = {
      ...formValue,
      patientId: Number(formValue.patientId),
      amount: Number(formValue.amount)
    };
    
    const request$ = this.payment ?
      this.paymentService.update(this.payment.id!, newPayment) :
      this.paymentService.create(newPayment);

    request$.subscribe({
      next: () => {
        this.isSubmitting = false;
        this.saved.emit();
      },
      error: (err) => {
        console.error('Error saving payment', err);
        this.isSubmitting = false;
        this.toastService.show('Ocurrió un error al guardar el cobro.', 'error');
      }
    });
  }

  onCancel(): void {
    this.cancelled.emit();
  }
}
