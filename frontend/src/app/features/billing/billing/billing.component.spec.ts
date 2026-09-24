import type { Mock } from 'vitest';
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
import { ViewPreferenceService } from '../../../shared/services/view-preference/view-preference.service';

function emptySummary(): PaymentSummary {
  return {
    incomeToday: 0,
    incomeMonth: 0,
    monthlyGrowth: 0,
    paymentsCountMonth: 0,
    averageTicket: 0,
    pendingBalance: 0,
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
  let viewPreferenceService: ViewPreferenceService;

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [BillingComponent, HttpClientTestingModule, RouterTestingModule]
    }).compileComponents();

    paymentService = TestBed.inject(PaymentService);
    catalogService = TestBed.inject(CatalogService);
    viewPreferenceService = TestBed.inject(ViewPreferenceService);

    vi.spyOn(catalogService, 'getActiveItemsByCatalogCode').mockReturnValue(of<CatalogItem[]>([]));
    vi.spyOn(paymentService, 'getSummary').mockReturnValue(of(emptySummary()));
    vi.spyOn(paymentService, 'getAll').mockReturnValue(of(emptyPage()));
    vi.spyOn(viewPreferenceService, 'getViewMode').mockReturnValue('cards');

    fixture = TestBed.createComponent(BillingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(viewPreferenceService.getViewMode).toHaveBeenCalledWith('billing_view_mode', 'table', 'cards');
    expect(component.viewMode).toBe('cards');
  });

  it('applyFilters resets page to 0 and reloads', () => {
    (paymentService.getAll as Mock).mockClear();
    component.currentPage = 3;
    component.filterDateFrom = '2026-01-01';

    component.applyFilters();

    expect(component.currentPage).toBe(0);
    expect(paymentService.getAll).toHaveBeenCalled();
  });

  it('clearFilters resets optional filters and restores the current month', () => {
    component.filterDateFrom = '2026-01-01';
    component.filterDateTo = '2026-01-31';
    component.filterMethod = 'EFECTIVO';
    component.filterStatus = 'PAGADO';
    component.searchTerm = 'Ana';
    component.selectedPreset = 'CUSTOM';

    component.clearFilters();

    expect(component.filterDateFrom).toMatch(/^\d{4}-\d{2}-01$/);
    expect(component.filterDateTo).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(component.filterMethod).toBe('');
    expect(component.filterStatus).toBe('');
    expect(component.searchTerm).toBe('');
    expect(component.selectedPreset).toBe('MONTH');
    expect(component.hasActiveFilters).toBe(false);
  });

  it('hasActiveFilters reflects optional filters or a non-default period', () => {
    component.selectedPreset = 'MONTH';
    component.filterMethod = '';
    expect(component.hasActiveFilters).toBe(false);

    component.filterMethod = 'YAPE';
    expect(component.hasActiveFilters).toBe(true);

    component.filterMethod = '';
    component.selectedPreset = 'WEEK';
    expect(component.hasActiveFilters).toBe(true);
  });

  it('a period preset updates the list and summary with the same dates', () => {
    (paymentService.getAll as Mock).mockClear();
    (paymentService.getSummary as Mock).mockClear();

    component.setPreset('TODAY');

    expect(component.filterDateFrom).toBe(component.filterDateTo);
    expect(paymentService.getAll).toHaveBeenCalledWith(0, component.pageSize, expect.objectContaining({ dateFrom: component.filterDateFrom, dateTo: component.filterDateTo }));
    expect(paymentService.getSummary).toHaveBeenCalledWith(component.filterDateFrom, component.filterDateTo);
  });

  it('does not query an invalid custom date range', () => {
    (paymentService.getAll as Mock).mockClear();
    (paymentService.getSummary as Mock).mockClear();
    component.filterDateFrom = '2026-02-10';
    component.filterDateTo = '2026-02-01';

    component.applyDateRange();

    expect(component.dateRangeInvalid).toBe(true);
    expect(paymentService.getAll).not.toHaveBeenCalled();
    expect(paymentService.getSummary).not.toHaveBeenCalled();
  });

  it('formatPaymentDate formats dates properly in Spanish style', () => {
    const formatted = component.formatPaymentDate('2026-08-19T07:18:00');
    expect(formatted).toContain('19');
    expect(formatted).toContain('ago');
    expect(formatted).toContain('7:18');
    expect(formatted).toContain('a.m.');
    expect(component.formatPaymentDate('')).toBe('—');
  });

  it('toggles and closes action menu correctly', () => {
    expect(component.openMenuPaymentId).toBeNull();

    const mockEvent = new MouseEvent('click');
    vi.spyOn(mockEvent, 'stopPropagation');

    component.toggleMenu(10, mockEvent);
    expect(component.openMenuPaymentId).toBe(10);
    expect(mockEvent.stopPropagation).toHaveBeenCalled();

    component.toggleMenu(10);
    expect(component.openMenuPaymentId).toBeNull();

    component.toggleMenu(20);
    expect(component.openMenuPaymentId).toBe(20);

    component.onDocumentClick();
    expect(component.openMenuPaymentId).toBeNull();

    component.toggleMenu(30);
    component.closeMenu();
    expect(component.openMenuPaymentId).toBeNull();
  });
});
