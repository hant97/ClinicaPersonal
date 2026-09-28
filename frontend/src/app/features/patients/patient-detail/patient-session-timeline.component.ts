import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { ClinicalSession } from '../../../core/models/clinical-session.model';
import { Edit, FileText, Plus, Trash2 } from '../../../shared/icons/lucide-icons';
import { sessionStatusBadgeClass, sessionStatusDotClass } from './patient-detail.utils';

@Component({
  selector: 'app-patient-session-timeline',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './patient-session-timeline.component.html'
})
export class PatientSessionTimelineComponent {
  readonly Edit = Edit;
  readonly FileText = FileText;
  readonly Plus = Plus;
  readonly Trash2 = Trash2;

  @Input({ required: true }) sessions: ClinicalSession[] = [];
  @Input() expandedSessionId: number | null = null;

  @Output() expansionRequested = new EventEmitter<number>();
  @Output() editRequested = new EventEmitter<number>();
  @Output() deleteRequested = new EventEmitter<number>();
  @Output() createRequested = new EventEmitter<void>();

  sessionStatusBadge(status?: string): string {
    return sessionStatusBadgeClass(status);
  }

  sessionStatusDot(status?: string): string {
    return sessionStatusDotClass(status);
  }

  requestEdit(id: number, event: Event): void {
    event.stopPropagation();
    this.editRequested.emit(id);
  }

  requestDelete(id: number, event: Event): void {
    event.stopPropagation();
    this.deleteRequested.emit(id);
  }
}
