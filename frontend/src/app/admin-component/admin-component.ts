import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { ProfileComponent } from '../profile-component/profile-component';
import {
  AdminRegistrationRequestItem,
  AdminSpaceMonthlyItem,
  AdminSpaceRequestItem,
  AdminStatSpaceItem,
  AdminUpdateUserRequest,
  AdminUserItem
} from '../models/admin/admin-models';
import { AdminService } from '../services/admin/admin-service';

@Component({
  selector: 'app-admin-component',
  imports: [CommonModule, FormsModule, ProfileComponent, BaseChartDirective],
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

  statsSpaces: AdminStatSpaceItem[] = [];
  statsSpaceId?: number;
  statsSpaceName: string = '';
  statsItems: AdminSpaceMonthlyItem[] = [];
  statsLoading: boolean = false;
  totalRevenue: number = 0;
  statsCurrency: string = 'RSD';

  reactionsChartType: 'bar' = 'bar';
  reactionsChartData: ChartData<'bar'> = { labels: [], datasets: [] };
  reactionsChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    scales: {
      x: { stacked: false },
      y: { beginAtZero: true }
    }
  };

  reservationsChartType: 'bar' = 'bar';
  reservationsChartData: ChartData<'bar'> = { labels: [], datasets: [] };
  reservationsChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    scales: {
      y: { beginAtZero: true }
    }
  };

  revenueChartType: 'line' = 'line';
  revenueChartData: ChartData<'line'> = { labels: [], datasets: [] };
  revenueChartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    scales: {
      y: { beginAtZero: true }
    }
  };

  statsYear: number = 2026;

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
    this.load_stats_spaces();
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

  load_stats_spaces(): void {
    this.statsLoading = true;
    this.adminService.stats_spaces().subscribe({
      next: (response) => {
        this.statsSpaces = response.spaces ?? [];
        if (!this.statsSpaceId && this.statsSpaces.length > 0) {
          this.statsSpaceId = this.statsSpaces[0].spaceId;
        }
        this.load_stats();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Neuspesno ucitavanje prostora za statistiku.';
        this.statsLoading = false;
      }
    });
  }

  load_stats(): void {
    if (!this.statsYear || this.statsYear < 2000 || this.statsYear > 2100) {
      this.errorMessage = 'Neispravan interval za statistiku.';
      this.statsLoading = false;
      return;
    }

    if (!this.statsSpaceId) {
      this.errorMessage = 'Izaberi prostor za statistiku.';
      this.statsLoading = false;
      return;
    }

    this.errorMessage = '';
    this.statsLoading = true;

    this.adminService.space_monthly_stats(this.statsSpaceId, this.statsYear).subscribe({
      next: (response) => {
        this.statsItems = response.items ?? [];
        this.statsSpaceName = response.spaceName ?? '';
        this.statsCurrency = this.statsItems[0]?.currency ?? 'RSD';
        this.totalRevenue = this.statsItems.reduce((sum, item) => sum + (item.revenue ?? 0), 0);
        this.rebuild_reactions_chart();
        this.rebuild_reservations_chart();
        this.rebuild_revenue_chart();
        this.statsLoading = false;
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Neuspesno ucitavanje statistike.';
        this.statsLoading = false;
      }
    });
  }

  month_label(month?: number): string {
    const labels = ['Jan', 'Feb', 'Mar', 'Apr', 'Maj', 'Jun', 'Jul', 'Avg', 'Sep', 'Okt', 'Nov', 'Dec'];
    const index = (month ?? 1) - 1;
    return labels[index] ?? String(month ?? '');
  }

  private rebuild_reactions_chart(): void {
    const labels = Array.from({ length: 12 }, (_, index) => this.month_label(index + 1));
    const likes = this.statsItems.map((item) => item.likes ?? 0);
    const dislikes = this.statsItems.map((item) => item.dislikes ?? 0);

    this.reactionsChartData = {
      labels,
      datasets: [
        {
          data: likes,
          label: 'Like'
        },
        {
          data: dislikes,
          label: 'Dislike'
        }
      ]
    };
  }

  private rebuild_reservations_chart(): void {
    const labels = Array.from({ length: 12 }, (_, index) => this.month_label(index + 1));
    const reservations = this.statsItems.map((item) => item.reservations ?? 0);

    this.reservationsChartData = {
      labels,
      datasets: [
        {
          data: reservations,
          label: 'Rezervacije'
        }
      ]
    };
  }

  private rebuild_revenue_chart(): void {
    const labels = Array.from({ length: 12 }, (_, index) => this.month_label(index + 1));
    const values = this.statsItems.map((item) => item.revenue ?? 0);

    this.revenueChartData = {
      labels,
      datasets: [
        {
          data: values,
          label: `Prihod (${this.statsCurrency})`
        }
      ]
    };
  }

}
