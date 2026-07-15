import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService, DashboardStats } from '../../../core/services/dashboard.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  stats$!: Observable<DashboardStats>;

  constructor(private dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.stats$ = this.dashboardService.getDashboardStats();
  }
}
