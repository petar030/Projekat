import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ProfileComponent } from '../profile-component/profile-component';
import { ManagerReservationItem, ManagerSpaceItem } from '../models/manager/manager-models';
import { ManagerService } from '../services/manager/manager-service';

@Component({
  selector: 'app-manager-component',
  imports: [CommonModule, ProfileComponent],
  templateUrl: './manager-component.html',
  styleUrl: './manager-component.css',
})
export class ManagerComponent implements OnInit {

  private managerService = inject(ManagerService);
  private router = inject(Router);

  spaces: ManagerSpaceItem[] = [];
  spacesLoading: boolean = false;
  spacesError: string = '';

  reservations: ManagerReservationItem[] = [];
  reservationsLoading: boolean = false;
  reservationsError: string = '';
  reservationsSuccess: string = '';

  ngOnInit(): void {
    this.load_spaces();
    this.load_reservations();
  }

  load_spaces(): void {
    this.spacesLoading = true;
    this.spacesError = '';

    this.managerService.spaces().subscribe({
      next: (response) => {
        this.spaces = response.spaces ?? [];
        this.spacesLoading = false;
      },
      error: (err) => {
        this.spacesError = err?.error?.message ?? 'Neuspesno ucitavanje prostora menadzera.';
        this.spacesLoading = false;
      }
    });
  }

  open_update(spaceId?: number): void {
    if (!spaceId) {
      return;
    }

    this.router.navigate(['/manager_update', spaceId]);
  }

  open_add(): void {
    this.router.navigate(['/manager_add']);
  }

  load_reservations(): void {
    this.reservationsLoading = true;
    this.reservationsError = '';

    this.managerService.reservations().subscribe({
      next: (response) => {
        this.reservations = response.content ?? [];
        this.reservationsLoading = false;
      },
      error: (err) => {
        this.reservationsError = err?.error?.message ?? 'Neuspesno ucitavanje rezervacija.';
        this.reservationsLoading = false;
      }
    });
  }

  confirm_reservation(reservationId?: number): void {
    if (!reservationId) {
      return;
    }

    const reservation = this.reservations.find(item => item.id === reservationId);
    if (!this.can_manage_on_front(reservation)) {
      this.reservationsError = 'Potvrda je dozvoljena samo u prvih 10 minuta od pocetka aktivne rezervacije.';
      return;
    }

    this.reservationsError = '';
    this.reservationsSuccess = '';

    this.managerService.confirm_reservation(reservationId).subscribe({
      next: () => {
        this.reservationsSuccess = 'Rezervacija je potvrdjena.';
        this.load_reservations();
      },
      error: (err) => {
        this.reservationsError = err?.error?.message ?? 'Neuspesna potvrda rezervacije.';
      }
    });
  }

  no_show_reservation(reservationId?: number): void {
    if (!reservationId) {
      return;
    }

    const reservation = this.reservations.find(item => item.id === reservationId);
    if (!this.can_manage_on_front(reservation)) {
      this.reservationsError = 'Odjavljivanje je dozvoljeno samo u prvih 10 minuta od pocetka aktivne rezervacije.';
      return;
    }

    this.reservationsError = '';
    this.reservationsSuccess = '';

    this.managerService.no_show_reservation(reservationId).subscribe({
      next: () => {
        this.reservationsSuccess = 'Clan je odjavljen (nepojavljivanje).';
        this.load_reservations();
      },
      error: (err) => {
        this.reservationsError = err?.error?.message ?? 'Neuspesno odjavljivanje clana.';
      }
    });
  }

  format_datetime_local(value?: string): string {
    const parsed = this.parse_backend_utc(value);
    if (!parsed) {
      return '';
    }

    return parsed.toLocaleString('sr-RS', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    });
  }

  can_manage_on_front(reservation?: ManagerReservationItem): boolean {
    if (!reservation) {
      return false;
    }

    if (reservation.status !== 'aktivna') {
      return false;
    }

    const start = this.parse_backend_utc(reservation.from);
    if (!start) {
      return false;
    }

    const now = new Date();
    const tenMinutesAfterStart = new Date(start.getTime() + (10 * 60 * 1000));
    return now >= start && now <= tenMinutesAfterStart;
  }

  private parse_backend_utc(value?: string): Date | null {
    if (!value) {
      return null;
    }

    const normalized = value.replace(' ', 'T');
    const hasOffset = /[zZ]|[+-]\d{2}:\d{2}$/.test(normalized);
    const candidate = hasOffset ? normalized : `${normalized}Z`;
    const parsed = new Date(candidate);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  }

}
