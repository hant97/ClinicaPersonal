import { Component, OnInit } from '@angular/core';

import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { UserService } from '../../../core/services/user.service';
import { UserProfile, CreateUserRequest, AdminUpdateUserRequest } from '../../../core/models/user-profile.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { LucideAngularModule } from 'lucide-angular';
import {
  Users,
  UserPlus,
  Edit2,
  KeyRound,
  ShieldCheck,
  Search,
  CheckCircle2,
  XCircle,
  X,
  Lock,
  UserCheck,
  UserX,
  Stethoscope,
  Brain
} from '../../../shared/icons/lucide-icons';

import { SpecialtyService } from '../../../core/services/specialty.service';
import { SpecialtyItem } from '../../../core/models/specialty.model';
import { ASSIGNABLE_ROLES, ROLES, RoleOption, describeRoles, roleSelectionError } from '../../../core/models/roles';

const rolesValidator = (control: AbstractControl): ValidationErrors | null => {
  const error = roleSelectionError(control.value as string[]);
  return error ? { roles: error } : null;
};

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [ReactiveFormsModule, PaginationComponent, LucideAngularModule],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.css'],
})
export class UserManagementComponent implements OnInit {
  readonly Users = Users;
  readonly UserPlus = UserPlus;
  readonly Edit2 = Edit2;
  readonly KeyRound = KeyRound;
  readonly ShieldCheck = ShieldCheck;
  readonly Search = Search;
  readonly CheckCircle2 = CheckCircle2;
  readonly XCircle = XCircle;
  readonly X = X;
  readonly Lock = Lock;
  readonly UserCheck = UserCheck;
  readonly UserX = UserX;
  readonly Stethoscope = Stethoscope;
  readonly Brain = Brain;

  users: UserProfile[] = [];
  specialties: SpecialtyItem[] = [];
  loading = true;
  saving = false;

  // Filters & Pagination
  searchQuery = '';
  selectedSpecialty = '';
  selectedEnabled: boolean | null = null;
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  // Modals state
  showUserModal = false;
  showPasswordModal = false;
  selectedUser: UserProfile | null = null;

  userForm!: FormGroup;
  passwordForm!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private specialtyService: SpecialtyService,
    private toast: ToastService,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    this.initForms();
    this.loadSpecialties();
    this.loadUsers();
  }

  private loadSpecialties(): void {
    this.specialtyService.getActiveSpecialties().subscribe({
      next: (list) => {
        this.specialties = list || [];
        if (this.specialties.length > 0 && !this.userForm.get('specialty')?.value) {
          this.userForm.patchValue({ specialty: this.specialties[0].code });
        }
      },
      error: () => {
        // Fallback en caso de error
        this.specialties = [
          { id: 1, code: 'PSICOLOGIA', name: 'Psicología', active: true, displayOrder: 1 },
          { id: 2, code: 'DERMATOLOGIA', name: 'Dermatología', active: true, displayOrder: 2 }
        ];
      }
    });
  }

  private initForms(): void {
    this.userForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.minLength(8)]],
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      email: ['', [Validators.email]],
      phone: [''],
      specialty: ['PSICOLOGIA', [Validators.required]],
      roles: [[ROLES.PROFESIONAL] as string[], [rolesValidator]],
      enabled: [true],
    });

    this.passwordForm = this.fb.group({
      newPassword: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,100}$/)
        ]
      ],
      confirmPassword: ['', [Validators.required]]
    });
  }

  loadUsers(): void {
    this.loading = true;
    this.userService
      .getAllUsers(
        this.searchQuery,
        this.selectedSpecialty,
        this.selectedEnabled !== null ? this.selectedEnabled : undefined,
        this.currentPage,
        this.pageSize
      )
      .subscribe({
        next: (page) => {
          this.users = page.content;
          this.currentPage = page.page.number;
          this.totalPages = page.page.totalPages;
          this.totalElements = page.page.totalElements;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.toast.show('Error al cargar la lista de usuarios', 'error');
        }
      });
  }

  onSearch(query: string): void {
    this.searchQuery = query;
    this.currentPage = 0;
    this.loadUsers();
  }

  onFilterSpecialty(specialty: string): void {
    this.selectedSpecialty = specialty;
    this.currentPage = 0;
    this.loadUsers();
  }

  onFilterStatus(status: string): void {
    if (status === 'active') this.selectedEnabled = true;
    else if (status === 'inactive') this.selectedEnabled = false;
    else this.selectedEnabled = null;
    this.currentPage = 0;
    this.loadUsers();
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadUsers();
  }

  openCreateModal(): void {
    this.selectedUser = null;
    const defaultSpecialty = this.specialties[0]?.code || 'PSICOLOGIA';
    this.userForm.reset({
      username: '',
      password: '',
      firstName: '',
      lastName: '',
      email: '',
      phone: '',
      specialty: defaultSpecialty,
      roles: [ROLES.PROFESIONAL],
      enabled: true,
    });
    this.userForm.get('username')?.enable();
    this.userForm.get('password')?.setValidators([
      Validators.required,
      Validators.minLength(8),
      Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,100}$/)
    ]);
    this.userForm.get('password')?.updateValueAndValidity();
    this.showUserModal = true;
  }

  openEditModal(user: UserProfile): void {
    this.selectedUser = user;
    this.userForm.reset({
      username: user.username,
      password: '',
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      phone: user.phone,
      specialty: user.specialty || 'PSICOLOGIA',
      // Se conservan todos los roles (incluido ROLE_SITE_ADMIN, que no tiene casilla): el
      // backend reemplaza el conjunto completo al guardar.
      roles: [...(user.roles ?? [])],
      enabled: user.enabled !== false,
    });
    this.userForm.get('username')?.disable();
    this.userForm.get('password')?.clearValidators();
    this.userForm.get('password')?.updateValueAndValidity();
    this.showUserModal = true;
  }

  closeUserModal(): void {
    this.showUserModal = false;
    this.selectedUser = null;
  }

  saveUser(): void {
    if (this.userForm.invalid) {
      this.userForm.markAllAsTouched();
      return;
    }

    this.saving = true;
    const formVal = this.userForm.getRawValue();

    if (this.selectedUser) {
      // Edit
      const request: AdminUpdateUserRequest = {
        firstName: formVal.firstName,
        lastName: formVal.lastName,
        email: formVal.email || undefined,
        phone: formVal.phone || undefined,
        specialty: formVal.specialty,
        roles: formVal.roles,
        enabled: formVal.enabled,
      };

      this.userService.adminUpdateUser(this.selectedUser.id, request).subscribe({
        next: () => {
          this.toast.show('Usuario actualizado con éxito', 'success');
          this.saving = false;
          this.closeUserModal();
          this.loadUsers();
        },
        error: (err) => {
          this.saving = false;
          const msg = err?.error?.message || 'Error al actualizar usuario';
          this.toast.show(msg, 'error');
        }
      });
    } else {
      // Create
      const request: CreateUserRequest = {
        username: formVal.username,
        password: formVal.password,
        firstName: formVal.firstName,
        lastName: formVal.lastName,
        email: formVal.email || undefined,
        phone: formVal.phone || undefined,
        specialty: formVal.specialty,
        roles: formVal.roles,
      };

      this.userService.createUser(request).subscribe({
        next: () => {
          this.toast.show('Usuario creado con éxito', 'success');
          this.saving = false;
          this.closeUserModal();
          this.loadUsers();
        },
        error: (err) => {
          this.saving = false;
          const msg = err?.error?.message || 'Error al registrar usuario';
          this.toast.show(msg, 'error');
        }
      });
    }
  }

  async toggleStatus(user: UserProfile): Promise<void> {
    const targetStatus = !(user.enabled !== false);
    const actionText = targetStatus ? 'activar' : 'desactivar';
    const confirmed = await this.notification.confirm(
      `${targetStatus ? 'Activar' : 'Desactivar'} Usuario`,
      `¿Está seguro de ${actionText} la cuenta de "${user.firstName || user.username}"?`,
      `Sí, ${actionText}`,
      'Cancelar'
    );

    if (confirmed) {
      this.userService.toggleUserStatus(user.id, targetStatus).subscribe({
        next: () => {
          this.toast.show(`Usuario ${targetStatus ? 'activado' : 'desactivado'} exitosamente`, 'success');
          this.loadUsers();
        },
        error: () => this.toast.show('No se pudo cambiar el estado del usuario', 'error')
      });
    }
  }

  openPasswordModal(user: UserProfile): void {
    this.selectedUser = user;
    this.passwordForm.reset({
      newPassword: '',
      confirmPassword: '',
    });
    this.showPasswordModal = true;
  }

  closePasswordModal(): void {
    this.showPasswordModal = false;
    this.selectedUser = null;
  }

  savePassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    const { newPassword, confirmPassword } = this.passwordForm.value;
    if (newPassword !== confirmPassword) {
      this.toast.show('Las contraseñas ingresadas no coinciden', 'error');
      return;
    }

    if (!this.selectedUser) return;

    this.saving = true;
    this.userService.adminResetPassword(this.selectedUser.id, { newPassword }).subscribe({
      next: () => {
        this.toast.show('Contraseña actualizada correctamente', 'success');
        this.saving = false;
        this.closePasswordModal();
      },
      error: (err) => {
        this.saving = false;
        const msg = err?.error?.message || 'Error al cambiar la contraseña';
        this.toast.show(msg, 'error');
      }
    });
  }

  readonly roleOptions = ASSIGNABLE_ROLES;

  get selectedRoles(): string[] {
    return (this.userForm.get('roles')?.value as string[]) ?? [];
  }

  get rolesError(): string | null {
    return (this.userForm.get('roles')?.errors?.['roles'] as string | undefined) ?? null;
  }

  hasSelectedRole(code: string): boolean {
    return this.selectedRoles.includes(code);
  }

  toggleRole(code: string, checked: boolean): void {
    const roles = this.selectedRoles.filter(role => role !== code);
    const control = this.userForm.get('roles');
    control?.setValue(checked ? [...roles, code] : roles);
    control?.markAsTouched();
  }

  getRoleBadges(roles?: string[]): RoleOption[] {
    return describeRoles(roles);
  }

  getSpecialtyLabel(code?: string): string {
    if (!code) return 'General';
    const found = this.specialties.find(s => s.code.toUpperCase() === code.toUpperCase());
    if (found) return found.name;
    if (code === 'PSICOLOGIA') return 'Psicología';
    if (code === 'DERMATOLOGIA') return 'Dermatología';
    return code.charAt(0) + code.slice(1).toLowerCase();
  }

  getSpecialtyBadgeClass(code?: string): string {
    switch (code?.toUpperCase()) {
      case 'DERMATOLOGIA':
        return 'bg-cyan-50 text-cyan-700 border-cyan-200';
      case 'PSICOLOGIA':
        return 'bg-purple-50 text-purple-700 border-purple-200';
      default:
        return 'bg-indigo-50 text-indigo-700 border-indigo-200';
    }
  }
}
