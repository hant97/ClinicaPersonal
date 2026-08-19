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

  it('should return desktop default when viewport is desktop and no storage exists', () => {
    spyOnProperty(window, 'innerWidth', 'get').and.returnValue(1024);
    const mode = service.getViewMode('test_key', 'table', 'cards');
    expect(mode).toBe('table');
  });

  it('should return mobile default when viewport is mobile (< 768) and no storage exists', () => {
    spyOnProperty(window, 'innerWidth', 'get').and.returnValue(400);
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
