import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SpecialtyService } from '../services/specialty.service';
import { ToastService } from '../../shared/services/toast/toast.service';

export const specialtyGuard = (allowedSpecialty: 'PSICOLOGIA' | 'DERMATOLOGIA'): CanActivateFn => {
  return (route, state) => {
    const specialtyService = inject(SpecialtyService);
    const router = inject(Router);
    const toastService = inject(ToastService);

    if (specialtyService.getSpecialty() === allowedSpecialty) {
      return true;
    }

    toastService.show('No tienes acceso a esta sección', 'info');
    router.navigate(['/dashboard']);
    return false;
  };
};
