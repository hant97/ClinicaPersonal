import type { MockedObject } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CatalogManagementComponent } from './catalog-management.component';
import { CatalogService } from '../../../core/services/catalog.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { of } from 'rxjs';
import { Catalog, CatalogItem } from '../../../core/models/catalog.model';
import { SpecialtyItem } from '../../../core/models/specialty.model';
import { AuthService } from '../../../core/services/auth.service';

describe('CatalogManagementComponent', () => {
  let component: CatalogManagementComponent;
  let fixture: ComponentFixture<CatalogManagementComponent>;
  let catalogServiceSpy: MockedObject<CatalogService>;
  let specialtyServiceSpy: MockedObject<SpecialtyService>;
  let notificationServiceSpy: MockedObject<NotificationService>;
  let toastServiceSpy: MockedObject<ToastService>;
  let authServiceSpy: MockedObject<AuthService>;

  const mockSpecialties: SpecialtyItem[] = [
    { id: 1, code: 'PSICOLOGIA', name: 'Psicología', active: true, displayOrder: 1 },
    { id: 2, code: 'DERMATOLOGIA', name: 'Dermatología', active: true, displayOrder: 2 }
  ];

  const mockCatalogs: Catalog[] = [
    {
      id: 1,
      code: 'PAYMENT_METHOD',
      name: 'Método de Pago',
      description: 'Formas de pago',
      specialty: 'GENERAL',
      items: [
        { id: 101, catalogId: 1, itemCode: 'EFECTIVO', itemName: 'Efectivo', isActive: true, orderIndex: 0 },
        { id: 102, catalogId: 1, itemCode: 'YAPE', itemName: 'Yape', isActive: true, orderIndex: 1 },
        { id: 103, catalogId: 1, itemCode: 'PLIN', itemName: 'Plin', isActive: false, orderIndex: 2 }
      ]
    },
    {
      id: 2,
      code: 'RISK_ALERT_TYPE',
      name: 'Tipo de Alerta de Riesgo',
      description: 'Riesgos clínicos',
      specialty: 'PSICOLOGIA',
      items: [
        { id: 201, catalogId: 2, itemCode: 'AUTOLESION', itemName: 'Autolesión', isActive: true, orderIndex: 0 }
      ]
    },
    {
      id: 3,
      code: 'SKIN_TYPE',
      name: 'Tipo de Piel',
      description: 'Dermatología piel',
      specialty: 'DERMATOLOGIA',
      items: [
        { id: 301, catalogId: 3, itemCode: 'PIEL_SECA', itemName: 'Piel Seca', isActive: true, orderIndex: 0 }
      ]
    }
  ];

  beforeEach(async () => {
    catalogServiceSpy = {
      getAllAccessibleCatalogs: vi.fn().mockName('CatalogService.getAllAccessibleCatalogs'),
      addCatalogItem: vi.fn().mockName('CatalogService.addCatalogItem'),
      updateCatalogItem: vi.fn().mockName('CatalogService.updateCatalogItem'),
      deleteCatalogItem: vi.fn().mockName('CatalogService.deleteCatalogItem'),
      reorderCatalogItems: vi.fn().mockName('CatalogService.reorderCatalogItems'),
      createCatalog: vi.fn().mockName('CatalogService.createCatalog'),
      updateCatalog: vi.fn().mockName('CatalogService.updateCatalog')
    } as unknown as MockedObject<CatalogService>;
    specialtyServiceSpy = {
      getActiveSpecialties: vi.fn().mockName('SpecialtyService.getActiveSpecialties')
    } as unknown as MockedObject<SpecialtyService>;
    notificationServiceSpy = {
      alert: vi.fn().mockName('NotificationService.alert')
    } as unknown as MockedObject<NotificationService>;
    toastServiceSpy = {
      success: vi.fn().mockName('ToastService.success'),
      error: vi.fn().mockName('ToastService.error'),
      info: vi.fn().mockName('ToastService.info'),
      warning: vi.fn().mockName('ToastService.warning'),
      show: vi.fn().mockName('ToastService.show')
    } as unknown as MockedObject<ToastService>;
    authServiceSpy = {
      hasRole: vi.fn().mockName('AuthService.hasRole')
    } as unknown as MockedObject<AuthService>;
    authServiceSpy.hasRole.mockReturnValue(false);

    specialtyServiceSpy.getActiveSpecialties.mockReturnValue(of(mockSpecialties));

    const clonedCatalogs: Catalog[] = JSON.parse(JSON.stringify(mockCatalogs));
    catalogServiceSpy.getAllAccessibleCatalogs.mockReturnValue(of(clonedCatalogs));

    await TestBed.configureTestingModule({
      imports: [CatalogManagementComponent],
      providers: [
        { provide: CatalogService, useValue: catalogServiceSpy },
        { provide: SpecialtyService, useValue: specialtyServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy },
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CatalogManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe inicializarse y cargar los catálogos seleccionando el primero', () => {
    expect(catalogServiceSpy.getAllAccessibleCatalogs).toHaveBeenCalledWith(undefined);
    expect(component.catalogs.length).toBe(3);
    expect(component.selectedCatalog?.code).toBe('PAYMENT_METHOD');
    expect(component.items.length).toBe(3);
  });

  it('solo solicita todos los ámbitos para el administrador global', () => {
    catalogServiceSpy.getAllAccessibleCatalogs.mockClear();
    authServiceSpy.hasRole.mockReturnValue(true);

    component.loadCatalogs();

    expect(catalogServiceSpy.getAllAccessibleCatalogs).toHaveBeenCalledWith('ALL');
  });

  it('debe filtrar catálogos por especialidad', () => {
    component.setSpecialtyFilter('PSICOLOGIA');
    expect(component.filteredCatalogs.length).toBe(1);
    expect(component.filteredCatalogs[0].code).toBe('RISK_ALERT_TYPE');
    expect(component.selectedCatalog?.code).toBe('RISK_ALERT_TYPE');

    component.setSpecialtyFilter('DERMATOLOGIA');
    expect(component.filteredCatalogs.length).toBe(1);
    expect(component.filteredCatalogs[0].code).toBe('SKIN_TYPE');

    component.setSpecialtyFilter('ALL');
    expect(component.filteredCatalogs.length).toBe(3);
  });

  it('debe filtrar catálogos por texto de búsqueda', () => {
    component.catalogSearchQuery = 'Piel';
    expect(component.filteredCatalogs.length).toBe(1);
    expect(component.filteredCatalogs[0].name).toBe('Tipo de Piel');
  });

  it('debe auto-generar el código interno al escribir el nombre de una nueva opción', () => {
    component.newItemName = 'Tarjeta de Crédito / Débito';
    component.onNewItemNameChange();
    expect(component.newItemCode).toBe('TARJETA_DE_CREDITO_DEBITO');
  });

  it('debe agregar una nueva opción con código auto-generado', () => {
    const savedItem: CatalogItem = {
      id: 104,
      catalogId: 1,
      itemCode: 'TRANSFERENCIA',
      itemName: 'Transferencia',
      isActive: true,
      orderIndex: 3
    };
    catalogServiceSpy.addCatalogItem.mockReturnValue(of(savedItem));

    component.newItemName = 'Transferencia';
    component.addItem();

    expect(catalogServiceSpy.addCatalogItem).toHaveBeenCalledWith('PAYMENT_METHOD', expect.objectContaining({
      itemCode: 'TRANSFERENCIA',
      itemName: 'Transferencia',
      isActive: true
    }));
    expect(component.items.length).toBe(4);
    expect(toastServiceSpy.success).toHaveBeenCalledWith(expect.stringMatching(/Transferencia/));
  });

  it('debe permitir la edición inline del nombre de una opción', () => {
    const targetItem = component.items[0];
    component.startEditItem(targetItem);
    expect(component.editingItemId).toBe(targetItem.id!);

    component.editingItemName = 'Efectivo en Caja';
    const updatedItem = { ...targetItem, itemName: 'Efectivo en Caja' };
    catalogServiceSpy.updateCatalogItem.mockReturnValue(of(updatedItem));

    component.saveEditItem(targetItem);

    expect(catalogServiceSpy.updateCatalogItem).toHaveBeenCalledWith(targetItem.id!, expect.objectContaining({
      itemName: 'Efectivo en Caja'
    }), 'PAYMENT_METHOD');
    expect(targetItem.itemName).toBe('Efectivo en Caja');
    expect(component.editingItemId).toBeNull();
  });

  it('debe alternar el estado activo/inactivo', () => {
    const item = component.items[0];
    item.isActive = false;
    catalogServiceSpy.updateCatalogItem.mockReturnValue(of(item));

    component.toggleActive(item);

    expect(catalogServiceSpy.updateCatalogItem).toHaveBeenCalled();
    expect(toastServiceSpy.success).toHaveBeenCalledWith(expect.stringMatching(/desactivada/));
  });

  it('debe reordenar opciones hacia arriba y abajo', () => {
    const reordered: CatalogItem[] = [component.items[1], component.items[0], component.items[2]];
    catalogServiceSpy.reorderCatalogItems.mockReturnValue(of(reordered));

    component.moveItemDown(0);

    expect(catalogServiceSpy.reorderCatalogItems).toHaveBeenCalledWith('PAYMENT_METHOD', [102, 101, 103]);
    expect(component.items[0].itemCode).toBe('YAPE');
  });

  it('debe eliminar una opción tras confirmar en el modal', () => {
    const itemToDelete = component.items[0];
    component.confirmDeleteItem(itemToDelete);
    expect(component.showDeleteModal).toBe(true);
    expect(component.itemToDelete).toBe(itemToDelete);

    catalogServiceSpy.deleteCatalogItem.mockReturnValue(of(void 0));
    component.executeDeleteItem();

    expect(catalogServiceSpy.deleteCatalogItem).toHaveBeenCalledWith(itemToDelete.id!, 'PAYMENT_METHOD');
    expect(component.items.find(i => i.id === itemToDelete.id)).toBeUndefined();
    expect(component.showDeleteModal).toBe(false);
  });

  it('debe crear un nuevo catálogo maestro', () => {
    component.openCreateCatalogModal();
    expect(component.showCatalogModal).toBe(true);
    expect(component.isEditingCatalog).toBe(false);

    component.catalogFormName = 'Tipo de Sangre';
    component.onCatalogFormNameChange();
    expect(component.catalogFormCode).toBe('TIPO_DE_SANGRE');

    const createdCatalog: Catalog = {
      id: 4,
      name: 'Tipo de Sangre',
      code: 'TIPO_DE_SANGRE',
      specialty: 'GENERAL',
      items: []
    };
    catalogServiceSpy.createCatalog.mockReturnValue(of(createdCatalog));

    component.saveCatalog();

    expect(catalogServiceSpy.createCatalog).toHaveBeenCalledWith(expect.objectContaining({
      name: 'Tipo de Sangre',
      code: 'TIPO_DE_SANGRE'
    }));
    expect(component.showCatalogModal).toBe(false);
  });

  it('debe resolver la etiqueta de especialidad dinámicamente', () => {
    expect(component.getSpecialtyLabel('GENERAL')).toBe('General');
    expect(component.getSpecialtyLabel('PSICOLOGIA')).toBe('Psicología');
    expect(component.getSpecialtyLabel('DERMATOLOGIA')).toBe('Dermatología');
    expect(component.getSpecialtyLabel('NUTRICION')).toBe('Nutricion');
  });
});
