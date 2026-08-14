import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { WebsiteSettingsService } from '../../../core/services/website-settings.service';
import { PublicLanding } from '../../../core/models/public-landing.model';
import { LandingViewComponent } from '../landing-view/landing-view.component';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink, LandingViewComponent],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.css'
})
export class LandingComponent implements OnInit {
  landing: PublicLanding | null = null;
  isSiteAdmin = false;

  constructor(
    private readonly websiteService: WebsiteSettingsService,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.websiteService.getPublicLanding().subscribe({
      next: landing => this.landing = landing,
      error: error => console.error('No se pudo cargar el contenido público del sitio', error)
    });

    this.authService.refresh().subscribe({
      next: () => this.isSiteAdmin = this.authService.hasRole('ROLE_SITE_ADMIN'),
      error: () => { /* sesión anónima o expirada: no mostrar el acceso de administración */ }
    });
  }
}
