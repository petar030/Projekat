import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import {
  MemberCancelReservationResponse,
  MemberReservationsResponse,
  MemberSearchRequest,
  MemberSearchResponse
} from '../../models/member/member-models';

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

  search_spaces(request: MemberSearchRequest) {
    let params = new HttpParams();

    if (request.name) {
      params = params.set('name', request.name);
    }

    if (request.cities && request.cities.length > 0) {
      request.cities.forEach(city => {
        params = params.append('cities', city);
      });
    }

    if (request.type) {
      params = params.set('type', request.type);
    }

    if (request.type === 'kancelarija' && request.officeMinDesks !== undefined && request.officeMinDesks !== null) {
      params = params.set('officeMinDesks', request.officeMinDesks);
    }

    return this.client.get<MemberSearchResponse>(`${this.apiUrl}/spaces`, {
      headers: this.auth_headers(),
      params
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