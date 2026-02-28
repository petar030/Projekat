import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth/auth-service';

@Component({
  selector: 'app-admin-login-component',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-login-component.html',
  styleUrl: './admin-login-component.css',
})
export class AdminLoginComponent {

  private authService = inject(AuthService);

  username: string = '';
  password: string = '';
  errorMessage: string = '';

  login() {
    this.errorMessage = '';

    this.authService.admin_login(this.username, this.password).subscribe({
      next: (response: any) => {
        this.authService.save_session(response);
        this.authService.navigate_for_role(response?.user?.role);
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
