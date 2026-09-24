import type { MockedObject } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Observable, firstValueFrom, of, throwError } from 'rxjs';

import { authGuard } from './auth.guard';
import { AuthResponse, AuthService } from '../services/auth.service';

describe('authGuard', () => {
  let authService: MockedObject<AuthService>;
  let router: MockedObject<Router>;

  beforeEach(() => {
    authService = {
      getToken: vi.fn().mockName('AuthService.getToken'),
      refresh: vi.fn().mockName('AuthService.refresh')
    } as unknown as MockedObject<AuthService>;
    router = {
      navigate: vi.fn().mockName('Router.navigate')
    } as unknown as MockedObject<Router>;
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router }
      ]
    });
  });

  it('permite navegar inmediatamente cuando existe un access token', () => {
    authService.getToken.mockReturnValue('access-token');

    const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));

    expect(result).toBe(true);
    expect(authService.refresh).not.toHaveBeenCalled();
  });

  it('recupera la sesión mediante refresh cuando no hay access token', async () => {
    authService.getToken.mockReturnValue(null);
    authService.refresh.mockReturnValue(of({ token: 'renewed-token' }));

    const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));

    await expect(firstValueFrom(result as Observable<boolean>)).resolves.toEqual(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('redirige al login cuando no puede recuperar la sesión', async () => {
    authService.getToken.mockReturnValue(null);
    authService.refresh.mockReturnValue(throwError(() => new Error('Refresh rechazado')) as Observable<AuthResponse>);

    const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));

    await expect(firstValueFrom(result as Observable<boolean>)).resolves.toEqual(false);
    expect(router.navigate).toHaveBeenCalledWith(['/login'], expect.anything());
  });
});
