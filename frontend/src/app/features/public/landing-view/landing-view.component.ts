import { Component, EventEmitter, Input, Output } from '@angular/core';
import { LucideAngularModule, type LucideIconData } from 'lucide-angular';
import {
  ArrowRight,
  Brain,
  HeartHandshake,
  Menu,
  Microscope,
  ShieldCheck,
  Sparkles,
  Stethoscope,
  X
} from '../../../shared/icons/lucide-icons';
import { PublicLanding } from '../../../core/models/public-landing.model';
import { LandingBlockRef, LandingBlockType } from '../../../core/models/website-editor.model';

@Component({
  selector: 'app-landing-view',
  standalone: true,
  imports: [LucideAngularModule],
  templateUrl: './landing-view.component.html',
  styleUrl: './landing-view.component.css'
})
export class LandingViewComponent {
  private readonly defaultHeroImage = '/images/landing-hero.jpg';
  private readonly defaultApproachImage = '/images/landing-approach.jpg';
  readonly currentYear = new Date().getFullYear();
  @Input({ required: true }) landing!: PublicLanding;
  @Input() selectable = false;
  @Input() selectedType: LandingBlockType | null = null;
  @Output() blockSelect = new EventEmitter<LandingBlockRef>();

  isMenuOpen = false;

  readonly ArrowRight = ArrowRight;
  readonly Brain = Brain;
  readonly HeartHandshake = HeartHandshake;
  readonly Menu = Menu;
  readonly Microscope = Microscope;
  readonly ShieldCheck = ShieldCheck;
  readonly Sparkles = Sparkles;
  readonly Stethoscope = Stethoscope;
  readonly X = X;

  getIcon(code?: string): LucideIconData {
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

  heroImageUrl(url?: string): string {
    return !url || url.includes('photo-1576091160399-112ba8d25d1d')
      ? this.defaultHeroImage
      : url;
  }

  approachImageUrl(url?: string): string {
    return !url || url.includes('photo-1544168190-79c17527004f')
      ? this.defaultApproachImage
      : url;
  }

  whatsappUrl(number?: string): string {
    return `https://wa.me/${(number || '').replace(/\D/g, '')}`;
  }

  hasContactChannel(contact: PublicLanding['contact']): boolean {
    return Boolean(contact.whatsapp || contact.email || contact.phone);
  }

  hasContactInfo(contact: PublicLanding['contact']): boolean {
    return Boolean(this.hasContactChannel(contact) || contact.address || contact.hours);
  }

  select(type: LandingBlockType): void {
    if (this.selectable) {
      this.blockSelect.emit({ type });
    }
  }
}
