import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { DashboardStats } from '../../../core/services/dashboard.service';
import { OnboardingService } from '../../services/onboarding/onboarding.service';
import { CheckCircle2, Circle, X } from '../../icons/lucide-icons';

interface GettingStartedStep {
  title: string;
  description: string;
  routerLink: string;
  done: boolean;
}

/**
 * Guía de "primeros pasos" mostrada en el dashboard cuando la cuenta todavía
 * no tiene pacientes registrados. Cada paso se marca como hecho a partir de
 * datos reales del panel operativo (nunca de un flag inventado), para que el
 * estado "completo" sea siempre veraz.
 */
@Component({
  selector: 'app-getting-started-checklist',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  templateUrl: './getting-started-checklist.component.html',
})
export class GettingStartedChecklistComponent {
  @Input() stats!: DashboardStats;
  @Output() dismissed = new EventEmitter<void>();

  readonly CheckCircle2 = CheckCircle2;
  readonly Circle = Circle;
  readonly X = X;

  constructor(private onboardingService: OnboardingService) {}

  get steps(): GettingStartedStep[] {
    const hasAppointments =
      this.stats.appointmentsToday > 0 || (this.stats.upcomingAppointments?.length ?? 0) > 0;
    return [
      {
        title: 'Configura los datos de tu clínica',
        description: 'Nombre, logo y datos de contacto que verán tus pacientes.',
        routerLink: '/settings',
        done: false,
      },
      {
        title: 'Agrega tu primer paciente',
        description: 'Registra los datos básicos de la persona que vas a atender.',
        routerLink: '/patients/new',
        done: this.stats.activePatients > 0,
      },
      {
        title: 'Configura tus servicios y tarifas',
        description: 'Define qué servicios ofreces y cuánto cobras por cada uno.',
        routerLink: '/services',
        done: false,
      },
      {
        title: 'Agenda tu primera cita',
        description: 'Programa una cita para empezar a usar la agenda del día a día.',
        routerLink: '/agenda',
        done: hasAppointments,
      },
    ];
  }

  dismiss(): void {
    this.onboardingService.dismissChecklist();
    this.dismissed.emit();
  }
}
