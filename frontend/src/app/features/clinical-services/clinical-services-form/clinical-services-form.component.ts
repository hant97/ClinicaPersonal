import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';

import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ClinicalServiceService } from '../../../core/services/clinical-service.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { CatalogItem } from '../../../core/models/catalog.model';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { LucideAngularModule } from 'lucide-angular';
import {
  X } from '../../../shared/icons/lucide-icons';
import { FocusTrapDirective } from '../../../shared/directives/focus-trap.directive';
import { CLINICAL_SERVICE_CATEGORY_CATALOG_CODE } from '../clinical-service-catalog.constants';

@Component({
  selector: 'app-clinical-services-form',
  standalone: true,
  imports: [ReactiveFormsModule, LucideAngularModule, FocusTrapDirective],
  templateUrl: './clinical-services-form.component.html'
})
export class ClinicalServicesFormComponent implements OnInit {
  @Input() serviceId: number | null = null;
  @Output() closeModal = new EventEmitter<boolean>();

  readonly X = X;

  form!: FormGroup;
  isSubmitting = false;
  isEditMode = false;
  categories: CatalogItem[] = [];
  categoriesLoading = false;
  categoriesLoadError = false;

  constructor(
    private fb: FormBuilder,
    private serviceService: ClinicalServiceService,
    private catalogService: CatalogService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.isEditMode = !!this.serviceId;
    this.initForm();
    this.loadCategories();
    if (this.isEditMode) {
      this.loadService();
    }
  }

  loadCategories(): void {
    this.categoriesLoading = true;
    this.categoriesLoadError = false;
    this.catalogService.getActiveItemsByCatalogCode(CLINICAL_SERVICE_CATEGORY_CATALOG_CODE).subscribe({
      next: (items) => {
        this.categories = items || [];
        this.categoriesLoading = false;
      },
      error: () => {
        this.categories = [];
        this.categoriesLoading = false;
        this.categoriesLoadError = true;
        this.toastService.show('Error al cargar las categorías de servicios', 'error');
      }
    });
  }

  initForm(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required]],
      description: [''],
      price: [0, [Validators.required, Validators.min(0)]],
      category: [''],
      durationMinutes: [null, [Validators.min(5)]],
      imageUrl: [''],
      active: [true]
    });
  }

  loadService(): void {
    this.serviceService.getServiceById(this.serviceId!).subscribe({
      next: (service) => {
        this.form.patchValue({
          name: service.name,
          description: service.description,
          price: service.price,
          category: service.category,
          durationMinutes: service.durationMinutes,
          imageUrl: service.imageUrl,
          active: service.active !== false
        });
      },
      error: () => {
        this.toastService.show('Error al cargar datos del servicio', 'error');
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
      ? this.serviceService.updateService(this.serviceId!, formData)
      : this.serviceService.createService(formData);

    request$.subscribe({
      next: () => {
        this.toastService.show(`Servicio ${this.isEditMode ? 'actualizado' : 'creado'} exitosamente`, 'success');
        this.isSubmitting = false;
        this.close(true);
      },
      error: () => {
        this.toastService.show(`Error al ${this.isEditMode ? 'actualizar' : 'crear'} el servicio`, 'error');
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
