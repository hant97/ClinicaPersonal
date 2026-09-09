import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import { CatalogService } from '../../../core/services/catalog.service';
import { ClinicalServiceService } from '../../../core/services/clinical-service.service';
import { CatalogItem } from '../../../core/models/catalog.model';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { ClinicalServicesFormComponent } from './clinical-services-form.component';

describe('ClinicalServicesFormComponent', () => {
  let fixture: ComponentFixture<ClinicalServicesFormComponent>;
  let catalogService: jasmine.SpyObj<CatalogService>;

  const categories: CatalogItem[] = [
    { id: 1, catalogId: 10, itemCode: 'EVALUACION', itemName: 'Evaluación', isActive: true, orderIndex: 0 },
    { id: 2, catalogId: 10, itemCode: 'TERAPIA', itemName: 'Terapia', isActive: true, orderIndex: 1 }
  ];

  beforeEach(async () => {
    catalogService = jasmine.createSpyObj('CatalogService', ['getActiveItemsByCatalogCode']);
    catalogService.getActiveItemsByCatalogCode.and.returnValue(of(categories));

    await TestBed.configureTestingModule({
      imports: [ClinicalServicesFormComponent],
      providers: [
        { provide: CatalogService, useValue: catalogService },
        { provide: ClinicalServiceService, useValue: jasmine.createSpyObj('ClinicalServiceService', ['getServiceById', 'createService', 'updateService']) },
        { provide: ToastService, useValue: jasmine.createSpyObj('ToastService', ['show']) }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ClinicalServicesFormComponent);
    fixture.detectChanges();
  });

  it('carga las categorías activas del catálogo maestro', () => {
    expect(catalogService.getActiveItemsByCatalogCode).toHaveBeenCalledWith('CLINICAL_SERVICE_CATEGORY');

    const options = fixture.debugElement.queryAll(By.css('#category option'));
    expect(options.map(option => option.nativeElement.textContent.trim())).toEqual([
      'Sin categoría',
      'Evaluación',
      'Terapia'
    ]);
    expect(options[1].nativeElement.value).toBe('EVALUACION');
  });
});
