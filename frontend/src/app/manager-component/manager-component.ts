import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ProfileComponent } from '../profile-component/profile-component';
import { ManagerReservationItem, ManagerSpaceItem } from '../models/manager/manager-models';
import { ManagerService } from '../services/manager/manager-service';

@Component({
  selector: 'app-manager-component',
  imports: [CommonModule, FormsModule, ProfileComponent],
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

  reportSpaceId?: number;
  reportYear: number;
  reportMonthNumber: number;
  reportYears: number[] = [];
  readonly reportMonths: Array<{ value: number; label: string }> = [
    { value: 1, label: 'Januar' },
    { value: 2, label: 'Februar' },
    { value: 3, label: 'Mart' },
    { value: 4, label: 'April' },
    { value: 5, label: 'Maj' },
    { value: 6, label: 'Jun' },
    { value: 7, label: 'Jul' },
    { value: 8, label: 'Avgust' },
    { value: 9, label: 'Septembar' },
    { value: 10, label: 'Oktobar' },
    { value: 11, label: 'Novembar' },
    { value: 12, label: 'Decembar' }
  ];
  reportLoading: boolean = false;
  reportError: string = '';
  reportSuccess: string = '';

  constructor() {
    const now = new Date();
    this.reportYear = now.getFullYear();
    this.reportMonthNumber = now.getMonth() + 1;
    for (let year = this.reportYear; year >= this.reportYear - 5; year--) {
      this.reportYears.push(year);
    }
  }

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
        if (!this.reportSpaceId) {
          this.reportSpaceId = this.spaces[0]?.id;
        }
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

  open_calendar(): void {
    this.router.navigate(['/manager_calendar']);
  }

  generate_report(): void {
    this.reportError = '';
    this.reportSuccess = '';

    if (!this.reportSpaceId || !this.reportYear || !this.reportMonthNumber) {
      this.reportError = 'Prostor, godina i mesec su obavezni.';
      return;
    }

    const year = this.reportYear;
    const month = this.reportMonthNumber;
    if (!year || !month || month < 1 || month > 12) {
      this.reportError = 'Neispravan format meseca.';
      return;
    }

    this.reportLoading = true;
    this.managerService.occupancy_report(this.reportSpaceId, year, month).subscribe({
      next: (response) => {
        const fileName = this.file_name_from_header(response.headers.get('Content-Disposition'))
          ?? `occupancy-space-${this.reportSpaceId}-${year}-${String(month).padStart(2, '0')}.pdf`;

        const blob = response.body;
        if (!blob) {
          this.reportError = 'Prazan odgovor servera.';
          this.reportLoading = false;
          return;
        }

        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);

        this.reportSuccess = 'PDF izvestaj je uspesno generisan.';
        this.reportLoading = false;
      },
      error: (err) => {
        this.reportError = err?.error?.message ?? 'Neuspesno generisanje izvestaja.';
        this.reportLoading = false;
      }
    });
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

  private file_name_from_header(contentDisposition?: string | null): string | null {
    if (!contentDisposition) {
      return null;
    }

    const match = /filename="?([^";]+)"?/i.exec(contentDisposition);
    return match?.[1] ?? null;
  }

}
