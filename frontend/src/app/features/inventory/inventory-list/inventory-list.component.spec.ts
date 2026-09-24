import type { MockedObject } from 'vitest';
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
  let viewPreferenceService: MockedObject<ViewPreferenceService>;

  beforeEach(async () => {
    const inventoryService = {
      getAllSupplies: vi.fn().mockName('InventoryService.getAllSupplies'),
      getStats: vi.fn().mockName('InventoryService.getStats'),
      getRecentTransactions: vi.fn().mockName('InventoryService.getRecentTransactions')
    };
    inventoryService.getAllSupplies.mockReturnValue(of({
      content: [],
      page: { totalElements: 0, totalPages: 0, number: 0, size: 10 }
    }));
    inventoryService.getStats.mockReturnValue(of({
      totalSupplies: 0,
      inventoryValue: 0,
      lowStockCount: 0,
      outOfStockCount: 0,
      expiringSoonCount: 0
    }));
    inventoryService.getRecentTransactions.mockReturnValue(of([]));

    const specialtyService = {
      getActiveSpecialties: vi.fn().mockName('SpecialtyService.getActiveSpecialties')
    };
    specialtyService.getActiveSpecialties.mockReturnValue(of([]));

    viewPreferenceService = {
      getViewMode: vi.fn().mockName('ViewPreferenceService.getViewMode'),
      setViewMode: vi.fn().mockName('ViewPreferenceService.setViewMode')
    } as unknown as MockedObject<ViewPreferenceService>;
    viewPreferenceService.getViewMode.mockReturnValue('cards');

    await TestBed.configureTestingModule({
      imports: [InventoryListComponent],
      providers: [
        { provide: InventoryService, useValue: inventoryService },
        { provide: SpecialtyService, useValue: specialtyService },
        { provide: ToastService, useValue: {
            show: vi.fn().mockName('ToastService.show')
          } },
        { provide: NotificationService, useValue: {
            confirm: vi.fn().mockName('NotificationService.confirm')
          } },
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
