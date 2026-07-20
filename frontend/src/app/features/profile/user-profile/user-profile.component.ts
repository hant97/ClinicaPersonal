import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { UserService } from '../../../core/services/user.service';
import { ClinicSettingsService } from '../../../core/services/clinic-settings.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { LucideAngularModule, User, Key, Save, Mail, Phone, Shield, Building2, ImagePlus } from 'lucide-angular';

@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LucideAngularModule],
  templateUrl: './user-profile.component.html',
  styleUrls: ['./user-profile.component.css']
})
export class UserProfileComponent implements OnInit {
  readonly UserIcon = User;
  readonly KeyIcon = Key;
  readonly SaveIcon = Save;
  readonly MailIcon = Mail;
  readonly PhoneIcon = Phone;
  readonly ShieldIcon = Shield;
  readonly Building2Icon = Building2;
  readonly ImagePlusIcon = ImagePlus;

  activeTab: 'personal' | 'password' | 'clinic' = 'personal';
  profileForm: FormGroup;
  passwordForm: FormGroup;
  clinicForm: FormGroup;
  isLoading = false;

  selectedLogo: File | null = null;
  logoPreview: string | null = null;

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private clinicSettingsService: ClinicSettingsService,
    private toastService: ToastService
  ) {
    this.profileForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phone: ['']
    });

    this.passwordForm = this.fb.group({
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });

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
    this.loadClinicSettings();
  }

  passwordMatchValidator(g: FormGroup) {
    return g.get('newPassword')?.value === g.get('confirmPassword')?.value
      ? null : { mismatch: true };
  }

  loadProfile(): void {
    this.isLoading = true;
    this.userService.getCurrentUserProfile().subscribe({
      next: (profile) => {
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
    if (this.profileForm.valid) {
      this.isLoading = true;
      this.userService.updateProfile(this.profileForm.value).subscribe({
        next: (profile) => {
          this.toastService.show('Perfil actualizado correctamente', 'success');
          // Optionally update layout header if communicating via a shared service
          this.isLoading = false;
        },
        error: () => {
          this.toastService.show('Error al actualizar el perfil', 'error');
          this.isLoading = false;
        }
      });
    }
  }

  changePassword(): void {
    if (this.passwordForm.valid) {
      this.isLoading = true;
      const request = {
        currentPassword: this.passwordForm.value.currentPassword,
        newPassword: this.passwordForm.value.newPassword
      };
      this.userService.updatePassword(request).subscribe({
        next: () => {
          this.toastService.show('Contraseña actualizada correctamente', 'success');
          this.passwordForm.reset();
          this.isLoading = false;
        },
        error: () => {
          this.toastService.show('Error al actualizar la contraseña', 'error');
          this.isLoading = false;
        }
      });
    }
  }

  switchTab(tab: 'personal' | 'password' | 'clinic'): void {
    this.activeTab = tab;
  }

  loadClinicSettings(): void {
    this.clinicSettingsService.getSettings().subscribe({
      next: (settings) => {
        this.clinicForm.patchValue({
          clinicName: settings.clinicName,
          shortName: settings.shortName,
          contactEmail: settings.contactEmail,
          contactPhone: settings.contactPhone,
          address: settings.address
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
      if (file.size > 2 * 1024 * 1024) { // 2MB
        this.toastService.show('La imagen no debe superar los 2MB', 'error');
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
    if (this.clinicForm.valid) {
      this.isLoading = true;
      this.clinicSettingsService.updateSettings(this.clinicForm.value).subscribe({
        next: () => {
          if (this.selectedLogo) {
            this.clinicSettingsService.uploadLogo(this.selectedLogo).subscribe({
              next: () => {
                this.toastService.show('Configuración y logo guardados', 'success');
                this.isLoading = false;
                this.selectedLogo = null;
              },
              error: () => {
                this.toastService.show('Error al guardar el logo', 'error');
                this.isLoading = false;
              }
            });
          } else {
            this.toastService.show('Configuración de la clínica actualizada', 'success');
            this.isLoading = false;
          }
        },
        error: () => {
          this.toastService.show('Error al actualizar configuración', 'error');
          this.isLoading = false;
        }
      });
    }
  }
}
