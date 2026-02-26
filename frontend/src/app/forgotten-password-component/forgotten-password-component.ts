import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, PasswordResetRequestResponse } from '../services/auth/auth-service';

@Component({
  selector: 'app-forgotten-password-component',
  imports: [CommonModule, FormsModule],
  templateUrl: './forgotten-password-component.html',
  styleUrl: './forgotten-password-component.css',
})
export class ForgottenPasswordComponent {

  private authService = inject(AuthService);
  private router = inject(Router);

  usernameOrEmail: string = '';
  errorMessage: string = '';

  requestReset() {
    this.errorMessage = '';
    this.authService.reset_request(this.usernameOrEmail).subscribe({
      next: (response: PasswordResetRequestResponse) => {
        this.router.navigate(['/recovery', response.token], {
          queryParams: {
            expiresAt: response.tokenExpiresAt
          }
        });
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Neuspešan zahtev za reset lozinke.';
      }
    });
  }

}
