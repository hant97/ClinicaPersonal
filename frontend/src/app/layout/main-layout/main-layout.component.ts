import { Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild } from '@angular/core';

import { NavigationEnd, Router, RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import {
  LayoutDashboard,
  Users,
  CalendarDays,
  Receipt,
  LogOut,
  ClipboardList,
  Package,
  Settings,
  Menu,
  Activity,
  Stethoscope,
  HeartHandshake,
  Search,
  ChevronRight,
  ChevronLeft,
  ChevronDown,
  Sparkles,
  Brain,
  Shield,
  X,
  Plus,
  UserCog,
  User,
  BarChart3
} from '../../shared/icons/lucide-icons';
import { UserService } from '../../core/services/user.service';
import { ClinicSettingsService, ClinicSettings } from '../../core/services/clinic-settings.service';
import { SpecialtyService } from '../../core/services/specialty.service';
import { AuthService } from '../../core/services/auth.service';
import { UserProfile } from '../../core/models/user-profile.model';
import { Subscription, filter } from 'rxjs';
import { CommandPaletteComponent } from '../../shared/components/command-palette/command-palette.component';
import { VIEWPORT_BREAKPOINTS } from '../../shared/services/view-preference/view-preference.service';
import { OnboardingService } from '../../shared/services/onboarding/onboarding.service';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterModule, LucideAngularModule, CommandPaletteComponent],
  templateUrl: './main-layout.component.html',
})
export class MainLayoutComponent implements OnInit, OnDestroy {
  readonly LayoutDashboard = LayoutDashboard;
  readonly Users = Users;
  readonly CalendarDays = CalendarDays;
  readonly Receipt = Receipt;
  readonly LogOut = LogOut;
  readonly ClipboardList = ClipboardList;
  readonly Package = Package;
  readonly Settings = Settings;
  readonly Menu = Menu;
  readonly Activity = Activity;
  readonly Stethoscope = Stethoscope;
  readonly HeartHandshake = HeartHandshake;
  readonly Search = Search;
  readonly ChevronRight = ChevronRight;
  readonly ChevronLeft = ChevronLeft;
  readonly ChevronDown = ChevronDown;
  readonly Sparkles = Sparkles;
  readonly Brain = Brain;
  readonly Shield = Shield;
  readonly X = X;
  readonly Plus = Plus;
  readonly UserCog = UserCog;
  readonly User = User;
  readonly BarChart3 = BarChart3;

  isSidebarOpen = false;
  isSidebarExpanded = false;
  isDesktopSidebar = false;
  isCommandPaletteOpen = false;
  isProfileMenuOpen = false;
  
  userProfile: UserProfile | null = null;
  greetingName = 'Profesional';
  avatarLetter = 'P';
  clinicSettings: ClinicSettings | null = null;
  isPsychology = false;
  isDermatology = false;
  isAdmin = false;
  isSiteAdmin = false;
  currentRouteTitle = 'Dashboard';

  // Sección "Operación & Recursos" del sidebar: colapsada por defecto la
  // primera vez para que un usuario nuevo vea primero solo lo operativo
  // diario (Dashboard, Agenda, Atenciones, Pacientes, Cobros).
  operationsSectionCollapsed = true;
  private static readonly OPERATIONS_ROUTE_PREFIXES = [
    '/services',
    '/tests-catalog',
    '/inventory',
    '/settings/catalogs',
    '/settings/users',
    '/settings/audit',
    '/settings/productivity',
  ];

  isMobile = false;
  private mobileQuery = window.matchMedia(
    `(max-width: ${VIEWPORT_BREAKPOINTS.mobile - 1}px)`
  );
  private desktopSidebarPreference = true;
  private desktopSidebarQuery = window.matchMedia(
    `(min-width: ${VIEWPORT_BREAKPOINTS.expandedSidebar}px)`
  );
  private readonly onMobileChange = (event: MediaQueryListEvent): void => {
    this.isMobile = event.matches;
    if (!this.isMobile) {
      this.setScrollLock(false);
    }
  };
  private readonly onDesktopSidebarChange = (event: MediaQueryListEvent): void => {
    this.applyDesktopSidebarMode(event.matches);
  };

  @ViewChild('menuToggle', { static: false }) menuToggle?: ElementRef<HTMLButtonElement>;
  @ViewChild('profileMenuToggle', { static: false }) profileMenuToggle?: ElementRef<HTMLButtonElement>;
  @ViewChild('profileDropdownContainer', { static: false }) profileDropdownContainer?: ElementRef<HTMLElement>;

  private profileSubscription?: Subscription;
  private settingsSubscription?: Subscription;
  private routerSubscription?: Subscription;

  get userFullName(): string {
    if (this.userProfile?.firstName || this.userProfile?.lastName) {
      return `${this.userProfile.firstName || ''} ${this.userProfile.lastName || ''}`.trim();
    }
    return this.userProfile?.username || this.greetingName || 'Usuario';
  }

  get roleLabel(): string {
    if (this.isSiteAdmin) return 'Super Admin';
    if (this.isAdmin) return 'Administrador';
    if (this.userProfile?.specialty) return this.userProfile.specialty;
    if (this.isPsychology) return 'Psicología';
    if (this.isDermatology) return 'Dermatología';
    return 'Profesional';
  }

  constructor(
    private userService: UserService,
    private clinicSettingsService: ClinicSettingsService,
    private specialtyService: SpecialtyService,
    private authService: AuthService,
    private onboardingService: OnboardingService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.isPsychology = this.specialtyService.isPsychology();
    this.isDermatology = this.specialtyService.isDermatology();
    this.isAdmin = this.authService.hasRole('ROLE_ADMIN');
    this.isSiteAdmin = this.authService.hasRole('ROLE_SITE_ADMIN');
    
    try {
      const saved = localStorage.getItem('sidebar_expanded');
      if (saved !== null) {
        const parsedPreference: unknown = JSON.parse(saved);
        if (typeof parsedPreference === 'boolean') {
          this.desktopSidebarPreference = parsedPreference;
        }
      }
    } catch {
      this.desktopSidebarPreference = true;
    }
    this.applyDesktopSidebarMode(this.desktopSidebarQuery.matches);

    this.loadProfile();
    this.updateRouteTitle(this.router.url);

    this.operationsSectionCollapsed = this.onboardingService.isSidebarSectionCollapsed('operations', true);
    this.autoExpandOperationsSection(this.router.url);

    this.routerSubscription = this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => {
        this.updateRouteTitle(event.urlAfterRedirects);
        this.autoExpandOperationsSection(event.urlAfterRedirects);
        this.closeProfileMenu();
        if (this.isMobile) {
          this.closeSidebar();
        }
      });

    this.profileSubscription = this.userService.profileUpdated$.subscribe(
      profile => this.updateHeaderProfile(profile)
    );
    this.settingsSubscription = this.clinicSettingsService.settings$.subscribe(
      settings => this.clinicSettings = settings
    );
    this.isMobile = this.mobileQuery.matches;
    this.mobileQuery.addEventListener('change', this.onMobileChange);
    this.desktopSidebarQuery.addEventListener('change', this.onDesktopSidebarChange);
  }

  ngOnDestroy(): void {
    this.profileSubscription?.unsubscribe();
    this.settingsSubscription?.unsubscribe();
    this.routerSubscription?.unsubscribe();
    this.mobileQuery.removeEventListener('change', this.onMobileChange);
    this.desktopSidebarQuery.removeEventListener('change', this.onDesktopSidebarChange);
    this.setScrollLock(false);
  }

  private updateRouteTitle(url: string): void {
    if (url.includes('/dashboard')) this.currentRouteTitle = 'Dashboard';
    else if (url.includes('/agenda')) this.currentRouteTitle = 'Agenda';
    else if (url.includes('/attentions')) this.currentRouteTitle = 'Atenciones';
    else if (url.includes('/patients')) this.currentRouteTitle = 'Pacientes';
    else if (url.includes('/billing')) this.currentRouteTitle = 'Cobros';
    else if (url.includes('/services')) this.currentRouteTitle = 'Servicios Clínicos';
    else if (url.includes('/inventory')) this.currentRouteTitle = 'Inventario';
    else if (url.includes('/tests-catalog')) this.currentRouteTitle = 'Pruebas Psicométricas';
    else if (url.includes('/settings/users')) this.currentRouteTitle = 'Personal y Cuentas';
    else if (url.includes('/settings/productivity')) this.currentRouteTitle = 'Reporte de Productividad';
    else if (url.includes('/settings')) this.currentRouteTitle = 'Configuración';
    else if (url.includes('/profile')) this.currentRouteTitle = 'Mi Perfil';
    else this.currentRouteTitle = 'FlowGrid OS';
  }

  private loadProfile(): void {
    this.userService.getCurrentUserProfile().subscribe({
      next: (profile) => this.updateHeaderProfile(profile),
      error: () => console.error('No se pudo cargar el perfil del usuario')
    });
  }

  private updateHeaderProfile(profile: UserProfile): void {
    this.userProfile = profile;
    this.isAdmin = profile.roles?.some(r => r === 'ROLE_ADMIN') === true;
    this.isSiteAdmin = profile.roles?.some(r => r === 'ROLE_SITE_ADMIN') === true;
    if (profile.firstName) {
      this.greetingName = profile.firstName;
      this.avatarLetter = profile.firstName.charAt(0).toUpperCase();
    } else if (profile.username) {
      this.greetingName = profile.username;
      this.avatarLetter = profile.username.charAt(0).toUpperCase();
    }
  }

  toggleSidebar(): void {
    this.isSidebarOpen = !this.isSidebarOpen;
    this.setScrollLock(this.isSidebarOpen);
  }
  
  closeSidebar(): void {
    if (this.isSidebarOpen) {
      this.isSidebarOpen = false;
      this.setScrollLock(false);
    }
  }

  toggleSidebarCollapse(): void {
    if (!this.isDesktopSidebar) {
      return;
    }
    this.isSidebarExpanded = !this.isSidebarExpanded;
    this.desktopSidebarPreference = this.isSidebarExpanded;
    try {
      localStorage.setItem('sidebar_expanded', JSON.stringify(this.isSidebarExpanded));
    } catch {
      // ignore
    }
  }

  toggleOperationsSection(): void {
    this.operationsSectionCollapsed = !this.operationsSectionCollapsed;
    this.onboardingService.setSidebarSectionCollapsed('operations', this.operationsSectionCollapsed);
  }

  private autoExpandOperationsSection(url: string): void {
    if (!this.operationsSectionCollapsed) return;
    const isOperationsRoute = MainLayoutComponent.OPERATIONS_ROUTE_PREFIXES.some((prefix) => url.startsWith(prefix));
    if (isOperationsRoute) {
      this.operationsSectionCollapsed = false;
      this.onboardingService.setSidebarSectionCollapsed('operations', false);
    }
  }

  private applyDesktopSidebarMode(isDesktopSidebar: boolean): void {
    this.isDesktopSidebar = isDesktopSidebar;
    this.isSidebarExpanded = isDesktopSidebar ? this.desktopSidebarPreference : false;
  }

  toggleProfileMenu(event?: MouseEvent): void {
    if (event) {
      event.stopPropagation();
    }
    this.isProfileMenuOpen = !this.isProfileMenuOpen;
  }

  closeProfileMenu(): void {
    this.isProfileMenuOpen = false;
  }

  openCommandPalette(): void {
    this.isCommandPaletteOpen = true;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (
      this.isProfileMenuOpen &&
      this.profileDropdownContainer &&
      !this.profileDropdownContainer.nativeElement.contains(event.target as Node)
    ) {
      this.closeProfileMenu();
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.isProfileMenuOpen) {
      this.closeProfileMenu();
      this.profileMenuToggle?.nativeElement.focus();
    }
    if (this.isSidebarOpen) {
      this.closeSidebar();
      this.menuToggle?.nativeElement.focus();
    }
  }

  @HostListener('document:keydown', ['$event'])
  onGlobalKeydown(event: KeyboardEvent): void {
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
      event.preventDefault();
      this.isCommandPaletteOpen = !this.isCommandPaletteOpen;
    }
  }

  private setScrollLock(locked: boolean): void {
    document.body.style.overflow = locked ? 'hidden' : '';
  }

  getLogoUrl(path: string | undefined): string {
    if (!path) return '';
    return this.clinicSettingsService.getLogoUrl(path);
  }

  logout(): void {
    this.closeProfileMenu();
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
