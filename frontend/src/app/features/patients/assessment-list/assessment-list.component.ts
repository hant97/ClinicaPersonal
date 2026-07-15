import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AssessmentService } from '../../../core/services/assessment.service';
import { Assessment } from '../../../core/models/assessment.model';
import { AssessmentFormComponent } from '../assessment-form/assessment-form.component';

@Component({
  selector: 'app-assessment-list',
  standalone: true,
  imports: [CommonModule, AssessmentFormComponent],
  templateUrl: './assessment-list.component.html',
  styleUrl: './assessment-list.component.css'
})
export class AssessmentListComponent implements OnInit {
  @Input() patientId!: number;
  assessments: Assessment[] = [];
  showForm: boolean = false;

  constructor(private assessmentService: AssessmentService) {}

  ngOnInit() {
    this.loadAssessments();
  }

  loadAssessments() {
    this.assessmentService.getAssessmentsByPatient(this.patientId).subscribe(data => {
      this.assessments = data;
    });
  }

  openForm() {
    this.showForm = true;
  }

  closeForm(refresh: boolean) {
    this.showForm = false;
    if (refresh) {
      this.loadAssessments();
    }
  }
}
