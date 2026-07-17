import { Component, EventEmitter, Output, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { PaymentService } from '../../../core/services/payment.service';
import { PatientService } from '../../../core/services/patient/patient.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { Patient } from '../../../core/models/patient.model';
import { Payment } from '../../../core/models/payment.model';
import { CatalogItem } from '../../../core/models/catalog.model';
import { Supply } from '../../../core/services/inventory.service';
import { InventoryService } from '../../../core/services/inventory.service';
import { ClinicalService } from '../../../core/models/clinical-service.model';
import { ClinicalServiceService } from '../../../core/services/clinical-service.service';
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
  paymentMethods: CatalogItem[] = [];
  suppliesList: Supply[] = [];
  clinicalServices: ClinicalService[] = [];

  constructor(
    private fb: FormBuilder,
    private paymentService: PaymentService,
    private patientService: PatientService,
    private catalogService: CatalogService,
    private inventoryService: InventoryService,
    private clinicalServiceService: ClinicalServiceService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.loadCatalogs();

    const now = new Date();
    const tzOffset = now.getTimezoneOffset() * 60000;
    const localISO = new Date(now.getTime() - tzOffset).toISOString().substring(0, 16);

    this.paymentForm = this.fb.group({
      patientId: [{ value: this.payment?.patientId || '', disabled: !!this.payment }, Validators.required],
      amount: [this.payment?.amount || 0, [Validators.required, Validators.min(0.01)]],
      paymentDate: [this.payment ? this.payment.paymentDate.substring(0, 16) : localISO, Validators.required],
      paymentMethod: [this.payment?.paymentMethod || 'CASH', Validators.required],
      description: [this.payment?.description || '', [Validators.maxLength(255)]],
      services: this.fb.array([]),
      supplies: this.fb.array([])
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
    this.services.valueChanges.subscribe(() => this.calculateTotal());
    this.supplies.valueChanges.subscribe(() => this.calculateTotal());
  }

  get services(): import('@angular/forms').FormArray {
    return this.paymentForm.get('services') as import('@angular/forms').FormArray;
  }

  get supplies(): import('@angular/forms').FormArray {
    return this.paymentForm.get('supplies') as import('@angular/forms').FormArray;
  }

  addService(itemData?: any): void {
    const itemGroup = this.fb.group({
      description: [itemData?.description || '', Validators.required],
      quantity: [itemData?.quantity || 1, [Validators.required, Validators.min(1)]],
      unitPrice: [itemData?.unitPrice || 0, [Validators.required, Validators.min(0)]],
      totalPrice: [{ value: itemData?.totalPrice || 0, disabled: true }],
      clinicalServiceId: [itemData?.clinicalServiceId || null, Validators.required]
    });

    itemGroup.get('quantity')?.valueChanges.subscribe(() => this.updateItemTotal(itemGroup));
    itemGroup.get('unitPrice')?.valueChanges.subscribe(() => this.updateItemTotal(itemGroup));

    this.services.push(itemGroup);
  }

  addSupply(itemData?: any): void {
    const itemGroup = this.fb.group({
      description: [itemData?.description || '', Validators.required],
      quantity: [itemData?.quantity || 1, [Validators.required, Validators.min(1)]],
      unitPrice: [itemData?.unitPrice || 0, [Validators.required, Validators.min(0)]],
      totalPrice: [{ value: itemData?.totalPrice || 0, disabled: true }],
      supplyId: [itemData?.supplyId || null, Validators.required]
    });

    itemGroup.get('quantity')?.valueChanges.subscribe(() => this.updateItemTotal(itemGroup));
    itemGroup.get('unitPrice')?.valueChanges.subscribe(() => this.updateItemTotal(itemGroup));

    this.supplies.push(itemGroup);
  }

  removeService(index: number): void {
    this.services.removeAt(index);
  }

  removeSupply(index: number): void {
    this.supplies.removeAt(index);
  }

  updateItemTotal(group: FormGroup): void {
    const qty = group.get('quantity')?.value || 0;
    const price = group.get('unitPrice')?.value || 0;
    group.get('totalPrice')?.setValue(qty * price, { emitEvent: false });
    this.calculateTotal();
  }

  calculateTotal(): void {
    let total = 0;
    this.services.controls.forEach(control => {
      const qty = control.get('quantity')?.value || 0;
      const price = control.get('unitPrice')?.value || 0;
      total += qty * price;
    });
    this.supplies.controls.forEach(control => {
      const qty = control.get('quantity')?.value || 0;
      const price = control.get('unitPrice')?.value || 0;
      total += qty * price;
    });
    this.paymentForm.get('amount')?.setValue(total, { emitEvent: false });
  }

  onClinicalServiceSelect(index: number, event: any): void {
    const serviceId = event.target.value;
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

  onSupplySelect(index: number, event: any): void {
    const supplyId = event.target.value;
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
      },
      error: () => {
        this.toastService.show('Error al cargar servicios clínicos', 'error');
      }
    });
  }

  onSubmit(): void {
    if (this.paymentForm.invalid) {
      this.toastService.show('Por favor, complete todos los campos obligatorios correctamente.', 'error');
      return;
    }

    this.isSubmitting = true;
    const formValue = this.paymentForm.getRawValue();
    
    // Validate we have at least one service
    if (formValue.services.length === 0) {
      this.toastService.show('Debe agregar al menos un servicio a la factura.', 'error');
      this.isSubmitting = false;
      return;
    }

    // Map items from form arrays
    const mappedServices = formValue.services.map((item: any) => ({
      description: item.description,
      quantity: Number(item.quantity),
      unitPrice: Number(item.unitPrice),
      totalPrice: Number(item.quantity) * Number(item.unitPrice),
      clinicalServiceId: Number(item.clinicalServiceId)
    }));

    const mappedSupplies = formValue.supplies.map((item: any) => ({
      description: item.description,
      quantity: Number(item.quantity),
      unitPrice: Number(item.unitPrice),
      totalPrice: Number(item.quantity) * Number(item.unitPrice),
      supplyId: Number(item.supplyId)
    }));

    const mappedItems = [...mappedServices, ...mappedSupplies];

    const newPayment = {
      ...formValue,
      patientId: Number(formValue.patientId),
      amount: Number(formValue.amount),
      items: mappedItems
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
