import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FullCalendarModule } from '@fullcalendar/angular';
import { CalendarOptions, EventDropArg, EventInput } from '@fullcalendar/core';
import interactionPlugin from '@fullcalendar/interaction';
import timeGridPlugin from '@fullcalendar/timegrid';
import { ManagerSpaceItem } from '../models/manager/manager-models';
import { ManagerService } from '../services/manager/manager-service';

@Component({
  selector: 'app-manager-calendar',
  imports: [CommonModule, FormsModule, FullCalendarModule],
  templateUrl: './manager-calendar.html',
  styleUrl: './manager-calendar.css',
})
export class ManagerCalendar implements OnInit {

  private managerService = inject(ManagerService);

  spaces: ManagerSpaceItem[] = [];
  spacesLoading: boolean = false;
  spacesError: string = '';

  selectedSpaceId?: number;
  selectedType: 'otvoreni' | 'kancelarija' | 'sala' = 'otvoreni';
  selectedResourceId?: number;
  from: string = '';
  to: string = '';

  resources: Array<{ id: number; label: string }> = [];

  calendarLoading: boolean = false;
  calendarError: string = '';

  calendarOptions: CalendarOptions = {
    plugins: [timeGridPlugin, interactionPlugin],
    initialView: 'timeGridWeek',
    timeZone: 'local',
    height: 'auto',
    contentHeight: 'auto',
    eventBackgroundColor: '#64748b',
    eventBorderColor: '#64748b',
    eventTextColor: '#ffffff',
    editable: true,
    dragScroll: false,
    eventStartEditable: true,
    eventDurationEditable: false,
    snapDuration: '00:01:00',
    slotDuration: '00:30:00',
    selectable: false,
    eventDrop: this.onEventDrop.bind(this),
    headerToolbar: {
      left: 'prev,next today',
      center: 'title',
      right: 'timeGridWeek,timeGridDay'
    },
    allDaySlot: false,
    events: []
  };

  ngOnInit(): void {
    if (this.isSmallScreen()) {
      this.calendarOptions = {
        ...this.calendarOptions,
        initialView: 'timeGridDay',
        headerToolbar: {
          left: 'prev,next today',
          center: 'title',
          right: 'timeGridDay'
        }
      };
    }

    this.setDefaultRange();
    this.loadSpaces();
  }

  private isSmallScreen(): boolean {
    return typeof window !== 'undefined' && window.innerWidth <= 768;
  }

  loadSpaces(): void {
    this.spacesLoading = true;
    this.spacesError = '';

    this.managerService.spaces().subscribe({
      next: (response) => {
        this.spaces = response.spaces ?? [];
        this.selectedSpaceId = this.spaces[0]?.id;
        this.rebuildResources();
        this.spacesLoading = false;
        this.tryAutoLoadCalendar();
      },
      error: (err) => {
        this.spacesError = err?.error?.message ?? 'Neuspesno ucitavanje prostora.';
        this.spacesLoading = false;
      }
    });
  }

  onSpaceOrTypeChange(): void {
    this.rebuildResources();
    this.tryAutoLoadCalendar();
  }

  loadCalendar(): void {
    this.calendarError = '';

    if (!this.selectedSpaceId || !this.selectedResourceId || !this.from || !this.to) {
      this.calendarError = 'Prostor, tip, resurs, from i to su obavezni.';
      return;
    }

    const fromIso = this.toUtcDateTimeIsoFromInput(this.from);
    const toIso = this.toUtcDateTimeIsoFromInput(this.to);
    if (!fromIso || !toIso) {
      this.calendarError = 'Neispravan from/to format.';
      return;
    }

    if (new Date(this.from).getTime() >= new Date(this.to).getTime()) {
      this.calendarError = 'to mora biti posle from.';
      return;
    }

    this.calendarLoading = true;
    this.managerService.calendar(this.selectedSpaceId, this.selectedType, this.selectedResourceId, fromIso, toIso)
      .subscribe({
        next: (response) => {
          const events: EventInput[] = (response.events ?? [])
            .map(item => {
              const start = this.parseBackendUtc(item.from);
              const end = this.parseBackendUtc(item.to);
              if (!start || !end) {
                return null;
              }

              return {
                id: String(item.reservationId ?? ''),
                title: `${item.title ?? 'clan'} (${item.status ?? ''})`,
                start,
                end
              } as EventInput;
            })
            .filter((item): item is EventInput => item !== null);

          this.calendarOptions = {
            ...this.calendarOptions,
            events
          };
          this.calendarLoading = false;
        },
        error: (err) => {
          this.calendarError = err?.error?.message ?? 'Neuspesno ucitavanje kalendara.';
          this.calendarLoading = false;
        }
      });
  }

  private tryAutoLoadCalendar(): void {
    if (!this.selectedSpaceId || !this.selectedResourceId || !this.from || !this.to) {
      return;
    }

    this.loadCalendar();
  }

  private onEventDrop(dropInfo: EventDropArg): void {
    const reservationId = Number(dropInfo.event.id);
    const start = dropInfo.event.start;
    const end = dropInfo.event.end;

    if (!reservationId || !start || !end) {
      this.calendarError = 'Neispravni podaci za pomeranje rezervacije.';
      dropInfo.revert();
      return;
    }

    const from = this.toUtcDateTimeIso(start);
    const to = this.toUtcDateTimeIso(end);
    if (!from || !to) {
      this.calendarError = 'Neispravan datum nakon pomeranja.';
      dropInfo.revert();
      return;
    }

    this.calendarError = '';
    this.managerService.move_reservation(reservationId, { from, to }).subscribe({
      next: () => {
        this.loadCalendar();
      },
      error: (err) => {
        this.calendarError = err?.error?.message ?? 'Neuspesno pomeranje rezervacije.';
        dropInfo.revert();
      }
    });
  }

  private rebuildResources(): void {
    const selectedSpace = this.spaces.find(item => item.id === this.selectedSpaceId);
    if (!selectedSpace?.elements) {
      this.resources = [];
      this.selectedResourceId = undefined;
      return;
    }

    if (this.selectedType === 'otvoreni') {
      const open = selectedSpace.elements.openSpace;
      this.resources = open?.id ? [{ id: open.id, label: 'Otvoreni prostor' }] : [];
    } else if (this.selectedType === 'kancelarija') {
      this.resources = (selectedSpace.elements.offices ?? [])
        .filter(item => !!item.id)
        .map(item => ({ id: item.id!, label: item.naziv ?? `Kancelarija ${item.id}` }));
    } else {
      this.resources = (selectedSpace.elements.meetingRooms ?? [])
        .filter(item => !!item.id)
        .map(item => ({ id: item.id!, label: item.naziv ?? `Sala ${item.id}` }));
    }

    this.selectedResourceId = this.resources[0]?.id;
  }

  private setDefaultRange(): void {
    const now = new Date();
    const start = new Date(now);
    start.setDate(start.getDate() - 7);
    start.setHours(0, 0, 0, 0);
    const end = new Date(start);
    end.setDate(end.getDate() + 28);

    this.from = this.toInputDateTime(start);
    this.to = this.toInputDateTime(end);
  }

  private toInputDateTime(value: Date): string {
    const year = value.getFullYear();
    const month = String(value.getMonth() + 1).padStart(2, '0');
    const day = String(value.getDate()).padStart(2, '0');
    const hour = String(value.getHours()).padStart(2, '0');
    const minute = String(value.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hour}:${minute}`;
  }

  private toUtcDateTimeIsoFromInput(value: string): string | null {
    if (!value) {
      return null;
    }

    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
      return null;
    }

    return this.toUtcDateTimeIso(parsed);
  }

  private toUtcDateTimeIso(value: Date): string {
    const year = value.getUTCFullYear();
    const month = String(value.getUTCMonth() + 1).padStart(2, '0');
    const day = String(value.getUTCDate()).padStart(2, '0');
    const hour = String(value.getUTCHours()).padStart(2, '0');
    const minute = String(value.getUTCMinutes()).padStart(2, '0');
    const second = String(value.getUTCSeconds()).padStart(2, '0');
    return `${year}-${month}-${day}T${hour}:${minute}:${second}`;
  }

  private parseBackendUtc(value?: string): Date | null {
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
