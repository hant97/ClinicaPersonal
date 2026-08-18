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
  let websiteService: jasmine.SpyObj<WebsiteSettingsService>;
  let router: jasmine.SpyObj<Router>;
  let notification: jasmine.SpyObj<NotificationService>;
  let toast: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    websiteService = jasmine.createSpyObj<WebsiteSettingsService>('WebsiteSettingsService', [
      'getEditor', 'getPublicLanding', 'saveDraft', 'publish', 'resetDraft',
      'uploadDraftAsset', 'deleteDraftAsset', 'uploadDraftProfessionalPhoto'
    ]);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    notification = jasmine.createSpyObj<NotificationService>('NotificationService', ['confirm', 'alert']);
    toast = jasmine.createSpyObj<ToastService>('ToastService', ['show']);

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
    websiteService.getEditor.and.returnValue(of(editorWith(draft)));
    websiteService.getPublicLanding.and.returnValue(of({ specialties: [] } as unknown as PublicLanding));

    component.ngOnInit();

    expect(component.draft?.commercialName).toBe('Clínica Demo');
    expect(component.loading).toBeFalse();
    expect(component.viewLanding?.hero.title).toBe('Tu bienestar');
  });

  it('editar el borrador actualiza la vista previa sin publicar', () => {
    const draft = validDraft();
    websiteService.getEditor.and.returnValue(of(editorWith(draft)));
    websiteService.getPublicLanding.and.returnValue(of({ specialties: [] } as unknown as PublicLanding));

    component.ngOnInit();
    component.draft!.heroTitle = 'Nuevo título';

    expect(component.viewLanding?.hero.title).toBe('Nuevo título');
    expect(websiteService.publish).not.toHaveBeenCalled();
  });

  it('guarda el borrador con la revisión actual', async () => {
    const draft = validDraft();
    websiteService.getEditor.and.returnValue(of(editorWith(draft)));
    websiteService.getPublicLanding.and.returnValue(of({ specialties: [] } as unknown as PublicLanding));
    websiteService.saveDraft.and.returnValue(of(editorWith(draft, 4)));

    component.ngOnInit();
    await component.save();

    expect(websiteService.saveDraft).toHaveBeenCalledWith(draft, 3);
  });

  it('bloquea la publicación cuando existen errores de validación', async () => {
    const invalid = validDraft();
    invalid.commercialName = '';
    websiteService.getEditor.and.returnValue(of(editorWith(invalid)));
    websiteService.getPublicLanding.and.returnValue(of({ specialties: [] } as unknown as PublicLanding));

    component.ngOnInit();
    await component.publish();

    expect(websiteService.publish).not.toHaveBeenCalled();
    expect(toast.show).toHaveBeenCalledWith('Corrige los errores de validación antes de publicar', 'error');
  });

  it('confirma antes de salir cuando hay cambios sin guardar', async () => {
    const draft = validDraft();
    websiteService.getEditor.and.returnValue(of(editorWith(draft)));
    websiteService.getPublicLanding.and.returnValue(of({ specialties: [] } as unknown as PublicLanding));
    notification.confirm.and.returnValue(Promise.resolve(true));

    component.ngOnInit();
    component.draft!.heroTitle = 'Sin guardar';
    await component.exit();

    expect(notification.confirm).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });
});
