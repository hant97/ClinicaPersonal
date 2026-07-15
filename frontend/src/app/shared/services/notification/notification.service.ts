import { Injectable } from '@angular/core';
import Swal, { SweetAlertIcon } from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  // Colores extraídos del styles.css para mantener coherencia visual
  private readonly primaryColor = '#5ea99f';
  private readonly dangerColor = '#e57373';

  constructor() { }

  /**
   * Muestra un modal de confirmación con dos botones.
   * @param title Título del modal
   * @param text Texto o pregunta del modal
   * @param confirmButtonText Texto del botón de confirmación
   * @param cancelButtonText Texto del botón de cancelación
   * @returns Promesa que se resuelve a true si el usuario confirmó, o false en caso contrario.
   */
  async confirm(
    title: string,
    text: string,
    confirmButtonText: string = 'Aceptar',
    cancelButtonText: string = 'Cancelar'
  ): Promise<boolean> {
    const result = await Swal.fire({
      title,
      text,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: this.primaryColor,
      cancelButtonColor: this.dangerColor,
      confirmButtonText,
      cancelButtonText,
      focusCancel: true
    });

    return result.isConfirmed;
  }

  /**
   * Muestra un modal de alerta simple bloqueante.
   * @param title Título del modal
   * @param text Texto del modal
   * @param icon Tipo de icono (success, error, warning, info, question)
   */
  async alert(title: string, text: string, icon: SweetAlertIcon = 'info'): Promise<void> {
    await Swal.fire({
      title,
      text,
      icon,
      confirmButtonColor: this.primaryColor,
      confirmButtonText: 'Aceptar'
    });
  }
}
