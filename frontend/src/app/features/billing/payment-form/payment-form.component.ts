import { Component, DestroyRef, EventEmitter, Output, OnInit, OnChanges, Input, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormArray } from '@angular/forms';
import { PaymentService } from '../../../core/services/payment.service';
import { PatientService } from '../../../core/services/patient/patient.service';
import { AppointmentService } from '../../../core/services/appointment.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { Payment, PaymentItem, PaymentTransaction } from '../../../core/models/payment.model';
import { Appointment } from '../../../core/models/appointment.model';
import { CatalogItem } from '../../../core/models/catalog.model';
import { Supply, InventoryService } from '../../../core/services/inventory.service';
import { ClinicalService } from '../../../core/models/clinical-service.model';
import { ClinicalServiceService } from '../../../core/services/clinical-service.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { PatientAutocompleteComponent } from '../../../shared/components/patient-autocomplete/patient-autocomplete.component';
import { FocusTrapDirective } from '../../../shared/directives/focus-trap.directive';
import {
  LucideAngularModule,
  Receipt,
  Plus,
  Trash2,
  X,
  CheckCircle2,
  User,
  Clock,
  Package,
  Stethoscope,
  Sparkles,
  CreditCard,
  Banknote,
  QrCode,
  Building,
  DollarSign,
  AlertCircle,
  CalendarCheck
} from 'lucide-angular';

interface PaymentLineFormValue {
  description: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  clinicalServiceId?: number | null;
  supplyId?: number | null;
}

interface PaymentFormValue {
  patientId: number | string;
  amount: number;
  paymentDate: string;
  paymentMethod: string;
  description?: string;
  dueDate?: string;
  paymentMode?: 'FULL' | 'PARTIAL' | 'PENDING';
  initialPayment?: number;
  services: PaymentLineFormValue[];
  supplies: PaymentLineFormValue[];
}

@Component({
  selector: 'app-payment-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PatientAutocompleteComponent, FocusTrapDirective, LucideAngularModule],
  templateUrl: './payment-form.component.html'
})
export class PaymentFormComponent implements OnInit, OnChanges {
  private readonly destroyRef = inject(DestroyRef);
  // Lucide Icons
  readonly Receipt = Receipt;
  readonly Plus = Plus;
  readonly Trash2 = Trash2;
  readonly X = X;
  readonly CheckCircle2 = CheckCircle2;
  readonly User = User;
  readonly Clock = Clock;
  readonly Package = Package;
  readonly Stethoscope = Stethoscope;
  readonly Sparkles = Sparkles;
  readonly CreditCard = CreditCard;
  readonly Banknote = Banknote;
  readonly QrCode = QrCode;
  readonly Building = Building;
  readonly DollarSign = DollarSign;
  readonly AlertCircle = AlertCircle;
  readonly CalendarCheck = CalendarCheck;

  @Input() payment: Payment | null = null;
  @Input() initialData: { patientId?: number; appointmentId?: number; attentionId?: number; clinicalServiceId?: number; description?: string } | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  paymentForm!: FormGroup;
  isSubmitting = false;
  paymentMethods: CatalogItem[] = [];
  suppliesList: Supply[] = [];
  clinicalServices: ClinicalService[] = [];
  patientAppointments: Appointment[] = [];
  selectedAppointmentId: number | null = null;
  isLoadingAppointments = false;

  constructor(
    private fb: FormBuilder,
    private paymentService: PaymentService,
    private patientService: PatientService,
    private appointmentService: AppointmentService,
    private catalogService: CatalogService,
    private inventoryService: InventoryService,
    private clinicalServiceService: ClinicalServiceService,
    private toastService: ToastService
  ) {}

  ngOnChanges(): void {
    if (this.initialData?.patientId && this.paymentForm) {
      this.paymentForm.get('patientId')?.setValue(this.initialData.patientId);
      if (this.initialData.appointmentId || this.initialData.attentionId) {
        this.paymentForm.get('patientId')?.disable({ emitEvent: false });
        if (this.initialData.appointmentId) {
          this.selectedAppointmentId = this.initialData.appointmentId;
        }
      }
      if (!this.initialData.appointmentId && !this.initialData.attentionId) {
        this.loadPatientAppointments(this.initialData.patientId);
      }
    }
    this.prefillInitialClinicalService();
  }

  ngOnInit(): void {
    this.selectedAppointmentId = this.payment?.appointmentId ?? this.initialData?.appointmentId ?? null;
    this.loadCatalogs();

    const now = new Date();
    const tzOffset = now.getTimezoneOffset() * 60000;
    const localISO = new Date(now.getTime() - tzOffset).toISOString().substring(0, 16);

    const initialPatientId = this.payment?.patientId || this.initialData?.patientId || '';

    this.paymentForm = this.fb.group({
      patientId: [{ value: initialPatientId, disabled: !!this.payment || !!this.initialData?.appointmentId || !!this.initialData?.attentionId }, Validators.required],
      amount: [this.payment?.amount || 0, [Validators.required, Validators.min(0.01)]],
      paymentDate: [this.payment ? this.payment.paymentDate.substring(0, 16) : localISO, Validators.required],
      paymentMethod: [this.payment?.paymentMethod || '', Validators.required],
      description: [this.payment?.description || this.initialData?.description || '', [Validators.maxLength(255)]],
      dueDate: [this.payment?.dueDate || ''],
      paymentMode: ['FULL'],
      initialPayment: [0],
      services: this.fb.array([]),
      supplies: this.fb.array([])
    });

    if (initialPatientId && !this.initialData?.appointmentId && !this.initialData?.attentionId) {
      this.loadPatientAppointments(initialPatientId);
    }

    this.paymentForm.get('patientId')?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(val => {
      if (!this.initialData?.appointmentId) {
        this.selectedAppointmentId = null;
      }
      if (!this.initialData?.appointmentId && !this.initialData?.attentionId) {
        this.loadPatientAppointments(val);
      }
    });

    if (this.payment?.items && this.payment.items.length > 0) {
      const paymentServices = this.payment.items.filter(i => i.clinicalServiceId != null);
      const paymentSupplies = this.payment.items.filter(i => i.clinicalServiceId == null);
      
      paymentServices.forEach(item => this.addService(item));
      paymentSupplies.forEach(item => this.addSupply(item));
      
      if (paymentServices.length === 0) {
        this.addService();
      }
    } else {
      this.addService(); // Add one default empty service
    }

    // Auto-calculate total amount based on items
    this.services.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.calculateTotal());
    this.supplies.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.calculateTotal());
  }

  get services(): FormArray {
    return this.paymentForm.get('services') as FormArray;
  }

  get supplies(): FormArray {
    return this.paymentForm.get('supplies') as FormArray;
  }

  get servicesTotal(): number {
    let total = 0;
    this.services.controls.forEach(control => {
      const qty = Number(control.get('quantity')?.value || 0);
      const price = Number(control.get('unitPrice')?.value || 0);
      total += qty * price;
    });
    return total;
  }

  get suppliesTotal(): number {
    let total = 0;
    this.supplies.controls.forEach(control => {
      const qty = Number(control.get('quantity')?.value || 0);
      const price = Number(control.get('unitPrice')?.value || 0);
      total += qty * price;
    });
    return total;
  }

  get grandTotal(): number {
    return this.servicesTotal + this.suppliesTotal;
  }

  setPaymentMethod(code: string): void {
    this.paymentForm.get('paymentMethod')?.setValue(code);
    this.paymentForm.get('paymentMethod')?.markAsDirty();
  }

  get paymentMode(): string {
    return this.paymentForm.get('paymentMode')?.value || 'FULL';
  }

  addService(itemData?: PaymentItem): void {
    const itemGroup = this.fb.group({
      description: [itemData?.description || '', Validators.required],
      quantity: [itemData?.quantity || 1, [Validators.required, Validators.min(1)]],
      unitPrice: [itemData?.unitPrice || 0, [Validators.required, Validators.min(0)]],
      totalPrice: [{ value: itemData?.totalPrice || 0, disabled: true }],
      clinicalServiceId: [itemData?.clinicalServiceId || null, Validators.required]
    });

    itemGroup.get('quantity')?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.updateItemTotal(itemGroup));
    itemGroup.get('unitPrice')?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.updateItemTotal(itemGroup));

    this.services.push(itemGroup);
  }

  addSupply(itemData?: PaymentItem): void {
    const itemGroup = this.fb.group({
      description: [itemData?.description || '', Validators.required],
      quantity: [itemData?.quantity || 1, [Validators.required, Validators.min(1)]],
      unitPrice: [itemData?.unitPrice || 0, [Validators.required, Validators.min(0)]],
      totalPrice: [{ value: itemData?.totalPrice || 0, disabled: true }],
      supplyId: [itemData?.supplyId || null, Validators.required]
    });

    itemGroup.get('quantity')?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.updateItemTotal(itemGroup));
    itemGroup.get('unitPrice')?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.updateItemTotal(itemGroup));

    this.supplies.push(itemGroup);
  }

  removeService(index: number): void {
    this.services.removeAt(index);
  }

  removeSupply(index: number): void {
    this.supplies.removeAt(index);
  }

  updateItemTotal(group: FormGroup): void {
    const qty = Number(group.get('quantity')?.value || 0);
    const price = Number(group.get('unitPrice')?.value || 0);
    group.get('totalPrice')?.setValue(qty * price, { emitEvent: false });
    this.calculateTotal();
  }

  calculateTotal(): void {
    const total = this.grandTotal;
    this.paymentForm.get('amount')?.setValue(total, { emitEvent: false });
  }

  onClinicalServiceSelect(index: number, event: Event): void {
    const serviceId = (event.target as HTMLSelectElement).value;
    if (serviceId) {
      const selectedService = this.clinicalServices.find(s => s.id === Number(serviceId));
      if (selectedService) {
        const itemGroup = this.services.at(index) as FormGroup;
        itemGroup.patchValue({
          description: selectedService.name,
          unitPrice: selectedService.price || 0
        });
      }
    }
  }

  onSupplySelect(index: number, event: Event): void {
    const supplyId = (event.target as HTMLSelectElement).value;
    if (supplyId) {
      const selectedSupply = this.suppliesList.find(s => s.id === Number(supplyId));
      if (selectedSupply) {
        const itemGroup = this.supplies.at(index) as FormGroup;
        itemGroup.patchValue({
          description: selectedSupply.name,
          unitPrice: selectedSupply.price || 0
        });
      }
    }
  }

  loadCatalogs(): void {
    this.catalogService.getActiveItemsByCatalogCode('PAYMENT_METHOD').subscribe({
      next: (items) => {
        this.paymentMethods = items;
      },
      error: () => {
        this.toastService.show('Error al cargar métodos de pago', 'error');
      }
    });

    this.inventoryService.getAllSupplies('', 0, 100).subscribe({
      next: (res) => {
        this.suppliesList = res.content;
      },
      error: () => {
        this.toastService.show('Error al cargar insumos', 'error');
      }
    });

    this.clinicalServiceService.getAllActiveServices().subscribe({
      next: (res) => {
        this.clinicalServices = res;
        this.prefillInitialClinicalService();
      },
      error: () => {
        this.toastService.show('Error al cargar servicios clínicos', 'error');
      }
    });
  }

  loadPatientAppointments(patientId: number | string): void {
    const pid = Number(patientId);
    if (!pid) {
      this.patientAppointments = [];
      return;
    }
    this.isLoadingAppointments = true;
    this.appointmentService.getByPatientId(pid, 0, 50).subscribe({
      next: (res) => {
        this.patientAppointments = res.content || [];
        this.isLoadingAppointments = false;
        // If an appointment was specified via initialData or payment, ensure it's selected
        if (this.initialData?.appointmentId) {
          this.selectedAppointmentId = this.initialData.appointmentId;
        } else if (this.payment?.appointmentId) {
          this.selectedAppointmentId = this.payment.appointmentId;
        }
      },
      error: () => {
        this.isLoadingAppointments = false;
      }
    });
  }

  onAppointmentSelect(event: Event): void {
    const target = event.target as HTMLSelectElement;
    const value = target.value ? Number(target.value) : null;
    this.selectedAppointmentId = value;
    if (value) {
      const app = this.patientAppointments.find(a => a.id === value);
      if (app && app.clinicalServiceId) {
        const firstGroup = this.services.at(0) as FormGroup;
        if (firstGroup && (!firstGroup.get('clinicalServiceId')?.value || this.services.length === 1)) {
          const srv = this.clinicalServices.find(s => s.id === Number(app.clinicalServiceId));
          if (srv) {
            firstGroup.patchValue({
              clinicalServiceId: srv.id,
              description: srv.name,
              unitPrice: srv.price || 0,
              quantity: 1
            });
            this.updateItemTotal(firstGroup);
          }
        }
      }
    }
  }

  private prefillInitialClinicalService(): void {
    if (!this.payment && this.initialData?.clinicalServiceId && this.clinicalServices.length > 0 && this.services.length > 0) {
      const selectedService = this.clinicalServices.find(s => s.id === Number(this.initialData?.clinicalServiceId));
      if (selectedService) {
        const firstGroup = this.services.at(0) as FormGroup;
        firstGroup.patchValue({
          clinicalServiceId: selectedService.id,
          description: selectedService.name,
          unitPrice: selectedService.price || 0,
          quantity: 1
        });
        this.updateItemTotal(firstGroup);
      }
    }
  }

  onSubmit(): void {
    if (this.paymentForm.invalid) {
      this.toastService.show('Por favor, complete todos los campos obligatorios correctamente.', 'error');
      return;
    }

    this.isSubmitting = true;
    const formValue = this.paymentForm.getRawValue() as PaymentFormValue;
    
    // Validate we have at least one service
    if (formValue.services.length === 0) {
      this.toastService.show('Debe agregar al menos un servicio a la factura.', 'error');
      this.isSubmitting = false;
      return;
    }

    // Map items from form arrays
    const mappedServices: PaymentItem[] = formValue.services.map((item) => ({
      description: item.description,
      quantity: Number(item.quantity),
      unitPrice: Number(item.unitPrice),
      totalPrice: Number(item.quantity) * Number(item.unitPrice),
      clinicalServiceId: Number(item.clinicalServiceId)
    }));

    const mappedSupplies: PaymentItem[] = formValue.supplies.map((item) => ({
      description: item.description,
      quantity: Number(item.quantity),
      unitPrice: Number(item.unitPrice),
      totalPrice: Number(item.quantity) * Number(item.unitPrice),
      supplyId: Number(item.supplyId)
    }));

    const mappedItems = [...mappedServices, ...mappedSupplies];
    const totalAmount = Number(formValue.amount);

    // Al crear, el abono inicial se traduce en una transacción (o ninguna = PENDIENTE)
    let transactions: PaymentTransaction[] | undefined;
    if (!this.payment) {
      const mode = formValue.paymentMode || 'FULL';
      if (mode === 'FULL') {
        transactions = [{ amount: totalAmount, transactionDate: formValue.paymentDate, paymentMethod: formValue.paymentMethod }];
      } else if (mode === 'PARTIAL') {
        const initial = Number(formValue.initialPayment || 0);
        if (initial <= 0) {
          this.toastService.show('Ingrese un monto de abono inicial mayor a 0.', 'error');
          this.isSubmitting = false;
          return;
        }
        if (initial >= totalAmount) {
          this.toastService.show('El abono inicial debe ser menor al total del cobro.', 'error');
          this.isSubmitting = false;
          return;
        }
        transactions = [{ amount: initial, transactionDate: formValue.paymentDate, paymentMethod: formValue.paymentMethod }];
      }
      // mode === 'PENDING' => sin transacciones
    }

    const newPayment: Payment = {
      patientId: Number(this.paymentForm.get('patientId')?.value || formValue.patientId),
      amount: totalAmount,
      paymentDate: formValue.paymentDate,
      paymentMethod: formValue.paymentMethod,
      description: formValue.description,
      dueDate: formValue.dueDate || undefined,
      appointmentId: this.selectedAppointmentId ?? (this.payment?.appointmentId ?? this.initialData?.appointmentId),
      attentionId: this.initialData?.attentionId ?? this.payment?.attentionId,
      items: mappedItems,
      transactions
    };
    
    const request$ = this.payment ?
      this.paymentService.update(this.payment.id!, newPayment) :
      this.paymentService.create(newPayment);

    request$.subscribe({
      next: () => {
        this.isSubmitting = false;
        this.toastService.show('Cobro registrado exitosamente', 'success');
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
