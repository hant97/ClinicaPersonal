import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideDynamicIcon } from '@lucide/angular';
import { ClinicalHistory } from '../../../core/models/clinical-history.model';
import { ClinicalSession } from '../../../core/models/clinical-session.model';
import { ChevronDown, ChevronUp, FileText, Lock, Printer, Settings2, X } from '../../../shared/icons/lucide-icons';
import type { PrintMode, PrintSectionsConfig, SessionFilterType } from './clinical-history-print.component';

@Component({
  selector: 'app-clinical-history-print-controls',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideDynamicIcon],
  templateUrl: './clinical-history-print-controls.component.html'
})
export class ClinicalHistoryPrintControlsComponent {
  readonly ChevronDown = ChevronDown;
  readonly ChevronUp = ChevronUp;
  readonly FileText = FileText;
  readonly Lock = Lock;
  readonly Printer = Printer;
  readonly Settings2 = Settings2;
  readonly X = X;

  @Input({ required: true }) sections!: PrintSectionsConfig;
  @Input() history: ClinicalHistory | null = null;
  @Input() sessions: ClinicalSession[] = [];
  @Input() filteredSessionsCount = 0;
  @Input() printMode: PrintMode = 'completa';
  @Input() showConfigPanel = false;
  @Input() sessionFilter: SessionFilterType = 'all';
  @Input() sessionDateFrom = '';
  @Input() sessionDateTo = '';
  @Input() excludeConfidential = true;

  @Output() modeRequested = new EventEmitter<PrintMode>();
  @Output() configToggleRequested = new EventEmitter<void>();
  @Output() printRequested = new EventEmitter<void>();
  @Output() closeRequested = new EventEmitter<void>();
  @Output() sessionFilterChange = new EventEmitter<SessionFilterType>();
  @Output() sessionDateFromChange = new EventEmitter<string>();
  @Output() sessionDateToChange = new EventEmitter<string>();
  @Output() excludeConfidentialChange = new EventEmitter<boolean>();
}
