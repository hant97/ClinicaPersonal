import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AssessmentService } from '../../../core/services/assessment.service';
import { Assessment, PsychometricTest, Question } from '../../../core/models/assessment.model';

@Component({
  selector: 'app-assessment-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './assessment-form.component.html',
  styleUrl: './assessment-form.component.css'
})
export class AssessmentFormComponent implements OnInit {
  @Input() patientId!: number;
  @Output() close = new EventEmitter<boolean>();

  tests: PsychometricTest[] = [];
  selectedTest: PsychometricTest | null = null;
  questions: Question[] = [];
  answers: { [questionId: number]: number } = {};
  notes: string = '';
  isSaving: boolean = false;

  constructor(private assessmentService: AssessmentService) {}

  ngOnInit() {
    this.assessmentService.getAvailableTests().subscribe(data => {
      this.tests = data;
    });
  }

  onTestSelect(event: Event) {
    const testId = Number((event.target as HTMLSelectElement).value);
    this.selectedTest = this.tests.find(t => t.id === testId) || null;
    if (this.selectedTest) {
      this.questions = JSON.parse(this.selectedTest.questionsJson);
      this.answers = {}; // Reset answers
    } else {
      this.questions = [];
    }
  }

  calculateTotalScore(): number {
    let total = 0;
    for (const qId in this.answers) {
      if (this.answers.hasOwnProperty(qId)) {
        total += this.answers[qId];
      }
    }
    return total;
  }

  isValid(): boolean {
    if (!this.selectedTest) return false;
    // Check if all questions have an answer
    return this.questions.length > 0 && Object.keys(this.answers).length === this.questions.length;
  }

  saveAssessment() {
    if (!this.isValid() || !this.selectedTest) return;

    this.isSaving = true;
    const totalScore = this.calculateTotalScore();
    const assessment: Assessment = {
      patientId: this.patientId,
      psychometricTestId: this.selectedTest.id,
      totalScore: totalScore,
      answersJson: JSON.stringify(this.answers),
      notes: this.notes
    };

    this.assessmentService.saveAssessment(assessment).subscribe({
      next: () => {
        this.isSaving = false;
        this.close.emit(true);
      },
      error: (err) => {
        console.error('Error saving assessment', err);
        this.isSaving = false;
        // Ideally show a notification here
      }
    });
  }

  cancel() {
    this.close.emit(false);
  }
}
