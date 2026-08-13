import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LucideAngularModule, Eye, Plus, Save, Trash2 } from 'lucide-angular';
import { WebsiteSettingsService } from '../../../core/services/website-settings.service';
import { WebsiteBenefitAdmin, WebsiteProcessStepAdmin, WebsiteProfessionalAdmin, WebsiteSettingsAdmin } from '../../../core/models/website-settings.model';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { environment } from '../../../../environments/environment';
import { IconPickerComponent } from '../../../shared/components/icon-picker/icon-picker.component';

const emptySettings = (): WebsiteSettingsAdmin => ({
  commercialName: '', heroTitle: '', specialties: [], benefits: [], processSteps: [], professionals: []
});

@Component({
  selector: 'app-website-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, LucideAngularModule, IconPickerComponent],
  templateUrl: './website-settings.component.html',
  styleUrl: './website-settings.component.css'
})
export class WebsiteSettingsComponent implements OnInit {
  readonly Eye = Eye;
  readonly Plus = Plus;
  readonly Save = Save;
  readonly Trash2 = Trash2;
  settings: WebsiteSettingsAdmin = emptySettings();
  loading = true;
  saving = false;
  previewUrls: Record<string, string> = {};

  constructor(private readonly websiteService: WebsiteSettingsService, private readonly notification: NotificationService) {}

  ngOnInit(): void {
    this.websiteService.getAdminSettings().subscribe({ next: value => { this.settings = value; this.loading = false; }, error: () => { this.loading = false; this.notification.alert('Error', 'No se pudo cargar la configuración del sitio', 'error'); } });
  }

  save(): void {
    if (!this.settings.commercialName?.trim() || !this.settings.heroTitle?.trim()) {
      this.notification.alert('Validación', 'Completa el nombre comercial y el título principal', 'error'); return;
    }
    this.saving = true;
    this.websiteService.update(this.settings).subscribe({ next: value => { this.settings = value; this.saving = false; this.notification.alert('Éxito', 'Configuración del sitio actualizada correctamente', 'success'); }, error: error => { this.saving = false; this.notification.alert('Error', error?.error?.message || 'No se pudo guardar la configuración', 'error'); } });
  }

  addSpecialty(): void { this.settings.specialties.push({ code: '', label: '', title: '', iconCode: 'STETHOSCOPE', displayOrder: this.settings.specialties.length + 1, visible: true, services: [] }); }
  removeSpecialty(index: number): void { this.settings.specialties.splice(index, 1); }
  addBenefit(): void { this.settings.benefits.push({ title: '', iconCode: 'SPARKLES', displayOrder: this.settings.benefits.length + 1, active: true }); }
  removeBenefit(index: number): void { this.settings.benefits.splice(index, 1); }
  addStep(): void { const number = this.settings.processSteps.length + 1; this.settings.processSteps.push({ stepNumber: number, title: '', displayOrder: number, active: true }); }
  removeStep(index: number): void { this.settings.processSteps.splice(index, 1); }
  addProfessional(): void { this.settings.professionals.push({ name: '', displayOrder: this.settings.professionals.length + 1, active: true }); }
  removeProfessional(index: number): void { this.settings.professionals.splice(index, 1); }

  onImageSelected(category: string, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if (!['image/png', 'image/jpeg', 'image/webp', 'image/svg+xml'].includes(file.type) || file.size > 2 * 1024 * 1024) {
      this.notification.alert('Validación', 'Usa PNG, JPG, WEBP o SVG de hasta 2 MB', 'error'); input.value = ''; return;
    }
    this.previewUrls[category] = URL.createObjectURL(file);
    this.websiteService.uploadAsset(category, file).subscribe({ next: value => { this.settings = value; this.notification.alert('Éxito', 'Imagen actualizada correctamente', 'success'); }, error: () => this.notification.alert('Error', 'No se pudo cargar la imagen', 'error') });
  }

  deleteImage(category: string): void { this.websiteService.deleteAsset(category).subscribe({ next: value => { this.settings = value; delete this.previewUrls[category]; this.notification.alert('Éxito', 'Imagen eliminada correctamente', 'success'); }, error: () => this.notification.alert('Error', 'No se pudo eliminar la imagen', 'error') }); }
  onProfessionalPhotoSelected(professional: WebsiteProfessionalAdmin, event: Event): void { const input = event.target as HTMLInputElement; const file = input.files?.[0]; if (!file || !professional.id) return; if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type) || file.size > 2 * 1024 * 1024) { this.notification.alert('Validación', 'La fotografía debe ser PNG, JPG o WEBP de hasta 2 MB', 'error'); return; } this.websiteService.uploadProfessionalPhoto(professional.id, file).subscribe({ next: value => { this.settings = value; this.notification.alert('Éxito', 'Fotografía actualizada correctamente', 'success'); }, error: () => this.notification.alert('Error', 'No se pudo cargar la fotografía', 'error') }); }
  imageUrl(category: string, external?: string, asset?: string): string { return this.previewUrls[category] || external || (asset ? `${environment.apiUrl}/v1/public/website-assets/${asset}` : ''); }
  trackIndex(index: number): number { return index; }
}
