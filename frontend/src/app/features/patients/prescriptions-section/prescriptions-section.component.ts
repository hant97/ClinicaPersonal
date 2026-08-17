import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { PrescriptionService } from '../../../core/services/prescription.service';
import { Prescription, PrescriptionItem } from '../../../core/models/prescription.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { QrCodeComponent } from '../../../shared/components/qr-code/qr-code.component';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { PatientService } from '../../../core/services/patient/patient.service';
import { Patient } from '../../../core/models/patient.model';
import { ClinicSettingsService, ClinicSettings } from '../../../core/services/clinic-settings.service';
import { UserService } from '../../../core/services/user.service';
import { UserProfile } from '../../../core/models/user-profile.model';
import { LucideAngularModule, FileText, Printer, Plus, Trash2, X, Edit, QrCode, ShieldCheck } from 'lucide-angular';

@Component({
  selector: 'app-prescriptions-section',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PaginationComponent, QrCodeComponent, LucideAngularModule],
  templateUrl: './prescriptions-section.component.html',
  styleUrls: ['./prescriptions-section.component.css'],
})
export class PrescriptionsSectionComponent implements OnInit {
  readonly FileText = FileText;
  readonly Printer = Printer;
  readonly Plus = Plus;
  readonly Trash2 = Trash2;
  readonly X = X;
  readonly Edit = Edit;
  readonly QrCode = QrCode;
  readonly ShieldCheck = ShieldCheck;

  @Input() patientId!: number;

  prescriptions: Prescription[] = [];
  patient: Patient | null = null;
  clinicSettings: ClinicSettings | null = null;
  currentProfessional: UserProfile | null = null;
  showForm = false;
  showPrint = false;
  selected?: Prescription;
  printPrescription?: Prescription;
  form!: FormGroup;
  isSaving = false;
  loading = true;

  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  constructor(
    private fb: FormBuilder,
    private service: PrescriptionService,
    private toast: ToastService,
    private notification: NotificationService,
    private patientService: PatientService,
    private clinicSettingsService: ClinicSettingsService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.load();
    this.patientService.getById(this.patientId).subscribe({
      next: (patient) => (this.patient = patient),
      error: () => {}
    });
    this.clinicSettingsService.settings$.subscribe((settings) => (this.clinicSettings = settings));
    this.clinicSettingsService.loadSettings();
    this.userService.getCurrentUserProfile().subscribe({
      next: (profile) => (this.currentProfessional = profile),
      error: () => {}
    });
  }

  get items(): FormArray {
    return this.form.get('items') as FormArray;
  }

  load(): void {
    this.loading = true;
    this.service.getByPatientId(this.patientId, this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        this.currentPage = page.page.number;
        this.totalPages = page.page.totalPages;
        this.totalElements = page.page.totalElements;
        this.prescriptions = page.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.show('Error al cargar recetas', 'error');
      }
    });
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.load();
  }

  openForm(prescription?: Prescription): void {
    this.selected = prescription;
    this.form = this.fb.group({
      prescriptionDate: [prescription?.prescriptionDate || this.todayIso()],
      validUntil: [prescription?.validUntil || ''],
      notes: [prescription?.notes || ''],
      items: this.fb.array([])
    });
    const items = prescription?.items?.length ? prescription.items : [{} as PrescriptionItem];
    items.forEach((item) => this.items.push(this.newItem(item)));
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
    this.selected = undefined;
  }

  newItem(item?: PrescriptionItem): FormGroup {
    return this.fb.group({
      name: [item?.name || '', Validators.required],
      dose: [item?.dose || ''],
      frequency: [item?.frequency || ''],
      duration: [item?.duration || ''],
      route: [item?.route || ''],
      instructions: [item?.instructions || '']
    });
  }

  addItem(): void {
    this.items.push(this.newItem());
  }

  removeItem(index: number): void {
    this.items.removeAt(index);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isSaving = true;
    const data: Prescription = { ...this.form.value, patientId: this.patientId };

    const request = this.selected?.id
      ? this.service.update(this.selected.id, data)
      : this.service.create(this.patientId, data);

    request.subscribe({
      next: () => {
        this.toast.show(this.selected?.id ? 'Receta actualizada' : 'Receta creada', 'success');
        this.isSaving = false;
        this.closeForm();
        this.load();
      },
      error: () => {
        this.isSaving = false;
        this.toast.show('Error al guardar la receta', 'error');
      }
    });
  }

  async delete(id: number): Promise<void> {
    const confirmed = await this.notification.confirm(
      'Eliminar Receta',
      '¿Está seguro de eliminar esta receta?',
      'Sí, eliminar',
      'Cancelar'
    );
    if (confirmed) {
      this.service.delete(id).subscribe({
        next: () => {
          this.toast.show('Receta eliminada', 'success');
          this.load();
        },
        error: () => this.toast.show('Error al eliminar la receta', 'error')
      });
    }
  }

  openPrint(prescription: Prescription): void {
    this.printPrescription = prescription;
    this.showPrint = true;
  }

  closePrint(): void {
    this.showPrint = false;
    this.printPrescription = undefined;
  }

  print(): void {
    window.print();
  }

  getLogoUrl(path: string | undefined): string {
    if (!path) {
      return '';
    }
    return this.clinicSettingsService.getLogoUrl(path);
  }

  hasInstructions(prescription: Prescription): boolean {
    return prescription.items.some((item) => !!item.instructions);
  }

  getVerificationUrl(prescription?: Prescription): string {
    if (!prescription) return '';
    const code = prescription.verificationCode || `REC-${prescription.id}`;
    return `${window.location.origin}/verificar-receta/${code}`;
  }

  getProfessionalDisplayName(): string {
    if (!this.currentProfessional) return 'Profesional de la Salud';
    const name = `${this.currentProfessional.firstName || ''} ${this.currentProfessional.lastName || ''}`.trim();
    return name || this.currentProfessional.username;
  }

  private todayIso(): string {
    return new Date().toISOString().slice(0, 10);
  }
}
