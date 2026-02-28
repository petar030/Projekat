import { AfterViewInit, Component, ElementRef, OnDestroy, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import * as L from 'leaflet';
import { ManagerCreateSpaceRequest } from '../models/manager/manager-models';
import { ManagerService } from '../services/manager/manager-service';

@Component({
  selector: 'app-manager-add-component',
  imports: [CommonModule, FormsModule],
  templateUrl: './manager-add-component.html',
  styleUrl: './manager-add-component.css',
})
export class ManagerAddComponent implements AfterViewInit, OnDestroy {

  private static readonly MAX_IMAGE_COUNT = 5;
  private static readonly MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;
  private static readonly MAX_TOTAL_IMAGES_SIZE_BYTES = 25 * 1024 * 1024;
  private static readonly MAX_JSON_FILE_SIZE_BYTES = 1024 * 1024;

  @ViewChild('spaceMap', { static: false })
  spaceMapElement?: ElementRef<HTMLDivElement>;

  private managerService = inject(ManagerService);
  private router = inject(Router);

  mode: 'forma' | 'json' = 'forma';

  naziv: string = '';
  grad: string = '';
  adresa: string = '';
  opis: string = '';
  cenaPoSatu?: number;
  pragKazni?: number;
  geografskaSirina?: number;
  geografskaDuzina?: number;
  openSpaceBrojStolova?: number;

  private map?: L.Map;
  private marker?: L.Marker;
  private readonly defaultLat = 44.8176;
  private readonly defaultLng = 20.4633;

  selectedJsonFile: File | null = null;
  selectedJsonPayload: unknown = null;
  jsonFileInfo: string = '';
  selectedImages: File[] = [];
  imageInfo: string = '';

  successMessage: string = '';
  errorMessage: string = '';

  ngAfterViewInit(): void {
    this.initializeMap();
  }

  ngOnDestroy(): void {
    this.destroyMap();
  }

  setMode(mode: 'forma' | 'json'): void {
    this.mode = mode;
    this.successMessage = '';
    this.errorMessage = '';

    if (mode === 'json') {
      this.destroyMap();
      return;
    }

    setTimeout(() => this.initializeMap(), 0);
  }

  onImagesSelected(event: Event): void {
    this.errorMessage = '';
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);

    if (files.length > ManagerAddComponent.MAX_IMAGE_COUNT) {
      this.selectedImages = [];
      this.imageInfo = '';
      this.errorMessage = 'Maksimalan broj slika je 5.';
      input.value = '';
      return;
    }

    const tooLargeFile = files.find(file => file.size > ManagerAddComponent.MAX_IMAGE_SIZE_BYTES);
    if (tooLargeFile) {
      this.selectedImages = [];
      this.imageInfo = '';
      this.errorMessage = `Slika ${tooLargeFile.name} prelazi 5MB.`;
      input.value = '';
      return;
    }

    const totalSize = files.reduce((sum, file) => sum + file.size, 0);
    if (totalSize > ManagerAddComponent.MAX_TOTAL_IMAGES_SIZE_BYTES) {
      this.selectedImages = [];
      this.imageInfo = '';
      this.errorMessage = 'Ukupna velicina slika ne sme biti veca od 25MB.';
      input.value = '';
      return;
    }

    this.selectedImages = files;
    this.imageInfo = files.map(file => file.name).join(', ');
  }

  async onJsonFileSelected(event: Event): Promise<void> {
    this.errorMessage = '';
    this.successMessage = '';

    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);

    if (files.length !== 1) {
      this.clearJsonSelection(input);
      this.errorMessage = 'Potrebno je izabrati tacno jedan JSON fajl.';
      return;
    }

    const file = files[0];
    const lowerName = file.name.toLowerCase();
    if (!lowerName.endsWith('.json')) {
      this.clearJsonSelection(input);
      this.errorMessage = 'Dozvoljen je samo .json fajl.';
      return;
    }

    if (file.size === 0) {
      this.clearJsonSelection(input);
      this.errorMessage = 'JSON fajl je prazan.';
      return;
    }

    if (file.size > ManagerAddComponent.MAX_JSON_FILE_SIZE_BYTES) {
      this.clearJsonSelection(input);
      this.errorMessage = 'JSON fajl je prevelik (maksimalno 1MB).';
      return;
    }

    let parsedPayload: unknown;
    try {
      const content = await file.text();
      parsedPayload = JSON.parse(content);
    } catch {
      this.clearJsonSelection(input);
      this.errorMessage = 'Sadrzaj fajla nije validan JSON.';
      return;
    }

    const validationError = this.validateJsonPayload(parsedPayload);
    if (validationError) {
      this.clearJsonSelection(input);
      this.errorMessage = validationError;
      return;
    }

    this.selectedJsonFile = file;
    this.selectedJsonPayload = parsedPayload;
    this.jsonFileInfo = `${file.name} (${(file.size / 1024).toFixed(1)} KB)`;
  }

  submitForm(): void {
    this.successMessage = '';
    this.errorMessage = '';

    this.resolveCoordinatesFromMarker();

    if (!this.naziv.trim() || !this.grad.trim() || !this.adresa.trim() || !this.cenaPoSatu || !this.pragKazni || !this.openSpaceBrojStolova) {
      this.errorMessage = 'Sva obavezna polja forme moraju biti popunjena.';
      return;
    }

    const payload: ManagerCreateSpaceRequest = {
      naziv: this.naziv.trim(),
      grad: this.grad.trim(),
      adresa: this.adresa.trim(),
      opis: this.opis.trim() || undefined,
      cenaPoSatu: this.cenaPoSatu,
      pragKazni: this.pragKazni,
      geografskaSirina: this.geografskaSirina,
      geografskaDuzina: this.geografskaDuzina,
      openSpace: {
        brojStolova: this.openSpaceBrojStolova
      }
    };

    this.managerService.create_space(payload, this.selectedImages).subscribe({
      next: (response) => {
        this.successMessage = response.message ?? 'Prostor je uspesno kreiran.';
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Neuspesno kreiranje prostora.';
      }
    });
  }

  submitJson(): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (!this.selectedJsonFile || this.selectedJsonPayload == null) {
      this.errorMessage = 'Prvo izaberi validan JSON fajl.';
      return;
    }

    const validationError = this.validateJsonPayload(this.selectedJsonPayload);
    if (validationError) {
      this.errorMessage = validationError;
      return;
    }

    this.managerService.import_space_json(this.selectedJsonPayload, this.selectedImages).subscribe({
      next: (response) => {
        this.successMessage = response.message ?? 'Prostor je uspesno kreiran iz JSON-a.';
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Neuspesan import prostora iz JSON-a.';
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/manager']);
  }

  private initializeMap(): void {
    if (!this.spaceMapElement?.nativeElement || this.map) {
      return;
    }

    const initialLat = this.geografskaSirina ?? this.defaultLat;
    const initialLng = this.geografskaDuzina ?? this.defaultLng;

    this.map = L.map(this.spaceMapElement.nativeElement).setView([initialLat, initialLng], 12);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);

    this.map.on('click', (event: L.LeafletMouseEvent) => {
      const lat = Number(event.latlng.lat.toFixed(7));
      const lng = Number(event.latlng.lng.toFixed(7));

      this.geografskaSirina = lat;
      this.geografskaDuzina = lng;

      if (!this.marker) {
        this.marker = L.marker([lat, lng]).addTo(this.map!);
      } else {
        this.marker.setLatLng([lat, lng]);
      }
    });

    if (this.geografskaSirina != null && this.geografskaDuzina != null) {
      this.marker = L.marker([this.geografskaSirina, this.geografskaDuzina]).addTo(this.map);
    }
  }

  private resolveCoordinatesFromMarker(): void {
    if (!this.marker) {
      return;
    }

    const markerPosition = this.marker.getLatLng();
    this.geografskaSirina = Number(markerPosition.lat.toFixed(7));
    this.geografskaDuzina = Number(markerPosition.lng.toFixed(7));
  }

  private destroyMap(): void {
    this.map?.remove();
    this.map = undefined;
    this.marker = undefined;
  }

  private clearJsonSelection(input?: HTMLInputElement): void {
    this.selectedJsonFile = null;
    this.selectedJsonPayload = null;
    this.jsonFileInfo = '';
    if (input) {
      input.value = '';
    }
  }

  private validateJsonPayload(payload: unknown): string | null {
    if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
      return 'JSON root mora biti objekat.';
    }

    const data = payload as Record<string, unknown>;

    if (!this.isNonEmptyString(data['naziv'])) {
      return 'Polje "naziv" je obavezno i mora biti tekst.';
    }
    if (!this.isNonEmptyString(data['grad'])) {
      return 'Polje "grad" je obavezno i mora biti tekst.';
    }
    if (!this.isNonEmptyString(data['adresa'])) {
      return 'Polje "adresa" je obavezno i mora biti tekst.';
    }
    if (data['opis'] != null && typeof data['opis'] !== 'string') {
      return 'Polje "opis" mora biti tekst ako je zadato.';
    }
    if (!this.isPositiveNumber(data['cenaPoSatu'])) {
      return 'Polje "cenaPoSatu" je obavezno i mora biti broj > 0.';
    }
    if (!this.isPositiveInteger(data['pragKazni'])) {
      return 'Polje "pragKazni" je obavezno i mora biti ceo broj >= 1.';
    }

    if (data['geografskaSirina'] != null && !this.isNumberInRange(data['geografskaSirina'], -90, 90)) {
      return 'Polje "geografskaSirina" mora biti broj u opsegu [-90, 90].';
    }
    if (data['geografskaDuzina'] != null && !this.isNumberInRange(data['geografskaDuzina'], -180, 180)) {
      return 'Polje "geografskaDuzina" mora biti broj u opsegu [-180, 180].';
    }

    const openSpace = data['openSpace'];
    if (!openSpace || typeof openSpace !== 'object' || Array.isArray(openSpace)) {
      return 'Polje "openSpace" je obavezno i mora biti objekat.';
    }

    const openSpaceRecord = openSpace as Record<string, unknown>;
    if (!this.isPositiveInteger(openSpaceRecord['brojStolova'])) {
      return 'Polje "openSpace.brojStolova" je obavezno i mora biti ceo broj >= 1.';
    }
    if ((openSpaceRecord['brojStolova'] as number) < 5) {
      return 'Polje "openSpace.brojStolova" mora biti >= 5.';
    }

    const officesError = this.validateOfficeArray(data['offices']);
    if (officesError) {
      return officesError;
    }

    const meetingRoomsError = this.validateMeetingRoomArray(data['meetingRooms']);
    if (meetingRoomsError) {
      return meetingRoomsError;
    }

    return null;
  }

  private validateOfficeArray(value: unknown): string | null {
    if (value == null) {
      return null;
    }
    if (!Array.isArray(value)) {
      return 'Polje "offices" mora biti niz ako je zadato.';
    }

    const usedNames = new Set<string>();
    for (let index = 0; index < value.length; index++) {
      const item = value[index];
      if (!item || typeof item !== 'object' || Array.isArray(item)) {
        return `Stavka offices[${index}] mora biti objekat.`;
      }

      const office = item as Record<string, unknown>;
      if (!this.isNonEmptyString(office['naziv'])) {
        return `Polje offices[${index}].naziv je obavezno.`;
      }
      if (!this.isPositiveInteger(office['brojStolova'])) {
        return `Polje offices[${index}].brojStolova mora biti ceo broj >= 1.`;
      }

      const normalizedName = (office['naziv'] as string).trim().toLocaleLowerCase();
      if (usedNames.has(normalizedName)) {
        return 'Nazivi kancelarija u okviru istog prostora moraju biti jedinstveni.';
      }
      usedNames.add(normalizedName);
    }

    return null;
  }

  private validateMeetingRoomArray(value: unknown): string | null {
    if (value == null) {
      return null;
    }
    if (!Array.isArray(value)) {
      return 'Polje "meetingRooms" mora biti niz ako je zadato.';
    }

    const usedNames = new Set<string>();
    for (let index = 0; index < value.length; index++) {
      const item = value[index];
      if (!item || typeof item !== 'object' || Array.isArray(item)) {
        return `Stavka meetingRooms[${index}] mora biti objekat.`;
      }

      const room = item as Record<string, unknown>;
      if (!this.isNonEmptyString(room['naziv'])) {
        return `Polje meetingRooms[${index}].naziv je obavezno.`;
      }
      if (room['dodatnaOprema'] != null && typeof room['dodatnaOprema'] !== 'string') {
        return `Polje meetingRooms[${index}].dodatnaOprema mora biti tekst ako je zadato.`;
      }
      if (typeof room['dodatnaOprema'] === 'string' && room['dodatnaOprema'].length > 300) {
        return `Polje meetingRooms[${index}].dodatnaOprema ne sme biti duze od 300 karaktera.`;
      }
      if (room['brojMesta'] != null) {
        return `Polje meetingRooms[${index}].brojMesta se ne unosi (podrazumevano je 10-12).`;
      }

      const normalizedName = (room['naziv'] as string).trim().toLocaleLowerCase();
      if (usedNames.has(normalizedName)) {
        return 'Nazivi konferencijskih sala u okviru istog prostora moraju biti jedinstveni.';
      }
      usedNames.add(normalizedName);
    }

    return null;
  }

  private isNonEmptyString(value: unknown): value is string {
    return typeof value === 'string' && value.trim().length > 0;
  }

  private isPositiveNumber(value: unknown): value is number {
    return typeof value === 'number' && Number.isFinite(value) && value > 0;
  }

  private isPositiveInteger(value: unknown): value is number {
    return typeof value === 'number' && Number.isInteger(value) && value >= 1;
  }

  private isNumberInRange(value: unknown, min: number, max: number): value is number {
    return typeof value === 'number' && Number.isFinite(value) && value >= min && value <= max;
  }

}
