import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LucideAngularModule, LayoutDashboard, Users, CalendarDays, Receipt, LogOut, ClipboardList, Package, Settings } from 'lucide-angular';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  templateUrl: './main-layout.component.html',
  styleUrls: ['./main-layout.component.css']
})
export class MainLayoutComponent {
  readonly LayoutDashboard = LayoutDashboard;
  readonly Users = Users;
  readonly CalendarDays = CalendarDays;
  readonly Receipt = Receipt;
  readonly LogOut = LogOut;
  readonly ClipboardList = ClipboardList;
  readonly Package = Package;
  readonly Settings = Settings;
}
