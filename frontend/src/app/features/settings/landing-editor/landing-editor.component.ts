import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { WebsiteSettingsService } from '../../../core/services/website-settings.service';
import { WebsiteEditor, WebsiteDraft, LandingBlockRef } from '../../../core/models/website-editor.model';
import { PublicLanding } from '../../../core/models/public-landing.model';
import { LandingViewComponent } from '../../../features/public/landing-view/landing-view.component';
import { EditorToolbarComponent, EditorViewport } from './editor-toolbar/editor-toolbar.component';
import { EditorPanelComponent } from './editor-panel/editor-panel.component';
import { draftToViewLanding } from './landing-editor.mapper';
import { validateDraft } from './landing-editor.validation';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-landing-editor',
  standalone: true,
  imports: [CommonModule, LandingViewComponent, EditorToolbarComponent, EditorPanelComponent],
  templateUrl: './landing-editor.component.html',
  styleUrl: './landing-editor.component.css'
})
export class LandingEditorComponent implements OnInit {
  editor: WebsiteEditor | null = null;
  draft: WebsiteDraft | null = null;
  selected: LandingBlockRef | null = null;
  viewport: EditorViewport = 'desktop';
  saving = false;
  publishing = false;
  loading = true;
  error: string | null = null;
  servicesByCode: Record<string, string[]> = {};

  readonly assetBase = `${environment.apiUrl}/v1/public/website-assets`;

  constructor(
    private readonly websiteService: WebsiteSettingsService,
    private readonly router: Router,
    private readonly notification: NotificationService,
    private readonly toast: ToastService
  ) {}

  ngOnInit(): void {
    this.reload();
  }

  get viewLanding(): PublicLanding | null {
    return this.draft ? draftToViewLanding(this.draft, this.assetBase) : null;
  }

  get dirty(): boolean {
    return Boolean(this.draft && this.editor && JSON.stringify(this.draft) !== JSON.stringify(this.editor.draft));
  }

  get validationErrors(): Record<string, string> {
    return this.draft ? validateDraft(this.draft) : {};
  }

  get hasValidationErrors(): boolean {
    return Object.keys(this.validationErrors).length > 0;
  }

  selectBlock(ref: LandingBlockRef): void {
    this.selected = ref;
  }

  closePanel(): void {
    this.selected = null;
  }

  setViewport(viewport: EditorViewport): void {
    this.viewport = viewport;
  }

  openPublic(): void {
    window.open('/', '_blank', 'noopener');
  }

  async exit(): Promise<void> {
    if (this.dirty) {
      const ok = await this.notification.confirm(
        'Salir del editor',
        'Tienes cambios sin guardar. Si sales ahora, se perderán. ¿Deseas salir?',
        'Salir sin guardar'
      );
      if (!ok) return;
    }
    this.router.navigate(['/dashboard']);
  }

  @HostListener('window:beforeunload', ['$event'])
  onBeforeUnload(event: BeforeUnloadEvent): void {
    if (this.dirty) {
      event.preventDefault();
      event.returnValue = 'Tienes cambios sin guardar en el editor';
    }
  }

  async save(): Promise<void> {
    if (this.saving || this.publishing || !this.draft || !this.editor) return;
    this.saving = true;
    try {
      const updated = await firstValueFrom(this.websiteService.saveDraft(this.draft, this.editor.revision));
      this.applyEditor(updated);
      this.toast.show('Borrador guardado', 'success');
    } catch (error) {
      this.handleError(error);
    } finally {
      this.saving = false;
    }
  }

  async publish(): Promise<void> {
    if (this.saving || this.publishing || !this.editor) return;
    if (this.hasValidationErrors) {
      this.toast.show('Corrige los errores de validación antes de publicar', 'error');
      return;
    }
    const ok = await this.notification.confirm(
      'Publicar sitio',
      'Se reemplazará la versión pública del sitio con el contenido actual del borrador. ¿Deseas continuar?',
      'Publicar'
    );
    if (!ok) return;

    this.publishing = true;
    try {
      let revision = this.editor.revision;
      if (this.dirty && this.draft) {
        const saved = await firstValueFrom(this.websiteService.saveDraft(this.draft, revision));
        this.applyEditor(saved);
        revision = saved.revision;
      }
      const published = await firstValueFrom(this.websiteService.publish(revision));
      this.applyEditor(published);
      this.toast.show('Sitio publicado correctamente', 'success');
    } catch (error) {
      this.handleError(error);
    } finally {
      this.publishing = false;
    }
  }

  async reset(): Promise<void> {
    if (this.saving || this.publishing || !this.editor) return;
    const ok = await this.notification.confirm(
      'Restaurar borrador',
      'Se descartarán los cambios actuales del borrador y se restaurará desde la versión publicada. ¿Continuar?',
      'Restaurar'
    );
    if (!ok) return;

    this.saving = true;
    try {
      const updated = await firstValueFrom(this.websiteService.resetDraft(this.editor.revision));
      this.applyEditor(updated);
      this.selected = null;
      this.toast.show('Borrador restaurado', 'success');
    } catch (error) {
      this.handleError(error);
    } finally {
      this.saving = false;
    }
  }

  revertBlock(): void {
    if (!this.draft || !this.editor || !this.selected) return;
    const src = this.editor.draft;
    const dst = this.draft;

    const fieldSets: Record<string, (keyof WebsiteDraft)[]> = {
      general: ['commercialName', 'tagline', 'description', 'logoExternalImageUrl', 'logoAssetKey'],
      hero: ['heroEyebrow', 'heroTitle', 'heroHighlight', 'heroDescription', 'heroPrimaryButtonText', 'heroSecondaryButtonText', 'heroExternalImageUrl', 'heroAssetKey'],
      approach: ['approachTitle', 'approachHighlight', 'approachDescription', 'approachSecondaryDescription', 'approachCtaText', 'approachExternalImageUrl', 'approachAssetKey'],
      contact: ['contactHeading', 'contactDescription', 'contactPhone', 'contactWhatsapp', 'contactEmail', 'contactAddress', 'contactHours', 'mapUrl'],
      social: ['facebookUrl', 'instagramUrl', 'tiktokUrl', 'linkedinUrl'],
      seo: ['seoTitle', 'seoDescription', 'seoSiteName', 'seoExternalImageUrl', 'seoAssetKey'],
      specialties: ['specialties'],
      benefits: ['benefits'],
      processSteps: ['processSteps'],
      professionals: ['professionals']
    };

    const fields = fieldSets[this.selected.type];
    if (!fields) return;
    for (const field of fields) {
      (dst as unknown as Record<string, unknown>)[field] = this.clone((src as unknown as Record<string, unknown>)[field]);
    }
  }

  async uploadImage(category: string, file: File): Promise<void> {
    await this.persistThen(() => this.websiteService.uploadDraftAsset(category, file), 'Imagen actualizada');
  }

  async removeImage(category: string): Promise<void> {
    await this.persistThen(() => this.websiteService.deleteDraftAsset(category), 'Imagen eliminada');
  }

  async uploadProfessionalPhoto(draftKey: string, file: File): Promise<void> {
    await this.persistThen(() => this.websiteService.uploadDraftProfessionalPhoto(draftKey, file), 'Foto actualizada');
  }

  private async persistThen(action: () => ReturnType<WebsiteSettingsService['uploadDraftAsset']>, successMessage: string): Promise<void> {
    if (this.saving || this.publishing || !this.editor) return;
    this.saving = true;
    try {
      if (this.dirty && this.draft) {
        const saved = await firstValueFrom(this.websiteService.saveDraft(this.draft, this.editor.revision));
        this.applyEditor(saved);
      }
      const updated = await firstValueFrom(action());
      this.applyEditor(updated);
      this.toast.show(successMessage, 'success');
    } catch (error) {
      this.handleError(error);
    } finally {
      this.saving = false;
    }
  }

  private reload(): void {
    this.loading = true;
    this.error = null;
    this.websiteService.getEditor().subscribe({
      next: editor => {
        this.applyEditor(editor);
        this.loading = false;
        this.loadDerivedServices();
      },
      error: () => {
        this.error = 'No se pudo cargar el editor del sitio';
        this.loading = false;
      }
    });
  }

  private loadDerivedServices(): void {
    this.websiteService.getPublicLanding().subscribe({
      next: landing => {
        this.servicesByCode = {};
        for (const specialty of landing.specialties) {
          this.servicesByCode[specialty.code] = specialty.services ?? [];
        }
      },
      error: () => {
        this.servicesByCode = {};
      }
    });
  }

  private applyEditor(editor: WebsiteEditor): void {
    this.editor = editor;
    this.draft = this.clone(editor.draft);
  }

  private handleError(error: unknown): void {
    const httpError = error as { status?: number; error?: { message?: string } };
    const message = httpError?.error?.message;
    if (httpError?.status === 409) {
      this.toast.show(message || 'El borrador cambió en otro dispositivo. Recargando…', 'error');
      this.reload();
    } else {
      this.toast.show(message || 'Ocurrió un error al guardar', 'error');
    }
  }

  private clone<T>(value: T): T {
    return JSON.parse(JSON.stringify(value)) as T;
  }
}
