import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { FullCalendarModule } from '@fullcalendar/angular';
import { CalendarOptions, EventInput } from '@fullcalendar/core';
import timeGridPlugin from '@fullcalendar/timegrid';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { CookieService } from 'ngx-cookie-service';
import { PublicSpaceDetailsResponse } from '../models/public/public-models';
import { MemberAvailabilityResource, MemberCommentItem } from '../models/member/member-models';
import { MemberService } from '../services/member/member-service';
import { PublicService } from '../services/public/public-service';

@Component({
  selector: 'app-member-details-component',
  imports: [CommonModule, FormsModule, FullCalendarModule],
  providers: [CookieService],
  templateUrl: './member-details-component.html',
  styleUrl: './member-details-component.css'
})
export class MemberDetailsComponent implements OnInit {

  private route = inject(ActivatedRoute);
  private publicService = inject(PublicService);
  private memberService = inject(MemberService);
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
  weekStart: string = '';
  availabilityResources: MemberAvailabilityResource[] = [];
  currentResourceIndex: number = 0;
  availabilityLoading: boolean = false;
  availabilityError: string = '';
  selectedDate: string = '';
  fromTime: string = '';
  toTime: string = '';
  reservationMessage: string = '';
  reservationError: string = '';
  reactionMessage: string = '';
  reactionError: string = '';
  comments: MemberCommentItem[] = [];
  commentsLoading: boolean = false;
  commentsError: string = '';
  commentText: string = '';
  commentMessage: string = '';
  commentError: string = '';
  private readonly cookiePrefix = 'member_space_main_image_';
  private currentWeekEnd: string = '';
  private reservationWeekEndInclusive: string = '';
  calendarOptions: CalendarOptions = {
    plugins: [timeGridPlugin],
    initialView: 'timeGridWeek',
    timeZone: 'UTC',
    headerToolbar: false,
    firstDay: 1,
    allDaySlot: false,
    selectable: false,
    slotDuration: '01:00:00',
    slotLabelInterval: '01:00:00',
    slotMinTime: '06:00:00',
    slotMaxTime: '23:00:00',
    editable: false,
    eventStartEditable: false,
    eventDurationEditable: false,
    events: [],
    validRange: {
      start: '',
      end: ''
    }
  };

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

    this.weekStart = this.currentWeekMonday();
    this.currentWeekEnd = this.addDays(this.weekStart, 7);
    this.reservationWeekEndInclusive = this.addDays(this.weekStart, 6);
    this.selectedDate = this.weekStart;
    this.calendarOptions = {
      ...this.calendarOptions,
      initialDate: this.weekStart,
      validRange: {
        start: this.weekStart,
        end: this.currentWeekEnd
      }
    };

    this.publicService.space_details(this.spaceId).subscribe({
      next: (response) => {
        this.details = response;
        this.selectedImageIndex = this.getSavedImageIndex(this.spaceId!, response.images?.length || 0);
        this.mapUrl = this.buildMapUrl(response.geolocation?.lat, response.geolocation?.lng);
        this.loading = false;
        this.loadAvailability();
        this.loadComments();
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

  loadAvailability(): void {
    if (!this.spaceId || this.matchingSubspaceIds.length === 0) {
      this.availabilityResources = [];
      this.updateCalendarEvents();
      return;
    }

    this.availabilityLoading = true;
    this.availabilityError = '';
    const selectedResourceId = this.currentResource()?.resourceId;

    this.memberService.availability(this.spaceId, {
      type: this.selectedType,
      resourceIds: this.matchingSubspaceIds,
      weekStart: this.weekStart
    }).subscribe({
      next: (response) => {
        console.log('[member-details] availability response:', response);
        this.availabilityResources = response.resources ?? [];
        if (selectedResourceId) {
          const selectedIndex = this.availabilityResources.findIndex(item => item.resourceId === selectedResourceId);
          this.currentResourceIndex = selectedIndex >= 0 ? selectedIndex : 0;
        } else {
          this.currentResourceIndex = 0;
        }
        console.log('[member-details] selected resource busySlots:', this.currentResource()?.busySlots ?? []);
        this.updateCalendarEvents();
        this.availabilityLoading = false;
      },
      error: (err) => {
        this.availabilityError = err?.error?.message ?? 'Greska pri ucitavanju zauzetosti.';
        this.updateCalendarEvents();
        this.availabilityLoading = false;
      }
    });
  }

  previousResource(): void {
    if (this.availabilityResources.length <= 1) {
      return;
    }

    this.currentResourceIndex = (this.currentResourceIndex - 1 + this.availabilityResources.length) % this.availabilityResources.length;
    this.updateCalendarEvents();
  }

  nextResource(): void {
    if (this.availabilityResources.length <= 1) {
      return;
    }

    this.currentResourceIndex = (this.currentResourceIndex + 1) % this.availabilityResources.length;
    this.updateCalendarEvents();
  }

  currentResource(): MemberAvailabilityResource | null {
    if (this.availabilityResources.length === 0) {
      return null;
    }

    return this.availabilityResources[this.currentResourceIndex] ?? null;
  }

  reserve(): void {
    this.reservationMessage = '';
    this.reservationError = '';

    const resource = this.currentResource();
    if (!this.spaceId || !resource?.resourceId) {
      this.reservationError = 'Nema izabranog resursa za rezervaciju.';
      return;
    }

    if (!this.selectedDate || !this.fromTime || !this.toTime) {
      this.reservationError = 'Datum i vreme od-do su obavezni.';
      return;
    }

    if (!this.isDateInDisplayedWeek(this.selectedDate)) {
      this.reservationError = 'Datum rezervacije mora biti u prikazanoj sedmici.';
      return;
    }

    const from = `${this.selectedDate}T${this.fromTime}:00`;
    const to = `${this.selectedDate}T${this.toTime}:00`;

    this.memberService.create_reservation({
      spaceId: this.spaceId,
      type: this.selectedType,
      resourceId: resource.resourceId,
      from,
      to
    }).subscribe({
      next: () => {
        this.reservationMessage = 'Rezervacija je uspesno kreirana.';
        this.loadAvailability();
      },
      error: (err) => {
        this.reservationError = err?.error?.message ?? 'Neuspesno kreiranje rezervacije.';
      }
    });
  }

  react(tip: 'svidjanje' | 'nesvidjanje'): void {
    if (!this.spaceId) {
      return;
    }

    this.reactionMessage = '';
    this.reactionError = '';

    this.memberService.create_reaction(this.spaceId, { tip }).subscribe({
      next: () => {
        this.reactionMessage = 'Reakcija je uspesno sacuvana.';
        this.reloadSpaceDetails();
      },
      error: (err) => {
        this.reactionError = err?.error?.message ?? 'Neuspesno cuvanje reakcije.';
      }
    });
  }

  loadComments(): void {
    if (!this.spaceId) {
      return;
    }

    this.commentsLoading = true;
    this.commentsError = '';

    this.memberService.latest_comments(this.spaceId, 10).subscribe({
      next: (response) => {
        this.comments = response.comments ?? [];
        this.commentsLoading = false;
      },
      error: (err) => {
        this.commentsError = err?.error?.message ?? 'Neuspesno ucitavanje komentara.';
        this.commentsLoading = false;
      }
    });
  }

  sendComment(): void {
    if (!this.spaceId) {
      return;
    }

    this.commentMessage = '';
    this.commentError = '';

    const text = this.commentText.trim();
    if (!text) {
      this.commentError = 'Komentar je obavezan.';
      return;
    }

    this.memberService.create_comment(this.spaceId, { text }).subscribe({
      next: () => {
        this.commentMessage = 'Komentar je uspesno sacuvan.';
        this.commentText = '';
        this.loadComments();
      },
      error: (err) => {
        this.commentError = err?.error?.message ?? 'Neuspesno slanje komentara.';
      }
    });
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

  private reloadSpaceDetails(): void {
    if (!this.spaceId) {
      return;
    }

    this.publicService.space_details(this.spaceId).subscribe({
      next: (response) => {
        this.details = response;
      },
      error: () => {}
    });
  }

  private currentWeekMonday(): string {
    const now = new Date();
    const day = now.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    now.setDate(now.getDate() + diff);
    return now.toISOString().slice(0, 10);
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

  private updateCalendarEvents(): void {
    const busySlots = this.currentResource()?.busySlots ?? [];
    const events: EventInput[] = busySlots
      .filter(slot => !!slot.from && !!slot.to)
      .map(slot => ({
        title: 'Zauzeto',
        start: this.toUtcWallClockDate(slot.from),
        end: this.toUtcWallClockDate(slot.to)
      }));

    this.calendarOptions = {
      ...this.calendarOptions,
      events
    };
  }

  private toUtcWallClockDate(value?: string): Date | undefined {
    if (!value) {
      return undefined;
    }

    const [datePart, timePart = '00:00:00'] = value.split('T');
    const [yearString, monthString, dayString] = datePart.split('-');
    const [hourString, minuteString, secondString = '0'] = timePart.split(':');

    const year = Number(yearString);
    const month = Number(monthString);
    const day = Number(dayString);
    const hour = Number(hourString);
    const minute = Number(minuteString);
    const second = Number(secondString);

    if ([year, month, day, hour, minute, second].some(Number.isNaN)) {
      return undefined;
    }

    return new Date(Date.UTC(year, month - 1, day, hour, minute, second));
  }

  private addDays(isoDate: string, days: number): string {
    const value = new Date(`${isoDate}T00:00:00`);
    value.setDate(value.getDate() + days);
    return value.toISOString().slice(0, 10);
  }

  private isDateInDisplayedWeek(value: string): boolean {
    if (!value) {
      return false;
    }

    return value >= this.weekStart && value <= this.reservationWeekEndInclusive;
  }

  reservationDateMin(): string {
    return this.weekStart;
  }

  reservationDateMax(): string {
    return this.reservationWeekEndInclusive;
  }

}
