import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type StatusPillVariant = 'critical' | 'urgent' | 'priority' | 'stable' | 'neutral' | 'specialty' | 'info';

@Component({
  selector: 'app-status-pill',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span
      class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium border transition-colors"
      [ngClass]="variantClasses"
    >
      @if (showDot) {
        <span
          class="h-1.5 w-1.5 rounded-full shrink-0"
          [ngClass]="dotClasses"
        ></span>
      }
      <span class="truncate">{{ label }}</span>
    </span>
  `,
})
export class StatusPillComponent {
  @Input() label: string = '';
  @Input() variant: StatusPillVariant = 'neutral';
  @Input() showDot: boolean = true;
  @Input() pulse: boolean = false;

  get variantClasses(): string {
    switch (this.variant) {
      case 'critical':
        return 'border-red-200 bg-red-50 text-red-700';
      case 'urgent':
        return 'border-amber-200 bg-amber-50 text-amber-800';
      case 'priority':
        return 'border-blue-200 bg-blue-50 text-blue-700';
      case 'stable':
        return 'border-emerald-200 bg-emerald-50 text-emerald-700';
      case 'specialty':
        return 'border-purple-200 bg-purple-50 text-purple-700';
      case 'info':
        return 'border-sky-200 bg-sky-50 text-sky-700';
      default:
        return 'border-slate-200 bg-slate-100 text-slate-700';
    }
  }

  get dotClasses(): string {
    const pulseClass = (this.pulse || this.variant === 'critical') ? 'animate-pulse' : '';
    switch (this.variant) {
      case 'critical':
        return `bg-red-500 ${pulseClass}`;
      case 'urgent':
        return `bg-amber-500 ${pulseClass}`;
      case 'priority':
        return `bg-blue-500 ${pulseClass}`;
      case 'stable':
        return `bg-emerald-500 ${pulseClass}`;
      case 'specialty':
        return `bg-purple-500 ${pulseClass}`;
      case 'info':
        return `bg-sky-500 ${pulseClass}`;
      default:
        return `bg-slate-400 ${pulseClass}`;
    }
  }
}
