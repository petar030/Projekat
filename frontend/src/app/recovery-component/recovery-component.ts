import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../services/auth/auth-service';

@Component({
  selector: 'app-recovery-component',
  imports: [CommonModule, FormsModule],
  templateUrl: './recovery-component.html',
  styleUrl: './recovery-component.css',
})
export class RecoveryComponent implements OnInit, OnDestroy {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthService);

  token: string = '';
  newPassword: string = '';
  confirmPassword: string = '';
  errorMessage: string = '';
  successMessage: string = '';
  remainingSeconds: number = 0;

  private timerId: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.token = this.route.snapshot.paramMap.get('token') ?? '';

    const expiresInSecondsParam = Number(this.route.snapshot.queryParamMap.get('expiresInSeconds'));
    const requestedAtParam = Number(this.route.snapshot.queryParamMap.get('requestedAt'));

    if (Number.isFinite(expiresInSecondsParam) && expiresInSecondsParam > 0 && Number.isFinite(requestedAtParam) && requestedAtParam > 0) {
      const elapsedSeconds = Math.max(0, Math.floor((Date.now() - requestedAtParam) / 1000));
      this.remainingSeconds = Math.max(0, Math.floor(expiresInSecondsParam) - elapsedSeconds);
    } else {
      this.remainingSeconds = 1800;
    }

    this.startCountdown();
  }

  ngOnDestroy(): void {
    if (this.timerId) {
      clearInterval(this.timerId);
    }
  }

  get remainingTimeLabel(): string {
    const minutes = Math.floor(this.remainingSeconds / 60);
    const seconds = this.remainingSeconds % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }

  confirmReset(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.token) {
      this.errorMessage = 'Token nedostaje.';
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.errorMessage = 'Lozinke se ne poklapaju.';
      return;
    }

    this.authService.reset_confirm(this.token, this.newPassword).subscribe({
      next: (response: any) => {
        this.successMessage = response?.message ?? 'Lozinka je uspešno promenjena.';
        setTimeout(() => this.router.navigate(['/login']), 1200);
      },
      error: (err) => {
        this.errorMessage = err?.error?.message ?? 'Neuspešna promena lozinke.';
      }
    });
  }

  private startCountdown(): void {
    if (this.timerId) {
      clearInterval(this.timerId);
    }

    this.timerId = setInterval(() => {
      if (this.remainingSeconds > 0) {
        this.remainingSeconds -= 1;
      }
    }, 1000);
  }

}
