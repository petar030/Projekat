import { Component, inject } from '@angular/core';
import { AuthService } from '../services/auth/auth-service';
import { Router } from '@angular/router';
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
  private router = inject(Router);
  username: string = '';
  password: string = '';
  errorMessage: string = '';


  login(){
    this.errorMessage = '';

    this.authService.login(this.username, this.password).subscribe({
      next: (response: any) => {
        localStorage.setItem('userToken', response.accessToken);
        if (response?.user?.username) {
          localStorage.setItem('userUsername', response.user.username);
        }
        if (response?.user?.id !== undefined && response?.user?.id !== null) {
          localStorage.setItem('userId', String(response.user.id));
        }
        if(response.user.role === 'clan'){
          this.router.navigate(['/member']);
        } else if(response.user.role === 'menadzer'){
          //console.log("Prijavljen menadzer, id: " + response.user.id);
          this.router.navigate(['/manager']);
        }
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
