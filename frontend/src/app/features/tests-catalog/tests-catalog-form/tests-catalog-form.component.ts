import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AssessmentService } from '../../../core/services/assessment.service';
import { PsychometricTest } from '../../../core/models/assessment.model';

@Component({
  selector: 'app-tests-catalog-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './tests-catalog-form.component.html',
  styleUrl: './tests-catalog-form.component.css'
})
export class TestsCatalogFormComponent implements OnInit {
  testForm!: FormGroup;
  isEditMode = false;
  testId?: number;
  isSaving = false;

  constructor(
    private fb: FormBuilder,
    private assessmentService: AssessmentService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.createForm();
  }

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode = true;
      this.testId = Number(idParam);
      this.loadTest(this.testId);
    } else {
      // Si es nuevo, inicializar con una pregunta vacía
      this.addQuestion();
    }
  }

  createForm() {
    this.testForm = this.fb.group({
      name: ['', Validators.required],
      description: ['', Validators.required],
      questions: this.fb.array([])
    });
  }

  get questions() {
    return this.testForm.get('questions') as FormArray;
  }

  options(questionIndex: number) {
    return this.questions.at(questionIndex).get('options') as FormArray;
  }

  addQuestion() {
    const questionGroup = this.fb.group({
      id: [this.questions.length + 1],
      text: ['', Validators.required],
      options: this.fb.array([])
    });
    this.questions.push(questionGroup);
    // Add default option
    this.addOption(this.questions.length - 1);
  }

  removeQuestion(index: number) {
    this.questions.removeAt(index);
  }

  addOption(questionIndex: number) {
    const optionGroup = this.fb.group({
      score: [0, Validators.required],
      text: ['', Validators.required]
    });
    this.options(questionIndex).push(optionGroup);
  }

  removeOption(questionIndex: number, optionIndex: number) {
    this.options(questionIndex).removeAt(optionIndex);
  }

  loadTest(id: number) {
    this.assessmentService.getTestById(id).subscribe(test => {
      this.testForm.patchValue({
        name: test.name,
        description: test.description
      });
      
      const parsedQuestions = JSON.parse(test.questionsJson);
      parsedQuestions.forEach((q: any, qIndex: number) => {
        const questionGroup = this.fb.group({
          id: [q.id],
          text: [q.text, Validators.required],
          options: this.fb.array([])
        });
        this.questions.push(questionGroup);
        
        q.options.forEach((opt: any) => {
          const optionGroup = this.fb.group({
            score: [opt.score, Validators.required],
            text: [opt.text, Validators.required]
          });
          this.options(qIndex).push(optionGroup);
        });
      });
    });
  }

  saveTest() {
    if (this.testForm.invalid) {
      this.testForm.markAllAsTouched();
      return;
    }

    this.isSaving = true;
    const formValue = this.testForm.value;
    
    // Ensure IDs are sequential for questions
    const questionsJsonObj = formValue.questions.map((q: any, i: number) => ({
      ...q,
      id: i + 1
    }));

    const testData: PsychometricTest = {
      id: this.testId || 0,
      name: formValue.name,
      description: formValue.description,
      questionsJson: JSON.stringify(questionsJsonObj)
    };

    if (this.isEditMode) {
      this.assessmentService.updateTest(this.testId!, testData).subscribe({
        next: () => this.goBack(),
        error: (err) => { this.isSaving = false; alert('Error: ' + err.message); }
      });
    } else {
      this.assessmentService.createTest(testData).subscribe({
        next: () => this.goBack(),
        error: (err) => { this.isSaving = false; alert('Error: ' + err.message); }
      });
    }
  }

  goBack() {
    this.router.navigate(['/tests-catalog']);
  }
}
