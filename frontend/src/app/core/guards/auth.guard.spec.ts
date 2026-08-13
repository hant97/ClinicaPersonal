import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Observable, firstValueFrom, of, throwError } from 'rxjs';

import { authGuard } from './auth.guard';
import { AuthResponse, AuthService } from '../services/auth.service';

describe('authGuard', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['getToken', 'refresh']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router }
      ]
    });
  });

  it('permite navegar inmediatamente cuando existe un access token', () => {
    authService.getToken.and.returnValue('access-token');

    const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));

    expect(result).toBeTrue();
    expect(authService.refresh).not.toHaveBeenCalled();
  });

  it('recupera la sesión mediante refresh cuando no hay access token', async () => {
    authService.getToken.and.returnValue(null);
    authService.refresh.and.returnValue(of({ token: 'renewed-token' }));

    const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));

    await expectAsync(firstValueFrom(result as Observable<boolean>)).toBeResolvedTo(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('redirige al login cuando no puede recuperar la sesión', async () => {
    authService.getToken.and.returnValue(null);
    authService.refresh.and.returnValue(
      throwError(() => new Error('Refresh rechazado')) as Observable<AuthResponse>
    );

    const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));

    await expectAsync(firstValueFrom(result as Observable<boolean>)).toBeResolvedTo(false);
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
