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
  let scheduleService: jasmine.SpyObj<ProfessionalScheduleService>;
  let blockService: jasmine.SpyObj<ScheduleBlockService>;
  let userService: jasmine.SpyObj<UserService>;

  beforeEach(async () => {
    const schedSpy = jasmine.createSpyObj('ProfessionalScheduleService', ['getWeeklySchedule', 'saveWeeklySchedule']);
    schedSpy.getWeeklySchedule.and.returnValue(of({
      professionalId: 1,
      schedules: [
        { dayOfWeek: 1, startTime: '09:00', endTime: '18:00', active: true }
      ]
    }));
    schedSpy.saveWeeklySchedule.and.returnValue(of({
      professionalId: 1,
      schedules: []
    }));

    const blockSpy = jasmine.createSpyObj('ScheduleBlockService', ['getBlocks', 'createBlock', 'deleteBlock']);
    blockSpy.getBlocks.and.returnValue(of([
      { id: 1, title: 'Vacaciones', startDate: '2026-09-01', endDate: '2026-09-10' }
    ]));
    blockSpy.createBlock.and.returnValue(of({ id: 2, title: 'Congreso', startDate: '2026-09-20', endDate: '2026-09-21' }));
    blockSpy.deleteBlock.and.returnValue(of(undefined));

    const userSpy = jasmine.createSpyObj('UserService', ['getProfessionals']);
    userSpy.getProfessionals.and.returnValue(of([
      { id: 1, username: 'dr1', firstName: 'Juan', lastName: 'Pérez', specialty: 'PSICOLOGIA', enabled: true, roles: ['ROLE_ADMIN'] }
    ]));

    await TestBed.configureTestingModule({
      imports: [ScheduleManagementComponent, HttpClientTestingModule],
      providers: [
        { provide: ProfessionalScheduleService, useValue: schedSpy },
        { provide: ScheduleBlockService, useValue: blockSpy },
        { provide: UserService, useValue: userSpy }
      ]
    }).compileComponents();

    scheduleService = TestBed.inject(ProfessionalScheduleService) as jasmine.SpyObj<ProfessionalScheduleService>;
    blockService = TestBed.inject(ScheduleBlockService) as jasmine.SpyObj<ScheduleBlockService>;
    userService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>;

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
