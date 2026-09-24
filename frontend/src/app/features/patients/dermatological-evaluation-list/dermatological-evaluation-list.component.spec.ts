import type { MockedObject } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { DermatologicalEvaluationListComponent } from './dermatological-evaluation-list.component';
import { DermatologicalEvaluationService } from '../../../core/services/dermatological-evaluation.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { PageResponse } from '../../../core/models/page.model';
import { DermatologicalEvaluation } from '../../../core/models/dermatological-evaluation.model';

const firstPage: PageResponse<DermatologicalEvaluation> = {
  content: [{ id: 1, patientId: 42, evaluationDate: '2026-08-01', dermatologicalDiagnosis: 'Acné' }],
  page: { number: 0, size: 10, totalElements: 11, totalPages: 2 }
};

describe('DermatologicalEvaluationListComponent', () => {
  let component: DermatologicalEvaluationListComponent;
  let fixture: ComponentFixture<DermatologicalEvaluationListComponent>;
  let evaluationService: MockedObject<DermatologicalEvaluationService>;
  let toastService: MockedObject<ToastService>;

  beforeEach(async () => {
    evaluationService = {
      getByPatientId: vi.fn().mockName('DermatologicalEvaluationService.getByPatientId'),
      delete: vi.fn().mockName('DermatologicalEvaluationService.delete')
    } as unknown as MockedObject<DermatologicalEvaluationService>;
    toastService = {
      show: vi.fn().mockName('ToastService.show')
    } as unknown as MockedObject<ToastService>;
    evaluationService.getByPatientId.mockReturnValue(of(firstPage));

    await TestBed.configureTestingModule({
      imports: [DermatologicalEvaluationListComponent],
      providers: [
        { provide: DermatologicalEvaluationService, useValue: evaluationService },
        { provide: ToastService, useValue: toastService },
        { provide: NotificationService, useValue: { confirm: () => Promise.resolve(false) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DermatologicalEvaluationListComponent);
    component = fixture.componentInstance;
    component.patientId = 42;
  });

  it('carga la primera página y muestra sus totales', () => {
    fixture.detectChanges();

    expect(evaluationService.getByPatientId).toHaveBeenCalledWith(42, 0, 10);
    expect(component.evaluations).toEqual(firstPage.content);
    expect(component.totalPages).toBe(2);
    expect(component.totalElements).toBe(11);
    expect(fixture.nativeElement.textContent).toContain('11 evaluaciones registradas');
  });

  it('conserva paciente y tamaño al ir a la página siguiente', () => {
    evaluationService.getByPatientId.mockReturnValueOnce(of(firstPage)).mockReturnValueOnce(of({ ...firstPage, content: [], page: { ...firstPage.page, number: 1 } }));
    fixture.detectChanges();

    component.onPageChange(1);

    expect(evaluationService.getByPatientId).toHaveBeenCalledWith(42, 1, 10);
    expect(component.currentPage).toBe(1);
  });

  it('muestra el estado vacío cuando la página no tiene evaluaciones', () => {
    evaluationService.getByPatientId.mockReturnValue(of({
      content: [], page: { number: 0, size: 10, totalElements: 0, totalPages: 0 }
    }));

    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No hay evaluaciones dermatológicas registradas');
  });

  it('informa el error de carga al usuario', () => {
    evaluationService.getByPatientId.mockReturnValue(throwError(() => new Error('Error de red')));
    vi.spyOn(console, 'error');

    fixture.detectChanges();

    expect(toastService.show).toHaveBeenCalledWith('Error al cargar evaluaciones', 'error');
  });
});
