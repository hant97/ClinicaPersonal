import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { UserService } from '../services/user.service';
import { ToastService } from '../../shared/services/toast/toast.service';
import { ROLES } from '../models/roles';

/** Rutas con información clínica: solo para usuarios con rol de profesional de salud. */
export const professionalGuard: CanActivateFn = (_route, _state) => {
  const userService = inject(UserService);
  const router = inject(Router);
  const toastService = inject(ToastService);

  const deny = () => {
    toastService.show('La información clínica solo está disponible para profesionales de salud', 'info');
    return router.parseUrl('/dashboard');
  };

  return userService.getCurrentUserProfile().pipe(
    map(profile => (profile.roles?.includes(ROLES.PROFESIONAL) ? true : deny())),
    catchError(() => of(deny()))
  );
};
