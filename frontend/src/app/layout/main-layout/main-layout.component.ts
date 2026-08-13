import { Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LucideAngularModule, LayoutDashboard, Users, CalendarDays, Receipt, LogOut, ClipboardList, Package, Settings, Menu, Activity, Stethoscope, HeartHandshake } from 'lucide-angular';
import { UserService } from '../../core/services/user.service';
import { ClinicSettingsService, ClinicSettings } from '../../core/services/clinic-settings.service';
import { SpecialtyService } from '../../core/services/specialty.service';
import { AuthService } from '../../core/services/auth.service';
import { Router } from '@angular/router';
import { UserProfile } from '../../core/models/user-profile.model';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
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

  isSidebarOpen = false;
  userProfile: UserProfile | null = null;
  greetingName = 'Usuario';
  avatarLetter = 'U';
  clinicSettings: ClinicSettings | null = null;
  isPsychology = false;
  isDermatology = false;
  isSiteAdmin = false;

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
    if (this.profileSubscription) {
      this.profileSubscription.unsubscribe();
    }
    if (this.settingsSubscription) {
      this.settingsSubscription.unsubscribe();
    }
    this.mobileQuery.removeEventListener('change', this.onMobileChange);
    this.setScrollLock(false);
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

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.isSidebarOpen) {
      this.closeSidebar();
      this.menuToggle?.nativeElement.focus();
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
