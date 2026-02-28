import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FullCalendarModule } from '@fullcalendar/angular';
import { CalendarOptions, EventInput } from '@fullcalendar/core';
import timeGridPlugin from '@fullcalendar/timegrid';
import { ProfileComponent } from '../profile-component/profile-component';
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
    plugins: [timeGridPlugin],
    initialView: 'timeGridWeek',
    timeZone: 'local',
    editable: false,
    eventStartEditable: false,
    eventDurationEditable: false,
    selectable: false,
    headerToolbar: {
      left: 'prev,next today',
      center: 'title',
      right: 'timeGridWeek,timeGridDay'
    },
    allDaySlot: false,
    events: []
  };

  ngOnInit(): void {
    this.setDefaultRange();
    this.loadSpaces();
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
    console.log('[manager-calendar] loadCalendar called', {
      selectedSpaceId: this.selectedSpaceId,
      selectedType: this.selectedType,
      selectedResourceId: this.selectedResourceId,
      from: this.from,
      to: this.to
    });

    if (!this.selectedSpaceId || !this.selectedResourceId || !this.from || !this.to) {
      this.calendarError = 'Prostor, tip, resurs, from i to su obavezni.';
      console.log('[manager-calendar] loadCalendar blocked by validation', {
        selectedSpaceId: this.selectedSpaceId,
        selectedResourceId: this.selectedResourceId,
        from: this.from,
        to: this.to
      });
      return;
    }

    const fromIso = this.toLocalDateTimeIso(this.from);
    const toIso = this.toLocalDateTimeIso(this.to);
    if (!fromIso || !toIso) {
      this.calendarError = 'Neispravan from/to format.';
      console.log('[manager-calendar] invalid from/to format', { fromIso, toIso });
      return;
    }

    if (new Date(this.from).getTime() >= new Date(this.to).getTime()) {
      this.calendarError = 'to mora biti posle from.';
      console.log('[manager-calendar] invalid range, to <= from', { from: this.from, to: this.to });
      return;
    }

    this.calendarLoading = true;
    console.log('[manager-calendar] sending request', {
      spaceId: this.selectedSpaceId,
      type: this.selectedType,
      resourceId: this.selectedResourceId,
      from: fromIso,
      to: toIso
    });
    this.managerService.calendar(this.selectedSpaceId, this.selectedType, this.selectedResourceId, fromIso, toIso)
      .subscribe({
        next: (response) => {
          console.log('[manager-calendar] response received', response);
          const events: EventInput[] = (response.events ?? [])
            .map(item => {
              const start = this.parseBackendUtc(item.from);
              const end = this.parseBackendUtc(item.to);
              if (!start || !end) {
                return null;
              }

              return {
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
          console.log('[manager-calendar] events mapped', events);
          this.calendarLoading = false;
        },
        error: (err) => {
          console.log('[manager-calendar] request failed', err);
          this.calendarError = err?.error?.message ?? 'Neuspesno ucitavanje kalendara.';
          this.calendarLoading = false;
        }
      });
  }

  private tryAutoLoadCalendar(): void {
    if (!this.selectedSpaceId || !this.selectedResourceId || !this.from || !this.to) {
      console.log('[manager-calendar] auto-load skipped', {
        selectedSpaceId: this.selectedSpaceId,
        selectedResourceId: this.selectedResourceId,
        from: this.from,
        to: this.to
      });
      return;
    }

    console.log('[manager-calendar] auto-load triggered');
    this.loadCalendar();
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

  private toLocalDateTimeIso(value: string): string | null {
    if (!value) {
      return null;
    }

    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
      return null;
    }

    const year = parsed.getFullYear();
    const month = String(parsed.getMonth() + 1).padStart(2, '0');
    const day = String(parsed.getDate()).padStart(2, '0');
    const hour = String(parsed.getHours()).padStart(2, '0');
    const minute = String(parsed.getMinutes()).padStart(2, '0');
    const second = String(parsed.getSeconds()).padStart(2, '0');
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
