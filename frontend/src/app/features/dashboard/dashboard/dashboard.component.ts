import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService, DashboardStats } from '../../../core/services/dashboard.service';
import { Observable } from 'rxjs';
import { LucideAngularModule, Users, Calendar, DollarSign, Activity, UserPlus, TrendingUp, TrendingDown, Clock, AlertTriangle, Package } from 'lucide-angular';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  stats$!: Observable<DashboardStats>;
  
  readonly Users = Users;
  readonly Calendar = Calendar;
  readonly DollarSign = DollarSign;
  readonly Activity = Activity;
  readonly UserPlus = UserPlus;
  readonly TrendingUp = TrendingUp;
  readonly TrendingDown = TrendingDown;
  readonly Clock = Clock;
  readonly AlertTriangle = AlertTriangle;
  readonly Package = Package;

  constructor(private dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.stats$ = this.dashboardService.getDashboardStats();
  }
}
