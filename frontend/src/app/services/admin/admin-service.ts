import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import {
  AdminRegistrationRequestsResponse,
  AdminSpaceRequestsResponse,
  AdminSpaceStatusResponse,
  AdminUpdateUserRequest,
  AdminUserStatusResponse,
  AdminUsersResponse
} from '../../models/admin/admin-models';

@Injectable({
  providedIn: 'root',
})
export class AdminService {

  private apiUrl = 'http://localhost:8080/api/admin';
  private client = inject(HttpClient);

  users(role?: string, status?: string, search?: string, sortBy: string = 'id', sortDir: 'asc' | 'desc' = 'asc') {
    let params = new HttpParams().set('sortBy', sortBy).set('sortDir', sortDir);
    if (role) {
      params = params.set('role', role);
    }
    if (status) {
      params = params.set('status', status);
    }
    if (search) {
      params = params.set('search', search);
    }

    return this.client.get<AdminUsersResponse>(`${this.apiUrl}/users`, {
      headers: this.auth_headers(),
      params
    });
  }

  update_user(userId: number, request: AdminUpdateUserRequest) {
    return this.client.put<AdminUserStatusResponse>(`${this.apiUrl}/users/${userId}`, request, { headers: this.auth_headers() });
  }

  delete_user(userId: number) {
    return this.client.delete(`${this.apiUrl}/users/${userId}`, { headers: this.auth_headers() });
  }

  registration_requests() {
    return this.client.get<AdminRegistrationRequestsResponse>(`${this.apiUrl}/registration-requests`, { headers: this.auth_headers() });
  }

  approve_registration_request(userId: number) {
    return this.client.patch<AdminUserStatusResponse>(`${this.apiUrl}/registration-requests/${userId}/approve`, {}, { headers: this.auth_headers() });
  }

  reject_registration_request(userId: number, reason: string = '') {
    return this.client.patch<AdminUserStatusResponse>(`${this.apiUrl}/registration-requests/${userId}/reject`, { reason }, { headers: this.auth_headers() });
  }

  space_requests() {
    return this.client.get<AdminSpaceRequestsResponse>(`${this.apiUrl}/space-requests`, { headers: this.auth_headers() });
  }

  approve_space_request(spaceId: number) {
    return this.client.patch<AdminSpaceStatusResponse>(`${this.apiUrl}/space-requests/${spaceId}/approve`, {}, { headers: this.auth_headers() });
  }

  reject_space_request(spaceId: number, reason: string = '') {
    return this.client.patch<AdminSpaceStatusResponse>(`${this.apiUrl}/space-requests/${spaceId}/reject`, { reason }, { headers: this.auth_headers() });
  }

  private auth_headers(): HttpHeaders {
    const token = localStorage.getItem('userToken') ?? '';
    return new HttpHeaders({
      Authorization: `Bearer ${token}`
    });
  }
}
