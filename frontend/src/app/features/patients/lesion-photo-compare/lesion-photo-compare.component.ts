import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Lesion } from '../../../core/models/lesion.model';
import { LesionPhoto } from '../../../core/models/lesion-photo.model';
import { LesionPhotoService } from '../../../core/services/lesion-photo.service';
import {
  LucideAngularModule,
  Calendar,
  Activity,
  X,
  Sparkles,
  Layers,
  Clock,
  ArrowLeftRight
} from 'lucide-angular';

export interface PhotoWithUrl {
  photo: LesionPhoto;
  url?: string;
}

@Component({
  selector: 'app-lesion-photo-compare',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './lesion-photo-compare.component.html',
})
export class LesionPhotoCompareComponent implements OnInit, OnDestroy {
  readonly Calendar = Calendar;
  readonly Activity = Activity;
  readonly X = X;
  readonly Sparkles = Sparkles;
  readonly Layers = Layers;
  readonly Clock = Clock;
  readonly ArrowLeftRight = ArrowLeftRight;

  @Input() lesion!: Lesion;
  @Input() photos: LesionPhoto[] = [];
  @Input() patientName?: string;
  @Output() close = new EventEmitter<void>();

  photoItems: PhotoWithUrl[] = [];
  selectedPhotoA: PhotoWithUrl | null = null;
  selectedPhotoB: PhotoWithUrl | null = null;

  compareMode: 'side-by-side' | 'slider' = 'side-by-side';
  sliderPosition: number = 50; // percentage for split view

  daysBetween: number | null = null;

  constructor(private photoService: LesionPhotoService) {}

  ngOnInit(): void {
    this.initPhotos();
  }

  ngOnDestroy(): void {
    this.revokeAllUrls();
  }

  private initPhotos(): void {
    // Sort chronologically ascending (earliest first)
    const sorted = [...this.photos].sort((a, b) => {
      const dateA = this.getPhotoDate(a).getTime();
      const dateB = this.getPhotoDate(b).getTime();
      return dateA - dateB;
    });

    this.photoItems = sorted.map(photo => ({ photo, url: undefined }));

    // Load blobs for all photos
    this.photoItems.forEach(item => this.loadBlob(item));

    if (this.photoItems.length > 0) {
      this.selectedPhotoA = this.photoItems[0];
    }
    if (this.photoItems.length > 1) {
      this.selectedPhotoB = this.photoItems[this.photoItems.length - 1];
    } else {
      this.selectedPhotoB = this.selectedPhotoA;
    }

    this.calculateDaysBetween();
  }

  private loadBlob(item: PhotoWithUrl): void {
    if (!item.photo.id) return;
    this.photoService.getPhotoFile(item.photo.id).subscribe({
      next: (blob) => {
        item.url = URL.createObjectURL(blob);
      },
      error: () => {}
    });
  }

  selectPhotoA(item: PhotoWithUrl): void {
    this.selectedPhotoA = item;
    this.calculateDaysBetween();
  }

  selectPhotoB(item: PhotoWithUrl): void {
    this.selectedPhotoB = item;
    this.calculateDaysBetween();
  }

  swapPhotos(): void {
    const temp = this.selectedPhotoA;
    this.selectedPhotoA = this.selectedPhotoB;
    this.selectedPhotoB = temp;
    this.calculateDaysBetween();
  }

  setMode(mode: 'side-by-side' | 'slider'): void {
    this.compareMode = mode;
  }

  private calculateDaysBetween(): void {
    if (!this.selectedPhotoA?.photo || !this.selectedPhotoB?.photo) {
      this.daysBetween = null;
      return;
    }
    const dateA = this.getPhotoDate(this.selectedPhotoA.photo).getTime();
    const dateB = this.getPhotoDate(this.selectedPhotoB.photo).getTime();
    const diffMs = Math.abs(dateB - dateA);
    this.daysBetween = Math.round(diffMs / (1000 * 60 * 60 * 24));
  }

  getPhotoDate(photo: LesionPhoto): Date {
    if (photo.takenDate) {
      return new Date(photo.takenDate);
    }
    if (photo.createdAt) {
      return new Date(photo.createdAt);
    }
    return new Date();
  }

  private revokeAllUrls(): void {
    this.photoItems.forEach(item => {
      if (item.url) {
        URL.revokeObjectURL(item.url);
      }
    });
  }

  onSliderInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.sliderPosition = Number(input.value);
  }

  closeModal(): void {
    this.close.emit();
  }
}
