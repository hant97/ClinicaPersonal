import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AssessmentService } from '../../../core/services/assessment.service';
import { PsychometricTest } from '../../../core/models/assessment.model';

import { LucideAngularModule, Edit, Trash2 } from 'lucide-angular';

@Component({
  selector: 'app-tests-catalog-list',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './tests-catalog-list.component.html',
  styleUrl: './tests-catalog-list.component.css'
})
export class TestsCatalogListComponent implements OnInit {
  readonly Edit = Edit;
  readonly Trash2 = Trash2;

  tests: PsychometricTest[] = [];

  constructor(
    private assessmentService: AssessmentService,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadTests();
  }

  loadTests() {
    this.assessmentService.getAvailableTests().subscribe(data => {
      this.tests = data;
    });
  }

  createNewTest() {
    this.router.navigate(['/tests-catalog/new']);
  }

  editTest(id: number) {
    this.router.navigate(['/tests-catalog/edit', id]);
  }

  deleteTest(id: number) {
    if (confirm('¿Está seguro de que desea eliminar este test? Si el test ya ha sido utilizado por pacientes, no se podrá eliminar.')) {
      this.assessmentService.deleteTest(id).subscribe({
        next: () => {
          this.loadTests();
        },
        error: (err) => {
          alert('Error al eliminar: ' + (err.error?.message || err.message));
        }
      });
    }
  }
}
