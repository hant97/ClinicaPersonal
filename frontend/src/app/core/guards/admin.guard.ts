import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { UserService } from '../services/user.service';
import { ToastService } from '../../shared/services/toast/toast.service';

export const adminGuard: CanActivateFn = (_route, _state) => {
  const userService = inject(UserService);
  const router = inject(Router);
  const toastService = inject(ToastService);

  const deny = () => {
    toastService.show('Acceso restringido a administradores', 'info');
    return router.parseUrl('/dashboard');
  };

  return userService.getCurrentUserProfile().pipe(
    map(profile => {
      const isAdmin = profile.roles?.some(r => r === 'ROLE_ADMIN' || r === 'ROLE_SITE_ADMIN');
      return isAdmin ? true : deny();
    }),
    catchError(() => of(deny()))
  );
};
