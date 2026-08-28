import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import {
  Eye,
  Monitor,
  RotateCcw,
  Save,
  Smartphone,
  Tablet,
  Upload,
  X
} from '../../../../shared/icons/lucide-icons';

export type EditorViewport = 'desktop' | 'tablet' | 'mobile';

@Component({
  selector: 'app-editor-toolbar',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './editor-toolbar.component.html',
  styleUrl: './editor-toolbar.component.css'
})
export class EditorToolbarComponent {
  @Input() dirty = false;
  @Input() saving = false;
  @Input() publishing = false;
  @Input() busy = false;
  @Input() hasValidationErrors = false;
  @Input() viewport: EditorViewport = 'desktop';
  @Input() publishedAt?: string;
  @Input() publishedBy?: number;

  @Output() save = new EventEmitter<void>();
  @Output() publish = new EventEmitter<void>();
  @Output() reset = new EventEmitter<void>();
  @Output() exit = new EventEmitter<void>();
  @Output() openPublic = new EventEmitter<void>();
  @Output() viewportChange = new EventEmitter<EditorViewport>();

  readonly Eye = Eye;
  readonly Monitor = Monitor;
  readonly RotateCcw = RotateCcw;
  readonly Save = Save;
  readonly Smartphone = Smartphone;
  readonly Tablet = Tablet;
  readonly Upload = Upload;
  readonly X = X;

  get statusLabel(): string {
    if (this.saving || this.publishing) return 'Guardando…';
    if (this.dirty) return 'Cambios sin guardar';
    return 'Guardado';
  }

  setViewport(viewport: EditorViewport): void {
    this.viewportChange.emit(viewport);
  }
}
