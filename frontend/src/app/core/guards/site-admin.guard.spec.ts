import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { siteAdminGuard } from './site-admin.guard';
import { UserService } from '../services/user.service';
import { ToastService } from '../../shared/services/toast/toast.service';
import { UserProfile } from '../models/user-profile.model';

describe('siteAdminGuard', () => {
  let userService: jasmine.SpyObj<UserService>;
  let router: jasmine.SpyObj<Router>;
  let toastService: jasmine.SpyObj<ToastService>;

  beforeEach(() => {
    userService = jasmine.createSpyObj<UserService>('UserService', ['getCurrentUserProfile']);
    router = jasmine.createSpyObj<Router>('Router', ['parseUrl']);
    toastService = jasmine.createSpyObj<ToastService>('ToastService', ['show']);

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
    userService.getCurrentUserProfile.and.returnValue(of({ roles: ['ROLE_SITE_ADMIN'] } as UserProfile));

    let allowed: boolean | UrlTree | undefined;
    runGuard().subscribe(value => allowed = value);

    expect(allowed).toBeTrue();
    expect(toastService.show).not.toHaveBeenCalled();
    expect(router.parseUrl).not.toHaveBeenCalled();
  });

  it('rechaza el acceso sin ROLE_SITE_ADMIN y redirige al dashboard', () => {
    userService.getCurrentUserProfile.and.returnValue(of({ roles: ['ROLE_STAFF'] } as UserProfile));
    const urlTree = {} as UrlTree;
    router.parseUrl.and.returnValue(urlTree);

    let allowed: boolean | UrlTree | undefined;
    runGuard().subscribe(value => allowed = value);

    expect(allowed).toBe(urlTree);
    expect(toastService.show).toHaveBeenCalled();
    expect(router.parseUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('rechaza el acceso si falla la carga del perfil', () => {
    userService.getCurrentUserProfile.and.returnValue(throwError(() => new Error('no autorizado')));
    const urlTree = {} as UrlTree;
    router.parseUrl.and.returnValue(urlTree);

    let allowed: boolean | UrlTree | undefined;
    runGuard().subscribe(value => allowed = value);

    expect(allowed).toBe(urlTree);
  });
});
