import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { LesionService } from '../../../core/services/lesion.service';
import { LesionPhotoService } from '../../../core/services/lesion-photo.service';
import { Lesion } from '../../../core/models/lesion.model';
import { LesionPhoto } from '../../../core/models/lesion-photo.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { LesionPhotoCompareComponent } from '../lesion-photo-compare/lesion-photo-compare.component';
import {
  LucideAngularModule,
  Activity,
  ImagePlus,
  Edit,
  Trash2,
  Sparkles,
  Layers
} from 'lucide-angular';

interface PhotoView {
  photo: LesionPhoto;
  url?: string;
}

@Component({
  selector: 'app-lesions-section',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    PaginationComponent,
    LucideAngularModule,
    LesionPhotoCompareComponent
  ],
  templateUrl: './lesions-section.component.html',
})
export class LesionsSectionComponent implements OnInit, OnDestroy {
  readonly Activity = Activity;
  readonly ImagePlus = ImagePlus;
  readonly Edit = Edit;
  readonly Trash2 = Trash2;
  readonly Sparkles = Sparkles;
  readonly Layers = Layers;

  @Input() patientId!: number;

  lesions: Lesion[] = [];
  showForm = false;
  selected?: Lesion;
  form!: FormGroup;
  isSaving = false;
  loading = true;

  expandedPhotosLesionId: number | null = null;
  photos: PhotoView[] = [];

  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  showCompareModal = false;
  compareLesion: Lesion | null = null;
  comparePhotos: LesionPhoto[] = [];

  constructor(
    private fb: FormBuilder,
    private service: LesionService,
    private photoService: LesionPhotoService,
    private toast: ToastService,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.revokeUrls();
  }

  load(): void {
    this.loading = true;
    this.service.getByPatientId(this.patientId, this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        this.currentPage = page.page.number;
        this.totalPages = page.page.totalPages;
        this.totalElements = page.page.totalElements;
        this.lesions = page.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.show('Error al cargar lesiones', 'error');
      }
    });
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.load();
  }

  openForm(lesion?: Lesion): void {
    this.selected = lesion;
    this.form = this.fb.group({
      bodyArea: [lesion?.bodyArea || ''],
      lesionType: [lesion?.lesionType || ''],
      size: [lesion?.size || ''],
      morphology: [lesion?.morphology || ''],
      color: [lesion?.color || ''],
      sinceDate: [lesion?.sinceDate || ''],
      evolution: [lesion?.evolution || ''],
      notes: [lesion?.notes || '']
    });
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
    this.selected = undefined;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isSaving = true;
    const data: Lesion = { ...this.form.value, patientId: this.patientId };

    const request = this.selected?.id
      ? this.service.update(this.selected.id, data)
      : this.service.create(this.patientId, data);

    request.subscribe({
      next: () => {
        this.toast.show(this.selected?.id ? 'Lesión actualizada' : 'Lesión creada', 'success');
        this.isSaving = false;
        this.closeForm();
        this.load();
      },
      error: () => {
        this.isSaving = false;
        this.toast.show('Error al guardar la lesión', 'error');
      }
    });
  }

  async delete(id: number): Promise<void> {
    const confirmed = await this.notification.confirm(
      'Eliminar Lesión',
      '¿Está seguro de eliminar esta lesión y sus fotografías?',
      'Sí, eliminar',
      'Cancelar'
    );
    if (confirmed) {
      this.service.delete(id).subscribe({
        next: () => {
          this.toast.show('Lesión eliminada', 'success');
          this.load();
        },
        error: () => this.toast.show('Error al eliminar la lesión', 'error')
      });
    }
  }

  togglePhotos(lesionId: number): void {
    if (this.expandedPhotosLesionId === lesionId) {
      this.expandedPhotosLesionId = null;
      this.clearPhotos();
    } else {
      this.expandedPhotosLesionId = lesionId;
      this.loadPhotos(lesionId);
    }
  }

  loadPhotos(lesionId: number): void {
    this.clearPhotos();
    this.photoService.getPhotos(lesionId).subscribe({
      next: (photos) => {
        this.photos = photos.map((photo) => ({ photo, url: undefined }));
        this.photos.forEach((item) => this.loadPhotoBlob(item));
      },
      error: () => this.toast.show('Error al cargar fotografías', 'error')
    });
  }

  private loadPhotoBlob(item: PhotoView): void {
    if (!item.photo.id) return;
    this.photoService.getPhotoFile(item.photo.id).subscribe({
      next: (blob) => {
        item.url = URL.createObjectURL(blob);
      },
      error: () => {}
    });
  }

  onFileSelected(event: Event, lesionId: number): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.photoService.upload(lesionId, file).subscribe({
      next: () => {
        this.toast.show('Fotografía subida', 'success');
        input.value = '';
        this.loadPhotos(lesionId);
      },
      error: () => this.toast.show('Error al subir la fotografía', 'error')
    });
  }

  async deletePhoto(photoId: number, lesionId: number): Promise<void> {
    const confirmed = await this.notification.confirm(
      'Eliminar Fotografía',
      '¿Está seguro de eliminar esta fotografía?',
      'Sí, eliminar',
      'Cancelar'
    );
    if (confirmed) {
      this.photoService.delete(photoId).subscribe({
        next: () => {
          this.toast.show('Fotografía eliminada', 'success');
          this.loadPhotos(lesionId);
        },
        error: () => this.toast.show('Error al eliminar la fotografía', 'error')
      });
    }
  }

  private clearPhotos(): void {
    this.revokeUrls();
    this.photos = [];
  }

  private revokeUrls(): void {
    this.photos.forEach((item) => {
      if (item.url) {
        URL.revokeObjectURL(item.url);
      }
    });
  }

  openCompare(lesion: Lesion): void {
    if (!lesion.id) return;
    this.photoService.getPhotos(lesion.id).subscribe({
      next: (photos) => {
        this.compareLesion = lesion;
        this.comparePhotos = photos;
        this.showCompareModal = true;
      },
      error: () => this.toast.show('Error al cargar fotografías para comparación', 'error')
    });
  }

  closeCompare(): void {
    this.showCompareModal = false;
    this.compareLesion = null;
    this.comparePhotos = [];
  }
}
