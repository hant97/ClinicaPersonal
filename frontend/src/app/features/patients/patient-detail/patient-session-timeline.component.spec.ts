import { TestBed } from '@angular/core/testing';
import { ClinicalSession } from '../../../core/models/clinical-session.model';
import { PatientSessionTimelineComponent } from './patient-session-timeline.component';

describe('PatientSessionTimelineComponent', () => {
  const session: ClinicalSession = {
    id: 7,
    patientId: 42,
    sessionDate: '2026-09-28',
    startTime: '10:00',
    endTime: '10:45',
    sessionType: 'CONTROL',
    modality: 'PRESENCIAL',
    status: 'COMPLETADA',
    subjective: 'Dolor leve'
  };

  it('offers creation when there are no sessions', async () => {
    await TestBed.configureTestingModule({ imports: [PatientSessionTimelineComponent] }).compileComponents();
    const fixture = TestBed.createComponent(PatientSessionTimelineComponent);
    const createRequested = vi.fn();
    fixture.componentInstance.createRequested.subscribe(createRequested);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aún no hay sesiones');
    (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();
    expect(createRequested).toHaveBeenCalledOnce();
  });

  it('keeps card expansion separate from edit and delete actions', async () => {
    await TestBed.configureTestingModule({ imports: [PatientSessionTimelineComponent] }).compileComponents();
    const fixture = TestBed.createComponent(PatientSessionTimelineComponent);
    fixture.componentInstance.sessions = [session];
    const expansionRequested = vi.fn();
    const editRequested = vi.fn();
    const deleteRequested = vi.fn();
    fixture.componentInstance.expansionRequested.subscribe(expansionRequested);
    fixture.componentInstance.editRequested.subscribe(editRequested);
    fixture.componentInstance.deleteRequested.subscribe(deleteRequested);
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('[aria-label="Editar sesión"]') as HTMLButtonElement).click();
    (fixture.nativeElement.querySelector('[aria-label="Eliminar sesión"]') as HTMLButtonElement).click();
    expect(editRequested).toHaveBeenCalledWith(7);
    expect(deleteRequested).toHaveBeenCalledWith(7);
    expect(expansionRequested).not.toHaveBeenCalled();

    (fixture.nativeElement.querySelector('.card') as HTMLElement).click();
    expect(expansionRequested).toHaveBeenCalledWith(7);

    fixture.componentInstance.expandedSessionId = 7;
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Dolor leve');
  });
});
