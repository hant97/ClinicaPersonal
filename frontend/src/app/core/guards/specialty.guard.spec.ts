import type { MockedObject } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { specialtyGuard } from './specialty.guard';
import { SpecialtyService } from '../services/specialty.service';

describe('specialtyGuard', () => {
  let specialtyService: MockedObject<SpecialtyService>;
  let router: MockedObject<Router>;

  beforeEach(() => {
    specialtyService = {
      getSpecialty: vi.fn().mockName('SpecialtyService.getSpecialty')
    } as unknown as MockedObject<SpecialtyService>;
    router = {
      navigate: vi.fn().mockName('Router.navigate')
    } as unknown as MockedObject<Router>;
    TestBed.configureTestingModule({
      providers: [
        { provide: SpecialtyService, useValue: specialtyService },
        { provide: Router, useValue: router }
      ]
    });
  });

  it('permite la navegación a una ruta dermatológica al dermatólogo', () => {
    specialtyService.getSpecialty.mockReturnValue('DERMATOLOGIA');

    const allowed = TestBed.runInInjectionContext(() => specialtyGuard('DERMATOLOGIA')({} as never, {} as never));

    expect(allowed).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('rechaza la navegación dermatológica para otra especialidad', () => {
    specialtyService.getSpecialty.mockReturnValue('PSICOLOGIA');

    const allowed = TestBed.runInInjectionContext(() => specialtyGuard('DERMATOLOGIA')({} as never, {} as never));

    expect(allowed).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });
});
