import type { MockedObject } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { LandingEditorComponent } from './landing-editor.component';
import { WebsiteSettingsService } from '../../../core/services/website-settings.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { WebsiteEditor, WebsiteDraft } from '../../../core/models/website-editor.model';
import { PublicLanding } from '../../../core/models/public-landing.model';

function validDraft(): WebsiteDraft {
  return {
    commercialName: 'Clínica Demo',
    heroTitle: 'Tu bienestar',
    specialties: [],
    benefits: [],
    processSteps: [],
    professionals: []
  };
}

function editorWith(draft: WebsiteDraft, revision = 3): WebsiteEditor {
  return { draft, revision, hasUnpublishedChanges: false, publishedRevision: revision };
}

describe('LandingEditorComponent', () => {
  let component: LandingEditorComponent;
  let websiteService: MockedObject<WebsiteSettingsService>;
  let router: MockedObject<Router>;
  let notification: MockedObject<NotificationService>;
  let toast: MockedObject<ToastService>;

  beforeEach(async () => {
    websiteService = {
      getEditor: vi.fn().mockName('WebsiteSettingsService.getEditor'),
      getPublicLanding: vi.fn().mockName('WebsiteSettingsService.getPublicLanding'),
      saveDraft: vi.fn().mockName('WebsiteSettingsService.saveDraft'),
      publish: vi.fn().mockName('WebsiteSettingsService.publish'),
      resetDraft: vi.fn().mockName('WebsiteSettingsService.resetDraft'),
      uploadDraftAsset: vi.fn().mockName('WebsiteSettingsService.uploadDraftAsset'),
      deleteDraftAsset: vi.fn().mockName('WebsiteSettingsService.deleteDraftAsset'),
      uploadDraftProfessionalPhoto: vi.fn().mockName('WebsiteSettingsService.uploadDraftProfessionalPhoto')
    } as unknown as MockedObject<WebsiteSettingsService>;
    router = {
      navigate: vi.fn().mockName('Router.navigate')
    } as unknown as MockedObject<Router>;
    notification = {
      confirm: vi.fn().mockName('NotificationService.confirm'),
      alert: vi.fn().mockName('NotificationService.alert')
    } as unknown as MockedObject<NotificationService>;
    toast = {
      show: vi.fn().mockName('ToastService.show')
    } as unknown as MockedObject<ToastService>;

    await TestBed.configureTestingModule({
      imports: [LandingEditorComponent],
      providers: [
        { provide: WebsiteSettingsService, useValue: websiteService },
        { provide: Router, useValue: router },
        { provide: NotificationService, useValue: notification },
        { provide: ToastService, useValue: toast }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(LandingEditorComponent);
    component = fixture.componentInstance;
  });

  it('carga el borrador y deriva la vista previa', () => {
    const draft = validDraft();
    websiteService.getEditor.mockReturnValue(of(editorWith(draft)));
    websiteService.getPublicLanding.mockReturnValue(of({ specialties: [] } as unknown as PublicLanding));

    component.ngOnInit();

    expect(component.draft?.commercialName).toBe('Clínica Demo');
    expect(component.loading).toBe(false);
    expect(component.viewLanding?.hero.title).toBe('Tu bienestar');
  });

  it('editar el borrador actualiza la vista previa sin publicar', () => {
    const draft = validDraft();
    websiteService.getEditor.mockReturnValue(of(editorWith(draft)));
    websiteService.getPublicLanding.mockReturnValue(of({ specialties: [] } as unknown as PublicLanding));

    component.ngOnInit();
    component.draft!.heroTitle = 'Nuevo título';

    expect(component.viewLanding?.hero.title).toBe('Nuevo título');
    expect(websiteService.publish).not.toHaveBeenCalled();
  });

  it('guarda el borrador con la revisión actual', async () => {
    const draft = validDraft();
    websiteService.getEditor.mockReturnValue(of(editorWith(draft)));
    websiteService.getPublicLanding.mockReturnValue(of({ specialties: [] } as unknown as PublicLanding));
    websiteService.saveDraft.mockReturnValue(of(editorWith(draft, 4)));

    component.ngOnInit();
    await component.save();

    expect(websiteService.saveDraft).toHaveBeenCalledWith(draft, 3);
  });

  it('bloquea la publicación cuando existen errores de validación', async () => {
    const invalid = validDraft();
    invalid.commercialName = '';
    websiteService.getEditor.mockReturnValue(of(editorWith(invalid)));
    websiteService.getPublicLanding.mockReturnValue(of({ specialties: [] } as unknown as PublicLanding));

    component.ngOnInit();
    await component.publish();

    expect(websiteService.publish).not.toHaveBeenCalled();
    expect(toast.show).toHaveBeenCalledWith('Corrige los errores de validación antes de publicar', 'error');
  });

  it('confirma antes de salir cuando hay cambios sin guardar', async () => {
    const draft = validDraft();
    websiteService.getEditor.mockReturnValue(of(editorWith(draft)));
    websiteService.getPublicLanding.mockReturnValue(of({ specialties: [] } as unknown as PublicLanding));
    notification.confirm.mockReturnValue(Promise.resolve(true));

    component.ngOnInit();
    component.draft!.heroTitle = 'Sin guardar';
    await component.exit();

    expect(notification.confirm).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });
});
