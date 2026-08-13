import { Component, EventEmitter, HostListener, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, takeUntil } from 'rxjs/operators';
import {
  LucideAngularModule,
  Search,
  Users,
  Calendar,
  DollarSign,
  Activity,
  Package,
  Settings,
  ClipboardList,
  ArrowRight,
  X,
  User,
  PlusCircle,
  FileText
} from 'lucide-angular';
import { PatientService } from '../../../core/services/patient/patient.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { Patient } from '../../../core/models/patient.model';

export interface CommandItem {
  id: string;
  title: string;
  category: 'Navegación' | 'Acciones Rápidas' | 'Pacientes';
  icon: any;
  route?: string;
  action?: () => void;
  shortcut?: string;
}

@Component({
  selector: 'app-command-palette',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LucideAngularModule],
  template: `
    @if (isOpen) {
      <div
        class="fixed inset-0 z-50 flex items-start justify-center pt-16 sm:pt-24 p-4"
        role="dialog"
        aria-modal="true"
        aria-label="Paleta de comandos"
      >
        <!-- Backdrop -->
        <div
          class="fixed inset-0 bg-slate-950/40 backdrop-blur-sm transition-opacity"
          (click)="close()"
        ></div>

        <!-- Palette Card -->
        <div
          class="relative w-full max-w-2xl overflow-hidden rounded-xl border border-line bg-surface shadow-floating transform transition-all"
        >
          <!-- Search Header -->
          <div class="relative flex items-center border-b border-line px-4 bg-slate-50/50">
            <lucide-icon
              [img]="Search"
              [size]="18"
              class="text-muted shrink-0"
            ></lucide-icon>
            <input
              #searchInput
              type="text"
              [formControl]="searchControl"
              placeholder="Escribe un comando o busca un paciente..."
              class="w-full bg-transparent px-3 py-3.5 text-sm text-ink outline-none placeholder:text-muted"
              (keydown)="onKeydown($event)"
            />
            <div class="flex items-center gap-1.5 shrink-0">
              <kbd class="rounded border border-line bg-surface px-1.5 py-0.5 text-[10px] font-mono text-muted">ESC</kbd>
              <button
                type="button"
                class="btn-text !p-1 text-muted"
                aria-label="Cerrar paleta"
                (click)="close()"
              >
                <lucide-icon [img]="X" [size]="16"></lucide-icon>
              </button>
            </div>
          </div>

          <!-- Results List -->
          <div class="max-h-96 overflow-y-auto p-2 space-y-3">
            @if (patientResults.length > 0) {
              <div>
                <p class="px-3 py-1 text-[10px] font-semibold uppercase tracking-wider text-muted">Pacientes encontrados</p>
                <div class="space-y-0.5">
                  @for (patient of patientResults; track patient.id; let i = $index) {
                    <div
                      class="flex items-center justify-between rounded-lg px-3 py-2 text-sm cursor-pointer transition"
                      [class.bg-primary-50]="selectedIndex === i"
                      [class.text-primary-700]="selectedIndex === i"
                      [class.text-ink]="selectedIndex !== i"
                      (mouseenter)="selectedIndex = i"
                      (click)="selectPatient(patient)"
                    >
                      <div class="flex items-center gap-2.5 min-w-0">
                        <span class="avatar-initials !h-7 !w-7 !text-xs">{{ getInitials(patient.firstName, patient.lastName) }}</span>
                        <div class="truncate">
                          <p class="font-medium text-ink">{{ patient.firstName }} {{ patient.lastName }}</p>
                          <p class="text-xs text-muted">Doc: {{ patient.identificationDocument || 'S/D' }}</p>
                        </div>
                      </div>
                      <span class="text-xs text-muted flex items-center gap-1">Ver ficha <lucide-icon [img]="ArrowRight" [size]="14"></lucide-icon></span>
                    </div>
                  }
                </div>
              </div>
            }

            @if (filteredCommands.length > 0) {
              <div>
                <p class="px-3 py-1 text-[10px] font-semibold uppercase tracking-wider text-muted">Comandos y Navegación</p>
                <div class="space-y-0.5">
                  @for (cmd of filteredCommands; track cmd.id; let i = $index) {
                    <div
                      class="flex items-center justify-between rounded-lg px-3 py-2 text-sm cursor-pointer transition"
                      [class.bg-primary-50]="selectedIndex === (patientResults.length + i)"
                      [class.text-primary-700]="selectedIndex === (patientResults.length + i)"
                      [class.text-ink]="selectedIndex !== (patientResults.length + i)"
                      (mouseenter)="selectedIndex = patientResults.length + i"
                      (click)="executeCommand(cmd)"
                    >
                      <div class="flex items-center gap-2.5">
                        <lucide-icon [img]="cmd.icon" [size]="16" class="text-muted"></lucide-icon>
                        <span class="font-medium">{{ cmd.title }}</span>
                      </div>
                      @if (cmd.shortcut) {
                        <kbd class="rounded border border-line bg-surface px-1.5 py-0.5 text-[10px] font-mono text-muted">{{ cmd.shortcut }}</kbd>
                      } @else {
                        <span class="text-xs text-muted">{{ cmd.category }}</span>
                      }
                    </div>
                  }
                </div>
              </div>
            }

            @if (searchControl.value && filteredCommands.length === 0 && patientResults.length === 0 && !isSearching) {
              <div class="py-8 text-center text-sm text-muted">
                No se encontraron resultados para "{{ searchControl.value }}"
              </div>
            }
          </div>

          <!-- Footer -->
          <div class="flex items-center justify-between border-t border-line px-4 py-2 bg-slate-50 text-[11px] text-muted">
            <div class="flex items-center gap-3">
              <span class="flex items-center gap-1"><kbd class="rounded border border-line bg-surface px-1 text-[10px]">↑</kbd><kbd class="rounded border border-line bg-surface px-1 text-[10px]">↓</kbd> Navegar</span>
              <span class="flex items-center gap-1"><kbd class="rounded border border-line bg-surface px-1 text-[10px]">↵</kbd> Seleccionar</span>
            </div>
            <span>FlowGrid Command</span>
          </div>
        </div>
      </div>
    }
  `,
})
export class CommandPaletteComponent implements OnInit, OnDestroy {
  readonly Search = Search;
  readonly ArrowRight = ArrowRight;
  readonly X = X;

  @Input() isOpen: boolean = false;
  @Output() closed = new EventEmitter<void>();

  searchControl = new FormControl('');
  selectedIndex = 0;
  isSearching = false;
  patientResults: Patient[] = [];
  
  private destroy$ = new Subject<void>();

  baseCommands: CommandItem[] = [
    { id: 'nav-dashboard', title: 'Ir a Dashboard', category: 'Navegación', icon: Activity, route: '/dashboard', shortcut: 'G D' },
    { id: 'nav-agenda', title: 'Ir a Agenda de Citas', category: 'Navegación', icon: Calendar, route: '/agenda', shortcut: 'G A' },
    { id: 'nav-patients', title: 'Ir a Directorio de Pacientes', category: 'Navegación', icon: Users, route: '/patients', shortcut: 'G P' },
    { id: 'nav-billing', title: 'Ir a Cobros y Facturación', category: 'Navegación', icon: DollarSign, route: '/billing', shortcut: 'G B' },
    { id: 'nav-inventory', title: 'Ir a Inventario de Insumos', category: 'Navegación', icon: Package, route: '/inventory', shortcut: 'G I' },
    { id: 'nav-services', title: 'Ir a Servicios Clínicos', category: 'Navegación', icon: FileText, route: '/services' },
    { id: 'nav-settings', title: 'Ir a Configuración', category: 'Navegación', icon: Settings, route: '/settings/catalogs' },
    { id: 'act-new-patient', title: 'Registrar Nuevo Paciente', category: 'Acciones Rápidas', icon: PlusCircle, route: '/patients' },
    { id: 'act-new-appointment', title: 'Agendar Nueva Cita', category: 'Acciones Rápidas', icon: Calendar, route: '/agenda' },
  ];

  constructor(
    private router: Router,
    private patientService: PatientService,
    private specialtyService: SpecialtyService
  ) {}

  ngOnInit(): void {
    if (this.specialtyService.isPsychology()) {
      this.baseCommands.push({
        id: 'nav-tests',
        title: 'Ir a Pruebas Psicométricas',
        category: 'Navegación',
        icon: ClipboardList,
        route: '/tests-catalog',
      });
    }

    this.searchControl.valueChanges
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe((query) => {
        this.selectedIndex = 0;
        if (query && query.trim().length >= 2) {
          this.searchPatients(query.trim());
        } else {
          this.patientResults = [];
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  @HostListener('document:keydown', ['$event'])
  handleGlobalShortcut(event: KeyboardEvent): void {
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
      event.preventDefault();
      this.toggle();
    }
  }

  toggle(): void {
    if (this.isOpen) {
      this.close();
    } else {
      this.open();
    }
  }

  open(): void {
    this.isOpen = true;
    this.selectedIndex = 0;
    this.searchControl.setValue('');
    this.patientResults = [];
  }

  close(): void {
    this.isOpen = false;
    this.closed.emit();
  }

  get filteredCommands(): CommandItem[] {
    const query = (this.searchControl.value || '').toLowerCase().trim();
    if (!query) return this.baseCommands;
    return this.baseCommands.filter(
      (cmd) => cmd.title.toLowerCase().includes(query) || cmd.category.toLowerCase().includes(query)
    );
  }

  get totalItemsCount(): number {
    return this.patientResults.length + this.filteredCommands.length;
  }

  searchPatients(query: string): void {
    this.isSearching = true;
    this.patientService.search(query, 0, 4).subscribe({
      next: (res) => {
        this.patientResults = res.content || [];
        this.isSearching = false;
      },
      error: () => {
        this.patientResults = [];
        this.isSearching = false;
      },
    });
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      event.preventDefault();
      this.close();
    } else if (event.key === 'ArrowDown') {
      event.preventDefault();
      if (this.totalItemsCount > 0) {
        this.selectedIndex = (this.selectedIndex + 1) % this.totalItemsCount;
      }
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      if (this.totalItemsCount > 0) {
        this.selectedIndex = (this.selectedIndex - 1 + this.totalItemsCount) % this.totalItemsCount;
      }
    } else if (event.key === 'Enter') {
      event.preventDefault();
      this.triggerSelected();
    }
  }

  triggerSelected(): void {
    if (this.selectedIndex < this.patientResults.length) {
      this.selectPatient(this.patientResults[this.selectedIndex]);
    } else {
      const cmdIndex = this.selectedIndex - this.patientResults.length;
      if (this.filteredCommands[cmdIndex]) {
        this.executeCommand(this.filteredCommands[cmdIndex]);
      }
    }
  }

  selectPatient(patient: Patient): void {
    this.close();
    if (patient.id) {
      this.router.navigate(['/patients', patient.id]);
    }
  }

  executeCommand(cmd: CommandItem): void {
    this.close();
    if (cmd.action) {
      cmd.action();
    } else if (cmd.route) {
      this.router.navigateByUrl(cmd.route);
    }
  }

  getInitials(firstName?: string, lastName?: string): string {
    const f = (firstName || '').charAt(0).toUpperCase();
    const l = (lastName || '').charAt(0).toUpperCase();
    return `${f}${l}` || 'P';
  }
}
