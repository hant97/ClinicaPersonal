import type { MockedObject } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { Observable, of } from 'rxjs';

import { professionalGuard } from './professional.guard';
import { UserService } from '../services/user.service';
import { ToastService } from '../../shared/services/toast/toast.service';
import { UserProfile } from '../models/user-profile.model';

describe('professionalGuard', () => {
  let userService: MockedObject<UserService>;
  let router: MockedObject<Router>;
  let toastService: MockedObject<ToastService>;

  beforeEach(() => {
    userService = {
      getCurrentUserProfile: vi.fn().mockName('UserService.getCurrentUserProfile')
    } as unknown as MockedObject<UserService>;
    router = {
      parseUrl: vi.fn().mockName('Router.parseUrl')
    } as unknown as MockedObject<Router>;
    toastService = {
      show: vi.fn().mockName('ToastService.show')
    } as unknown as MockedObject<ToastService>;

    TestBed.configureTestingModule({
      providers: [
        { provide: UserService, useValue: userService },
        { provide: Router, useValue: router },
        { provide: ToastService, useValue: toastService }
      ]
    });
  });

  function runGuard(): Observable<boolean | UrlTree> {
    return TestBed.runInInjectionContext(() => professionalGuard({} as never, {} as never)) as Observable<boolean | UrlTree>;
  }

  it('permite el acceso a un profesional, también si además es administrador', () => {
    userService.getCurrentUserProfile.mockReturnValue(of({ roles: ['ROLE_PROFESIONAL', 'ROLE_ADMIN'] } as UserProfile));

    let allowed: boolean | UrlTree | undefined;
    runGuard().subscribe(value => allowed = value);

    expect(allowed).toBe(true);
  });

  it.each([['ROLE_ASISTENTE'], ['ROLE_ADMIN'], ['ROLE_SITE_ADMIN']])(
    'redirige al dashboard a quien solo tiene %s',
    role => {
      userService.getCurrentUserProfile.mockReturnValue(of({ roles: [role] } as UserProfile));
      const urlTree = {} as UrlTree;
      router.parseUrl.mockReturnValue(urlTree);

      let allowed: boolean | UrlTree | undefined;
      runGuard().subscribe(value => allowed = value);

      expect(allowed).toBe(urlTree);
      expect(router.parseUrl).toHaveBeenCalledWith('/dashboard');
      expect(toastService.show).toHaveBeenCalled();
    }
  );
});
