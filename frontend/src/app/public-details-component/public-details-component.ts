import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CookieService } from 'ngx-cookie-service';
import { PublicSpaceDetailsResponse } from '../models/public/public-models';
import { PublicService } from '../services/public/public-service';

@Component({
  selector: 'app-public-details-component',
  imports: [CommonModule],
  providers: [CookieService],
  templateUrl: './public-details-component.html',
  styleUrl: './public-details-component.css',
})
export class PublicDetailsComponent implements OnInit {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private publicService = inject(PublicService);
  private cookieService = inject(CookieService);

  loading: boolean = true;
  errorMessage: string = '';
  details: PublicSpaceDetailsResponse | null = null;
  selectedImageIndex: number = 0;
  private spaceId: number | null = null;

  private readonly cookiePrefix = 'public_space_main_image_';

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = Number(idParam);

    if (!id || Number.isNaN(id)) {
      this.errorMessage = 'Neispravan id prostora.';
      this.loading = false;
      return;
    }

    this.spaceId = id;

    this.publicService.space_details(id).subscribe({
      next: (response) => {
        this.details = response;
        this.selectedImageIndex = this.getSavedImageIndex(id, response.images?.length || 0);
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Greska pri ucitavanju detalja prostora.';
        this.loading = false;
      }
    });
  }

  selectImage(index: number): void {
    this.selectedImageIndex = index;

    if (this.spaceId !== null) {
      this.saveImageIndex(this.spaceId, index);
    }
  }

  selectedImageUrl(): string {
    const images = this.details?.images || [];
    if (images.length === 0) {
      return '';
    }

    const safeIndex = Math.max(0, Math.min(this.selectedImageIndex, images.length - 1));
    return this.publicService.image_url(images[safeIndex]);
  }

  toImageUrl(path: string | undefined): string {
    return this.publicService.image_url(path);
  }

  backToStart(): void {
    this.router.navigate(['/']);
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

}
