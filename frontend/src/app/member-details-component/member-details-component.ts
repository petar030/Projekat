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
    timeZone: 'local',
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
        this.availabilityResources = response.resources ?? [];
        if (selectedResourceId) {
          const selectedIndex = this.availabilityResources.findIndex(item => item.resourceId === selectedResourceId);
          this.currentResourceIndex = selectedIndex >= 0 ? selectedIndex : 0;
        } else {
          this.currentResourceIndex = 0;
        }
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

    const from = this.toUtcDateTimeString(this.selectedDate, this.fromTime);
    const to = this.toUtcDateTimeString(this.selectedDate, this.toTime);

    if (!from || !to) {
      this.reservationError = 'Neispravan datum ili vreme rezervacije.';
      return;
    }

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

  private toUtcDateTimeString(datePart: string, timePart: string): string | null {
    if (!datePart || !timePart) {
      return null;
    }

    const localDate = new Date(`${datePart}T${timePart}:00`);
    if (Number.isNaN(localDate.getTime())) {
      return null;
    }

    return localDate.toISOString().slice(0, 19);
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

  format_datetime_local(value?: string): string {
    const parsed = this.parse_backend_utc(value);
    if (!parsed) {
      return '';
    }

    return parsed.toLocaleString('sr-RS', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
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
    return this.toLocalDateIso(now);
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
      .map(slot => {
        const start = this.parse_backend_utc(slot.from);
        const end = this.parse_backend_utc(slot.to);
        if (!start || !end) {
          return null;
        }

        return {
          title: 'Zauzeto',
          start,
          end
        } as EventInput;
      })
      .filter((event): event is EventInput => event !== null);

    this.calendarOptions = {
      ...this.calendarOptions,
      events
    };
  }

  private addDays(isoDate: string, days: number): string {
    const value = new Date(`${isoDate}T00:00:00`);
    value.setDate(value.getDate() + days);
    return this.toLocalDateIso(value);
  }

  private toLocalDateIso(value: Date): string {
    const year = value.getFullYear();
    const month = String(value.getMonth() + 1).padStart(2, '0');
    const day = String(value.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
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

  private parse_backend_utc(value?: string): Date | null {
    if (!value) {
      return null;
    }

    const normalized = value.replace(' ', 'T');
    const hasOffset = /[zZ]|[+-]\d{2}:\d{2}$/.test(normalized);
    const candidate = hasOffset ? normalized : `${normalized}Z`;
    const parsed = new Date(candidate);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  }

}
