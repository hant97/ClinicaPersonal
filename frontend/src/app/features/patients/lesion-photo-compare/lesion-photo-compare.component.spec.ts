import type { MockedObject } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LesionPhotoCompareComponent } from './lesion-photo-compare.component';
import { LesionPhotoService } from '../../../core/services/lesion-photo.service';
import { of } from 'rxjs';
import { Lesion } from '../../../core/models/lesion.model';
import { LesionPhoto } from '../../../core/models/lesion-photo.model';

describe('LesionPhotoCompareComponent', () => {
  let component: LesionPhotoCompareComponent;
  let fixture: ComponentFixture<LesionPhotoCompareComponent>;
  let photoServiceSpy: MockedObject<LesionPhotoService>;

  const mockLesion: Lesion = {
    id: 1,
    patientId: 10,
    bodyArea: 'Rostro - Mejilla Derecha',
    lesionType: 'Mácula eritematosa',
    size: '1.2 cm',
    color: 'Eritematosa',
    evolution: 'Evolución favorable con tratamiento tópico'
  };

  const mockPhotos: LesionPhoto[] = [
    {
      id: 101,
      lesionId: 1,
      takenDate: '2026-06-01',
      description: 'Línea base antes de tratamiento'
    },
    {
      id: 102,
      lesionId: 1,
      takenDate: '2026-07-15',
      description: 'Control a las 6 semanas'
    }
  ];

  beforeEach(async () => {
    photoServiceSpy = {
      getPhotoFile: vi.fn().mockName('LesionPhotoService.getPhotoFile')
    } as unknown as MockedObject<LesionPhotoService>;
    photoServiceSpy.getPhotoFile.mockReturnValue(of(new Blob(['fake-image'], { type: 'image/jpeg' })));

    await TestBed.configureTestingModule({
      imports: [LesionPhotoCompareComponent],
      providers: [
        { provide: LesionPhotoService, useValue: photoServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LesionPhotoCompareComponent);
    component = fixture.componentInstance;
    component.lesion = mockLesion;
    component.photos = mockPhotos;
    fixture.detectChanges();
  });

  it('should create component and initialize photo selections', () => {
    expect(component).toBeTruthy();
    expect(component.photoItems.length).toBe(2);
    expect(component.selectedPhotoA?.photo.id).toBe(101);
    expect(component.selectedPhotoB?.photo.id).toBe(102);
  });

  it('should correctly calculate days elapsed between photos', () => {
    expect(component.daysBetween).toBe(44); // June 1 to July 15 is 44 days
  });

  it('should swap Photo A and Photo B when swapPhotos is called', () => {
    component.swapPhotos();
    expect(component.selectedPhotoA?.photo.id).toBe(102);
    expect(component.selectedPhotoB?.photo.id).toBe(101);
  });

  it('should change compare mode between side-by-side and slider', () => {
    expect(component.compareMode).toBe('side-by-side');
    component.setMode('slider');
    expect(component.compareMode).toBe('slider');
  });

  it('should emit close event when closeModal is called', () => {
    vi.spyOn(component.close, 'emit');
    component.closeModal();
    expect(component.close.emit).toHaveBeenCalled();
  });
});
