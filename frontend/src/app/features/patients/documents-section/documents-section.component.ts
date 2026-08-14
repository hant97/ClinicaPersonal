import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ClinicalDocumentService } from '../../../core/services/clinical-document.service';
import { ClinicalDocument } from '../../../core/models/clinical-document.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { LucideAngularModule, FileText, Image, Upload, Download, Eye, Trash2, X, FolderOpen } from 'lucide-angular';

const CATEGORY_LABELS: Record<string, string> = {
  CONSENTIMIENTO: 'Consentimiento',
  RESULTADO: 'Resultado',
  DERIVACION: 'Derivación',
  RECETA: 'Receta',
  OTRO: 'Otro'
};

@Component({
  selector: 'app-documents-section',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PaginationComponent, LucideAngularModule],
  templateUrl: './documents-section.component.html',
})
export class DocumentsSectionComponent implements OnInit {
  readonly FileText = FileText;
  readonly Image = Image;
  readonly Upload = Upload;
  readonly Download = Download;
  readonly Eye = Eye;
  readonly Trash2 = Trash2;
  readonly X = X;
  readonly FolderOpen = FolderOpen;

  @Input() patientId!: number;

  documents: ClinicalDocument[] = [];
  loading = true;
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  showUpload = false;
  showPreview = false;
  isUploading = false;
  selectedFile: File | null = null;
  uploadForm!: FormGroup;

  previewDoc: ClinicalDocument | null = null;
  previewUrl: string | null = null;

  constructor(
    private fb: FormBuilder,
    private service: ClinicalDocumentService,
    private toast: ToastService,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.service.getByPatientId(this.patientId, this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        this.currentPage = page.page.number;
        this.totalPages = page.page.totalPages;
        this.totalElements = page.page.totalElements;
        this.documents = page.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.show('Error al cargar documentos', 'error');
      }
    });
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.load();
  }

  openUpload(): void {
    this.selectedFile = null;
    this.uploadForm = this.fb.group({
      category: ['OTRO'],
      documentDate: ['']
    });
    this.showUpload = true;
  }

  closeUpload(): void {
    this.showUpload = false;
    this.selectedFile = null;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
  }

  submitUpload(): void {
    if (!this.selectedFile) {
      this.toast.show('Seleccione un archivo para subir', 'error');
      return;
    }
    this.isUploading = true;
    const category = this.uploadForm.get('category')?.value || 'OTRO';
    const documentDate = this.uploadForm.get('documentDate')?.value || undefined;

    this.service.upload(this.patientId, this.selectedFile, category, undefined, documentDate).subscribe({
      next: () => {
        this.toast.show('Documento subido', 'success');
        this.isUploading = false;
        this.closeUpload();
        this.load();
      },
      error: () => {
        this.isUploading = false;
        this.toast.show('Error al subir el documento', 'error');
      }
    });
  }

  openPreview(document: ClinicalDocument): void {
    if (!document.id) return;
    this.service.getFile(document.id).subscribe({
      next: (blob) => {
        this.revokePreviewUrl();
        this.previewDoc = document;
        this.previewUrl = URL.createObjectURL(blob);
        this.showPreview = true;
      },
      error: () => this.toast.show('Error al abrir el documento', 'error')
    });
  }

  closePreview(): void {
    this.showPreview = false;
    this.revokePreviewUrl();
  }

  download(doc: ClinicalDocument): void {
    if (!doc.id) return;
    this.service.getFile(doc.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const anchor = window.document.createElement('a');
        anchor.href = url;
        anchor.download = this.downloadName(doc);
        window.document.body.appendChild(anchor);
        anchor.click();
        window.document.body.removeChild(anchor);
        URL.revokeObjectURL(url);
      },
      error: () => this.toast.show('Error al descargar el documento', 'error')
    });
  }

  async delete(id: number): Promise<void> {
    const confirmed = await this.notification.confirm(
      'Eliminar Documento',
      '¿Está seguro de eliminar este documento?',
      'Sí, eliminar',
      'Cancelar'
    );
    if (confirmed) {
      this.service.delete(id).subscribe({
        next: () => {
          this.toast.show('Documento eliminado', 'success');
          this.load();
        },
        error: () => this.toast.show('Error al eliminar el documento', 'error')
      });
    }
  }

  categoryLabel(category?: string): string {
    return CATEGORY_LABELS[category || ''] || category || 'Otro';
  }

  isImage(mimeType?: string): boolean {
    return (mimeType || '').startsWith('image/');
  }

  formatSize(sizeBytes?: number): string {
    if (sizeBytes == null) return '—';
    if (sizeBytes < 1024) return `${sizeBytes} B`;
    if (sizeBytes < 1024 * 1024) return `${(sizeBytes / 1024).toFixed(1)} KB`;
    return `${(sizeBytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  private downloadName(document: ClinicalDocument): string {
    const extension = this.extensionFor(document.mimeType);
    return `${document.name}${extension}`;
  }

  private extensionFor(mimeType?: string): string {
    switch (mimeType) {
      case 'application/pdf': return '.pdf';
      case 'image/png': return '.png';
      case 'image/webp': return '.webp';
      case 'image/jpeg': return '.jpg';
      default: return '';
    }
  }

  private revokePreviewUrl(): void {
    if (this.previewUrl) {
      URL.revokeObjectURL(this.previewUrl);
      this.previewUrl = null;
    }
  }
}
