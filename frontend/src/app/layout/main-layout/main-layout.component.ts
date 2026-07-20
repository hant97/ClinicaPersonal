import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LucideAngularModule, LayoutDashboard, Users, CalendarDays, Receipt, LogOut, ClipboardList, Package, Settings, Menu, X, User, Activity } from 'lucide-angular';
import { UserService } from '../../core/services/user.service';
import { ClinicSettingsService, ClinicSettings } from '../../core/services/clinic-settings.service';
import { UserProfile } from '../../core/models/user-profile.model';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  templateUrl: './main-layout.component.html',
  styleUrls: ['./main-layout.component.css']
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
  readonly X = X;
  readonly User = User;
  readonly Activity = Activity;

  isSidebarOpen = false;
  userProfile: UserProfile | null = null;
  greetingName = 'Usuario';
  avatarLetter = 'U';
  clinicSettings: ClinicSettings | null = null;

  private profileSubscription?: Subscription;
  private settingsSubscription?: Subscription;

  constructor(
    private userService: UserService,
    private clinicSettingsService: ClinicSettingsService
  ) {}

  ngOnInit(): void {
    this.loadProfile();
    this.profileSubscription = this.userService.profileUpdated$.subscribe(
      profile => this.updateHeaderProfile(profile)
    );
    this.settingsSubscription = this.clinicSettingsService.settings$.subscribe(
      settings => this.clinicSettings = settings
    );
  }

  ngOnDestroy(): void {
    if (this.profileSubscription) {
      this.profileSubscription.unsubscribe();
    }
    if (this.settingsSubscription) {
      this.settingsSubscription.unsubscribe();
    }
  }

  private loadProfile(): void {
    this.userService.getCurrentUserProfile().subscribe({
      next: (profile) => this.updateHeaderProfile(profile),
      error: () => console.error('No se pudo cargar el perfil del usuario')
    });
  }

  private updateHeaderProfile(profile: UserProfile): void {
    this.userProfile = profile;
    if (profile.firstName) {
      this.greetingName = profile.firstName;
      this.avatarLetter = profile.firstName.charAt(0).toUpperCase();
    } else if (profile.username) {
      this.greetingName = profile.username;
      this.avatarLetter = profile.username.charAt(0).toUpperCase();
    }
  }

  toggleSidebar() {
    this.isSidebarOpen = !this.isSidebarOpen;
  }
  
  closeSidebar() {
    this.isSidebarOpen = false;
  }

  getLogoUrl(path: string | undefined): string {
    if (!path) return '';
    return this.clinicSettingsService.getLogoUrl(path);
  }
}
