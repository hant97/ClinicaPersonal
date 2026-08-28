import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

import { LucideAngularModule } from 'lucide-angular';
import { Eye, EyeOff, HeartHandshake,
 ShieldCheck } from '../../../shared/icons/lucide-icons';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, LucideAngularModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent implements OnInit {
  static readonly REMEMBERED_USERNAME_KEY = 'clinica_remembered_username';

  loginForm: FormGroup;
  errorMessage: string = '';
  isLoading: boolean = false;
  showPassword: boolean = false;

  readonly HeartHandshake = HeartHandshake;
  readonly ShieldCheck = ShieldCheck;
  readonly Eye = Eye;
  readonly EyeOff = EyeOff;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      rememberUsername: [false]
    });
  }

  ngOnInit(): void {
    const savedUsername = localStorage.getItem(LoginComponent.REMEMBERED_USERNAME_KEY);
    if (savedUsername) {
      this.loginForm.patchValue({
        username: savedUsername,
        rememberUsername: true
      });
    }
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  private resolveReturnUrl(): string {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    if (returnUrl && returnUrl.startsWith('/') && !returnUrl.startsWith('//') && returnUrl !== '/login') {
      return returnUrl;
    }
    return this.authService.hasRole('ROLE_SITE_ADMIN') ? '/editar-sitio' : '/dashboard';
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      const { username, password, rememberUsername } = this.loginForm.value;
      const normalizedUsername = (username || '').trim();
      
      this.authService.login({ username: normalizedUsername, password }).subscribe({
        next: () => {
          this.isLoading = false;
          if (rememberUsername) {
            localStorage.setItem(LoginComponent.REMEMBERED_USERNAME_KEY, normalizedUsername);
          } else {
            localStorage.removeItem(LoginComponent.REMEMBERED_USERNAME_KEY);
          }
          this.router.navigateByUrl(this.resolveReturnUrl());
        },
        error: (err) => {
          this.isLoading = false;
          if (err.error && err.error.message) {
            this.errorMessage = err.error.message;
          } else if (err.status === 401) {
            this.errorMessage = 'Usuario o contraseña incorrectos.';
          } else {
            this.errorMessage = 'No se pudo conectar con el servidor. Intente nuevamente.';
          }
        }
      });
    }
  }
}

