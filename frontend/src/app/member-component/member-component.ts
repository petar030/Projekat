import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProfileComponent } from '../profile-component/profile-component';
import { MemberReservationItem } from '../models/member/member-models';
import { MemberService } from '../services/member/member-service';

@Component({
  selector: 'app-member-component',
  imports: [CommonModule, FormsModule, ProfileComponent],
  templateUrl: './member-component.html',
  styleUrl: './member-component.css',
})
export class MemberComponent implements OnInit {

  private memberService = inject(MemberService);

  reservations: MemberReservationItem[] = [];
  reservationsError: string = '';
  reservationsSuccess: string = '';

  ngOnInit(): void {
    this.load_reservations();
  }

  load_reservations(): void {
    this.reservationsError = '';
    this.reservationsSuccess = '';
    this.memberService.reservations().subscribe({
      next: (response) => {
        this.reservations = response.content ?? [];
      },
      error: (err) => {
        this.reservationsError = err?.error?.message ?? 'Neuspesno ucitavanje rezervacija.';
      }
    });
  }

  cancel_reservation(reservation: MemberReservationItem): void {
    this.reservationsError = '';
    this.reservationsSuccess = '';

    if (!reservation.id) {
      this.reservationsError = 'Neispravan ID rezervacije.';
      return;
    }

    this.memberService.cancel_reservation(reservation.id).subscribe({
      next: () => {
        this.reservationsSuccess = 'Rezervacija je uspesno otkazana.';
        this.load_reservations();
      },
      error: (err) => {
        this.reservationsError = err?.error?.message ?? 'Neuspesno otkazivanje rezervacije.';
      }
    });
  }

}
