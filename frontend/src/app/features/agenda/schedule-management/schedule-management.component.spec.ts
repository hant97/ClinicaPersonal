import type { MockedObject } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { ScheduleManagementComponent } from './schedule-management.component';
import { ProfessionalScheduleService } from '../../../core/services/professional-schedule.service';
import { ScheduleBlockService } from '../../../core/services/schedule-block.service';
import { UserService } from '../../../core/services/user.service';

describe('ScheduleManagementComponent', () => {
  let component: ScheduleManagementComponent;
  let fixture: ComponentFixture<ScheduleManagementComponent>;
  let scheduleService: MockedObject<ProfessionalScheduleService>;
  let blockService: MockedObject<ScheduleBlockService>;
  let userService: MockedObject<UserService>;

  beforeEach(async () => {
    const schedSpy = {
      getWeeklySchedule: vi.fn().mockName('ProfessionalScheduleService.getWeeklySchedule'),
      saveWeeklySchedule: vi.fn().mockName('ProfessionalScheduleService.saveWeeklySchedule')
    };
    schedSpy.getWeeklySchedule.mockReturnValue(of({
      professionalId: 1,
      schedules: [
        { dayOfWeek: 1, startTime: '09:00', endTime: '18:00', active: true }
      ]
    }));
    schedSpy.saveWeeklySchedule.mockReturnValue(of({
      professionalId: 1,
      schedules: []
    }));

    const blockSpy = {
      getBlocks: vi.fn().mockName('ScheduleBlockService.getBlocks'),
      createBlock: vi.fn().mockName('ScheduleBlockService.createBlock'),
      deleteBlock: vi.fn().mockName('ScheduleBlockService.deleteBlock')
    };
    blockSpy.getBlocks.mockReturnValue(of([
      { id: 1, title: 'Vacaciones', startDate: '2026-09-01', endDate: '2026-09-10' }
    ]));
    blockSpy.createBlock.mockReturnValue(of({ id: 2, title: 'Congreso', startDate: '2026-09-20', endDate: '2026-09-21' }));
    blockSpy.deleteBlock.mockReturnValue(of(undefined));

    const userSpy = {
      getProfessionals: vi.fn().mockName('UserService.getProfessionals'),
      getCurrentUserProfile: vi.fn().mockName('UserService.getCurrentUserProfile')
    };
    userSpy.getProfessionals.mockReturnValue(of([
      { id: 1, username: 'dr1', firstName: 'Juan', lastName: 'Pérez', specialty: 'PSICOLOGIA', enabled: true, roles: ['ROLE_ADMIN'] }
    ]));
    userSpy.getCurrentUserProfile.mockReturnValue(of({
      id: 1,
      username: 'dr1',
      firstName: 'Juan',
      lastName: 'Pérez',
      specialty: 'PSICOLOGIA',
      roles: ['ROLE_ADMIN'],
      enabled: true
    }));

    await TestBed.configureTestingModule({
      imports: [ScheduleManagementComponent, HttpClientTestingModule],
      providers: [
        { provide: ProfessionalScheduleService, useValue: schedSpy },
        { provide: ScheduleBlockService, useValue: blockSpy },
        { provide: UserService, useValue: userSpy }
      ]
    }).compileComponents();

    scheduleService = TestBed.inject(ProfessionalScheduleService) as MockedObject<ProfessionalScheduleService>;
    blockService = TestBed.inject(ScheduleBlockService) as MockedObject<ScheduleBlockService>;
    userService = TestBed.inject(UserService) as MockedObject<UserService>;

    fixture = TestBed.createComponent(ScheduleManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load professionals and blocks', () => {
    expect(component).toBeTruthy();
    expect(component.professionals.length).toBe(1);
    expect(component.blocks.length).toBe(1);
    expect(scheduleService.getWeeklySchedule).toHaveBeenCalledWith(1);
  });

  it('should switch tabs between schedules and blocks', () => {
    expect(component.activeTab).toBe('schedules');
    component.activeTab = 'blocks';
    expect(component.activeTab).toBe('blocks');
  });

  it('should save weekly schedule when valid', () => {
    component.saveSchedule();
    expect(scheduleService.saveWeeklySchedule).toHaveBeenCalled();
  });
});
