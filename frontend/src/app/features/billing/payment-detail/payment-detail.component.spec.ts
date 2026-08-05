import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject } from 'rxjs';
import { PaymentDetailComponent } from './payment-detail.component';
import { ClinicSettingsService, ClinicSettings } from '../../../core/services/clinic-settings.service';
import { Payment } from '../../../core/models/payment.model';

describe('PaymentDetailComponent', () => {
  let component: PaymentDetailComponent;
  let fixture: ComponentFixture<PaymentDetailComponent>;
  let settingsSubject: BehaviorSubject<ClinicSettings>;
  let clinicSettingsServiceMock: any;

  const mockPayment: Payment = {
    id: 1,
    patientId: 10,
    patientName: 'Juan Pérez',
    amount: 60.00,
    paymentDate: '2026-07-20T13:50:00',
    paymentMethod: 'CASH',
    description: 'Facturación de servicios y/o insumos',
    items: [
      {
        id: 1,
        paymentId: 1,
        description: 'Servicio: Orientación Vocacional',
        quantity: 1,
        unitPrice: 60.00,
        totalPrice: 60.00
      }
    ]
  };

  beforeEach(async () => {
    settingsSubject = new BehaviorSubject<ClinicSettings>({
      id: 1,
      clinicName: "Cousin'n",
      shortName: 'Cousin',
      logoUrl: '/api/settings/clinic/logo/test-logo.png',
      contactEmail: 'contacto@clinica.com',
      contactPhone: '999999999',
      address: 'Av. Principal 123'
    });

    clinicSettingsServiceMock = {
      settings$: settingsSubject.asObservable(),
      loadSettings: jasmine.createSpy('loadSettings'),
      getLogoUrl: jasmine.createSpy('getLogoUrl').and.callFake((path: string) => `http://localhost:8080${path}`)
    };

    await TestBed.configureTestingModule({
      imports: [PaymentDetailComponent],
      providers: [
        { provide: ClinicSettingsService, useValue: clinicSettingsServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PaymentDetailComponent);
    component = fixture.componentInstance;
    component.payment = mockPayment;
    component.paymentMethodText = 'Efectivo';
    fixture.detectChanges();
  });

  it('should create and load clinic settings on init', () => {
    expect(component).toBeTruthy();
    expect(clinicSettingsServiceMock.loadSettings).toHaveBeenCalled();
    expect(component.clinicSettings?.clinicName).toBe("Cousin'n");
  });

  it('should render clinic name and contact info dynamically in the receipt', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const headerTitle = compiled.querySelector('header h2');
    expect(headerTitle?.textContent?.trim()).toContain("Cousin'n");
    expect(compiled.textContent).toContain('Av. Principal 123');
    expect(compiled.textContent).toContain('contacto@clinica.com');
  });

  it('should display clinic logo if logoUrl is present', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const logoImg = compiled.querySelector('header img') as HTMLImageElement;
    expect(logoImg).toBeTruthy();
    expect(logoImg.src).toContain('http://localhost:8080/api/settings/clinic/logo/test-logo.png');
  });

  it('should display fallback icon if logoUrl is not present', () => {
    settingsSubject.next({
      clinicName: 'Clínica Salud',
      shortName: 'Salud',
      logoUrl: '',
      contactEmail: '',
      contactPhone: '',
      address: ''
    });
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const logoImg = compiled.querySelector('header img');
    expect(logoImg).toBeNull();
    expect(compiled.querySelector('header')?.textContent).toContain('🌿');
    expect(compiled.querySelector('header h2')?.textContent?.trim()).toContain('Clínica Salud');
  });

  it('should emit close event when onClose is called', () => {
    spyOn(component.close, 'emit');
    component.onClose();
    expect(component.close.emit).toHaveBeenCalled();
  });

  it('should call window.print when printReceipt is called', () => {
    spyOn(window, 'print');
    component.printReceipt();
    expect(window.print).toHaveBeenCalled();
  });
});
