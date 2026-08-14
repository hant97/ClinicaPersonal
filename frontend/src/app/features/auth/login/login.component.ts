import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { CommonModule } from '@angular/common';
import { HeartHandshake, LucideAngularModule, ShieldCheck } from 'lucide-angular';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LucideAngularModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  loginForm: FormGroup;
  errorMessage: string = '';
  isLoading: boolean = false;

  readonly HeartHandshake = HeartHandshake;
  readonly ShieldCheck = ShieldCheck;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
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
      const { username, password } = this.loginForm.value;
      
      this.authService.login({ username: username.trim(), password }).subscribe({
        next: () => {
          this.isLoading = false;
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
