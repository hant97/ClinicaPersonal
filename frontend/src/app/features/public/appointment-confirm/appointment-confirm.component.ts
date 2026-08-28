import { Component, DestroyRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AppointmentService } from '../../../core/services/appointment.service';
import { PublicAppointmentConfirmation } from '../../../core/models/appointment.model';
import { LucideAngularModule } from 'lucide-angular';
import {
  ShieldCheck,
  AlertTriangle,
  Calendar,
  Clock,
  CheckCircle2,
  ArrowLeft
} from '../../../shared/icons/lucide-icons';

@Component({
  selector: 'app-appointment-confirm',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  templateUrl: './appointment-confirm.component.html'
})
export class AppointmentConfirmComponent implements OnInit {
  readonly ShieldCheck = ShieldCheck;
  readonly AlertTriangle = AlertTriangle;
  readonly Calendar = Calendar;
  readonly Clock = Clock;
  readonly CheckCircle2 = CheckCircle2;
  readonly ArrowLeft = ArrowLeft;

  token = '';
  loading = true;
  submitting = false;
  error = false;
  actionError = false;
  result: PublicAppointmentConfirmation | null = null;

  constructor(
    private route: ActivatedRoute,
    private appointmentService: AppointmentService,
    private destroyRef: DestroyRef
  ) {}

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      const tokenParam = params.get('token');
      if (tokenParam) {
        this.token = tokenParam;
        this.loadConfirmation(tokenParam);
      } else {
        this.loading = false;
        this.error = true;
      }
    });
  }

  confirm(): void {
    if (!this.token || this.submitting || !this.result?.confirmable) {
      return;
    }

    this.submitting = true;
    this.actionError = false;
    this.appointmentService.confirm(this.token)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.result = data;
          this.submitting = false;
        },
        error: () => {
          this.actionError = true;
          this.submitting = false;
        }
      });
  }

  private loadConfirmation(token: string): void {
    this.loading = true;
    this.error = false;
    this.appointmentService.getConfirmation(token)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.result = data;
          this.loading = false;
        },
        error: () => {
          this.error = true;
          this.loading = false;
        }
      });
  }
}
