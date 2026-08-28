import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AssessmentService } from '../../../core/services/assessment.service';
import { PsychometricTest, Question } from '../../../core/models/assessment.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ViewPreferenceService } from '../../../shared/services/view-preference/view-preference.service';

import { LucideAngularModule } from 'lucide-angular';
import {
  Plus, Edit, Trash2, Eye, Brain, ClipboardList, LayoutGrid, List, X, Activity, Clock
} from '../../../shared/icons/lucide-icons';

@Component({
  selector: 'app-tests-catalog-list',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, PaginationComponent],
  templateUrl: './tests-catalog-list.component.html'
})
export class TestsCatalogListComponent implements OnInit {
  readonly Plus = Plus;
  readonly Edit = Edit;
  readonly Trash2 = Trash2;
  readonly Eye = Eye;
  readonly Brain = Brain;
  readonly ClipboardList = ClipboardList;
  readonly LayoutGrid = LayoutGrid;
  readonly List = List;
  readonly X = X;
  readonly Activity = Activity;
  readonly Clock = Clock;

  tests: PsychometricTest[] = [];
  viewMode: 'table' | 'cards' = 'cards';
  previewingTest: PsychometricTest | null = null;

  currentPage: number = 0;
  pageSize: number = 10;
  totalPages: number = 0;
  totalElements: number = 0;
  isLoading = false;
  loadError = false;

  constructor(
    private assessmentService: AssessmentService,
    private router: Router,
    private toastService: ToastService,
    private notificationService: NotificationService,
    private viewPreferenceService: ViewPreferenceService
  ) {}

  ngOnInit() {
    this.viewMode = this.viewPreferenceService.getViewMode<'table' | 'cards'>('tests_catalog_view_mode', 'cards', 'cards');
    this.loadTests();
  }

  loadTests() {
    this.isLoading = true;
    this.loadError = false;
    this.assessmentService.getAvailableTests(this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        this.totalPages = page.page.totalPages;
        this.totalElements = page.page.totalElements;
        this.tests = page.content;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.loadError = true;
        this.toastService.show('Error al cargar las pruebas', 'error');
      }
    });
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadTests();
  }

  createNewTest() {
    this.router.navigate(['/tests-catalog/new']);
  }

  editTest(id: number) {
    this.router.navigate(['/tests-catalog/edit', id]);
  }

  async deleteTest(id: number): Promise<void> {
    const confirmed = await this.notificationService.confirm(
      'Eliminar prueba',
      '¿Está seguro de que desea eliminar este test? Si el test ya ha sido utilizado por pacientes, no se podrá eliminar.',
      'Sí, eliminar',
      'Cancelar'
    );
    if (confirmed) {
      this.assessmentService.deleteTest(id).subscribe({
        next: () => {
          this.toastService.show('Prueba eliminada exitosamente', 'success');
          this.loadTests();
        },
        error: (err) => {
          this.toastService.show('Error al eliminar: ' + (err.error?.message || err.message), 'error');
        }
      });
    }
  }

  getQuestions(test: PsychometricTest): Question[] {
    try {
      const parsed = JSON.parse(test.questionsJson);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }

  questionCount(test: PsychometricTest): number {
    return this.getQuestions(test).length;
  }

  optionCount(test: PsychometricTest): number {
    return this.getQuestions(test).reduce((sum, q) => sum + (q.options?.length ?? 0), 0);
  }

  openPreview(test: PsychometricTest): void {
    this.previewingTest = test;
  }

  closePreview(): void {
    this.previewingTest = null;
  }

  setViewMode(mode: 'table' | 'cards'): void {
    this.viewMode = mode;
    this.viewPreferenceService.setViewMode('tests_catalog_view_mode', mode);
  }
}
