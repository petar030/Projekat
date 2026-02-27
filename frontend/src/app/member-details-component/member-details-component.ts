import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { CookieService } from 'ngx-cookie-service';
import { PublicSpaceDetailsResponse } from '../models/public/public-models';
import { PublicService } from '../services/public/public-service';

@Component({
  selector: 'app-member-details-component',
  imports: [CommonModule],
  providers: [CookieService],
  templateUrl: './member-details-component.html'
})
export class MemberDetailsComponent implements OnInit {

  private route = inject(ActivatedRoute);
  private publicService = inject(PublicService);
  private sanitizer = inject(DomSanitizer);
  private cookieService = inject(CookieService);

  spaceId?: number;
  selectedType: 'otvoreni' | 'kancelarija' | 'sala' = 'otvoreni';
  matchingSubspaceIds: number[] = [];
  loading: boolean = true;
  errorMessage: string = '';
  details: PublicSpaceDetailsResponse | null = null;
  selectedImageIndex: number = 0;
  mapUrl: SafeResourceUrl | null = null;
  private readonly cookiePrefix = 'member_space_main_image_';

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    this.spaceId = idParam ? Number(idParam) : undefined;
    if (!this.spaceId || Number.isNaN(this.spaceId)) {
      this.errorMessage = 'Neispravan ID prostora.';
      this.loading = false;
      return;
    }

    const typeParam = this.route.snapshot.queryParamMap.get('type');
    if (typeParam === 'otvoreni' || typeParam === 'kancelarija' || typeParam === 'sala') {
      this.selectedType = typeParam;
    }

    const childIdsParam = this.route.snapshot.queryParamMap.get('childIds') ?? '';
    this.matchingSubspaceIds = childIdsParam
      .split(',')
      .map(value => Number(value.trim()))
      .filter(value => Number.isInteger(value) && value > 0);

    this.publicService.space_details(this.spaceId).subscribe({
      next: (response) => {
        this.details = response;
        this.selectedImageIndex = this.getSavedImageIndex(this.spaceId!, response.images?.length || 0);
        this.mapUrl = this.buildMapUrl(response.geolocation?.lat, response.geolocation?.lng);
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Greska pri ucitavanju detalja prostora.';
        this.loading = false;
      }
    });
  }

  selectImage(index: number): void {
    this.selectedImageIndex = index;
    if (this.spaceId !== undefined) {
      this.saveImageIndex(this.spaceId, index);
    }
  }

  selectedImageUrl(): string {
    const images = this.details?.images ?? [];
    if (images.length === 0) {
      return '';
    }

    const safeIndex = Math.max(0, Math.min(this.selectedImageIndex, images.length - 1));
    return this.publicService.image_url(images[safeIndex]);
  }

  toImageUrl(path: string | undefined): string {
    return this.publicService.image_url(path);
  }

  private getSavedImageIndex(spaceId: number, imagesCount: number): number {
    if (imagesCount <= 0) {
      return 0;
    }

    const cookieValue = this.cookieService.get(this.cookieName(spaceId));
    const parsed = Number(cookieValue);

    if (Number.isInteger(parsed) && parsed >= 0 && parsed < imagesCount) {
      return parsed;
    }

    return 0;
  }

  private saveImageIndex(spaceId: number, index: number): void {
    this.cookieService.set(this.cookieName(spaceId), String(index), 30, '/', undefined, false, 'Lax');
  }

  private cookieName(spaceId: number): string {
    return `${this.cookiePrefix}${spaceId}`;
  }

  private buildMapUrl(lat?: number, lng?: number): SafeResourceUrl | null {
    if (lat === undefined || lng === undefined || lat === null || lng === null) {
      return null;
    }

    const delta = 0.01;
    const minLng = lng - delta;
    const minLat = lat - delta;
    const maxLng = lng + delta;
    const maxLat = lat + delta;

    const mapUrl = `https://www.openstreetmap.org/export/embed.html?bbox=${minLng}%2C${minLat}%2C${maxLng}%2C${maxLat}&layer=mapnik&marker=${lat}%2C${lng}`;
    return this.sanitizer.bypassSecurityTrustResourceUrl(mapUrl);
  }

}
