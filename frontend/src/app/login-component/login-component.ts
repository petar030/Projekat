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
    this.authService.login(this.username, this.password).subscribe({
      next: (response: any) => {
        //localStorage.setItem('token', response.accessToken);
        //this.router.navigate(['/home']);
        console.log(JSON.stringify(response, null, 2));
        this.errorMessage = 'Uspesno prijavljivanje! (Token je ispisan u konzoli)';
      },
      error: (err) => {
        this.errorMessage = err.error.message || 'Greška prilikom prijavljivanja';
      }
    });
  }
}
