import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AuthImageSrcDirective } from './auth-image-src.directive';
import { environment } from '../../../environments/environment';

@Component({
  standalone: true,
  imports: [AuthImageSrcDirective],
  template: `@if (visible) { <img [appAuthSrc]="source" alt="Foto" /> }`
})
class AuthImageHostComponent {
  source: string | null = null;
  visible = true;
}

describe('AuthImageSrcDirective', () => {
  let fixture: ComponentFixture<AuthImageHostComponent>;
  let host: AuthImageHostComponent;
  let httpMock: HttpTestingController;

  const image = (): HTMLImageElement => fixture.nativeElement.querySelector('img');

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuthImageHostComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(AuthImageHostComponent);
    host = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:foto-1');
    vi.spyOn(URL, 'revokeObjectURL');
  });

  afterEach(() => httpMock.verify());

  it('loads API images through HttpClient and shows them as blob URLs', () => {
    host.source = '/api/v1/patients/7/photo?v=abc';
    fixture.detectChanges();

    const request = httpMock.expectOne(`${environment.apiUrl}/v1/patients/7/photo?v=abc`);
    expect(request.request.responseType).toBe('blob');
    request.flush(new Blob(['x'], { type: 'image/png' }));

    expect(image().src).toBe('blob:foto-1');
  });

  it('uses external URLs directly without sending them through HttpClient', () => {
    host.source = 'https://example.org/foto.png';
    fixture.detectChanges();

    httpMock.expectNone(() => true);
    expect(image().src).toBe('https://example.org/foto.png');
  });

  it('revokes the blob URL when the image is destroyed', () => {
    host.source = '/api/v1/patients/7/photo';
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/v1/patients/7/photo`).flush(new Blob(['x']));

    host.visible = false;
    fixture.detectChanges();

    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:foto-1');
  });

  it('hides the image when it cannot be loaded', () => {
    host.source = '/api/v1/patients/7/photo';
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiUrl}/v1/patients/7/photo`)
      .flush(new Blob(), { status: 404, statusText: 'Not Found' });

    expect(image().style.visibility).toBe('hidden');
  });
});
