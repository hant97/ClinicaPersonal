import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { InventoryService } from '../../../core/services/inventory.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { CatalogItem } from '../../../core/models/catalog.model';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { LucideAngularModule, X } from 'lucide-angular';

@Component({
  selector: 'app-inventory-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LucideAngularModule],
  templateUrl: './inventory-form.component.html',
  styleUrls: ['./inventory-form.component.css']
})
export class InventoryFormComponent implements OnInit {
  @Input() supplyId: number | null = null;
  @Output() closeModal = new EventEmitter<boolean>();

  readonly X = X;

  form!: FormGroup;
  isSubmitting = false;
  isEditMode = false;
  supplyUnits: CatalogItem[] = [];

  constructor(
    private fb: FormBuilder,
    private inventoryService: InventoryService,
    private catalogService: CatalogService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.isEditMode = !!this.supplyId;
    this.initForm();
    this.loadCatalogs();
    if (this.isEditMode) {
      this.loadSupply();
    }
  }

  initForm(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required]],
      description: [''],
      currentStock: [0, [Validators.required, Validators.min(0)]],
      minStockLevel: [0, [Validators.required, Validators.min(0)]],
      unit: ['', [Validators.required]],
      price: [0, [Validators.min(0)]],
      expirationDate: ['']
    });
  }

  loadCatalogs(): void {
    this.catalogService.getActiveItemsByCatalogCode('SUPPLY_UNIT').subscribe({
      next: (items) => {
        this.supplyUnits = items;
      },
      error: () => {
        this.toastService.show('Error al cargar unidades de medida', 'error');
      }
    });
  }

  loadSupply(): void {
    this.inventoryService.getSupplyById(this.supplyId!).subscribe({
      next: (supply) => {
        this.form.patchValue(supply);
      },
      error: () => {
        this.toastService.show('Error al cargar datos del insumo', 'error');
        this.close();
      }
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.markFormGroupTouched(this.form);
      return;
    }

    this.isSubmitting = true;
    const formData = this.form.value;

    const request$ = this.isEditMode
      ? this.inventoryService.updateSupply(this.supplyId!, formData)
      : this.inventoryService.createSupply(formData);

    request$.subscribe({
      next: () => {
        this.toastService.show(`Insumo ${this.isEditMode ? 'actualizado' : 'creado'} exitosamente`, 'success');
        this.isSubmitting = false;
        this.close(true);
      },
      error: () => {
        this.toastService.show(`Error al ${this.isEditMode ? 'actualizar' : 'crear'} el insumo`, 'error');
        this.isSubmitting = false;
      }
    });
  }

  close(refresh: boolean = false): void {
    this.closeModal.emit(refresh);
  }

  private markFormGroupTouched(formGroup: FormGroup) {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
      if ((control as any).controls) {
        this.markFormGroupTouched(control as FormGroup);
      }
    });
  }
}
