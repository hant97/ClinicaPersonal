import { Component, EventEmitter, Input, Output } from '@angular/core';
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
import { PublicLanding } from '../../../core/models/public-landing.model';
import { LandingBlockRef, LandingBlockType } from '../../../core/models/website-editor.model';

@Component({
  selector: 'app-landing-view',
  standalone: true,
  imports: [RouterLink, LucideAngularModule],
  templateUrl: './landing-view.component.html',
  styleUrl: './landing-view.component.css'
})
export class LandingViewComponent {
  @Input({ required: true }) landing!: PublicLanding;
  @Input() selectable = false;
  @Input() selectedType: LandingBlockType | null = null;
  @Output() blockSelect = new EventEmitter<LandingBlockRef>();

  isMenuOpen = false;

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

  select(type: LandingBlockType): void {
    if (this.selectable) {
      this.blockSelect.emit({ type });
    }
  }
}
