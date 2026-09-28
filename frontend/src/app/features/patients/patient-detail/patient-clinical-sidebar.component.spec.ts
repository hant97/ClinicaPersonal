import { TestBed } from '@angular/core/testing';
import { PatientClinicalSidebarComponent } from './patient-clinical-sidebar.component';

describe('PatientClinicalSidebarComponent', () => {
  it('shows administrative details without clinical widgets for nonprofessionals', async () => {
    await TestBed.configureTestingModule({ imports: [PatientClinicalSidebarComponent] }).compileComponents();
    const fixture = TestBed.createComponent(PatientClinicalSidebarComponent);
    fixture.componentInstance.patient = {
      id: 42,
      firstName: 'Ana',
      lastName: 'Pérez',
      identificationDocument: '12345678',
      contactNumber: '999999999'
    } as PatientClinicalSidebarComponent['patient'];
    fixture.componentInstance.activeAlerts = [{ id: 1, type: 'ALERGIA', level: 'ALTA', description: 'Penicilina' }] as PatientClinicalSidebarComponent['activeAlerts'];
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Ana Pérez');
    expect(fixture.nativeElement.textContent).not.toContain('Alertas Activas');
    (fixture.nativeElement.querySelector('[aria-expanded="false"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('999999999');

    fixture.componentInstance.isProfessional = true;
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Alertas Activas');
  });
});
