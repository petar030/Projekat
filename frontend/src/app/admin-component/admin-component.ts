import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProfileComponent } from '../profile-component/profile-component';
import {
  AdminRegistrationRequestItem,
  AdminSpaceRequestItem,
  AdminUpdateUserRequest,
  AdminUserItem
} from '../models/admin/admin-models';
import { AdminService } from '../services/admin/admin-service';

@Component({
  selector: 'app-admin-component',
  imports: [CommonModule, FormsModule, ProfileComponent],
  templateUrl: './admin-component.html',
  styleUrl: './admin-component.css',
})
export class AdminComponent implements OnInit {

  private adminService = inject(AdminService);

  users: AdminUserItem[] = [];
  usersLoading: boolean = false;

  registrationRequests: AdminRegistrationRequestItem[] = [];
  registrationLoading: boolean = false;

  spaceRequests: AdminSpaceRequestItem[] = [];
  spaceLoading: boolean = false;

  search: string = '';
  roleFilter: string = '';
  statusFilter: string = '';

  updateUserId?: number;
  updateIme: string = '';
  updatePrezime: string = '';
  updateTelefon: string = '';
  updateEmail: string = '';
  updateStatus: 'na_cekanju' | 'odobren' | 'odbijen' = 'na_cekanju';

  errorMessage: string = '';
  successMessage: string = '';

  ngOnInit(): void {
    this.reload_all();
  }

  reload_all(): void {
    this.load_users();
    this.load_registration_requests();
    this.load_space_requests();
  }

  load_users(): void {
    this.usersLoading = true;
    this.adminService.users(this.roleFilter || undefined, this.statusFilter || undefined, this.search || undefined).subscribe({
      next: (response) => {
        this.users = response.content ?? [];
        this.usersLoading = false;
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Neuspesno ucitavanje korisnika.';
        this.usersLoading = false;
      }
    });
  }

  load_registration_requests(): void {
    this.registrationLoading = true;
    this.adminService.registration_requests().subscribe({
      next: (response) => {
        this.registrationRequests = response.requests ?? [];
        this.registrationLoading = false;
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Neuspesno ucitavanje zahteva za registraciju.';
        this.registrationLoading = false;
      }
    });
  }

  load_space_requests(): void {
    this.spaceLoading = true;
    this.adminService.space_requests().subscribe({
      next: (response) => {
        this.spaceRequests = response.requests ?? [];
        this.spaceLoading = false;
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Neuspesno ucitavanje zahteva za prostore.';
        this.spaceLoading = false;
      }
    });
  }

  open_update(user: AdminUserItem): void {
    this.updateUserId = user.id;
    this.updateIme = user.ime ?? '';
    this.updatePrezime = user.prezime ?? '';
    this.updateTelefon = user.telefon ?? '';
    this.updateEmail = user.email ?? '';
    this.updateStatus = ((user.status ?? 'na_cekanju') as 'na_cekanju' | 'odobren' | 'odbijen');
  }

  cancel_update(): void {
    this.updateUserId = undefined;
  }

  save_update(): void {
    if (!this.updateUserId) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    const request: AdminUpdateUserRequest = {
      ime: this.updateIme,
      prezime: this.updatePrezime,
      telefon: this.updateTelefon,
      email: this.updateEmail,
      status: this.updateStatus
    };

    this.adminService.update_user(this.updateUserId, request).subscribe({
      next: () => {
        this.successMessage = 'Korisnik je uspesno azuriran.';
        this.updateUserId = undefined;
        this.reload_all();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Neuspesno azuriranje korisnika.';
      }
    });
  }

  delete_user(userId?: number): void {
    if (!userId) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.adminService.delete_user(userId).subscribe({
      next: () => {
        this.successMessage = 'Korisnik je uspesno obrisan.';
        this.reload_all();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Neuspesno brisanje korisnika.';
      }
    });
  }

  approve_registration(userId?: number): void {
    if (!userId) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.adminService.approve_registration_request(userId).subscribe({
      next: () => {
        this.successMessage = 'Zahtev za registraciju je odobren.';
        this.reload_all();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Neuspesno odobravanje registracije.';
      }
    });
  }

  reject_registration(userId?: number): void {
    if (!userId) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.adminService.reject_registration_request(userId).subscribe({
      next: () => {
        this.successMessage = 'Zahtev za registraciju je odbijen.';
        this.reload_all();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Neuspesno odbijanje registracije.';
      }
    });
  }

  approve_space(spaceId?: number): void {
    if (!spaceId) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.adminService.approve_space_request(spaceId).subscribe({
      next: () => {
        this.successMessage = 'Prostor je odobren.';
        this.reload_all();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Neuspesno odobravanje prostora.';
      }
    });
  }

  reject_space(spaceId?: number): void {
    if (!spaceId) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.adminService.reject_space_request(spaceId).subscribe({
      next: () => {
        this.successMessage = 'Prostor je odbijen.';
        this.reload_all();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Neuspesno odbijanje prostora.';
      }
    });
  }

}
