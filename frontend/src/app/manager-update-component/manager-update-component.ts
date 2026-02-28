import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ManagerService } from '../services/manager/manager-service';

@Component({
  selector: 'app-manager-update-component',
  imports: [CommonModule, FormsModule],
  templateUrl: './manager-update-component.html',
  styleUrl: './manager-update-component.css',
})
export class ManagerUpdateComponent implements OnInit {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private managerService = inject(ManagerService);

  spaceId?: number;
  selectedType: 'kancelarija' | 'sala' = 'kancelarija';

  officeNaziv: string = '';
  officeBrojStolova?: number;

  roomNaziv: string = '';
  roomDodatnaOprema: string = '';

  successMessage: string = '';
  errorMessage: string = '';

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    this.spaceId = idParam ? Number(idParam) : undefined;

    if (!this.spaceId || Number.isNaN(this.spaceId)) {
      this.errorMessage = 'Neispravan ID prostora.';
    }
  }

  selectType(type: 'kancelarija' | 'sala'): void {
    this.selectedType = type;
    this.successMessage = '';
    this.errorMessage = '';
  }

  submit(): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (!this.spaceId) {
      this.errorMessage = 'Neispravan ID prostora.';
      return;
    }

    if (this.selectedType === 'kancelarija') {
      this.createOffice();
      return;
    }

    this.createMeetingRoom();
  }

  goBack(): void {
    this.router.navigate(['/manager']);
  }

  private createOffice(): void {
    const naziv = this.officeNaziv.trim();
    if (!naziv || !this.officeBrojStolova || this.officeBrojStolova < 1) {
      this.errorMessage = 'Naziv kancelarije i broj stolova (>=1) su obavezni.';
      return;
    }

    this.managerService.add_office(this.spaceId!, {
      naziv,
      brojStolova: this.officeBrojStolova
    }).subscribe({
      next: () => {
        this.successMessage = 'Kancelarija je uspesno dodata.';
        this.officeNaziv = '';
        this.officeBrojStolova = undefined;
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Neuspesno dodavanje kancelarije.';
      }
    });
  }

  private createMeetingRoom(): void {
    const naziv = this.roomNaziv.trim();
    if (!naziv) {
      this.errorMessage = 'Naziv sale je obavezan.';
      return;
    }

    this.managerService.add_meeting_room(this.spaceId!, {
      naziv,
      dodatnaOprema: this.roomDodatnaOprema.trim() || undefined
    }).subscribe({
      next: () => {
        this.successMessage = 'Konferencijska sala je uspesno dodata.';
        this.roomNaziv = '';
        this.roomDodatnaOprema = '';
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Neuspesno dodavanje konferencijske sale.';
      }
    });
  }

}
