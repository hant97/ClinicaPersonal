import { HttpClient } from '@angular/common/http';
import { Directive, ElementRef, Input, OnChanges, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { environment } from '../../../environments/environment';

const API_PATH_PREFIX = '/api/';

/**
 * Carga una imagen protegida de la API (por ejemplo, la foto de un paciente) con el token de
 * sesión y la muestra como URL `blob:`. Un `<img src>` normal no envía la cabecera
 * `Authorization`, así que no puede mostrar recursos autenticados.
 *
 * Solo las rutas que empiezan por `/api/` pasan por `HttpClient`: el interceptor añade el
 * token a toda petición, y no debe enviarse a dominios externos.
 */
@Directive({
  selector: 'img[appAuthSrc]',
  standalone: true
})
export class AuthImageSrcDirective implements OnChanges, OnDestroy {
  @Input('appAuthSrc') source: string | null | undefined;

  private objectUrl: string | null = null;
  private subscription: Subscription | null = null;

  constructor(
    private readonly http: HttpClient,
    private readonly elementRef: ElementRef<HTMLImageElement>
  ) {}

  ngOnChanges(): void {
    this.reset();
    const image = this.elementRef.nativeElement;
    if (!this.source) {
      image.removeAttribute('src');
      return;
    }
    if (!this.source.startsWith(API_PATH_PREFIX)) {
      image.src = this.source;
      return;
    }

    const url = `${environment.apiUrl}/${this.source.substring(API_PATH_PREFIX.length)}`;
    this.subscription = this.http.get(url, { responseType: 'blob' }).subscribe({
      next: blob => {
        this.objectUrl = URL.createObjectURL(blob);
        image.src = this.objectUrl;
      },
      // Sin imagen (borrada, sin permiso o sin conexión): se oculta en lugar de mostrar un icono roto.
      error: () => image.style.setProperty('visibility', 'hidden')
    });
  }

  ngOnDestroy(): void {
    this.reset();
  }

  private reset(): void {
    this.subscription?.unsubscribe();
    this.subscription = null;
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }
    this.elementRef.nativeElement.style.removeProperty('visibility');
  }
}
