import { Component, OnInit } from '@angular/core';

import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { ClinicSettingsService } from '../../../core/services/clinic-settings.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { UserProfile } from '../../../core/models/user-profile.model';
import { LucideAngularModule } from 'lucide-angular';
import {
  User,
  KeyRound,
  Save,
  Mail,
  Phone,
  Shield,
  ShieldCheck,
  Building2,
  ImagePlus,
  Eye,
  EyeOff,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Sparkles,
  Check,
  X,
  Lock,
  Brain,
  Stethoscope,
  ChevronRight,
  Info
} from '../../../shared/icons/lucide-icons';

@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [ReactiveFormsModule, LucideAngularModule],
  templateUrl: './user-profile.component.html',
})
export class UserProfileComponent implements OnInit {
  // Lucide Icons
  readonly UserIcon = User;
  readonly KeyRoundIcon = KeyRound;
  readonly SaveIcon = Save;
  readonly MailIcon = Mail;
  readonly PhoneIcon = Phone;
  readonly ShieldIcon = Shield;
  readonly ShieldCheckIcon = ShieldCheck;
  readonly Building2Icon = Building2;
  readonly ImagePlusIcon = ImagePlus;
  readonly EyeIcon = Eye;
  readonly EyeOffIcon = EyeOff;
  readonly CheckCircle2Icon = CheckCircle2;
  readonly AlertCircleIcon = AlertCircle;
  readonly RefreshCwIcon = RefreshCw;
  readonly SparklesIcon = Sparkles;
  readonly CheckIcon = Check;
  readonly XIcon = X;
  readonly LockIcon = Lock;
  readonly BrainIcon = Brain;
  readonly StethoscopeIcon = Stethoscope;
  readonly ChevronRightIcon = ChevronRight;
  readonly InfoIcon = Info;

  activeTab: 'personal' | 'password' | 'clinic' = 'personal';
  profileForm: FormGroup;
  passwordForm: FormGroup;
  clinicForm: FormGroup;

  userProfile: UserProfile | null = null;
  isLoading = false;
  savingProfile = false;
  savingPassword = false;
  savingClinic = false;

  // Password visibility flags
  showCurrentPassword = false;
  showNewPassword = false;
  showConfirmPassword = false;

  // Clinic Logo handling
  selectedLogo: File | null = null;
  logoPreview: string | null = null;

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private authService: AuthService,
    private clinicSettingsService: ClinicSettingsService,
    private toastService: ToastService
  ) {
    this.profileForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.maxLength(50)]],
      lastName: ['', [Validators.required, Validators.maxLength(50)]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.pattern(/^[0-9+() -]{6,20}$/)]]
    });

    this.passwordForm = this.fb.group(
      {
        currentPassword: ['', Validators.required],
        newPassword: [
          '',
          [
            Validators.required,
            Validators.minLength(8),
            Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}$/)
          ]
        ],
        confirmPassword: ['', Validators.required]
      },
      { validators: this.passwordMatchValidator }
    );

    this.clinicForm = this.fb.group({
      clinicName: ['', Validators.required],
      shortName: ['', Validators.required],
      contactEmail: ['', [Validators.required, Validators.email]],
      contactPhone: [''],
      address: ['']
    });
  }

  ngOnInit(): void {
    this.loadProfile();
    if (this.isAdmin) {
      this.loadClinicSettings();
    }
  }

  get isAdmin(): boolean {
    return this.authService.hasRole('ROLE_ADMIN') || this.authService.hasRole('ROLE_SITE_ADMIN');
  }

  get userFullName(): string {
    if (this.userProfile?.firstName || this.userProfile?.lastName) {
      return `${this.userProfile.firstName || ''} ${this.userProfile.lastName || ''}`.trim();
    }
    return this.userProfile?.username || 'Usuario';
  }

  get avatarLetter(): string {
    if (this.userProfile?.firstName) {
      return this.userProfile.firstName.charAt(0).toUpperCase();
    }
    if (this.userProfile?.username) {
      return this.userProfile.username.charAt(0).toUpperCase();
    }
    return 'U';
  }

  get specialty(): string {
    return this.userProfile?.specialty || this.authService.getSpecialty() || 'GENERAL';
  }

  get specialtyLabel(): string {
    const s = this.specialty;
    if (s === 'PSICOLOGIA') return 'Módulo Psicología';
    if (s === 'DERMATOLOGIA') return 'Módulo Dermatología';
    return 'Clínica Integral';
  }

  get specialtyBadgeClass(): string {
    const s = this.specialty;
    if (s === 'PSICOLOGIA') return 'bg-purple-50 text-purple-700 border-purple-200';
    if (s === 'DERMATOLOGIA') return 'bg-cyan-50 text-cyan-700 border-cyan-200';
    return 'bg-primary-50 text-primary-700 border-primary-200';
  }

  get roleLabel(): string {
    if (this.authService.hasRole('ROLE_SITE_ADMIN')) return 'Super Administrador';
    if (this.authService.hasRole('ROLE_ADMIN')) return 'Profesional Titular (Admin)';
    if (this.authService.hasRole('ROLE_ASISTENTE')) return 'Asistente / Recepción';
    return 'Usuario del Sistema';
  }

  passwordMatchValidator(g: FormGroup) {
    return g.get('newPassword')?.value === g.get('confirmPassword')?.value ? null : { mismatch: true };
  }

  // Password requirement check helpers
  hasMinLength(val: string): boolean {
    return val ? val.length >= 8 : false;
  }
  hasUppercase(val: string): boolean {
    return /[A-Z]/.test(val || '');
  }
  hasLowercase(val: string): boolean {
    return /[a-z]/.test(val || '');
  }
  hasNumber(val: string): boolean {
    return /\d/.test(val || '');
  }
  hasSpecialChar(val: string): boolean {
    return /[^A-Za-z\d]/.test(val || '');
  }

  loadProfile(): void {
    this.isLoading = true;
    this.userService.getCurrentUserProfile().subscribe({
      next: (profile) => {
        this.userProfile = profile;
        this.profileForm.patchValue({
          firstName: profile.firstName || '',
          lastName: profile.lastName || '',
          email: profile.email || '',
          phone: profile.phone || ''
        });
        this.isLoading = false;
      },
      error: () => {
        this.toastService.show('Error al cargar el perfil', 'error');
        this.isLoading = false;
      }
    });
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.savingProfile = true;
    this.userService.updateProfile(this.profileForm.value).subscribe({
      next: (profile) => {
        this.userProfile = { ...this.userProfile, ...profile };
        this.toastService.show('Perfil actualizado correctamente', 'success');
        this.savingProfile = false;
      },
      error: () => {
        this.toastService.show('Error al actualizar el perfil', 'error');
        this.savingProfile = false;
      }
    });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    this.savingPassword = true;
    const request = {
      currentPassword: this.passwordForm.value.currentPassword,
      newPassword: this.passwordForm.value.newPassword
    };

    this.userService.updatePassword(request).subscribe({
      next: (response) => {
        this.authService.loginResponse(response);
        this.toastService.show('Contraseña actualizada correctamente', 'success');
        this.passwordForm.reset();
        this.showCurrentPassword = false;
        this.showNewPassword = false;
        this.showConfirmPassword = false;
        this.savingPassword = false;
      },
      error: (err) => {
        const message = err?.error?.message || 'Error al actualizar la contraseña. Verifica la contraseña actual.';
        this.toastService.show(message, 'error');
        this.savingPassword = false;
      }
    });
  }

  switchTab(tab: 'personal' | 'password' | 'clinic'): void {
    this.activeTab = tab;
  }

  loadClinicSettings(): void {
    this.clinicSettingsService.getSettings().subscribe({
      next: (settings) => {
        this.clinicForm.patchValue({
          clinicName: settings.clinicName || '',
          shortName: settings.shortName || '',
          contactEmail: settings.contactEmail || '',
          contactPhone: settings.contactPhone || '',
          address: settings.address || ''
        });
        if (settings.logoUrl) {
          this.logoPreview = this.clinicSettingsService.getLogoUrl(settings.logoUrl);
        }
      },
      error: () => console.error('Error al cargar configuración de la clínica')
    });
  }

  onLogoSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) {
        this.toastService.show('Formato no permitido. Solo se aceptan PNG, JPG y WEBP.', 'error');
        event.target.value = '';
        return;
      }
      if (file.size > 2 * 1024 * 1024) {
        this.toastService.show('La imagen no debe superar los 2MB', 'error');
        event.target.value = '';
        return;
      }
      this.selectedLogo = file;
      const reader = new FileReader();
      reader.onload = () => {
        this.logoPreview = reader.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  saveClinicSettings(): void {
    if (this.clinicForm.invalid) {
      this.clinicForm.markAllAsTouched();
      return;
    }

    this.savingClinic = true;
    this.clinicSettingsService.updateSettings(this.clinicForm.value).subscribe({
      next: () => {
        if (this.selectedLogo) {
          this.clinicSettingsService.uploadLogo(this.selectedLogo).subscribe({
            next: () => {
              this.toastService.show('Configuración y logo guardados correctamente', 'success');
              this.savingClinic = false;
              this.selectedLogo = null;
            },
            error: () => {
              this.toastService.show('Error al guardar el logo', 'error');
              this.savingClinic = false;
            }
          });
        } else {
          this.toastService.show('Configuración de la clínica actualizada', 'success');
          this.savingClinic = false;
        }
      },
      error: () => {
        this.toastService.show('Error al actualizar la configuración de la clínica', 'error');
        this.savingClinic = false;
      }
    });
  }
}
