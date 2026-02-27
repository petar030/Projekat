import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ProfileComponent } from '../profile-component/profile-component';
import { MemberReservationItem, MemberSearchItem } from '../models/member/member-models';
import { MemberService } from '../services/member/member-service';
import { PublicService } from '../services/public/public-service';

@Component({
  selector: 'app-member-component',
  imports: [CommonModule, FormsModule, ProfileComponent],
  templateUrl: './member-component.html',
  styleUrl: './member-component.css',
})
export class MemberComponent implements OnInit {

  private memberService = inject(MemberService);
  private publicService = inject(PublicService);
  private router = inject(Router);

  reservations: MemberReservationItem[] = [];
  reservationsError: string = '';
  reservationsSuccess: string = '';

  searchName: string = '';
  selectedCities: string[] = [];
  cities: string[] = [];
  selectedType: 'otvoreni' | 'kancelarija' | 'sala' = 'otvoreni';
  officeMinDesks?: number;
  spaces: MemberSearchItem[] = [];
  searchError: string = '';
  searchLoading: boolean = false;
  sortBy: 'naziv' | 'grad' = 'naziv';
  sortDir: 'asc' | 'desc' = 'asc';

  ngOnInit(): void {
    this.load_reservations();
    this.load_cities();
    this.search_spaces();
  }

  load_reservations(): void {
    this.reservationsError = '';
    this.reservationsSuccess = '';
    this.memberService.reservations().subscribe({
      next: (response) => {
        this.reservations = response.content ?? [];
      },
      error: (err) => {
        this.reservationsError = err?.error?.message ?? 'Neuspesno ucitavanje rezervacija.';
      }
    });
  }

  cancel_reservation(reservation: MemberReservationItem): void {
    this.reservationsError = '';
    this.reservationsSuccess = '';

    if (!reservation.id) {
      this.reservationsError = 'Neispravan ID rezervacije.';
      return;
    }

    this.memberService.cancel_reservation(reservation.id).subscribe({
      next: () => {
        this.reservationsSuccess = 'Rezervacija je uspesno otkazana.';
        this.load_reservations();
      },
      error: (err) => {
        this.reservationsError = err?.error?.message ?? 'Neuspesno otkazivanje rezervacije.';
      }
    });
  }

  load_cities(): void {
    this.publicService.cities().subscribe({
      next: (response) => {
        this.cities = response.cities ?? [];
      },
      error: () => {
        this.searchError = 'Greska pri ucitavanju gradova.';
      }
    });
  }

  search_spaces(): void {
    this.searchLoading = true;
    this.searchError = '';

    this.memberService.search_spaces({
      name: this.searchName?.trim() || undefined,
      cities: this.selectedCities.length > 0 ? this.selectedCities : undefined,
      type: this.selectedType,
      officeMinDesks: this.selectedType === 'kancelarija' ? this.officeMinDesks : undefined
    }).subscribe({
      next: (response) => {
        this.spaces = response.content ?? [];
        this.apply_sort();
        this.searchLoading = false;
      },
      error: (err) => {
        this.searchError = err?.error?.message ?? 'Greska pri pretrazi prostora.';
        this.searchLoading = false;
      }
    });
  }

  clear_search(): void {
    this.searchName = '';
    this.selectedCities = [];
    this.selectedType = 'otvoreni';
    this.officeMinDesks = undefined;
    this.search_spaces();
  }

  toggle_city(city: string, checked: boolean): void {
    if (checked) {
      if (!this.selectedCities.includes(city)) {
        this.selectedCities = [...this.selectedCities, city];
      }
      return;
    }

    this.selectedCities = this.selectedCities.filter(selected => selected !== city);
  }

  selected_cities_label(): string {
    if (this.selectedCities.length === 0) {
      return 'Izaberi gradove';
    }

    return this.selectedCities.join(', ');
  }

  select_type(type: 'otvoreni' | 'kancelarija' | 'sala'): void {
    this.selectedType = type;
    if (type !== 'kancelarija') {
      this.officeMinDesks = undefined;
    }
  }

  is_type_selected(type: 'otvoreni' | 'kancelarija' | 'sala'): boolean {
    return this.selectedType === type;
  }

  change_sort(field: 'naziv' | 'grad'): void {
    if (this.sortBy === field) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = field;
      this.sortDir = 'asc';
    }

    this.apply_sort();
  }

  open_member_details(space: MemberSearchItem): void {
    if (!space.id || !space.matchingSubspaceIds || space.matchingSubspaceIds.length === 0) {
      return;
    }

    const childIds = space.matchingSubspaceIds.join(',');

    this.router.navigate(['/member_details', space.id], {
      queryParams: {
        type: this.selectedType,
        childIds
      }
    });
  }

  private apply_sort(): void {
    const isAsc = this.sortDir === 'asc';

    this.spaces = [...this.spaces].sort((a, b) => {
      const aValue = (this.sortBy === 'naziv' ? a.naziv : a.grad) || '';
      const bValue = (this.sortBy === 'naziv' ? b.naziv : b.grad) || '';
      const compare = aValue.localeCompare(bValue, 'sr', { sensitivity: 'base' });
      return isAsc ? compare : -compare;
    });
  }

}
