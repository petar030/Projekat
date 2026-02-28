import { Component, inject } from '@angular/core';
import { AuthService } from '../services/auth/auth-service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';


@Component({
  selector: 'app-login-component',
  imports: [CommonModule, FormsModule],
  templateUrl: './login-component.html',
  styleUrl: './login-component.css',
})
export class LoginComponent {

  private authService = inject(AuthService);
  username: string = '';
  password: string = '';
  errorMessage: string = '';


  login(){
    this.errorMessage = '';

    this.authService.login(this.username, this.password).subscribe({
      next: (response: any) => {
        this.authService.save_session(response);
        this.authService.navigate_for_role(response?.user?.role);
      },
      error: (err) => {
        if (err?.status === 0) {
          this.errorMessage = 'Backend nije dostupan. Proveri da li je server pokrenut.';
          return;
        }

        this.errorMessage = err?.error?.message ?? 'Greska prilikom prijavljivanja';
      }
    });
  }
}
