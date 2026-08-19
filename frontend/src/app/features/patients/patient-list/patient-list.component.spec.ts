import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { PatientListComponent } from './patient-list.component';

describe('PatientListComponent', () => {
  let component: PatientListComponent;
  let fixture: ComponentFixture<PatientListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PatientListComponent, HttpClientTestingModule, RouterTestingModule]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PatientListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should toggle and close action menu correctly', () => {
    expect(component.openMenuPatientId).toBeNull();

    const mockEvent = new MouseEvent('click');
    spyOn(mockEvent, 'stopPropagation');

    component.toggleMenu(10, mockEvent);
    expect(component.openMenuPatientId).toBe(10);
    expect(mockEvent.stopPropagation).toHaveBeenCalled();

    // Toggle same id closes it
    component.toggleMenu(10);
    expect(component.openMenuPatientId).toBeNull();

    // Toggle another id opens it
    component.toggleMenu(20);
    expect(component.openMenuPatientId).toBe(20);

    // Document click closes it
    component.onDocumentClick();
    expect(component.openMenuPatientId).toBeNull();

    // closeMenu closes it
    component.toggleMenu(30);
    expect(component.openMenuPatientId).toBe(30);
    component.closeMenu();
    expect(component.openMenuPatientId).toBeNull();
  });
});
