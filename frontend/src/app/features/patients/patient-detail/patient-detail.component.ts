import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { PatientService } from '../../../core/services/patient/patient.service';
import { MedicalRecordService } from '../../../core/services/medical-record.service';
import { Patient } from '../../../core/models/patient.model';
import { MedicalRecord } from '../../../core/models/medical-record.model';
import { MedicalRecordFormComponent } from '../medical-record-form/medical-record-form.component';

@Component({
  selector: 'app-patient-detail',
  standalone: true,
  imports: [CommonModule, MedicalRecordFormComponent],
  templateUrl: './patient-detail.component.html',
  styleUrls: ['./patient-detail.component.css']
})
export class PatientDetailComponent implements OnInit {
  patient: Patient | null = null;
  records: MedicalRecord[] = [];
  showForm = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private patientService: PatientService,
    private recordService: MedicalRecordService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadPatient(Number(id));
      this.loadRecords(Number(id));
    }
  }

  loadPatient(id: number): void {
    this.patientService.getById(id).subscribe({
      next: (data) => this.patient = data,
      error: (err) => console.error('Error fetching patient', err)
    });
  }

  loadRecords(patientId: number): void {
    this.recordService.getRecordsByPatientId(patientId).subscribe({
      next: (data) => this.records = data,
      error: (err) => console.error('Error fetching records', err)
    });
  }

  openForm(): void {
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
  }

  onRecordSaved(): void {
    this.showForm = false;
    if (this.patient?.id) {
      this.loadRecords(this.patient.id);
    }
  }

  goBack(): void {
    this.router.navigate(['/patients']);
  }
}
