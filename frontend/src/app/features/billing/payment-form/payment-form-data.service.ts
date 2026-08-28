import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { PaymentService } from '../../../core/services/payment.service';
import { AppointmentService } from '../../../core/services/appointment.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { InventoryService, Supply } from '../../../core/services/inventory.service';
import { ClinicalServiceService } from '../../../core/services/clinical-service.service';
import { Payment } from '../../../core/models/payment.model';
import { Appointment } from '../../../core/models/appointment.model';
import { CatalogItem } from '../../../core/models/catalog.model';
import { ClinicalService } from '../../../core/models/clinical-service.model';

/**
 * Orquesta las llamadas HTTP que necesita el formulario de cobros
 * (PaymentFormComponent): catálogos de apoyo (métodos de pago, insumos,
 * servicios clínicos), citas del paciente seleccionado y el guardado del
 * cobro (creación o actualización).
 */
@Injectable({
  providedIn: 'root'
})
export class PaymentFormDataService {
  constructor(
    private paymentService: PaymentService,
    private appointmentService: AppointmentService,
    private catalogService: CatalogService,
    private inventoryService: InventoryService,
    private clinicalServiceService: ClinicalServiceService
  ) {}

  loadPaymentMethods(): Observable<CatalogItem[]> {
    return this.catalogService.getActiveItemsByCatalogCode('PAYMENT_METHOD');
  }

  loadSupplies(): Observable<Supply[]> {
    return this.inventoryService.getAllSupplies('', 0, 100).pipe(
      map(res => res.content)
    );
  }

  loadClinicalServices(): Observable<ClinicalService[]> {
    return this.clinicalServiceService.getAllActiveServices();
  }

  loadPatientAppointments(patientId: number): Observable<Appointment[]> {
    return this.appointmentService.getByPatientId(patientId, 0, 50).pipe(
      map(res => res.content || [])
    );
  }

  savePayment(payment: Payment, existingId?: number): Observable<Payment> {
    return existingId ? this.paymentService.update(existingId, payment) : this.paymentService.create(payment);
  }
}
