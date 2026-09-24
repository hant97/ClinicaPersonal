import type { MockedObject } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import { CatalogService } from '../../../core/services/catalog.service';
import { ClinicalServiceService } from '../../../core/services/clinical-service.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { ViewPreferenceService } from '../../../shared/services/view-preference/view-preference.service';
import { ClinicalServicesListComponent } from './clinical-services-list.component';

describe('ClinicalServicesListComponent', () => {
  let fixture: ComponentFixture<ClinicalServicesListComponent>;
  let catalogService: MockedObject<CatalogService>;

  beforeEach(async () => {
    catalogService = {
      getActiveItemsByCatalogCode: vi.fn().mockName('CatalogService.getActiveItemsByCatalogCode')
    } as unknown as MockedObject<CatalogService>;
    catalogService.getActiveItemsByCatalogCode.mockReturnValue(of([
      { id: 1, catalogId: 10, itemCode: 'EVALUACION', itemName: 'Evaluación', isActive: true, orderIndex: 0 }
    ]));

    const clinicalService = {
      getAllServices: vi.fn().mockName('ClinicalServiceService.getAllServices'),
      getStats: vi.fn().mockName('ClinicalServiceService.getStats'),
      deleteService: vi.fn().mockName('ClinicalServiceService.deleteService')
    };
    clinicalService.getAllServices.mockReturnValue(of({
      content: [{ id: 1, name: 'Consulta general', price: 50, category: 'EVALUACION', active: true }],
      page: { totalElements: 1, totalPages: 1, number: 0, size: 10 }
    }));
    clinicalService.getStats.mockReturnValue(of({
      totalServices: 1,
      activeCount: 1,
      averagePrice: 50,
      topByRevenue: [],
      topByQuantity: []
    }));

    await TestBed.configureTestingModule({
      imports: [ClinicalServicesListComponent],
      providers: [
        { provide: CatalogService, useValue: catalogService },
        { provide: ClinicalServiceService, useValue: clinicalService },
        { provide: SpecialtyService, useValue: {
            getActiveSpecialties: vi.fn().mockName('SpecialtyService.getActiveSpecialties').mockReturnValue(of([]))
          } },
        { provide: ToastService, useValue: {
            show: vi.fn().mockName('ToastService.show')
          } },
        { provide: NotificationService, useValue: {
            confirm: vi.fn().mockName('NotificationService.confirm')
          } },
        { provide: ViewPreferenceService, useValue: {
            getViewMode: vi.fn().mockName('ViewPreferenceService.getViewMode').mockReturnValue('cards'),
            setViewMode: vi.fn().mockName('ViewPreferenceService.setViewMode').mockReturnValue(undefined)
          } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ClinicalServicesListComponent);
    fixture.detectChanges();
  });

  it('usa códigos del catálogo en el filtro y muestra sus etiquetas', () => {
    expect(catalogService.getActiveItemsByCatalogCode).toHaveBeenCalledWith('CLINICAL_SERVICE_CATEGORY');

    const options = fixture.debugElement.queryAll(By.css('#serviceCategoryFilter option'));
    expect(options[1].nativeElement.value).toBe('EVALUACION');
    expect(options[1].nativeElement.textContent.trim()).toBe('Evaluación');
    expect(fixture.nativeElement.textContent).toContain('Evaluación');
  });
});
