import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import {
  UpdatePasswordRequest,
  UpdateProfileImageResponse,
  UpdateUserProfileRequest,
  UserProfileResponse
} from '../../models/user/user-models';

@Injectable({
  providedIn: 'root',
})
export class UserService {

  private backendBaseUrl = 'http://localhost:8080';
  private apiUrl = 'http://localhost:8080/api/users';
  private client = inject(HttpClient);

  me() {
    return this.client.get<UserProfileResponse>(`${this.apiUrl}/me`, { headers: this.auth_headers() });
  }

  update_profile(data: UpdateUserProfileRequest) {
    return this.client.put<UserProfileResponse>(`${this.apiUrl}/me`, data, { headers: this.auth_headers() });
  }

  update_profile_image(profileImage: File) {
    const formData = new FormData();
    formData.append('profileImage', profileImage);
    return this.client.put<UpdateProfileImageResponse>(`${this.apiUrl}/me/profile-image`, formData, { headers: this.auth_headers() });
  }

  update_password(data: UpdatePasswordRequest) {
    return this.client.put(`${this.apiUrl}/me/password`, data, { headers: this.auth_headers() });
  }

  image_url(path: string | null | undefined): string {
    if (!path) {
      return '';
    }

    if (path.startsWith('http://') || path.startsWith('https://')) {
      return path;
    }

    if (path.startsWith('/')) {
      return `${this.backendBaseUrl}${path}`;
    }

    return `${this.backendBaseUrl}/${path}`;
  }

  private auth_headers(): HttpHeaders {
    const token = localStorage.getItem('userToken') ?? '';
    return new HttpHeaders({
      Authorization: `Bearer ${token}`
    });
  }
}
