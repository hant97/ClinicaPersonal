import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { AppointmentService } from '../../../core/services/appointment.service';
import { PublicAppointmentConfirmation } from '../../../core/models/appointment.model';
import {
  LucideAngularModule,
  ShieldCheck,
  AlertTriangle,
  Calendar,
  Clock,
  CheckCircle2,
  ArrowLeft
} from 'lucide-angular';

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
  error = false;
  result: PublicAppointmentConfirmation | null = null;

  constructor(
    private route: ActivatedRoute,
    private appointmentService: AppointmentService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const tokenParam = params.get('token');
      if (tokenParam) {
        this.token = tokenParam;
        this.confirm(tokenParam);
      } else {
        this.loading = false;
        this.error = true;
      }
    });
  }

  confirm(token: string): void {
    this.loading = true;
    this.error = false;
    this.appointmentService.confirmByToken(token).subscribe({
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
