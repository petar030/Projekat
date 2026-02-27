import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import {
  PublicCitiesResponse,
  PublicHomeResponse,
  PublicSearchRequest,
  PublicSearchResponse,
  PublicSpaceDetailsResponse
} from '../../models/public/public-models';

@Injectable({
  providedIn: 'root',
})
export class PublicService {

  private backendBaseUrl = 'http://localhost:8080';
  private apiUrl = 'http://localhost:8080/api/public';
  private client = inject(HttpClient);

  home() {
    return this.client.get<PublicHomeResponse>(`${this.apiUrl}/home`);
  }

  cities() {
    return this.client.get<PublicCitiesResponse>(`${this.apiUrl}/spaces/cities`);
  }

  search_spaces(request: PublicSearchRequest = {}) {
    let params = new HttpParams();

    if (request.name) {
      params = params.set('name', request.name);
    }

    if (request.sortBy) {
      params = params.set('sortBy', request.sortBy);
    }

    if (request.sortDir) {
      params = params.set('sortDir', request.sortDir);
    }

    if (request.cities && request.cities.length > 0) {
      request.cities.forEach(city => {
        params = params.append('cities', city);
      });
    }

    return this.client.get<PublicSearchResponse>(`${this.apiUrl}/spaces`, { params });
  }

  space_details(spaceId: number) {
    return this.client.get<PublicSpaceDetailsResponse>(`${this.apiUrl}/spaces/${spaceId}`);
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
}
