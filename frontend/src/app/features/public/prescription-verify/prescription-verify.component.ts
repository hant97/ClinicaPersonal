import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { PrescriptionService } from '../../../core/services/prescription.service';
import { PublicPrescriptionVerification } from '../../../core/models/prescription.model';
import { LucideAngularModule } from 'lucide-angular';
import {
  ShieldCheck,
  AlertTriangle,
  FileText,
  Calendar,
  User,
  Building2,
  Stethoscope,
  CheckCircle2,
  Clock,
  ArrowLeft
} from '../../../shared/icons/lucide-icons';

@Component({
  selector: 'app-prescription-verify',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  templateUrl: './prescription-verify.component.html',
  styleUrls: ['./prescription-verify.component.css'],
})
export class PrescriptionVerifyComponent implements OnInit {
  readonly ShieldCheck = ShieldCheck;
  readonly AlertTriangle = AlertTriangle;
  readonly FileText = FileText;
  readonly Calendar = Calendar;
  readonly User = User;
  readonly Building2 = Building2;
  readonly Stethoscope = Stethoscope;
  readonly CheckCircle2 = CheckCircle2;
  readonly Clock = Clock;
  readonly ArrowLeft = ArrowLeft;

  code = '';
  loading = true;
  error = false;
  verification: PublicPrescriptionVerification | null = null;

  constructor(
    private route: ActivatedRoute,
    private prescriptionService: PrescriptionService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const codeParam = params.get('code');
      if (codeParam) {
        this.code = codeParam;
        this.verify(this.code);
      } else {
        this.loading = false;
        this.error = true;
      }
    });
  }

  verify(code: string): void {
    this.loading = true;
    this.error = false;
    this.prescriptionService.verifyPrescription(code).subscribe({
      next: (data) => {
        this.verification = data;
        this.loading = false;
      },
      error: () => {
        this.error = true;
        this.loading = false;
      }
    });
  }
}
