import type { MockedObject } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AssessmentListComponent } from './assessment-list.component';
import { AssessmentService } from '../../../core/services/assessment.service';
import { of } from 'rxjs';
import { PageResponse } from '../../../core/models/page.model';
import { Assessment } from '../../../core/models/assessment.model';

describe('AssessmentListComponent', () => {
  let component: AssessmentListComponent;
  let fixture: ComponentFixture<AssessmentListComponent>;
  let assessmentServiceSpy: MockedObject<AssessmentService>;

  const mockAssessments: Assessment[] = [
    {
      id: 1,
      patientId: 10,
      psychometricTestId: 5,
      testName: 'PHQ-9',
      totalScore: 18,
      answersJson: '{"1":3}',
      assessmentDate: '2026-05-10T10:00:00',
      interpretationJson: '[{"min":15,"max":19,"label":"Grave","color":"red"}]'
    },
    {
      id: 2,
      patientId: 10,
      psychometricTestId: 5,
      testName: 'PHQ-9',
      totalScore: 8,
      answersJson: '{"1":1}',
      assessmentDate: '2026-08-10T10:00:00',
      interpretationJson: '[{"min":5,"max":9,"label":"Leve","color":"yellow"}]'
    }
  ];

  const mockPage: PageResponse<Assessment> = {
    content: mockAssessments,
    page: {
      totalElements: 2,
      totalPages: 1,
      size: 10,
      number: 0
    }
  };

  beforeEach(async () => {
    assessmentServiceSpy = {
      getAssessmentsByPatient: vi.fn().mockName('AssessmentService.getAssessmentsByPatient'),
      getPatientEvolution: vi.fn().mockName('AssessmentService.getPatientEvolution'),
      getAvailableTests: vi.fn().mockName('AssessmentService.getAvailableTests')
    } as unknown as MockedObject<AssessmentService>;
    assessmentServiceSpy.getAssessmentsByPatient.mockReturnValue(of(mockPage));
    assessmentServiceSpy.getPatientEvolution.mockReturnValue(of(mockAssessments));
    assessmentServiceSpy.getAvailableTests.mockReturnValue(of({ content: [], page: { totalElements: 0, totalPages: 0, size: 10, number: 0 } }));

    await TestBed.configureTestingModule({
      imports: [AssessmentListComponent],
      providers: [
        { provide: AssessmentService, useValue: assessmentServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AssessmentListComponent);
    component = fixture.componentInstance;
    component.patientId = 10;
    fixture.detectChanges();
  });

  it('should create and load evolution history', () => {
    expect(component).toBeTruthy();
    expect(component.allAssessments.length).toBe(2);
    expect(component.availableTestNames).toContain('PHQ-9');
  });

  it('should compute evolution summary and clinical improvement trend', () => {
    expect(component.summaries.length).toBe(1);
    const summary = component.summaries[0];
    expect(summary.testName).toBe('PHQ-9');
    expect(summary.initialScore).toBe(18);
    expect(summary.latestScore).toBe(8);
    expect(summary.delta).toBe(-10);
    expect(summary.trend).toBe('improvement');
  });

  it('should toggle form modal state', () => {
    expect(component.showForm).toBe(false);
    component.openForm();
    expect(component.showForm).toBe(true);
    component.closeForm(false);
    expect(component.showForm).toBe(false);
  });
});
