import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  AuthService,
  FirmaRequest,
  RegisterManagerRequest,
  RegisterMemberRequest,
  RegistrationResponse
} from '../services/auth/auth-service';

@Component({
  selector: 'app-registration-component',
  imports: [CommonModule, FormsModule],
  templateUrl: './registration-component.html',
  styleUrl: './registration-component.css'
})
export class RegistrationComponent {

  private authService = inject(AuthService);
  private router = inject(Router);

  accountType: 'member' | 'manager' = 'member';

  username: string = '';
  password: string = '';
  ime: string = '';
  prezime: string = '';
  telefon: string = '';
  email: string = '';

  firma_naziv: string = '';
  firma_adresa: string = '';
  firma_maticni_broj: string = '';
  firma_pib: string = '';

  profileImage?: File;
  profileImageInfo: string = '';

  errorMessage: string = '';
  successMessage: string = '';

  private readonly minImageWidth = 100;
  private readonly minImageHeight = 100;
  private readonly maxImageWidth = 300;
  private readonly maxImageHeight = 300;

  async on_file_selected(event: Event): Promise<void> {
    this.errorMessage = '';
    const input = event.target as HTMLInputElement;
    const selectedFile = input.files?.[0];

    if (!selectedFile) {
      this.profileImage = undefined;
      this.profileImageInfo = '';
      return;
    }

    const allowedTypes = ['image/jpeg', 'image/png'];
    if (!allowedTypes.includes(selectedFile.type)) {
      this.profileImage = undefined;
      this.profileImageInfo = '';
      this.errorMessage = 'Dozvoljeni formati slike su JPG i PNG.';
      input.value = '';
      return;
    }

    const dimensionsValid = await this.validate_image_dimensions(selectedFile);
    if (!dimensionsValid) {
      this.profileImage = undefined;
      this.profileImageInfo = '';
      input.value = '';
      return;
    }

    this.profileImage = selectedFile;
    this.profileImageInfo = `${selectedFile.name} (${Math.round(selectedFile.size / 1024)} KB)`;
  }

  register(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (this.accountType === 'member') {
      const payload: RegisterMemberRequest = {
        username: this.username,
        password: this.password,
        ime: this.ime,
        prezime: this.prezime,
        telefon: this.telefon,
        email: this.email
      };

      this.authService.register_member(payload, this.profileImage).subscribe({
        next: (response: RegistrationResponse) => {
          this.successMessage = response.message;
          setTimeout(() => this.router.navigate(['/']), 1200);
        },
        error: (err) => {
          this.errorMessage = err?.error?.message ?? 'Registracija nije uspela.';
        }
      });
      return;
    }

    const firma: FirmaRequest = {
      naziv: this.firma_naziv,
      adresa: this.firma_adresa,
      maticniBroj: this.firma_maticni_broj,
      pib: this.firma_pib
    };

    const payload: RegisterManagerRequest = {
      username: this.username,
      password: this.password,
      ime: this.ime,
      prezime: this.prezime,
      telefon: this.telefon,
      email: this.email,
      firma
    };

    this.authService.register_manager(payload, this.profileImage).subscribe({
      next: (response: RegistrationResponse) => {
        this.successMessage = response.message;
        setTimeout(() => this.router.navigate(['/admin']), 1200);
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Registracija nije uspela.';
      }
    });
  }

  private validate_image_dimensions(file: File): Promise<boolean> {
    return new Promise((resolve) => {
      const image = new Image();
      const objectUrl = URL.createObjectURL(file);

      image.onload = () => {
        const width = image.width;
        const height = image.height;
        URL.revokeObjectURL(objectUrl);

        if (width < this.minImageWidth || height < this.minImageHeight) {
          this.errorMessage = `Slika mora biti najmanje ${this.minImageWidth}x${this.minImageHeight}px.`;
          resolve(false);
          return;
        }

        if (width > this.maxImageWidth || height > this.maxImageHeight) {
          this.errorMessage = `Slika može biti najviše ${this.maxImageWidth}x${this.maxImageHeight}px.`;
          resolve(false);
          return;
        }

        resolve(true);
      };

      image.onerror = () => {
        URL.revokeObjectURL(objectUrl);
        this.errorMessage = 'Neispravan format slike.';
        resolve(false);
      };

      image.src = objectUrl;
    });
  }
}