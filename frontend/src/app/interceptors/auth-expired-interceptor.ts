import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const authExpiredInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const hasAuthHeader = req.headers.has('Authorization');
      const isAuthError = error.status === 401 || error.status === 403;

      if (hasAuthHeader && isAuthError) {
        localStorage.removeItem('userToken');
        localStorage.removeItem('userUsername');
        localStorage.removeItem('userId');
        //router.navigate(['/']);
      }

      return throwError(() => error);
    })
  );
};
