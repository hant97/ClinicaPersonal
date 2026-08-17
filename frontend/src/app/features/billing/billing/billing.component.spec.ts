import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { BillingComponent } from './billing.component';
import { PaymentService } from '../../../core/services/payment.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { Payment, PaymentSummary } from '../../../core/models/payment.model';
import { CatalogItem } from '../../../core/models/catalog.model';
import { PageResponse } from '../../../core/models/page.model';

function emptySummary(): PaymentSummary {
  return {
    incomeToday: 0,
    incomeMonth: 0,
    monthlyGrowth: 0,
    paymentsCountMonth: 0,
    averageTicket: 0,
    methodBreakdown: [],
    dailyIncome: [],
    topServices: []
  };
}

function emptyPage(): PageResponse<Payment> {
  return { content: [], page: { totalElements: 0, totalPages: 0, number: 0, size: 10 } };
}

describe('BillingComponent', () => {
  let component: BillingComponent;
  let fixture: ComponentFixture<BillingComponent>;
  let paymentService: PaymentService;
  let catalogService: CatalogService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BillingComponent, HttpClientTestingModule, RouterTestingModule]
    }).compileComponents();

    paymentService = TestBed.inject(PaymentService);
    catalogService = TestBed.inject(CatalogService);

    spyOn(catalogService, 'getActiveItemsByCatalogCode').and.returnValue(of<CatalogItem[]>([]));
    spyOn(paymentService, 'getSummary').and.returnValue(of(emptySummary()));
    spyOn(paymentService, 'getAll').and.returnValue(of(emptyPage()));

    fixture = TestBed.createComponent(BillingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('applyFilters resets page to 0 and reloads', () => {
    (paymentService.getAll as jasmine.Spy).calls.reset();
    component.currentPage = 3;
    component.filterDateFrom = '2026-01-01';

    component.applyFilters();

    expect(component.currentPage).toBe(0);
    expect(paymentService.getAll).toHaveBeenCalled();
  });

  it('clearFilters clears filters and reloads', () => {
    component.filterDateFrom = '2026-01-01';
    component.filterDateTo = '2026-01-31';
    component.filterMethod = 'EFECTIVO';

    component.clearFilters();

    expect(component.filterDateFrom).toBe('');
    expect(component.filterDateTo).toBe('');
    expect(component.filterMethod).toBe('');
    expect(component.hasActiveFilters).toBeFalse();
  });

  it('hasActiveFilters reflects date/method filters only', () => {
    component.filterDateFrom = '';
    component.filterDateTo = '';
    component.filterMethod = '';
    expect(component.hasActiveFilters).toBeFalse();

    component.filterMethod = 'YAPE';
    expect(component.hasActiveFilters).toBeTrue();
  });
});
