import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuditLogService } from '../../../core/services/audit-log.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { AuditLog, AuditLogFilter } from '../../../core/models/audit-log.model';
import { SpecialtyItem } from '../../../core/models/specialty.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import {
  LucideAngularModule,
  ClipboardList,
  Search,
  Filter,
  RotateCcw,
  Calendar,
  User,
  Shield,
  Activity,
  Globe,
  Info,
  X,
  FileText,
  CreditCard,
  UserCheck,
  Key,
  Stethoscope,
  Brain
} from 'lucide-angular';

@Component({
  selector: 'app-audit-log',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    PaginationComponent,
    LucideAngularModule
  ],
  templateUrl: './audit-log.component.html',
  styleUrls: ['./audit-log.component.css']
})
export class AuditLogComponent implements OnInit {
  readonly ClipboardList = ClipboardList;
  readonly Search = Search;
  readonly Filter = Filter;
  readonly RotateCcw = RotateCcw;
  readonly Calendar = Calendar;
  readonly User = User;
  readonly Shield = Shield;
  readonly Activity = Activity;
  readonly Globe = Globe;
  readonly Info = Info;
  readonly X = X;
  readonly FileText = FileText;
  readonly CreditCard = CreditCard;
  readonly UserCheck = UserCheck;
  readonly Key = Key;
  readonly Stethoscope = Stethoscope;
  readonly Brain = Brain;

  logs: AuditLog[] = [];
  specialties: SpecialtyItem[] = [];
  availableActions: string[] = [];
  availableEntityTypes: string[] = [];

  loading = true;
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 20;

  // Filters
  startDate = '';
  endDate = '';
  searchUsername = '';
  selectedSpecialty = '';
  selectedAction = '';
  selectedEntityType = '';
  searchQuery = '';

  // Selected Log for detail modal
  selectedLog: AuditLog | null = null;

  constructor(
    private auditLogService: AuditLogService,
    private specialtyService: SpecialtyService
  ) {}

  ngOnInit(): void {
    this.loadFilterMetadata();
    this.loadLogs();
  }

  loadFilterMetadata(): void {
    this.specialtyService.getActiveSpecialties().subscribe({
      next: (specs) => {
        this.specialties = specs;
      },
      error: () => {}
    });

    this.auditLogService.getDistinctActions().subscribe({
      next: (actions) => {
        this.availableActions = actions;
      },
      error: () => {}
    });

    this.auditLogService.getDistinctEntityTypes().subscribe({
      next: (types) => {
        this.availableEntityTypes = types;
      },
      error: () => {}
    });
  }

  loadLogs(page = 0): void {
    this.loading = true;
    this.currentPage = page;

    const filter: AuditLogFilter = {
      startDate: this.startDate || undefined,
      endDate: this.endDate || undefined,
      username: this.searchUsername || undefined,
      specialty: this.selectedSpecialty || undefined,
      action: this.selectedAction || undefined,
      entityType: this.selectedEntityType || undefined,
      query: this.searchQuery || undefined,
      page: this.currentPage,
      size: this.pageSize
    };

    this.auditLogService.getAuditLogs(filter).subscribe({
      next: (response) => {
        this.logs = response.content || [];
        this.totalElements = response.page?.totalElements ?? 0;
        this.totalPages = response.page?.totalPages ?? 0;
        this.loading = false;
      },
      error: () => {
        this.logs = [];
        this.totalElements = 0;
        this.totalPages = 0;
        this.loading = false;
      }
    });
  }

  onPageChange(page: number): void {
    this.loadLogs(page);
  }

  applyFilters(): void {
    this.loadLogs(0);
  }

  resetFilters(): void {
    this.startDate = '';
    this.endDate = '';
    this.searchUsername = '';
    this.selectedSpecialty = '';
    this.selectedAction = '';
    this.selectedEntityType = '';
    this.searchQuery = '';
    this.loadLogs(0);
  }

  openDetailModal(log: AuditLog): void {
    this.selectedLog = log;
  }

  closeDetailModal(): void {
    this.selectedLog = null;
  }

  getActionBadgeClass(action: string): string {
    switch (action?.toUpperCase()) {
      case 'CREATE':
      case 'LOGIN':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'UPDATE':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'DELETE':
      case 'LOGIN_FAILED':
        return 'bg-rose-50 text-rose-700 border-rose-200';
      case 'LOGOUT':
        return 'bg-slate-50 text-slate-700 border-slate-200';
      default:
        return 'bg-indigo-50 text-indigo-700 border-indigo-200';
    }
  }

  getEntityTypeBadgeClass(entityType: string): string {
    switch (entityType?.toUpperCase()) {
      case 'AUTH':
        return 'bg-purple-50 text-purple-700 border-purple-200';
      case 'PATIENT':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'PAYMENT':
      case 'PAYMENT_TRANSACTION':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'SESSION':
      case 'EVALUATION':
        return 'bg-teal-50 text-teal-700 border-teal-200';
      case 'PRESCRIPTION':
        return 'bg-cyan-50 text-cyan-700 border-cyan-200';
      case 'DOCUMENT':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'USER':
        return 'bg-indigo-50 text-indigo-700 border-indigo-200';
      default:
        return 'bg-gray-50 text-gray-700 border-gray-200';
    }
  }
}
