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
  showLogout: boolean = false;

  constructor() {
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => {
        this.syncRouteState();
      });

    this.syncRouteState();
  }

  private syncRouteState(): void {
    const url = this.router.url;
    this.isHomeRoute = url === '/';

    const isLoginRoute = url === '/login';
    const isPublicDetailsRoute = url.startsWith('/public_details/');
    this.showLogout = !this.isHomeRoute && !isLoginRoute && !isPublicDetailsRoute;
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
