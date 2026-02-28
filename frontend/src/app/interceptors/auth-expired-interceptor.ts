import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth/auth-service';
import { catchError, throwError } from 'rxjs';

export const authExpiredInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const hasAuthHeader = req.headers.has('Authorization');
      const isAuthError = error.status === 401 || error.status === 403;

      if (hasAuthHeader && isAuthError) {
        authService.clear_session();
      }

      return throwError(() => error);
    })
  );
};
