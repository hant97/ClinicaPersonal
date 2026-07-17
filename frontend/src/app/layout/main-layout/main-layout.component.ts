import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LucideAngularModule, LayoutDashboard, Users, CalendarDays, Receipt, LogOut, ClipboardList, Package, Settings, Menu, X, User, Activity } from 'lucide-angular';
import { UserService } from '../../core/services/user.service';
import { UserProfile } from '../../core/models/user-profile.model';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  templateUrl: './main-layout.component.html',
  styleUrls: ['./main-layout.component.css']
})
export class MainLayoutComponent implements OnInit {
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

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.userService.getCurrentUserProfile().subscribe({
      next: (profile) => {
        this.userProfile = profile;
        if (profile.firstName) {
          this.greetingName = profile.firstName;
          this.avatarLetter = profile.firstName.charAt(0).toUpperCase();
        } else if (profile.username) {
          this.greetingName = profile.username;
          this.avatarLetter = profile.username.charAt(0).toUpperCase();
        }
      },
      error: () => {
        console.error('No se pudo cargar el perfil del usuario');
      }
    });
  }

  toggleSidebar() {
    this.isSidebarOpen = !this.isSidebarOpen;
  }
  
  closeSidebar() {
    this.isSidebarOpen = false;
  }
}
