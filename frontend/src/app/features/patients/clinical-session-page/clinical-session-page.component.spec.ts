import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import type { MockedObject } from 'vitest';
import { ClinicalSessionPageComponent } from './clinical-session-page.component';
import { ClinicalSessionDraft, ClinicalSessionDraftService } from '../../../core/services/clinical-session-draft.service';
import { PatientService } from '../../../core/services/patient/patient.service';
import { ClinicalSessionService } from '../../../core/services/clinical-session.service';
import { ClinicalHistoryService } from '../../../core/services/clinical-history.service';
import { RiskAlertService } from '../../../core/services/risk-alert.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { Patient } from '../../../core/models/patient.model';
import { ClinicalHistory } from '../../../core/models/clinical-history.model';
import { ClinicalSession } from '../../../core/models/clinical-session.model';

describe('ClinicalSessionPageComponent draft conflicts', () => {
  let component: ClinicalSessionPageComponent;
  let drafts: MockedObject<ClinicalSessionDraftService>;
  let notification: MockedObject<NotificationService>;
  let router: MockedObject<Router>;
  let toast: MockedObject<ToastService>;
  let auth: MockedObject<AuthService>;
  let patients: MockedObject<PatientService>;
  let sessions: MockedObject<ClinicalSessionService>;
  let histories: MockedObject<ClinicalHistoryService>;
  let alerts: MockedObject<RiskAlertService>;
  let catalogs: MockedObject<CatalogService>;
  let specialties: MockedObject<SpecialtyService>;

  beforeEach(() => {
    drafts = {
      getDraft: vi.fn(),
      saveDraft: vi.fn(),
      deleteDraft: vi.fn(),
      migrateLegacyDrafts: vi.fn()
    } as unknown as MockedObject<ClinicalSessionDraftService>;
    notification = { confirm: vi.fn() } as unknown as MockedObject<NotificationService>;
    router = { navigate: vi.fn() } as unknown as MockedObject<Router>;
    toast = { success: vi.fn(), warning: vi.fn(), error: vi.fn() } as unknown as MockedObject<ToastService>;
    auth = { getUsername: vi.fn().mockReturnValue('doctor') } as unknown as MockedObject<AuthService>;
    patients = { getById: vi.fn() } as unknown as MockedObject<PatientService>;
    sessions = { getSessionsByPatientId: vi.fn(), createSession: vi.fn() } as unknown as MockedObject<ClinicalSessionService>;
    histories = { get: vi.fn() } as unknown as MockedObject<ClinicalHistoryService>;
    alerts = { getAlertsByPatientId: vi.fn() } as unknown as MockedObject<RiskAlertService>;
    catalogs = { getActiveItemsByCatalogCode: vi.fn() } as unknown as MockedObject<CatalogService>;
    specialties = { isPsychology: vi.fn().mockReturnValue(true), isDermatology: vi.fn().mockReturnValue(false) } as unknown as MockedObject<SpecialtyService>;
    component = new ClinicalSessionPageComponent(
      { snapshot: { params: { id: 'patient-7' }, queryParams: {} } } as unknown as ActivatedRoute,
      router,
      new FormBuilder(),
      patients,
      sessions,
      histories,
      alerts,
      catalogs,
      specialties,
      toast,
      notification,
      auth,
      drafts
    );
    component.patient = { id: 7 } as Patient;
    component.sessionForm = new FormBuilder().group({ subjective: ['local'] });
  });

  it('serializa guardados para usar la versión recibida antes de enviar el siguiente', () => {
    const first = new Subject<ClinicalSessionDraft>();
    drafts.saveDraft.mockReturnValueOnce(first.asObservable()).mockReturnValueOnce(of({
      content: { subjective: 'segunda' }, expiresAt: '', version: 1
    }));

    component['persistDraft']({ subjective: 'primera' });
    component['persistDraft']({ subjective: 'segunda' });
    expect(drafts.saveDraft).toHaveBeenCalledTimes(1);

    first.next({ content: { subjective: 'primera' }, expiresAt: '', version: 0 });
    first.complete();

    expect(drafts.saveDraft).toHaveBeenNthCalledWith(2, 7, { subjective: 'segunda' }, 0);
    expect(component.draftSaved).toBe(true);
  });

  it('detiene el autoguardado tras un conflicto hasta una decisión explícita', async () => {
    drafts.saveDraft.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 409 })))
      .mockReturnValueOnce(of({ content: { subjective: 'local' }, expiresAt: '', version: 4 }));
    drafts.getDraft.mockReturnValue(of({ content: { subjective: 'servidor' }, expiresAt: '', version: 3 }));
    notification.confirm.mockResolvedValue(true);

    component['persistDraft']({ subjective: 'local' });
    component['persistDraft']({ subjective: 'otro cambio' });
    expect(component.draftConflict).toBe(true);
    expect(drafts.saveDraft).toHaveBeenCalledTimes(1);

    await component.replaceServerDraft();

    expect(drafts.saveDraft).toHaveBeenNthCalledWith(2, 7, { subjective: 'local' }, 3);
    expect(component.draftConflict).toBe(false);
  });

  it('restaura la versión recibida y la usa en el siguiente autoguardado', () => {
    drafts.migrateLegacyDrafts.mockReturnValue(of(void 0));
    drafts.getDraft.mockReturnValue(of({ content: { subjective: 'servidor' }, expiresAt: '', version: 6 }));
    drafts.saveDraft.mockReturnValue(of({ content: { subjective: 'nuevo' }, expiresAt: '', version: 7 }));

    component['restoreDraftIfApplicable']();
    expect(component.sessionForm.get('subjective')?.value).toBe('servidor');
    expect(component.draftSaved).toBe(true);

    component['persistDraft']({ subjective: 'nuevo' });
    expect(drafts.saveDraft).toHaveBeenCalledWith(7, { subjective: 'nuevo' }, 6);
  });

  it('carga el borrador remoto completo solo tras confirmación y deja el formulario limpio', async () => {
    component.draftConflict = true;
    component.sessionForm.markAsDirty();
    drafts.getDraft.mockReturnValue(of({ content: { subjective: 'servidor' }, expiresAt: '', version: 4 }));
    notification.confirm.mockResolvedValue(true);

    await component.loadServerDraft();

    expect(component.sessionForm.get('subjective')?.value).toBe('servidor');
    expect(component.sessionForm.pristine).toBe(true);
    expect(component.draftConflict).toBe(false);
    expect(toast.success).toHaveBeenCalled();
    expect(drafts.saveDraft).not.toHaveBeenCalled();
  });

  it('conserva el texto local si el borrador remoto ya no existe', async () => {
    component.draftConflict = true;
    drafts.getDraft.mockReturnValue(of(null));
    notification.confirm.mockResolvedValue(true);

    await component.loadServerDraft();

    expect(component.sessionForm.get('subjective')?.value).toBe('local');
    expect(component.draftConflict).toBe(false);
    expect(toast.warning).toHaveBeenCalled();
  });

  it('no reemplaza el borrador remoto si se cancela la confirmación', async () => {
    component.draftConflict = true;
    notification.confirm.mockResolvedValue(false);

    await component.replaceServerDraft();

    expect(drafts.getDraft).not.toHaveBeenCalled();
    expect(drafts.saveDraft).not.toHaveBeenCalled();
    expect(component.draftConflict).toBe(true);
  });

  it('espera al guardado en curso y usa su nueva versión al borrar tras registrar la consulta', () => {
    const saving = new Subject<ClinicalSessionDraft>();
    drafts.saveDraft.mockReturnValue(saving.asObservable());
    drafts.deleteDraft.mockReturnValue(of(void 0));

    component['persistDraft']({ subjective: 'nota final' });
    component['deleteDraftAndReturn']('Consulta guardada');
    expect(drafts.deleteDraft).not.toHaveBeenCalled();

    saving.next({ content: { subjective: 'nota final' }, expiresAt: '', version: 2 });
    saving.complete();

    expect(drafts.deleteDraft).toHaveBeenCalledWith(7, 2);
    expect(toast.success).toHaveBeenCalledWith('Consulta guardada');
    expect(router.navigate).toHaveBeenCalled();
  });

  it('avisa si una versión nueva impide borrar el borrador después de guardar la consulta', () => {
    drafts.saveDraft.mockReturnValue(of({ content: { subjective: 'nota' }, expiresAt: '', version: 2 }));
    drafts.deleteDraft.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409 })));

    component['persistDraft']({ subjective: 'nota' });
    component['deleteDraftAndReturn']('Consulta guardada');

    expect(toast.warning).toHaveBeenCalledWith(expect.stringContaining('más reciente'));
    expect(router.navigate).toHaveBeenCalled();
  });

  it('distingue un error de red de un conflicto de versión', () => {
    drafts.saveDraft.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 503 })));

    component['persistDraft']({ subjective: 'nota' });

    expect(component.draftError).toBe(true);
    expect(component.draftConflict).toBe(false);
  });

  it('carga el expediente y elimina solo la versión observada al registrar la consulta', () => {
    const emptyPage = { content: [], page: { number: 0, size: 10, totalElements: 0, totalPages: 0 } };
    catalogs.getActiveItemsByCatalogCode.mockReturnValue(of([]));
    patients.getById.mockReturnValue(of({ id: 7, firstName: 'Ana', lastName: 'Paz' } as Patient));
    histories.get.mockReturnValue(of({} as ClinicalHistory));
    alerts.getAlertsByPatientId.mockReturnValue(of(emptyPage));
    sessions.getSessionsByPatientId.mockReturnValue(of(emptyPage));
    sessions.createSession.mockReturnValue(of({} as ClinicalSession));
    drafts.migrateLegacyDrafts.mockReturnValue(of(void 0));
    drafts.getDraft.mockReturnValue(of({ content: { subjective: 'nota recuperada' }, expiresAt: '', version: 5 }));
    drafts.deleteDraft.mockReturnValue(of(void 0));

    component.ngOnInit();

    expect(component.patientIdentifier).toBe('patient-7');
    expect(component.sessionForm.get('subjective')?.value).toBe('nota recuperada');
    expect(component.loadingPatient).toBe(false);
    component.onSubmit();

    expect(sessions.createSession).toHaveBeenCalled();
    expect(drafts.deleteDraft).toHaveBeenCalledWith(7, 5);
    expect(router.navigate).toHaveBeenCalledWith(['/patients', 'patient-7']);
  });
});
