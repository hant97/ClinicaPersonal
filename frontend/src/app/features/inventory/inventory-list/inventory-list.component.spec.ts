import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { InventoryListComponent } from './inventory-list.component';
import { InventoryService } from '../../../core/services/inventory.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ViewPreferenceService } from '../../../shared/services/view-preference/view-preference.service';

describe('InventoryListComponent', () => {
  let component: InventoryListComponent;
  let fixture: ComponentFixture<InventoryListComponent>;
  let viewPreferenceService: jasmine.SpyObj<ViewPreferenceService>;

  beforeEach(async () => {
    const inventoryService = jasmine.createSpyObj<InventoryService>('InventoryService', [
      'getAllSupplies',
      'getStats',
      'getRecentTransactions'
    ]);
    inventoryService.getAllSupplies.and.returnValue(of({
      content: [],
      page: { totalElements: 0, totalPages: 0, number: 0, size: 10 }
    }));
    inventoryService.getStats.and.returnValue(of({
      totalSupplies: 0,
      inventoryValue: 0,
      lowStockCount: 0,
      outOfStockCount: 0,
      expiringSoonCount: 0
    }));
    inventoryService.getRecentTransactions.and.returnValue(of([]));

    const specialtyService = jasmine.createSpyObj<SpecialtyService>('SpecialtyService', ['getActiveSpecialties']);
    specialtyService.getActiveSpecialties.and.returnValue(of([]));

    viewPreferenceService = jasmine.createSpyObj<ViewPreferenceService>('ViewPreferenceService', [
      'getViewMode',
      'setViewMode'
    ]);
    viewPreferenceService.getViewMode.and.returnValue('cards');

    await TestBed.configureTestingModule({
      imports: [InventoryListComponent],
      providers: [
        { provide: InventoryService, useValue: inventoryService },
        { provide: SpecialtyService, useValue: specialtyService },
        { provide: ToastService, useValue: jasmine.createSpyObj<ToastService>('ToastService', ['show']) },
        { provide: NotificationService, useValue: jasmine.createSpyObj<NotificationService>('NotificationService', ['confirm']) },
        { provide: ViewPreferenceService, useValue: viewPreferenceService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InventoryListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('uses cards as the compact initial view', () => {
    expect(component).toBeTruthy();
    expect(viewPreferenceService.getViewMode).toHaveBeenCalledWith('inventory_view_mode', 'table', 'cards');
    expect(component.viewMode).toBe('cards');
  });

  it('keeps the table available as an explicit preference', () => {
    component.setViewMode('table');

    expect(component.viewMode).toBe('table');
    expect(viewPreferenceService.setViewMode).toHaveBeenCalledWith('inventory_view_mode', 'table');
  });
});
