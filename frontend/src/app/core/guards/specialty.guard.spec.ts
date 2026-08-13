import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { specialtyGuard } from './specialty.guard';
import { SpecialtyService } from '../services/specialty.service';

describe('specialtyGuard', () => {
  let specialtyService: jasmine.SpyObj<SpecialtyService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    specialtyService = jasmine.createSpyObj<SpecialtyService>('SpecialtyService', ['getSpecialty']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    TestBed.configureTestingModule({
      providers: [
        { provide: SpecialtyService, useValue: specialtyService },
        { provide: Router, useValue: router }
      ]
    });
  });

  it('permite la navegación a una ruta dermatológica al dermatólogo', () => {
    specialtyService.getSpecialty.and.returnValue('DERMATOLOGIA');

    const allowed = TestBed.runInInjectionContext(() =>
      specialtyGuard('DERMATOLOGIA')({} as never, {} as never)
    );

    expect(allowed).toBeTrue();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('rechaza la navegación dermatológica para otra especialidad', () => {
    specialtyService.getSpecialty.and.returnValue('PSICOLOGIA');

    const allowed = TestBed.runInInjectionContext(() =>
      specialtyGuard('DERMATOLOGIA')({} as never, {} as never)
    );

    expect(allowed).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });
});
