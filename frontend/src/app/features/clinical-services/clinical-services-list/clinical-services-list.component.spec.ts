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
  let catalogService: jasmine.SpyObj<CatalogService>;

  beforeEach(async () => {
    catalogService = jasmine.createSpyObj('CatalogService', ['getActiveItemsByCatalogCode']);
    catalogService.getActiveItemsByCatalogCode.and.returnValue(of([
      { id: 1, catalogId: 10, itemCode: 'EVALUACION', itemName: 'Evaluación', isActive: true, orderIndex: 0 }
    ]));

    const clinicalService = jasmine.createSpyObj('ClinicalServiceService', ['getAllServices', 'getStats', 'deleteService']);
    clinicalService.getAllServices.and.returnValue(of({
      content: [{ id: 1, name: 'Consulta general', price: 50, category: 'EVALUACION', active: true }],
      page: { totalElements: 1, totalPages: 1, number: 0, size: 10 }
    }));
    clinicalService.getStats.and.returnValue(of({
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
        { provide: SpecialtyService, useValue: jasmine.createSpyObj('SpecialtyService', { getActiveSpecialties: of([]) }) },
        { provide: ToastService, useValue: jasmine.createSpyObj('ToastService', ['show']) },
        { provide: NotificationService, useValue: jasmine.createSpyObj('NotificationService', ['confirm']) },
        { provide: ViewPreferenceService, useValue: jasmine.createSpyObj('ViewPreferenceService', { getViewMode: 'cards', setViewMode: undefined }) }
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
