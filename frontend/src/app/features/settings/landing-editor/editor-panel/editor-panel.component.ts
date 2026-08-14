import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  ArrowDown,
  ArrowUp,
  Eye,
  EyeOff,
  ImagePlus,
  LucideAngularModule,
  Plus,
  RotateCcw,
  Trash2,
  X
} from 'lucide-angular';
import { WebsiteDraft, LandingBlockRef } from '../../../../core/models/website-editor.model';
import { IconPickerComponent } from '../../../../shared/components/icon-picker/icon-picker.component';
import { FIELD_LIMITS, ICON_LABELS } from '../landing-editor.validation';

@Component({
  selector: 'app-editor-panel',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, LucideAngularModule, IconPickerComponent],
  templateUrl: './editor-panel.component.html',
  styleUrl: './editor-panel.component.css'
})
export class EditorPanelComponent {
  @Input({ required: true }) draft!: WebsiteDraft;
  @Input() block: LandingBlockRef | null = null;
  @Input() validationErrors: Record<string, string> = {};
  @Input() assetBase = '';
  @Input() servicesByCode: Record<string, string[]> = {};

  @Output() close = new EventEmitter<void>();
  @Output() revert = new EventEmitter<void>();
  @Output() uploadImage = new EventEmitter<{ category: string; file: File }>();
  @Output() removeImage = new EventEmitter<{ category: string }>();
  @Output() uploadProfessionalPhoto = new EventEmitter<{ draftKey: string; file: File }>();

  readonly limits = FIELD_LIMITS;
  readonly iconLabels = ICON_LABELS;

  readonly ArrowDown = ArrowDown;
  readonly ArrowUp = ArrowUp;
  readonly Eye = Eye;
  readonly EyeOff = EyeOff;
  readonly ImagePlus = ImagePlus;
  readonly Plus = Plus;
  readonly RotateCcw = RotateCcw;
  readonly Trash2 = Trash2;
  readonly X = X;

  get title(): string {
    switch (this.block?.type) {
      case 'general': return 'Datos generales';
      case 'hero': return 'Sección principal';
      case 'approach': return 'Enfoque';
      case 'contact': return 'Contacto';
      case 'social': return 'Redes sociales';
      case 'seo': return 'SEO';
      case 'specialties': return 'Especialidades';
      case 'benefits': return 'Beneficios';
      case 'processSteps': return 'Pasos del proceso';
      case 'professionals': return 'Profesionales';
      default: return 'Editar bloque';
    }
  }

  hasError(field: string): boolean {
    return Boolean(this.validationErrors[field]);
  }

  errorText(field: string): string {
    return this.validationErrors[field] ?? '';
  }

  imageUrl(key?: string, external?: string): string | undefined {
    return key ? `${this.assetBase}/${key}` : external;
  }

  onFileSelected(event: Event, category: string): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) this.uploadImage.emit({ category, file });
    input.value = '';
  }

  onProfessionalFileSelected(event: Event, draftKey: string): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) this.uploadProfessionalPhoto.emit({ draftKey, file });
    input.value = '';
  }

  removeProfessionalPhoto(index: number): void {
    const professional = this.draft.professionals[index];
    if (professional) {
      professional.photoAssetKey = undefined;
      professional.photoExternalUrl = undefined;
    }
  }

  // --- Colecciones ---

  addBenefit(): void {
    this.draft.benefits.push({
      draftKey: this.newKey(),
      title: '',
      iconCode: 'SPARKLES',
      displayOrder: this.draft.benefits.length + 1,
      active: true
    });
  }

  addProcessStep(): void {
    this.draft.processSteps.push({
      draftKey: this.newKey(),
      stepNumber: this.draft.processSteps.length + 1,
      title: '',
      displayOrder: this.draft.processSteps.length + 1,
      active: true
    });
  }

  addProfessional(): void {
    this.draft.professionals.push({
      draftKey: this.newKey(),
      name: '',
      displayOrder: this.draft.professionals.length + 1,
      active: true
    });
  }

  removeAt(list: unknown[], index: number): void {
    list.splice(index, 1);
  }

  moveAt(list: unknown[], index: number, direction: -1 | 1): void {
    const target = index + direction;
    if (target < 0 || target >= list.length) return;
    [list[index], list[target]] = [list[target], list[index]];
  }

  newKey(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
    return 'item-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 10);
  }
}
