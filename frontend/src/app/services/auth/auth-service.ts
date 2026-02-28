import { inject, Injectable } from '@angular/core';
import { HttpClient } from "@angular/common/http";
import { Router } from '@angular/router';

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
  private router = inject(Router);

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

  save_session(response: any): void {
    if (response?.accessToken) {
      localStorage.setItem('userToken', response.accessToken);
      localStorage.setItem('accessToken', response.accessToken);
    }

    if (response?.user?.username) {
      localStorage.setItem('userUsername', response.user.username);
    }

    if (response?.user?.id !== undefined && response?.user?.id !== null) {
      localStorage.setItem('userId', String(response.user.id));
    }

    if (response?.user?.role) {
      localStorage.setItem('userRole', response.user.role);
    }
  }

  clear_session(): void {
    localStorage.removeItem('userToken');
    localStorage.removeItem('accessToken');
    localStorage.removeItem('userUsername');
    localStorage.removeItem('userId');
    localStorage.removeItem('userRole');
  }

  logout(): void {
    this.clear_session();
    this.router.navigate(['/']);
  }

  route_for_role(role?: string | null): string {
    if (role === 'admin') {
      return '/admin';
    }
    if (role === 'menadzer') {
      return '/manager';
    }
    return '/member';
  }

  navigate_for_role(role?: string | null): void {
    this.router.navigate([this.route_for_role(role)]);
  }



}
