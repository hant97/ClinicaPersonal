import { Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterModule } from '@angular/router';
import {
  LucideAngularModule,
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
  Sparkles,
  Brain,
  Shield,
  X,
  Plus
} from 'lucide-angular';
import { UserService } from '../../core/services/user.service';
import { ClinicSettingsService, ClinicSettings } from '../../core/services/clinic-settings.service';
import { SpecialtyService } from '../../core/services/specialty.service';
import { AuthService } from '../../core/services/auth.service';
import { UserProfile } from '../../core/models/user-profile.model';
import { Subscription, filter } from 'rxjs';
import { CommandPaletteComponent } from '../../shared/components/command-palette/command-palette.component';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule, CommandPaletteComponent],
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
  readonly Sparkles = Sparkles;
  readonly Brain = Brain;
  readonly Shield = Shield;
  readonly X = X;
  readonly Plus = Plus;

  isSidebarOpen = false;
  isSidebarExpanded = true;
  isCommandPaletteOpen = false;
  
  userProfile: UserProfile | null = null;
  greetingName = 'Profesional';
  avatarLetter = 'P';
  clinicSettings: ClinicSettings | null = null;
  isPsychology = false;
  isDermatology = false;
  isSiteAdmin = false;
  currentRouteTitle = 'Dashboard';

  isMobile = false;
  private mobileQuery = window.matchMedia('(max-width: 767px)');
  private readonly onMobileChange = (event: MediaQueryListEvent): void => {
    this.isMobile = event.matches;
    if (!this.isMobile) {
      this.setScrollLock(false);
    }
  };

  @ViewChild('menuToggle', { static: false }) menuToggle?: ElementRef<HTMLButtonElement>;

  private profileSubscription?: Subscription;
  private settingsSubscription?: Subscription;
  private routerSubscription?: Subscription;

  constructor(
    private userService: UserService,
    private clinicSettingsService: ClinicSettingsService,
    private specialtyService: SpecialtyService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.isPsychology = this.specialtyService.isPsychology();
    this.isDermatology = this.specialtyService.isDermatology();
    this.isSiteAdmin = this.authService.hasRole('ROLE_SITE_ADMIN');
    
    this.loadProfile();
    this.updateRouteTitle(this.router.url);

    this.routerSubscription = this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => {
        this.updateRouteTitle(event.urlAfterRedirects);
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
  }

  ngOnDestroy(): void {
    this.profileSubscription?.unsubscribe();
    this.settingsSubscription?.unsubscribe();
    this.routerSubscription?.unsubscribe();
    this.mobileQuery.removeEventListener('change', this.onMobileChange);
    this.setScrollLock(false);
  }

  private updateRouteTitle(url: string): void {
    if (url.includes('/dashboard')) this.currentRouteTitle = 'Dashboard';
    else if (url.includes('/agenda')) this.currentRouteTitle = 'Agenda';
    else if (url.includes('/patients')) this.currentRouteTitle = 'Pacientes';
    else if (url.includes('/billing')) this.currentRouteTitle = 'Cobros';
    else if (url.includes('/services')) this.currentRouteTitle = 'Servicios Clínicos';
    else if (url.includes('/inventory')) this.currentRouteTitle = 'Inventario';
    else if (url.includes('/tests-catalog')) this.currentRouteTitle = 'Pruebas Psicométricas';
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
    this.isSiteAdmin = profile.roles?.includes('ROLE_SITE_ADMIN') === true;
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

  openCommandPalette(): void {
    this.isCommandPaletteOpen = true;
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
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
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
