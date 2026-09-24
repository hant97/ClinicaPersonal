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
    const widthSpy = vi.spyOn(window, 'innerWidth', 'get');
    widthSpy.mockReturnValue(767);
    expect(service.isMobile()).toBe(true);

    widthSpy.mockReturnValue(768);
    expect(service.isMobile()).toBe(false);
  });

  it('should classify compact boundaries at 768, 1023 and 1024 pixels', () => {
    const widthSpy = vi.spyOn(window, 'innerWidth', 'get');
    widthSpy.mockReturnValue(768);
    expect(service.isCompact()).toBe(true);

    widthSpy.mockReturnValue(1023);
    expect(service.isCompact()).toBe(true);

    widthSpy.mockReturnValue(1024);
    expect(service.isCompact()).toBe(false);
  });

  it('should allow an expanded sidebar from 1280 pixels', () => {
    const widthSpy = vi.spyOn(window, 'innerWidth', 'get');
    widthSpy.mockReturnValue(1279);
    expect(service.canExpandSidebar()).toBe(false);

    widthSpy.mockReturnValue(1280);
    expect(service.canExpandSidebar()).toBe(true);
  });

  it('should return desktop default when viewport is not compact and no storage exists', () => {
    vi.spyOn(window, 'innerWidth', 'get').mockReturnValue(1024);
    const mode = service.getViewMode('test_key', 'table', 'cards');
    expect(mode).toBe('table');
  });

  it('should return compact default on tablet when no storage exists', () => {
    vi.spyOn(window, 'innerWidth', 'get').mockReturnValue(768);
    const mode = service.getViewMode('test_key', 'table', 'cards');
    expect(mode).toBe('cards');
  });

  it('should return saved preference over responsive default', () => {
    localStorage.setItem('test_key', 'cards');
    vi.spyOn(window, 'innerWidth', 'get').mockReturnValue(1200);

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
