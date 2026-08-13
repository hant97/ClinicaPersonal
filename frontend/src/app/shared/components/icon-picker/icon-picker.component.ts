import { Component, EventEmitter, Input, Output } from '@angular/core';
import { LucideAngularModule, Brain, HeartHandshake, Microscope, ShieldCheck, Sparkles, Stethoscope } from 'lucide-angular';

interface IconOption {
  code: string;
  label: string;
  icon: any;
}

@Component({
  selector: 'app-icon-picker',
  standalone: true,
  imports: [LucideAngularModule],
  templateUrl: './icon-picker.component.html',
  styleUrl: './icon-picker.component.css'
})
export class IconPickerComponent {
  @Input() value = 'SPARKLES';
  @Output() valueChange = new EventEmitter<string>();

  readonly options: IconOption[] = [
    { code: 'HEART_HANDSHAKE', label: 'Cuidado personalizado', icon: HeartHandshake },
    { code: 'SHIELD_CHECK', label: 'Confianza y privacidad', icon: ShieldCheck },
    { code: 'STETHOSCOPE', label: 'Atención profesional', icon: Stethoscope },
    { code: 'SPARKLES', label: 'Bienestar', icon: Sparkles },
    { code: 'MICROSCOPE', label: 'Piel y diagnóstico', icon: Microscope },
    { code: 'BRAIN', label: 'Mente y emociones', icon: Brain }
  ];

  select(code: string): void {
    this.valueChange.emit(code);
  }
}
