import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { UserService } from '../services/user.service';
import { ToastService } from '../../shared/services/toast/toast.service';

export const siteAdminGuard: CanActivateFn = (_route, _state) => {
  const userService = inject(UserService);
  const router = inject(Router);
  const toastService = inject(ToastService);

  const deny = () => {
    toastService.show('No tienes permisos de administrador', 'info');
    return router.parseUrl('/dashboard');
  };

  return userService.getCurrentUserProfile().pipe(
    map(profile => {
      const hasAdmin = profile.roles?.some(r => r === 'ROLE_SITE_ADMIN' || r === 'ROLE_ADMIN');
      return hasAdmin ? true : deny();
    }),
    catchError(() => of(deny()))
  );
};
