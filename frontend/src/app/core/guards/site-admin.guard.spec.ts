import type { MockedObject } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { siteAdminGuard } from './site-admin.guard';
import { UserService } from '../services/user.service';
import { ToastService } from '../../shared/services/toast/toast.service';
import { UserProfile } from '../models/user-profile.model';

describe('siteAdminGuard', () => {
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
    return TestBed.runInInjectionContext(() => siteAdminGuard({} as never, {} as never)) as Observable<boolean | UrlTree>;
  }

  it('permite el acceso a un usuario con ROLE_SITE_ADMIN', () => {
    userService.getCurrentUserProfile.mockReturnValue(of({ roles: ['ROLE_SITE_ADMIN'] } as UserProfile));

    let allowed: boolean | UrlTree | undefined;
    runGuard().subscribe(value => allowed = value);

    expect(allowed).toBe(true);
    expect(toastService.show).not.toHaveBeenCalled();
    expect(router.parseUrl).not.toHaveBeenCalled();
  });

  it('rechaza el acceso sin ROLE_SITE_ADMIN y redirige al dashboard', () => {
    userService.getCurrentUserProfile.mockReturnValue(of({ roles: ['ROLE_STAFF'] } as UserProfile));
    const urlTree = {} as UrlTree;
    router.parseUrl.mockReturnValue(urlTree);

    let allowed: boolean | UrlTree | undefined;
    runGuard().subscribe(value => allowed = value);

    expect(allowed).toBe(urlTree);
    expect(toastService.show).toHaveBeenCalled();
    expect(router.parseUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('rechaza el acceso si falla la carga del perfil', () => {
    userService.getCurrentUserProfile.mockReturnValue(throwError(() => new Error('no autorizado')));
    const urlTree = {} as UrlTree;
    router.parseUrl.mockReturnValue(urlTree);

    let allowed: boolean | UrlTree | undefined;
    runGuard().subscribe(value => allowed = value);

    expect(allowed).toBe(urlTree);
  });
});
