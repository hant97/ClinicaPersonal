import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

/** El token solo se envía a la API propia, nunca a otros dominios pedidos con HttpClient. */
const isApiRequest = (url: string): boolean => url.startsWith(`${environment.apiUrl}/`);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!isApiRequest(req.url)) {
    return next(req);
  }

  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  let authReq = req;
  if (token) {
    authReq = req.clone({
      headers: req.headers.set('Authorization', `Bearer ${token}`)
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/v1/auth/login') && !req.url.includes('/v1/auth/refresh')) {
        return authService.refresh().pipe(
          switchMap(() => next(req.clone({ headers: req.headers.set('Authorization', `Bearer ${authService.getToken()}`) }))),
          catchError(refreshError => {
            authService.clearSession();
            router.navigate(['/login']);
            return throwError(() => refreshError);
          })
        );
      }
      return throwError(() => error);
    })
  );
};
