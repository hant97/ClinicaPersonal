import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { UserService } from '../../../core/services/user.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { LucideAngularModule, User, Key, Save, Mail, Phone, Shield } from 'lucide-angular';

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

  activeTab: 'personal' | 'password' = 'personal';
  profileForm: FormGroup;
  passwordForm: FormGroup;
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
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
  }

  ngOnInit(): void {
    this.loadProfile();
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

  switchTab(tab: 'personal' | 'password'): void {
    this.activeTab = tab;
  }
}
