import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { ClinicalServiceService } from '../../../core/services/clinical-service.service';
import { ClinicalService } from '../../../core/models/clinical-service.model';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { LucideAngularModule, Plus, Edit, Trash2, Search, Activity } from 'lucide-angular';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { ClinicalServicesFormComponent } from '../clinical-services-form/clinical-services-form.component';

@Component({
  selector: 'app-clinical-services-list',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, PaginationComponent, ClinicalServicesFormComponent, DecimalPipe],
  templateUrl: './clinical-services-list.component.html',
  styleUrls: ['./clinical-services-list.component.css']
})
export class ClinicalServicesListComponent implements OnInit {
  services: ClinicalService[] = [];
  
  // Icons
  readonly Plus = Plus;
  readonly Edit = Edit;
  readonly Trash2 = Trash2;
  readonly Search = Search;
  readonly Activity = Activity;
  
  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;
  searchTerm = '';
  
  // Modal state
  showModal = false;
  selectedServiceId: number | null = null;
  
  constructor(
    private serviceService: ClinicalServiceService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.loadServices();
  }
  
  loadServices(): void {
    this.serviceService.getAllServices(this.searchTerm, this.currentPage, this.pageSize)
      .subscribe({
        next: (response) => {
          this.services = response.content;
          this.totalPages = response.page.totalPages;
          this.totalElements = response.page.totalElements;
        },
        error: () => {
          this.toastService.show('Error al cargar la lista de servicios', 'error');
        }
      });
  }
  
  onSearch(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchTerm = target.value;
    this.currentPage = 0; // Reset to first page
    this.loadServices();
  }
  
  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadServices();
  }
  
  openModal(serviceId: number | null = null): void {
    this.selectedServiceId = serviceId;
    this.showModal = true;
  }
  
  closeModal(refresh: boolean): void {
    this.showModal = false;
    this.selectedServiceId = null;
    if (refresh) {
      this.loadServices();
    }
  }
  
  deleteService(id: number): void {
    if (confirm('¿Está seguro de que desea eliminar este servicio?')) {
      this.serviceService.deleteService(id).subscribe({
        next: () => {
          this.toastService.show('Servicio eliminado exitosamente', 'success');
          this.loadServices();
        },
        error: () => {
          this.toastService.show('Error al eliminar el servicio', 'error');
        }
      });
    }
  }
}
