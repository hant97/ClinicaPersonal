import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';

import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { InventoryService } from '../../../core/services/inventory.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { CatalogItem } from '../../../core/models/catalog.model';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { LucideAngularModule } from 'lucide-angular';
import {
  X,
  Package,
  Upload,
  Image,
  AlertTriangle,
  CheckCircle2,
  DollarSign,
  Calendar,
  Save,
  Trash2,
  Plus
} from '../../../shared/icons/lucide-icons';
import { FocusTrapDirective } from '../../../shared/directives/focus-trap.directive';

@Component({
  selector: 'app-inventory-form',
  standalone: true,
  imports: [ReactiveFormsModule, LucideAngularModule, FocusTrapDirective],
  templateUrl: './inventory-form.component.html'
})
export class InventoryFormComponent implements OnInit, OnDestroy {
  @Input() supplyId: number | null = null;
  @Output() closeModal = new EventEmitter<boolean>();

  readonly X = X;
  readonly Package = Package;
  readonly Upload = Upload;
  readonly Image = Image;
  readonly AlertTriangle = AlertTriangle;
  readonly CheckCircle2 = CheckCircle2;
  readonly DollarSign = DollarSign;
  readonly Calendar = Calendar;
  readonly Save = Save;
  readonly Trash2 = Trash2;
  readonly Plus = Plus;

  form!: FormGroup;
  isSubmitting = false;
  isEditMode = false;
  supplyUnits: CatalogItem[] = [];
  selectedImageFile: File | null = null;
  imagePreviewUrl: string | null = null;

  constructor(
    private fb: FormBuilder,
    private inventoryService: InventoryService,
    private catalogService: CatalogService,
    private toastService: ToastService
  ) {}

  get imagePreview(): string | null {
    return this.imagePreviewUrl ?? (this.form?.get('imageUrl')?.value || null);
  }

  get isLowStock(): boolean {
    if (!this.form) return false;
    const current = Number(this.form.get('currentStock')?.value || 0);
    const min = Number(this.form.get('minStockLevel')?.value || 0);
    return min > 0 && current <= min;
  }

  ngOnInit(): void {
    this.isEditMode = !!this.supplyId;
    this.initForm();
    this.loadCatalogs();
    if (this.isEditMode) {
      this.loadSupply();
    }
  }

  ngOnDestroy(): void {
    this.revokeImagePreviewUrl();
  }

  initForm(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required]],
      description: [''],
      currentStock: [0, [Validators.required, Validators.min(0)]],
      minStockLevel: [0, [Validators.required, Validators.min(0)]],
      unit: ['', [Validators.required]],
      price: [0, [Validators.min(0)]],
      expirationDate: [''],
      imageUrl: ['']
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

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type) || file.size > 2 * 1024 * 1024) {
      this.toastService.show('La imagen debe ser PNG, JPG o WEBP de hasta 2 MB', 'error');
      input.value = '';
      return;
    }
    this.selectedImageFile = file;
    this.revokeImagePreviewUrl();
    this.imagePreviewUrl = URL.createObjectURL(file);
  }

  removeImage(): void {
    this.selectedImageFile = null;
    this.revokeImagePreviewUrl();
    this.form.get('imageUrl')?.setValue('');
  }

  private revokeImagePreviewUrl(): void {
    if (this.imagePreviewUrl) {
      URL.revokeObjectURL(this.imagePreviewUrl);
      this.imagePreviewUrl = null;
    }
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.markFormGroupTouched(this.form);
      this.toastService.show('Por favor, complete todos los campos obligatorios correctamente.', 'error');
      return;
    }

    this.isSubmitting = true;
    const formData = this.form.value;

    const request$ = this.isEditMode
      ? this.inventoryService.updateSupply(this.supplyId!, formData)
      : this.inventoryService.createSupply(formData);

    request$.subscribe({
      next: (saved) => {
        this.toastService.show(`Insumo ${this.isEditMode ? 'actualizado' : 'creado'} exitosamente`, 'success');
        if (this.selectedImageFile && saved.id != null) {
          this.uploadImage(saved.id);
          return;
        }
        this.isSubmitting = false;
        this.close(true);
      },
      error: () => {
        this.toastService.show(`Error al ${this.isEditMode ? 'actualizar' : 'crear'} el insumo`, 'error');
        this.isSubmitting = false;
      }
    });
  }

  private uploadImage(id: number): void {
    this.inventoryService.uploadSupplyImage(id, this.selectedImageFile!).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.close(true);
      },
      error: () => {
        this.toastService.show('Error al subir la imagen del insumo', 'error');
        this.isSubmitting = false;
        this.close(true);
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
