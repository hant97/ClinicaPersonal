import type { MockedObject } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { PatientFormComponent } from './patient-form.component';
import { SpecialtyService } from '../../../core/services/specialty.service';

describe('PatientFormComponent', () => {
  let component: PatientFormComponent;
  let fixture: ComponentFixture<PatientFormComponent>;
  let specialtyService: MockedObject<SpecialtyService>;

  beforeEach(async () => {
    const specialtySpy = {
      isDermatology: vi.fn().mockName('SpecialtyService.isDermatology'),
      isPsychology: vi.fn().mockName('SpecialtyService.isPsychology'),
      getSpecialty: vi.fn().mockName('SpecialtyService.getSpecialty')
    };
    specialtySpy.isDermatology.mockReturnValue(false);

    await TestBed.configureTestingModule({
      imports: [PatientFormComponent, HttpClientTestingModule],
      providers: [
        provideRouter([]),
        { provide: SpecialtyService, useValue: specialtySpy }
      ]
    })
      .compileComponents();

    specialtyService = TestBed.inject(SpecialtyService) as MockedObject<SpecialtyService>;
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PatientFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('oculta secciones no clínicas para el perfil dermatólogo', () => {
    specialtyService.isDermatology.mockReturnValue(true);

    fixture = TestBed.createComponent(PatientFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.isDermatology).toBe(true);
    expect(fixture.nativeElement.textContent).not.toContain('Estado Civil');
    expect(fixture.nativeElement.textContent).not.toContain('Dirección');
    expect(fixture.nativeElement.textContent).not.toContain('Contacto de Emergencia');
  });

  it('muestra secciones no clínicas para otros perfiles', () => {
    expect(component.isDermatology).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Estado Civil');
    expect(fixture.nativeElement.textContent).toContain('Dirección');
    expect(fixture.nativeElement.textContent).toContain('Contacto de Emergencia');
  });
});
