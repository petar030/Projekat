import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth/auth-service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-admin-login-component',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-login-component.html',
  styleUrl: './admin-login-component.css',
})
export class AdminLoginComponent {

  private authService = inject(AuthService);
  private router = inject(Router);

  username: string = '';
  password: string = '';
  errorMessage: string = '';

  login() {
    this.errorMessage = '';

    this.authService.admin_login(this.username, this.password).subscribe({
      next: (response: any) => {
        localStorage.setItem('userToken', response.accessToken);
        this.router.navigate(['/admin']);
      },
      error: (err) => {
        if (err?.status === 0) {
          this.errorMessage = 'Backend nije dostupan.';
          return;
        }

        this.errorMessage = err?.error?.message ?? 'Neuspesna admin prijava.';
      }
    });
  }

}
