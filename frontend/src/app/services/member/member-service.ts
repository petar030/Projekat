import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { MemberCancelReservationResponse, MemberReservationsResponse } from '../../models/member/member-models';

@Injectable({
  providedIn: 'root',
})
export class MemberService {

  private apiUrl = 'http://localhost:8080/api/member';
  private client = inject(HttpClient);

  reservations() {
    return this.client.get<MemberReservationsResponse>(`${this.apiUrl}/reservations`, {
      headers: this.auth_headers()
    });
  }

  cancel_reservation(reservationId: number) {
    return this.client.patch<MemberCancelReservationResponse>(
      `${this.apiUrl}/reservations/${reservationId}/cancel`,
      {},
      { headers: this.auth_headers() }
    );
  }

  private auth_headers(): HttpHeaders {
    const token = localStorage.getItem('userToken') ?? '';
    return new HttpHeaders({
      Authorization: `Bearer ${token}`
    });
  }
}