import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AuditLogComponent } from './audit-log.component';
import { AuditLogService } from '../../../core/services/audit-log.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { AuditLog } from '../../../core/models/audit-log.model';
import { PageResponse } from '../../../core/models/page.model';

describe('AuditLogComponent', () => {
  let component: AuditLogComponent;
  let fixture: ComponentFixture<AuditLogComponent>;
  let auditLogServiceSpy: jasmine.SpyObj<AuditLogService>;
  let specialtyServiceSpy: jasmine.SpyObj<SpecialtyService>;

  const mockLog: AuditLog = {
    id: 1,
    userId: 10,
    username: 'admin',
    specialty: 'PSICOLOGIA',
    action: 'CREATE',
    entityType: 'PATIENT',
    entityId: '100',
    detail: 'Paciente creado',
    ip: '127.0.0.1',
    createdAt: '2026-08-20T10:00:00'
  };

  const mockPageResponse: PageResponse<AuditLog> = {
    content: [mockLog],
    page: {
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20
    }
  };

  beforeEach(async () => {
    auditLogServiceSpy = jasmine.createSpyObj('AuditLogService', [
      'getAuditLogs',
      'getDistinctActions',
      'getDistinctEntityTypes'
    ]);
    specialtyServiceSpy = jasmine.createSpyObj('SpecialtyService', ['getActiveSpecialties']);

    auditLogServiceSpy.getAuditLogs.and.returnValue(of(mockPageResponse));
    auditLogServiceSpy.getDistinctActions.and.returnValue(of(['CREATE', 'DELETE', 'LOGIN']));
    auditLogServiceSpy.getDistinctEntityTypes.and.returnValue(of(['AUTH', 'PATIENT', 'USER']));
    specialtyServiceSpy.getActiveSpecialties.and.returnValue(of([
      { id: 1, code: 'PSICOLOGIA', name: 'Psicología', icon: 'Brain', active: true, displayOrder: 1 }
    ]));

    await TestBed.configureTestingModule({
      imports: [AuditLogComponent],
      providers: [
        { provide: AuditLogService, useValue: auditLogServiceSpy },
        { provide: SpecialtyService, useValue: specialtyServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AuditLogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse e inicializar datos de auditoria', () => {
    expect(component).toBeTruthy();
    expect(component.logs.length).toBe(1);
    expect(component.totalElements).toBe(1);
    expect(component.availableActions).toContain('CREATE');
    expect(component.availableEntityTypes).toContain('PATIENT');
  });

  it('debe aplicar filtros y consultar logs', () => {
    component.searchUsername = 'doctor';
    component.selectedAction = 'CREATE';
    component.applyFilters();

    expect(auditLogServiceSpy.getAuditLogs).toHaveBeenCalledWith(jasmine.objectContaining({
      username: 'doctor',
      action: 'CREATE',
      page: 0
    }));
  });

  it('debe limpiar filtros y recargar desde pagina 0', () => {
    component.searchUsername = 'doctor';
    component.selectedAction = 'CREATE';
    component.searchQuery = 'algo';
    component.resetFilters();

    expect(component.searchUsername).toBe('');
    expect(component.selectedAction).toBe('');
    expect(component.searchQuery).toBe('');
    expect(auditLogServiceSpy.getAuditLogs).toHaveBeenCalled();
  });

  it('debe abrir y cerrar modal de detalle', () => {
    component.openDetailModal(mockLog);
    expect(component.selectedLog).toEqual(mockLog);

    component.closeDetailModal();
    expect(component.selectedLog).toBeNull();
  });

  it('debe cambiar de pagina', () => {
    component.onPageChange(1);
    expect(auditLogServiceSpy.getAuditLogs).toHaveBeenCalledWith(jasmine.objectContaining({
      page: 1
    }));
  });
});
