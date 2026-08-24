import { TestBed } from '@angular/core/testing';
import { ViewPreferenceService } from './view-preference.service';

describe('ViewPreferenceService', () => {
  let service: ViewPreferenceService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ViewPreferenceService);
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should classify mobile boundaries at 767 and 768 pixels', () => {
    const widthSpy = spyOnProperty(window, 'innerWidth', 'get');
    widthSpy.and.returnValue(767);
    expect(service.isMobile()).toBeTrue();

    widthSpy.and.returnValue(768);
    expect(service.isMobile()).toBeFalse();
  });

  it('should classify compact boundaries at 768, 1023 and 1024 pixels', () => {
    const widthSpy = spyOnProperty(window, 'innerWidth', 'get');
    widthSpy.and.returnValue(768);
    expect(service.isCompact()).toBeTrue();

    widthSpy.and.returnValue(1023);
    expect(service.isCompact()).toBeTrue();

    widthSpy.and.returnValue(1024);
    expect(service.isCompact()).toBeFalse();
  });

  it('should allow an expanded sidebar from 1280 pixels', () => {
    const widthSpy = spyOnProperty(window, 'innerWidth', 'get');
    widthSpy.and.returnValue(1279);
    expect(service.canExpandSidebar()).toBeFalse();

    widthSpy.and.returnValue(1280);
    expect(service.canExpandSidebar()).toBeTrue();
  });

  it('should return desktop default when viewport is not compact and no storage exists', () => {
    spyOnProperty(window, 'innerWidth', 'get').and.returnValue(1024);
    const mode = service.getViewMode('test_key', 'table', 'cards');
    expect(mode).toBe('table');
  });

  it('should return compact default on tablet when no storage exists', () => {
    spyOnProperty(window, 'innerWidth', 'get').and.returnValue(768);
    const mode = service.getViewMode('test_key', 'table', 'cards');
    expect(mode).toBe('cards');
  });

  it('should return saved preference over responsive default', () => {
    localStorage.setItem('test_key', 'cards');
    spyOnProperty(window, 'innerWidth', 'get').and.returnValue(1200);

    const mode = service.getViewMode('test_key', 'table', 'cards');
    expect(mode).toBe('cards');
  });

  it('should persist preference in localStorage', () => {
    service.setViewMode('test_key', 'table');
    expect(localStorage.getItem('test_key')).toBe('table');

    service.setViewMode('test_key', 'cards');
    expect(localStorage.getItem('test_key')).toBe('cards');
  });
});
