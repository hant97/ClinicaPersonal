import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Allergy } from '../../../core/models/allergy.model';
import { LucideAngularModule, AlertTriangle } from 'lucide-angular';

@Component({
  selector: 'app-risk-alert-banner',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './risk-alert-banner.component.html',
})
export class RiskAlertBannerComponent {
  readonly AlertTriangle = AlertTriangle;

  @Input() allergies: Allergy[] = [];
  @Output() openAllergies = new EventEmitter<void>();

  get summary(): string {
    const items = this.allergies.map((a) => `${a.allergen} (${a.severity || 'Leve'})`);
    if (items.length <= 3) {
      return items.join(' · ');
    }
    return `${items.slice(0, 3).join(' · ')} +${items.length - 3} más`;
  }
}
