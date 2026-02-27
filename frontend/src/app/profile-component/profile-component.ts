import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../services/user/user-service';
import { UpdatePasswordRequest, UpdateUserProfileRequest, UserProfileResponse } from '../models/user/user-models';
import { Router } from '@angular/router';

@Component({
  selector: 'app-profile-component',
  imports: [CommonModule, FormsModule],
  templateUrl: './profile-component.html',
  styleUrl: './profile-component.css'
})
export class ProfileComponent implements OnInit {

  private userService = inject(UserService);
  private router = inject(Router);

  profile: UserProfileResponse | null = null;
  ime: string = '';
  prezime: string = '';
  telefon: string = '';
  email: string = '';

  selectedProfileImage?: File;
  profileImageInfo: string = '';

  newPassword: string = '';
  confirmNewPassword: string = '';

  errorMessage: string = '';
  successMessage: string = '';

  private readonly minImageWidth = 100;
  private readonly minImageHeight = 100;
  private readonly maxImageWidth = 300;
  private readonly maxImageHeight = 300;

  ngOnInit(): void {
    this.load_profile();
  }

  load_profile(): void {
    this.errorMessage = '';
    this.successMessage = '';

    this.userService.me().subscribe({
      next: (response) => {
        this.profile = response;
        this.ime = response.ime ?? '';
        this.prezime = response.prezime ?? '';
        this.telefon = response.telefon ?? '';
        this.email = response.email ?? '';
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Neuspesno ucitavanje profila.';
        this.router.navigate(['/login']);
      }
    });
  }

  update_profile(): void {
    this.errorMessage = '';
    this.successMessage = '';

    const payload: UpdateUserProfileRequest = {
      ime: this.ime,
      prezime: this.prezime,
      telefon: this.telefon,
      email: this.email
    };

    this.userService.update_profile(payload).subscribe({
      next: (response) => {
        this.profile = response;
        this.successMessage = 'Profil je uspesno azuriran.';
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Neuspesno azuriranje profila.';
      }
    });
  }

  async on_file_selected(event: Event): Promise<void> {
    this.errorMessage = '';
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file) {
      this.selectedProfileImage = undefined;
      this.profileImageInfo = '';
      return;
    }

    const allowedTypes = ['image/jpeg', 'image/png'];
    if (!allowedTypes.includes(file.type)) {
      this.selectedProfileImage = undefined;
      this.profileImageInfo = '';
      this.errorMessage = 'Dozvoljeni formati slike su JPG i PNG.';
      input.value = '';
      return;
    }

    const dimensionsValid = await this.validate_image_dimensions(file);
    if (!dimensionsValid) {
      this.selectedProfileImage = undefined;
      this.profileImageInfo = '';
      input.value = '';
      return;
    }

    this.selectedProfileImage = file;
    this.profileImageInfo = `${file.name} (${Math.round(file.size / 1024)} KB)`;
  }

  update_profile_image(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.selectedProfileImage) {
      this.errorMessage = 'Prvo izaberi sliku.';
      return;
    }

    this.userService.update_profile_image(this.selectedProfileImage).subscribe({
      next: (response) => {
        if (this.profile && response.profileImage) {
          this.profile.profilnaSlika = response.profileImage;
        }
        this.selectedProfileImage = undefined;
        this.profileImageInfo = '';
        this.successMessage = 'Profilna slika je uspesno azurirana.';
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Neuspesna promena profilne slike.';
      }
    });
  }

  update_password(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.newPassword || !this.confirmNewPassword) {
      this.errorMessage = 'Nova lozinka i potvrda lozinke su obavezne.';
      return;
    }

    if (this.newPassword !== this.confirmNewPassword) {
      this.errorMessage = 'Nova lozinka i potvrda lozinke moraju biti iste.';
      return;
    }

    const payload: UpdatePasswordRequest = {
      newPassword: this.newPassword
    };

    this.userService.update_password(payload).subscribe({
      next: (response: any) => {
        this.newPassword = '';
        this.confirmNewPassword = '';
        this.successMessage = response?.message ?? 'Lozinka je uspesno promenjena.';
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Neuspesna promena lozinke.';
      }
    });
  }

  profile_image_url(): string {
    return this.userService.image_url(this.profile?.profilnaSlika);
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
