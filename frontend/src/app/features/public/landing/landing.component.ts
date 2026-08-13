import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  ArrowRight,
  Brain,
  Check,
  ChevronDown,
  HeartHandshake,
  LucideAngularModule,
  Menu,
  Microscope,
  ShieldCheck,
  Sparkles,
  Stethoscope,
  X
} from 'lucide-angular';
import { WebsiteSettingsService } from '../../../core/services/website-settings.service';
import { PublicLanding } from '../../../core/models/public-landing.model';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink, LucideAngularModule],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.css'
})
export class LandingComponent implements OnInit {
  isMenuOpen = false;
  landing: PublicLanding | null = null;

  readonly ArrowRight = ArrowRight;
  readonly Brain = Brain;
  readonly Check = Check;
  readonly ChevronDown = ChevronDown;
  readonly HeartHandshake = HeartHandshake;
  readonly Menu = Menu;
  readonly Microscope = Microscope;
  readonly ShieldCheck = ShieldCheck;
  readonly Sparkles = Sparkles;
  readonly Stethoscope = Stethoscope;
  readonly X = X;

  constructor(private readonly websiteService: WebsiteSettingsService) {}

  ngOnInit(): void {
    this.websiteService.getPublicLanding().subscribe({
      next: landing => this.landing = landing,
      error: error => console.error('No se pudo cargar el contenido público del sitio', error)
    });
  }

  getIcon(code?: string): any {
    switch (code) {
      case 'BRAIN': return this.Brain;
      case 'MICROSCOPE': return this.Microscope;
      case 'SHIELD_CHECK': return this.ShieldCheck;
      case 'STETHOSCOPE': return this.Stethoscope;
      case 'SPARKLES': return this.Sparkles;
      default: return this.HeartHandshake;
    }
  }

  toggleMenu(): void {
    this.isMenuOpen = !this.isMenuOpen;
  }

  closeMenu(): void {
    this.isMenuOpen = false;
  }

  useLocalFallback(event: Event, fallbackPath: string): void {
    const image = event.target as HTMLImageElement;
    if (!image.src.endsWith(fallbackPath)) {
      image.src = fallbackPath;
    }
  }

  whatsappUrl(number?: string): string {
    return `https://wa.me/${(number || '').replace(/\D/g, '')}`;
  }
}
