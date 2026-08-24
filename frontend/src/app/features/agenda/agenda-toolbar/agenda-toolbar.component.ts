import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import {
  Calendar,
  CalendarDays,
  Clock,
  Download,
  Filter,
  LayoutList,
  LucideAngularModule,
  Plus,
  Search,
  User
} from 'lucide-angular';
import { UserProfile } from '../../../core/models/user-profile.model';

export type AgendaView = 'calendar' | 'list';

@Component({
  selector: 'app-agenda-toolbar',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LucideAngularModule],
  templateUrl: './agenda-toolbar.component.html'
})
export class AgendaToolbarComponent {
  @Input({ required: true }) filterForm!: FormGroup;
  @Input() currentView: AgendaView = 'calendar';
  @Input() professionals: UserProfile[] = [];
  @Input() todayCount = 0;

  @Output() viewChange = new EventEmitter<AgendaView>();
  @Output() manageSchedule = new EventEmitter<void>();
  @Output() exportRequested = new EventEmitter<void>();
  @Output() createRequested = new EventEmitter<void>();

  readonly Calendar = Calendar;
  readonly CalendarDays = CalendarDays;
  readonly Clock = Clock;
  readonly Download = Download;
  readonly Filter = Filter;
  readonly LayoutList = LayoutList;
  readonly Plus = Plus;
  readonly Search = Search;
  readonly User = User;

  getProfessionalDisplayName(professional: UserProfile): string {
    const fullName = [professional.firstName, professional.lastName].filter(Boolean).join(' ').trim();
    const name = fullName || professional.username || `Profesional #${professional.id}`;
    return professional.specialty ? `${name} (${professional.specialty})` : name;
  }
}
