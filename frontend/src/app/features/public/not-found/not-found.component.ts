import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="flex min-h-screen flex-col items-center justify-center gap-4 bg-canvas px-6 text-center">
      <p class="font-display text-6xl font-semibold text-clinic-600">404</p>
      <h1 class="text-2xl font-semibold">Página no encontrada</h1>
      <p class="max-w-md text-muted">La página que buscas no existe o fue movida.</p>
      <a class="btn-primary" routerLink="/dashboard">Volver al inicio</a>
    </div>
  `
})
export class NotFoundComponent {}
