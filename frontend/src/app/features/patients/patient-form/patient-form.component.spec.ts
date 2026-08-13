import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { PatientFormComponent } from './patient-form.component';
import { SpecialtyService } from '../../../core/services/specialty.service';

describe('PatientFormComponent', () => {
  let component: PatientFormComponent;
  let fixture: ComponentFixture<PatientFormComponent>;
  let specialtyService: jasmine.SpyObj<SpecialtyService>;

  beforeEach(async () => {
    const specialtySpy = jasmine.createSpyObj('SpecialtyService', [
      'isDermatology',
      'isPsychology',
      'getSpecialty'
    ]);
    specialtySpy.isDermatology.and.returnValue(false);

    await TestBed.configureTestingModule({
      imports: [PatientFormComponent, HttpClientTestingModule],
      providers: [{ provide: SpecialtyService, useValue: specialtySpy }]
    })
    .compileComponents();

    specialtyService = TestBed.inject(SpecialtyService) as jasmine.SpyObj<SpecialtyService>;
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
    specialtyService.isDermatology.and.returnValue(true);

    fixture = TestBed.createComponent(PatientFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.isDermatology).toBeTrue();
    expect(fixture.nativeElement.textContent).not.toContain('Estado Civil');
    expect(fixture.nativeElement.textContent).not.toContain('Dirección');
    expect(fixture.nativeElement.textContent).not.toContain('Contacto de Emergencia');
  });

  it('muestra secciones no clínicas para otros perfiles', () => {
    expect(component.isDermatology).toBeFalse();
    expect(fixture.nativeElement.textContent).toContain('Estado Civil');
    expect(fixture.nativeElement.textContent).toContain('Dirección');
    expect(fixture.nativeElement.textContent).toContain('Contacto de Emergencia');
  });
});
