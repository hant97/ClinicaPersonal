import { Component, OnInit } from '@angular/core';

import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AssessmentService } from '../../../core/services/assessment.service';
import { PsychometricTest, Question, Option, InterpretationBand, BAND_COLOR_PALETTE } from '../../../core/models/assessment.model';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { LucideAngularModule, Plus, Eye, ChevronUp, ChevronDown, ClipboardList, X } from 'lucide-angular';

interface TestCatalogFormValue {
  name: string;
  description: string;
  questions: Question[];
  interpretations: InterpretationBand[];
}

@Component({
  selector: 'app-tests-catalog-form',
  standalone: true,
  imports: [ReactiveFormsModule, LucideAngularModule],
  templateUrl: './tests-catalog-form.component.html'
})
export class TestsCatalogFormComponent implements OnInit {
  readonly Plus = Plus;
  readonly Eye = Eye;
  readonly ChevronUp = ChevronUp;
  readonly ChevronDown = ChevronDown;
  readonly ClipboardList = ClipboardList;
  readonly X = X;
  readonly BAND_COLOR_PALETTE = BAND_COLOR_PALETTE;

  testForm!: FormGroup;
  isEditMode = false;
  testId?: number;
  isSaving = false;
  showPreview = false;

  constructor(
    private fb: FormBuilder,
    private assessmentService: AssessmentService,
    private router: Router,
    private route: ActivatedRoute,
    private toastService: ToastService,
    private notificationService: NotificationService
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
      this.addQuestion();
    }
  }

  createForm() {
    this.testForm = this.fb.group({
      name: ['', Validators.required],
      description: ['', Validators.required],
      questions: this.fb.array([]),
      interpretations: this.fb.array([])
    });
  }

  get questions() {
    return this.testForm.get('questions') as FormArray;
  }

  get interpretations() {
    return this.testForm.get('interpretations') as FormArray;
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
    this.addOption(this.questions.length - 1);
  }

  removeQuestion(index: number) {
    this.questions.removeAt(index);
  }

  moveQuestion(index: number, direction: -1 | 1) {
    const newIndex = index + direction;
    if (newIndex < 0 || newIndex >= this.questions.length) {
      return;
    }
    const control = this.questions.at(index);
    this.questions.removeAt(index);
    this.questions.insert(newIndex, control);
  }

  addOption(questionIndex: number) {
    const optionGroup = this.fb.group({
      score: [0, [Validators.required, Validators.min(0)]],
      text: ['', Validators.required]
    });
    this.options(questionIndex).push(optionGroup);
  }

  removeOption(questionIndex: number, optionIndex: number) {
    this.options(questionIndex).removeAt(optionIndex);
  }

  moveOption(questionIndex: number, optionIndex: number, direction: -1 | 1) {
    const arr = this.options(questionIndex);
    const newIndex = optionIndex + direction;
    if (newIndex < 0 || newIndex >= arr.length) {
      return;
    }
    const control = arr.at(optionIndex);
    arr.removeAt(optionIndex);
    arr.insert(newIndex, control);
  }

  optionCountOf(questionIndex: number): number {
    return this.options(questionIndex).length;
  }

  addBand() {
    const bandGroup = this.fb.group({
      minScore: [0, [Validators.required, Validators.min(0)]],
      maxScore: [0, [Validators.required, Validators.min(0)]],
      label: ['', Validators.required],
      color: ['slate'],
      description: ['']
    });
    this.interpretations.push(bandGroup);
  }

  removeBand(index: number) {
    this.interpretations.removeAt(index);
  }

  moveBand(index: number, direction: -1 | 1) {
    const newIndex = index + direction;
    if (newIndex < 0 || newIndex >= this.interpretations.length) {
      return;
    }
    const control = this.interpretations.at(index);
    this.interpretations.removeAt(index);
    this.interpretations.insert(newIndex, control);
  }

  get maxPossibleScore(): number {
    const questions = this.questions.getRawValue() as Question[];
    return questions.reduce((sum, q) => {
      const scores = (q.options || []).map((o: Option) => Number(o.score) || 0);
      return sum + (scores.length ? Math.max(...scores) : 0);
    }, 0);
  }

  get hasOverlappingBands(): boolean {
    const raw = this.interpretations.getRawValue() as InterpretationBand[];
    const bands = raw
      .filter((b) => b.minScore != null && b.maxScore != null)
      .map((b) => ({ min: Number(b.minScore), max: Number(b.maxScore) }))
      .sort((a, b) => a.min - b.min);
    for (let i = 1; i < bands.length; i++) {
      if (bands[i].min <= bands[i - 1].max) {
        return true;
      }
    }
    return false;
  }

  openPreview(): void {
    this.showPreview = true;
  }

  closePreview(): void {
    this.showPreview = false;
  }

  loadTest(id: number) {
    this.assessmentService.getTestById(id).subscribe(test => {
      this.testForm.patchValue({
        name: test.name,
        description: test.description
      });

      const parsedQuestions = JSON.parse(test.questionsJson) as Question[];
      parsedQuestions.forEach((q, qIndex) => {
        const questionGroup = this.fb.group({
          id: [q.id],
          text: [q.text, Validators.required],
          options: this.fb.array([])
        });
        this.questions.push(questionGroup);

        q.options.forEach((opt) => {
          const optionGroup = this.fb.group({
            score: [opt.score, [Validators.required, Validators.min(0)]],
            text: [opt.text, Validators.required]
          });
          this.options(qIndex).push(optionGroup);
        });
      });

      if (test.interpretationJson) {
        const parsedBands = JSON.parse(test.interpretationJson) as InterpretationBand[];
        parsedBands.forEach((band) => {
          const bandGroup = this.fb.group({
            minScore: [band.minScore, [Validators.required, Validators.min(0)]],
            maxScore: [band.maxScore, [Validators.required, Validators.min(0)]],
            label: [band.label, Validators.required],
            color: [band.color || 'slate'],
            description: [band.description || '']
          });
          this.interpretations.push(bandGroup);
        });
      }

      this.testForm.markAsPristine();
    });
  }

  saveTest() {
    if (this.testForm.invalid) {
      this.testForm.markAllAsTouched();
      return;
    }

    this.isSaving = true;
    const formValue = this.testForm.getRawValue() as TestCatalogFormValue;

    // Ensure IDs are sequential for questions
    const questionsJsonObj = formValue.questions.map((q, i) => ({
      ...q,
      id: i + 1
    }));

    const interpretations = (formValue.interpretations || []).map((band) => ({
      minScore: Number(band.minScore),
      maxScore: Number(band.maxScore),
      label: band.label,
      color: band.color,
      description: band.description || ''
    }));

    const testData: PsychometricTest = {
      id: this.testId || 0,
      name: formValue.name,
      description: formValue.description,
      questionsJson: JSON.stringify(questionsJsonObj),
      interpretationJson: interpretations.length ? JSON.stringify(interpretations) : null
    };

    if (this.isEditMode) {
      this.assessmentService.updateTest(this.testId!, testData).subscribe({
        next: () => this.router.navigate(['/tests-catalog']),
        error: (err) => { this.isSaving = false; this.toastService.show('Error al guardar: ' + (err.error?.message || err.message), 'error'); }
      });
    } else {
      this.assessmentService.createTest(testData).subscribe({
        next: () => this.router.navigate(['/tests-catalog']),
        error: (err) => { this.isSaving = false; this.toastService.show('Error al guardar: ' + (err.error?.message || err.message), 'error'); }
      });
    }
  }

  async goBack() {
    if (this.testForm.dirty) {
      const confirmed = await this.notificationService.confirm(
        'Salir sin guardar',
        'Tienes cambios sin guardar. ¿Deseas salir de todas formas?',
        'Salir',
        'Cancelar'
      );
      if (!confirmed) {
        return;
      }
    }
    this.router.navigate(['/tests-catalog']);
  }
}
