import { inject, Injectable } from '@angular/core';
import { HttpClient } from "@angular/common/http";

export interface PasswordResetRequestResponse {
  token: string;
  tokenExpiresAt: string;
  expiresInSeconds: number;
}

export interface RegisterMemberRequest {
  username: string;
  password: string;
  ime: string;
  prezime: string;
  telefon: string;
  email: string;
}

export interface FirmaRequest {
  naziv: string;
  adresa: string;
  maticniBroj: string;
  pib: string;
}

export interface RegisterManagerRequest {
  username: string;
  password: string;
  ime: string;
  prezime: string;
  telefon: string;
  email: string;
  firma: FirmaRequest;
}

export interface RegistrationResponse {
  id: number;
  username: string;
  role: string;
  status: string;
  message: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {

  private apiUrl = 'http://localhost:8080/api/auth';
  private client = inject(HttpClient);

  login(username: string, password: string) {
    return this.client.post(`${this.apiUrl}/login`, { username, password });
  }

  admin_login(username: string, password: string) {
    return this.client.post(`${this.apiUrl}/admin/login`, { username, password });
  }

  reset_request(usernameOrEmail: string) {
    return this.client.post<PasswordResetRequestResponse>(`${this.apiUrl}/password-reset/request`, { usernameOrEmail });
  }

  reset_confirm(token: string, newPassword: string) {
    return this.client.post(`${this.apiUrl}/password-reset/confirm`, { token, newPassword });
  }

  register_member(data: RegisterMemberRequest, profileImage?: File) {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (profileImage) {
      formData.append('profileImage', profileImage);
    }
    return this.client.post<RegistrationResponse>(`${this.apiUrl}/register/member`, formData);
  }

  register_manager(data: RegisterManagerRequest, profileImage?: File) {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (profileImage) {
      formData.append('profileImage', profileImage);
    }
    return this.client.post<RegistrationResponse>(`${this.apiUrl}/register/manager`, formData);
  }



}
