import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PublicService } from '../services/public/public-service';
import { PublicHomeSpace, PublicSearchItem } from '../models/public/public-models';

@Component({
  selector: 'app-start-component',
  imports: [CommonModule, FormsModule],
  templateUrl: './start-component.html',
  styleUrl: './start-component.css',
})
export class StartComponent implements OnInit {

  private publicService = inject(PublicService);

  top5Spaces: PublicHomeSpace[] = [];
  spaces: PublicSearchItem[] = [];
  cities: string[] = [];

  searchName: string = '';
  selectedCities: string[] = [];

  sortBy: 'naziv' | 'grad' = 'naziv';
  sortDir: 'asc' | 'desc' = 'asc';

  loading: boolean = false;
  errorMessage: string = '';

  ngOnInit(): void {
    this.loadHome();
    this.loadCities();
    this.searchSpaces();
  }

  loadHome(): void {
    this.publicService.home().subscribe({
      next: (response) => {
        this.top5Spaces = response.top5Spaces || [];
      },
      error: () => {
        this.errorMessage = 'Greska pri ucitavanju TOP 5 prostora.';
      }
    });
  }

  loadCities(): void {
    this.publicService.cities().subscribe({
      next: (response) => {
        this.cities = response.cities || [];
      },
      error: () => {
        this.errorMessage = 'Greska pri ucitavanju gradova.';
      }
    });
  }

  searchSpaces(): void {
    this.loading = true;
    this.errorMessage = '';

    this.publicService.search_spaces({
      name: this.searchName?.trim() || undefined,
      cities: this.selectedCities.length > 0 ? this.selectedCities : undefined
    }).subscribe({
      next: (response) => {
        this.spaces = response.content || [];
        this.applySort();
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Greska pri pretrazi prostora.';
        this.loading = false;
      }
    });
  }

  clearSearch(): void {
    this.searchName = '';
    this.selectedCities = [];
    this.searchSpaces();
  }

  toggleCity(city: string, checked: boolean): void {
    if (checked) {
      if (!this.selectedCities.includes(city)) {
        this.selectedCities = [...this.selectedCities, city];
      }
      return;
    }

    this.selectedCities = this.selectedCities.filter(selected => selected !== city);
  }

  selectedCitiesLabel(): string {
    if (this.selectedCities.length === 0) {
      return 'Izaberi gradove';
    }

    return this.selectedCities.join(', ');
  }

  changeSort(field: 'naziv' | 'grad'): void {
    if (this.sortBy === field) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = field;
      this.sortDir = 'asc';
    }

    this.applySort();
  }

  private applySort(): void {
    const isAsc = this.sortDir === 'asc';

    this.spaces = [...this.spaces].sort((a, b) => {
      const aValue = (this.sortBy === 'naziv' ? a.naziv : a.grad) || '';
      const bValue = (this.sortBy === 'naziv' ? b.naziv : b.grad) || '';

      const compare = aValue.localeCompare(bValue, 'sr', { sensitivity: 'base' });
      return isAsc ? compare : -compare;
    });
  }

}
