import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import {
  ManagerCreateSpaceRequest,
  ManagerCreateSpaceResponse,
  ManagerCreateMeetingRoomRequest,
  ManagerCreateOfficeRequest,
  ManagerMeetingRoomResponse,
  ManagerOfficeResponse,
  ManagerReservationsResponse,
  ManagerReservationStatusResponse,
  ManagerSpacesResponse
} from '../../models/manager/manager-models';

@Injectable({
  providedIn: 'root',
})
export class ManagerService {

  private apiUrl = 'http://localhost:8080/api/manager';
  private client = inject(HttpClient);

  spaces() {
    return this.client.get<ManagerSpacesResponse>(`${this.apiUrl}/spaces`, { headers: this.auth_headers() });
  }

  reservations() {
    return this.client.get<ManagerReservationsResponse>(`${this.apiUrl}/reservations`, { headers: this.auth_headers() });
  }

  confirm_reservation(reservationId: number) {
    return this.client.patch<ManagerReservationStatusResponse>(`${this.apiUrl}/reservations/${reservationId}/confirm`, {}, { headers: this.auth_headers() });
  }

  no_show_reservation(reservationId: number) {
    return this.client.patch<ManagerReservationStatusResponse>(`${this.apiUrl}/reservations/${reservationId}/no-show`, {}, { headers: this.auth_headers() });
  }

  add_office(spaceId: number, data: ManagerCreateOfficeRequest) {
    return this.client.post<ManagerOfficeResponse>(`${this.apiUrl}/spaces/${spaceId}/offices`, data, { headers: this.auth_headers() });
  }

  add_meeting_room(spaceId: number, data: ManagerCreateMeetingRoomRequest) {
    return this.client.post<ManagerMeetingRoomResponse>(`${this.apiUrl}/spaces/${spaceId}/meeting-rooms`, data, { headers: this.auth_headers() });
  }

  create_space(data: ManagerCreateSpaceRequest, images?: File[]) {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    (images ?? []).forEach(image => formData.append('images', image));

    return this.client.post<ManagerCreateSpaceResponse>(`${this.apiUrl}/spaces`, formData, { headers: this.auth_headers() });
  }

  import_space_json(payload: unknown, images?: File[]) {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
    (images ?? []).forEach(image => formData.append('images', image));

    return this.client.post<ManagerCreateSpaceResponse>(`${this.apiUrl}/spaces`, formData, { headers: this.auth_headers() });
  }

  private auth_headers(): HttpHeaders {
    const token = localStorage.getItem('userToken') ?? '';
    return new HttpHeaders({
      Authorization: `Bearer ${token}`
    });
  }
}
