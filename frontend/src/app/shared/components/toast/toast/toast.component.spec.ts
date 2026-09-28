import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ToastComponent } from './toast.component';
import { ToastService } from '../../../services/toast/toast.service';

describe('ToastComponent', () => {
  let component: ToastComponent;
  let fixture: ComponentFixture<ToastComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToastComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ToastComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('solicita al servicio cerrar el aviso indicado', () => {
    const toastService = TestBed.inject(ToastService);
    const dismissSpy = vi.spyOn(toastService, 'dismiss');

    component.dismiss(42);

    expect(dismissSpy).toHaveBeenCalledWith(42);
  });
});
