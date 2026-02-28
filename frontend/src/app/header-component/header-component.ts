import { Component, inject } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../services/auth/auth-service';

@Component({
  selector: 'app-header-component',
  imports: [CommonModule],
  templateUrl: './header-component.html',
  styleUrl: './header-component.css',
})
export class HeaderComponent {

  private router = inject(Router);
  private location = inject(Location);
  private authService = inject(AuthService);

  isHomeRoute: boolean = this.router.url === '/';

  constructor() {
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => {
        this.isHomeRoute = this.router.url === '/';
      });
  }

  open_login(): void {
    this.router.navigate(['/login']);
  }

  go_back(): void {
    if (this.isHomeRoute) {
      return;
    }

    if (window.history.length > 1) {
      this.location.back();
      return;
    }

    this.router.navigate(['/']);
  }

  logout(): void {
    this.authService.logout();
  }

}
